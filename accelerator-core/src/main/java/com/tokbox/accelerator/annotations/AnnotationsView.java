package com.tokbox.accelerator.annotations;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.Build;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.tokbox.accelerator.annotations.config.OpenTokConfig;
import com.tokbox.accelerator.annotations.utils.AnnotationsVideoRenderer;
import com.tokbox.android.logging.OTKAnalytics;
import com.tokbox.android.logging.OTKAnalyticsData;
import com.tokbox.otsdkwrapper.R;
import com.tokbox.otsdkwrapper.listeners.SignalListener;
import com.tokbox.otsdkwrapper.signal.SignalInfo;
import com.tokbox.otsdkwrapper.wrapper.OTAcceleratorSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Defines the view to draw annotations
 */
public class AnnotationsView extends ViewGroup implements AnnotationsToolbar.ActionsListener, SignalListener {
    private static final String LOG_TAG = AnnotationsView.class.getSimpleName();
    private static final String SIGNAL_TYPE = "otAnnotation";
    private static final String SIGNAL_PLATFORM = "android";

    private AnnotationsPath mCurrentPath = new AnnotationsPath();
    private AnnotationsText mCurrentText = new AnnotationsText();
    private Paint mCurrentPaint;

    private AnnotationsVideoRenderer videoRenderer;

    private int mCurrentColor = 0;
    private int mSelectedColor = 0;
    private float mLineWidth = 10.0f;
    private float mIncomingLineWidth = 0.0f;
    private int mTextSize = 48;
    private AnnotationsManager mAnnotationsManager;

    private static final float TOLERANCE = 5;

    private int width;
    private int height;

    private Mode mode;

    private AnnotationsToolbar mToolbar;

    private boolean mAnnotationsActive = false;
    private boolean loaded = false;

    private AnnotationsListener mListener;
    private Annotatable mCurrentAnnotatable;

    private boolean defaultLayout = false;
    private String mRemoteConnId;

    private String mPartnerId;

    private OTKAnalyticsData mAnalyticsData;
    private OTKAnalytics mAnalytics;

    private boolean isScreensharing = false;

    private boolean mSignalMirrored = false;
    private boolean isStartPoint = false;
    private boolean mMirrored = false;

    private String canvasId;
    private boolean clear = false;

    private Context mContext;

    private ViewGroup mContentView;

    private OTAcceleratorSession mSession;


    /**
     * Monitors state changes in the Annotations component.
     **/
    public interface AnnotationsListener {

        /**
         * Invoked when a new screencapture is ready
         *
         * @param bmp Bitmap of the screencapture.
         */
        void onScreencaptureReady(Bitmap bmp);

        /**
         * Invoked when an annotations item in the toolbar is selected
         */
        void onAnnotationsSelected(Mode mode);

        /**
         * Invoked when the DONE button annotations item in the toolbar is selected
         */
        void onAnnotationsDone();

        /**
         * Invoked when an error happens
         *
         * @param error The error message.
         */
        void onError(String error);
    }

    /**
     * Annotations actions
     */
    public enum Mode {
        Pen("otAnnotation_pen"),
        Undo("otAnnotation_undo"),
        Clear("otAnnotation_clear"),
        Text("otAnnotation_text"),
        Color("otAnnotation_color"),
        Capture("otAnnotation_capture");

        private String type;

        Mode(String type) {
            this.type = type;
        }

        public String toString() {
            return this.type;
        }
    }

    /**
     * Constructor
     *
     * @param context Application context
     * @param attrs   A collection of attributes
     */
    public AnnotationsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Creates a new AnnotationView instance for local screensharing. isScreenSharing should be true
     *
     * @param context
     * @param session
     * @param partnerId
     * @param isScreenSharing
     * @throws Exception
     */
    public AnnotationsView(Context context, @NonNull OTAcceleratorSession session, String partnerId,
                           boolean isScreenSharing) throws IllegalArgumentException {
        super(context);

        if (session.getConnection() == null) {
            throw new IllegalArgumentException("Session is not connected");
        }

        this.mContext = context;
        this.mSession = session;
        this.mPartnerId = partnerId;
        //add a listener for each type of signal to avoid breaking the interoperability
        this.mSession.addSignalListener(Mode.Pen.toString(), this);
        this.mSession.addSignalListener(Mode.Text.toString(), this);
        this.mSession.addSignalListener(Mode.Undo.toString(), this);
        this.mSession.addSignalListener(Mode.Clear.toString(), this);

        this.isScreensharing = isScreenSharing;

        init();
    }

    /**
     * Creates a new AnnotationsView instance for remote screensharing
     *
     * @param context
     * @param session
     * @param partnerId
     * @param remoteConnId
     * @throws Exception
     */
    public AnnotationsView(Context context, @NonNull OTAcceleratorSession session, String partnerId,
                           String remoteConnId) throws IllegalArgumentException {
        super(context);

        if (session.getConnection() == null) {
            throw new IllegalArgumentException("Session is not connected");
        }

        this.mContext = context;
        this.mSession = session;
        this.mPartnerId = partnerId;
        //add a listener for each type of signal to avoid breaking the interoperability
        this.mSession.addSignalListener(Mode.Pen.toString(), this);
        this.mSession.addSignalListener(Mode.Text.toString(), this);
        this.mSession.addSignalListener(Mode.Undo.toString(), this);
        this.mSession.addSignalListener(Mode.Clear.toString(), this);

        this.isScreensharing = false;
        this.mRemoteConnId = remoteConnId;
        init();
    }

    /**
     * Attaches the Annotations Toolbar to the view
     *
     * @param toolbar AnnotationsToolbar
     */
    public void attachToolbar(@NonNull AnnotationsToolbar toolbar) throws Exception {
        addLogEvent(OpenTokConfig.LOG_ACTION_USE_TOOLBAR, OpenTokConfig.LOG_VARIATION_ATTEMPT);
        mToolbar = toolbar;
        mToolbar.setActionListener(this);

        addLogEvent(OpenTokConfig.LOG_ACTION_USE_TOOLBAR, OpenTokConfig.LOG_VARIATION_SUCCESS);
    }

    /**
     * Returns the AnnotationsToolbar
     */
    public AnnotationsToolbar getToolbar() {
        return mToolbar;
    }

    /**
     * Sets an AnnotationsVideoRenderer
     *
     * @param videoRenderer AnnotationsVideoRenderer
     */
    public void setVideoRenderer(@NonNull AnnotationsVideoRenderer videoRenderer) {
        this.videoRenderer = videoRenderer;
    }

    /**
     * Returns the AnnotationsVideoRenderer
     */
    public AnnotationsVideoRenderer getVideoRenderer() {
        return videoRenderer;
    }

    /**
     * Sets AnnotationsListener
     *
     * @param listener AnnotationsListener
     */
    public void setAnnotationsListener(AnnotationsListener listener) {
        this.mListener = listener;
    }

    /**
     * Restarts the AnnotationsView. Clear all the annotations.
     */
    public void restart() {
        clearAll(false, mSession.getConnection().getConnectionId());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        resize();
    }

    private void init() {
        addLogEvent(OpenTokConfig.LOG_ACTION_INITIALIZE, OpenTokConfig.LOG_VARIATION_ATTEMPT);
        String source = getContext().getPackageName();

        SharedPreferences prefs = getContext().getSharedPreferences("opentok", Context.MODE_PRIVATE);
        String guidVSol = prefs.getString("guidVSol", null);
        if (null == guidVSol) {
            guidVSol = UUID.randomUUID().toString();
            prefs.edit().putString("guidVSol", guidVSol).commit();
        }

        //init analytics
        mAnalyticsData = new OTKAnalyticsData.Builder(OpenTokConfig.LOG_CLIENT_VERSION, source, OpenTokConfig.LOG_COMPONENTID, guidVSol).build();
        mAnalytics = new OTKAnalytics(mAnalyticsData);
        if (mSession != null) {
            mAnalyticsData.setSessionId(mSession.getSessionId());
            mAnalyticsData.setConnectionId(mSession.getConnection().getConnectionId());
        }
        if (mPartnerId != null) {
            mAnalyticsData.setPartnerId(mPartnerId);
        }
        mAnalytics.setData(mAnalyticsData);

        setWillNotDraw(false);
        mAnnotationsManager = new AnnotationsManager();
        mCurrentColor = getResources().getColor(R.color.picker_color_orange);
        mSelectedColor = mCurrentColor;
        this.setVisibility(View.GONE);
        addLogEvent(OpenTokConfig.LOG_ACTION_INITIALIZE, OpenTokConfig.LOG_VARIATION_SUCCESS);
    }

    private void resize() {
        if (this.getLayoutParams() == null || this.getLayoutParams().width <= 0 || this.getLayoutParams().height <= 0 || defaultLayout) {
            //default case
            defaultLayout = true;
            getScreenRealSize();

            LayoutParams params = this.getLayoutParams();

            params.height = this.height - mToolbar.getHeight() - (this.height - getDisplayHeight());

            if (getActionBarHeight() != 0) {
                params.height = params.height - getActionBarHeight();
            }
            params.width = this.width;
            this.setLayoutParams(params);
        } else {

            defaultLayout = false;
        }
    }

    private void getScreenRealSize() {
        Display display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17) {
            //new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            //reflection for this weird in-between time
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                //this may not be 100% accurate, but it's all we've got
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
            }

        } else {
            //This should be close, as lower API devices should not have window navigation bars
            realWidth = display.getWidth();
            realHeight = display.getHeight();
        }

        this.width = realWidth;
        this.height = realHeight;
    }

    private int getDisplayHeight() {
        final WindowManager windowManager = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);

        final Point size = new Point();
        int screenHeight = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(size);
            screenHeight = size.y;
        } else {
            Display d = windowManager.getDefaultDisplay();
            screenHeight = d.getHeight();
        }
        return screenHeight;
    }

    private int getDisplayWidth() {
        final WindowManager windowManager = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);

        final Point size = new Point();
        int screenWidth = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(size);
            screenWidth = size.x;
        } else {
            Display d = windowManager.getDefaultDisplay();
            screenWidth = d.getWidth();
        }
        return screenWidth;
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private int getActionBarHeight() {
        int actionBarHeight = 0;
        Rect rect = new Rect();
        Window window = ((Activity) mContext).getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        int contentViewTop =
                window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight = contentViewTop - statusBarHeight;
        actionBarHeight = titleBarHeight;

        return actionBarHeight;
    }

    private int getDisplayContentHeight() {
        final WindowManager windowManager = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);

        final Point size = new Point();
        int screenHeight = 0;
        int actionBarHeight = getActionBarHeight();
        int contentTop = getStatusBarHeight();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(size);
            screenHeight = size.y;
        } else {
            Display d = windowManager.getDefaultDisplay();
            screenHeight = d.getHeight();
        }
        return (screenHeight - contentTop - actionBarHeight);
    }

    private void drawText() {
        invalidate();
    }

    private void beginTouch(float x, float y) {
        mCurrentPath.moveTo(x, y);
    }

    private void moveTouch(float x, float y, boolean curved) {
        if (mCurrentPath != null && mCurrentPath.getPoints() != null && mCurrentPath.getPoints().size() > 0) {
            float mX = mCurrentPath.getEndPoint().x;
            float mY = mCurrentPath.getEndPoint().y;

            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOLERANCE || dy >= TOLERANCE) {
                if (curved) {
                    mCurrentPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                } else {
                    mCurrentPath.lineTo(x, y);
                }
            }
        }
    }

    private void upTouch() {
        upTouch(false);
    }

    private void upTouch(boolean curved) {
        float mLastX = mCurrentPath.getEndPoint().x;
        float mLastY = mCurrentPath.getEndPoint().y;
        int index = mCurrentPath.getPoints().size() - 1; //-2
        float mX = mCurrentPath.getPoints().get(index).x;
        float mY = mCurrentPath.getPoints().get(index).y;
        if (curved) {
            mCurrentPath.quadTo(mLastX, mLastY, (mX + mLastX) / 2, (mY + mLastY) / 2);
        } else {
            mCurrentPath.lineTo(mX, mY);
        }
    }

    private void clearAll(boolean incoming, String cid) {
        JSONArray jsonArray = new JSONArray();
        if (mAnnotationsManager.getAnnotatableList().size() > 0) {
            int i = mAnnotationsManager.getAnnotatableList().size() - 1;
            while (i >= 0) {
                Annotatable annotatable = mAnnotationsManager.getAnnotatableList().get(i);
                if (annotatable.getCId().equals(cid)) {
                    mAnnotationsManager.getAnnotatableList().remove(i);
                    jsonArray.put(annotatable.getCId());
                    i--;
                }
                invalidate();
            }
            if (!incoming && !isScreensharing) {
                sendAnnotation(mode.toString(), null);
            }
        }
    }


    private void undoAnnotation(boolean incoming, String cid) {
        boolean removed = false;
        if (mAnnotationsManager.getAnnotatableList().size() > 0) {
            int i = mAnnotationsManager.getAnnotatableList().size() - 1;

            while (!removed && i >= 0) {
                Annotatable annotatable = mAnnotationsManager.getAnnotatableList().get(i);

                if (annotatable.getCId().equals(cid)) {
                    mAnnotationsManager.getAnnotatableList().remove(i);
                    removed = true;
                }
                i--;
            }
            invalidate();

            if (!incoming && !isScreensharing) {
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(cid);
                sendAnnotation(mode.toString(), jsonArray.toString());
            }
        }
    }

    private void createTextAnnotatable(EditText editText, float x, float y) {
        Log.i(LOG_TAG, "Create Text Annotatable");
        mCurrentPaint = new Paint();
        mCurrentPaint.setAntiAlias(true);
        mCurrentPaint.setColor(mCurrentColor);
        mCurrentPaint.setTextSize(mTextSize);
        mCurrentText = new AnnotationsText(editText, x, y);
    }

    private void createPathAnnotatable(boolean incoming) {
        Log.i(LOG_TAG, "Create Path Annotatable");
        mCurrentPaint = new Paint();
        mCurrentPaint.setAntiAlias(true);
        mCurrentPaint.setColor(mCurrentColor);
        mCurrentPaint.setStyle(Paint.Style.STROKE);
        mCurrentPaint.setStrokeJoin(Paint.Join.ROUND);
        if (incoming) {
            mCurrentPaint.setStrokeWidth(mIncomingLineWidth);
        } else {
            mCurrentPaint.setStrokeWidth(mLineWidth);
        }
        if (mode != null && mode == Mode.Pen) {
            mCurrentPath = new AnnotationsPath();
        }
    }

    private void addAnnotatable(String cid) {
        Log.i(LOG_TAG, "Add Annotatable");
        if (mode != null) {
            if (mode.equals(Mode.Pen)) {
                mCurrentAnnotatable = new Annotatable(mode, mCurrentPath, mCurrentPaint, cid);
                mCurrentAnnotatable.setType(Annotatable.AnnotatableType.PATH);
            } else {
                mCurrentAnnotatable = new Annotatable(mode, mCurrentText, mCurrentPaint, cid);
                mCurrentAnnotatable.setType(Annotatable.AnnotatableType.TEXT);
            }
            mAnnotationsManager.addAnnotatable(mCurrentAnnotatable);
        }
    }

    private Bitmap getScreenshot() {
        View v1 = mContentView;
        v1.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);

        return bitmap;
    }

    private void sendAnnotation(String type, String annotation) {
        if (mSession != null && !isScreensharing) {
            mSession.sendSignal(new SignalInfo(mSession.getConnection().getConnectionId(), null, type, annotation), null);
        }
    }

    private String buildSignalFromText(float x, float y, String text, boolean start, boolean end) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();

        boolean mirrored = false;

        int videoWidth = 0;
        int videoHeight = 0;

        if (videoRenderer != null) {
            mirrored = videoRenderer.isMirrored();
            videoWidth = videoRenderer.getVideoWidth();
            videoHeight = videoRenderer.getVideoHeight();
        }

        try {
            if ( mRemoteConnId != null ){
                jsonObject.put("id", mRemoteConnId);
            }
            else {
                jsonObject.put("id", mSession.getConnection().getConnectionId());
            }
            jsonObject.put("fromId", mSession.getConnection().getConnectionId());
            jsonObject.put("fromX", x);
            jsonObject.put("fromY", y);
            jsonObject.put("color", String.format("#%06X", (0xFFFFFF & mCurrentColor)));
            jsonObject.put("videoWidth", videoWidth);
            jsonObject.put("videoHeight", videoHeight);
            jsonObject.put("canvasWidth", this.width);
            jsonObject.put("canvasHeight", getDisplayHeight() - getActionBarHeight());
            jsonObject.put("mirrored", mirrored);
            jsonObject.put("text", text);
            jsonObject.put("font", "16px Arial"); //TODO: Fix font type
            jsonObject.put("platform", SIGNAL_PLATFORM);
            jsonObject.put("mode", mode);
            jsonArray.put(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray.toString();
    }


    private String buildSignalFromPoint(float x, float y, boolean startPoint, boolean endPoint) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        boolean mirrored = false;

        int videoWidth = 0;
        int videoHeight = 0;

        if (videoRenderer != null) {
            mirrored = videoRenderer.isMirrored();
            videoWidth = videoRenderer.getVideoWidth();
            videoHeight = videoRenderer.getVideoHeight();
        }
        try {
            if ( mRemoteConnId != null ){
                jsonObject.put("id", mRemoteConnId);
            }
            else {
                jsonObject.put("id", mSession.getConnection().getConnectionId());
            }
            jsonObject.put("fromId", mSession.getConnection().getConnectionId());
            jsonObject.put("fromX", mCurrentPath.getEndPoint().x);
            jsonObject.put("fromY", mCurrentPath.getEndPoint().y);
            jsonObject.put("toX", x);
            jsonObject.put("toY", y);
            jsonObject.put("color", String.format("#%06X", (0xFFFFFF & mCurrentColor)));
            jsonObject.put("lineWidth", 2);
            jsonObject.put("videoWidth", videoWidth);
            jsonObject.put("videoHeight", videoHeight);
            jsonObject.put("canvasWidth", this.width);
            jsonObject.put("canvasHeight", getDisplayHeight() - getActionBarHeight());
            jsonObject.put("mirrored", mirrored);
            jsonObject.put("smoothed", false);
            jsonObject.put("startPoint", startPoint);
            jsonObject.put("endPoint", endPoint);
            jsonObject.put("platform", SIGNAL_PLATFORM);
            jsonObject.put("mode", mode);
            jsonArray.put(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonArray.toString();
    }

    private void penAnnotations(String connectionId, String data) {
        mode = Mode.Pen;
        // Build object from JSON array
        try {
            JSONArray updates = new JSONArray(data);

            for (int i = 0; i < updates.length(); i++) {
                JSONObject json = updates.getJSONObject(i);

                String platform = null;
                if (!json.isNull("platform")) {
                    platform = (String) json.get("platform");
                }

                String id = (String) json.get("id");
                if (json.get("mirrored") instanceof Number) {
                    Number value = (Number) json.get("mirrored");
                    mSignalMirrored = value.intValue() == 1;
                } else {
                    mSignalMirrored = (boolean) json.get("mirrored");
                }

                boolean initialPoint = false;
                boolean secondPoint = false;
                boolean endPoint = false;

                if (!json.isNull("endPoint")) {
                    if (json.get("endPoint") instanceof Number) {
                        Number value = (Number) json.get("endPoint");
                        endPoint = value.intValue() == 1;
                    } else {
                        endPoint = (boolean) json.get("endPoint");
                    }
                }
                if (!json.isNull("startPoint")) {
                    if (json.get("startPoint") instanceof Number) {
                        Number value = (Number) json.get("startPoint");
                        initialPoint = value.intValue() == 1;
                    } else {
                        initialPoint = (boolean) json.get("startPoint");
                    }

                    if (initialPoint) {

                        isStartPoint = true;
                    } else {
                        // If the start point flag was already set, we received the next point in the sequence
                        if (isStartPoint) {
                            secondPoint = true;
                            isStartPoint = false;
                        }
                    }
                }
                if (!json.isNull("color")) {
                    mCurrentColor = Color.parseColor(((String) json.get("color")).toLowerCase());
                }
                if (!json.isNull("lineWidth")) {
                    mIncomingLineWidth = ((Number) json.get("lineWidth")).floatValue();
                }

                float scale = 1;
                float localWidth = 0;
                float localHeight = 0;
                if (videoRenderer != null) {
                    localWidth = (float) videoRenderer.getVideoWidth();
                    localHeight = (float) videoRenderer.getVideoHeight();
                }

                if (localWidth == 0) {
                    localWidth = getDisplayWidth();
                }
                if (localHeight == 0) {
                    localHeight = getDisplayHeight();
                }

                Map<String, Float> canvas = new HashMap<>();
                canvas.put("width", localWidth);
                canvas.put("height", localHeight);

                Map<String, Float> iCanvas = new HashMap<>();
                iCanvas.put("width", ((Number) json.get("canvasWidth")).floatValue());
                iCanvas.put("height", ((Number) json.get("canvasHeight")).floatValue());

                float canvasRatio = canvas.get("width") / canvas.get("height");

                if (canvasRatio < 0) {
                    scale = canvas.get("width") / iCanvas.get("width");
                } else {
                    scale = canvas.get("height") / iCanvas.get("height");
                }

                float centerX = canvas.get("width") / 2f;
                float centerY = canvas.get("height") / 2f;

                float iCenterX = iCanvas.get("width") / 2f;
                float iCenterY = iCanvas.get("height") / 2f;

                float fromX = ((Number) json.get("fromX")).floatValue();
                float fromY = ((Number) json.get("fromY")).floatValue();
                float toY = ((Number) json.get("toY")).floatValue();
                float toX = ((Number) json.get("toX")).floatValue();

                if (platform.equals(SIGNAL_PLATFORM)) {
                    fromX = centerX - (scale * (iCenterX - ((Number) json.get("fromX")).floatValue()));
                    toX = centerX - (scale * (iCenterX - ((Number) json.get("toX")).floatValue()));

                    fromY = centerY - (scale * (iCenterY - ((Number) json.get("fromY")).floatValue()));
                    toY = centerY - (scale * (iCenterY - ((Number) json.get("toY")).floatValue()));
                }
                fromY = fromY - getActionBarHeight();
                toY = toY - getActionBarHeight();

                if (mSignalMirrored) {
                    Log.i(LOG_TAG, "Signal is mirrored");
                    fromX = this.width - fromX;
                    toX = this.width - toX;
                }
                mMirrored = videoRenderer.isMirrored();
                if (mMirrored) {
                    Log.i(LOG_TAG, "Feed is mirrored");
                    // Revert (Double negative)
                    fromX = this.width - fromX;
                    toX = this.width - toX;
                }

                boolean smoothed = false;

                if (!json.isNull("smoothed")) {
                    if (json.get("smoothed") instanceof Number) {
                        Number value = (Number) json.get("smoothed");
                        smoothed = value.intValue() == 1;
                    } else {
                        smoothed = (boolean) json.get("smoothed");
                    }
                }
                if (smoothed) {
                    if (isStartPoint) {
                        mAnnotationsActive = true;
                        createPathAnnotatable(false);
                        mCurrentPath.addPoint(new PointF(toX, toY));
                    } else if (secondPoint) {
                        beginTouch((toX + mCurrentPath.getEndPoint().x) / 2, (toY + mCurrentPath.getEndPoint().y) / 2);
                        mCurrentPath.addPoint(new PointF(toX, toY));
                    } else {
                        moveTouch(toX, toY, true);
                        mCurrentPath.addPoint(new PointF(toX, toY));

                        if (endPoint) {
                            try {
                                addAnnotatable(connectionId);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, e.toString());
                            }
                            mAnnotationsActive = false;
                        }
                    }
                } else {
                    if (isStartPoint && endPoint) {
                        mAnnotationsActive = true;
                        createPathAnnotatable(false);
                        mCurrentPath.addPoint(new PointF(fromX, fromY));
                        // We have a straight line
                        beginTouch(fromX, fromY);
                        moveTouch(toX, toY, false);
                        upTouch();
                        try {
                            addAnnotatable(connectionId);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.toString());
                        }
                    } else if (isStartPoint) {
                        mAnnotationsActive = true;
                        createPathAnnotatable(false);
                        mCurrentPath.addPoint(new PointF(fromX, fromY));
                        beginTouch(toX, toY);
                    } else if (endPoint) {
                        moveTouch(toX, toY, false);
                        upTouch();
                        try {
                            addAnnotatable(connectionId);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.toString());
                        }
                        mAnnotationsActive = false;
                    } else {
                        moveTouch(toX, toY, false);
                        mCurrentPath.addPoint(new PointF(toX, toY));
                    }
                }

                invalidate(); // Need this to finalize the drawing on the screen
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void textAnnotation(String connectionId, String data) {

        mode = Mode.Text;
        // Build object from JSON array
        try {
            JSONArray updates = new JSONArray(data);

            for (int i = 0; i < updates.length(); i++) {
                JSONObject json = updates.getJSONObject(i);

                String id = (String) json.get("id");
                if (json.get("mirrored") instanceof Number) {
                    Number value = (Number) json.get("mirrored");
                    mSignalMirrored = value.intValue() == 1;
                } else {
                    mSignalMirrored = (boolean) json.get("mirrored");
                }

                if (!json.isNull("color")) {
                    mCurrentColor = Color.parseColor(((String) json.get("color")).toLowerCase());
                }

                //get text
                String text = ((String) json.get("text"));

                //TODO: fix font style
                String font = ((String) json.get("font")); // "font":"16px Arial"
                //mTextSize = Integer.valueOf(font.split("px")[0]);

                boolean end = true;
                boolean start = true;


                if (!json.isNull("end")) {
                    if (json.get("end") instanceof Number) {
                        Number value = (Number) json.get("end");
                        end = value.intValue() == 1;
                    } else {
                        end = (boolean) json.get("end");
                    }
                }

                if (!json.isNull("start")) {
                    if (json.get("start") instanceof Number) {
                        Number value = (Number) json.get("start");
                        start = value.intValue() == 1;
                    } else {
                        start = (boolean) json.get("start");
                    }
                }

                float scale = 1;
                float localWidth = 0;
                float localHeight = 0;
                if (videoRenderer != null) {
                    localWidth = (float) videoRenderer.getVideoWidth();
                    localHeight = (float) videoRenderer.getVideoHeight();
                }

                if (localWidth == 0) {
                    localWidth = getDisplayWidth();
                }
                if (localHeight == 0) {
                    localHeight = getDisplayHeight();
                }

                Map<String, Float> canvas = new HashMap<>();
                canvas.put("width", localWidth);
                canvas.put("height", localHeight);

                Map<String, Float> iCanvas = new HashMap<>();
                iCanvas.put("width", ((Number) json.get("canvasWidth")).floatValue());
                iCanvas.put("height", ((Number) json.get("canvasHeight")).floatValue());
                float canvasRatio = canvas.get("width") / canvas.get("height");

                if (canvasRatio < 0) {
                    scale = canvas.get("width") / iCanvas.get("width");
                } else {
                    scale = canvas.get("height") / iCanvas.get("height");
                }

                Log.i(LOG_TAG, "Scale: " + scale);

                float centerX = canvas.get("width") / 2f;
                float centerY = canvas.get("height") / 2f;

                float iCenterX = iCanvas.get("width") / 2f;
                float iCenterY = iCanvas.get("height") / 2f;


                float textX = centerX - (scale * (iCenterX - ((Number) json.get("fromX")).floatValue()));
                float textY = centerY - (scale * (iCenterY - ((Number) json.get("fromY")).floatValue()));

                textY = textY - getActionBarHeight();

                EditText editText = new EditText(getContext());
                editText.setVisibility(VISIBLE);
                editText.setImeOptions(EditorInfo.IME_ACTION_DONE);

                // Add whatever you want as size
                int editTextHeight = 70;
                int editTextWidth = 200;

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(editTextWidth, editTextHeight);

                //You could adjust the position
                params.topMargin = (int) (textX);
                params.leftMargin = (int) (textY);
                this.addView(editText, params);
                editText.setVisibility(VISIBLE);
                editText.setSingleLine();
                editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                editText.requestFocus();
                editText.setText(text);
                editText.setTextSize(mTextSize);

                createTextAnnotatable(editText, textX, textY);
                mCurrentText.getEditText().setText(text.toString());

                mAnnotationsActive = false;
                addAnnotatable(connectionId);
                mCurrentText = null;
                invalidate(); // Need this to finalize the drawing on the screen
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //add log events
    private void addLogEvent(String action, String variation) {
        if (mAnalytics != null) {
            mAnalytics.logEvent(action, variation);
        }
    }

    /**
     * ==== Touch Events ====
     **/

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.loaded = false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        if (mode != null) {
            mCurrentColor = mSelectedColor;
            if (mode == Mode.Pen) {
                addLogEvent(OpenTokConfig.LOG_ACTION_FREEHAND, OpenTokConfig.LOG_VARIATION_ATTEMPT);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        addLogEvent(OpenTokConfig.LOG_ACTION_START_DRAWING, OpenTokConfig.LOG_VARIATION_ATTEMPT);
                        mAnnotationsActive = true;
                        createPathAnnotatable(false);
                        beginTouch(x, y);
                        mCurrentPath.addPoint(new PointF(x, y));
                        sendAnnotation(mode.toString(), buildSignalFromPoint(x, y, true, false));
                        invalidate();
                        addLogEvent(OpenTokConfig.LOG_ACTION_START_DRAWING, OpenTokConfig.LOG_VARIATION_SUCCESS);
                    }
                    break;
                    case MotionEvent.ACTION_MOVE: {
                        moveTouch(x, y, true);

                        sendAnnotation(mode.toString(), buildSignalFromPoint(x, y, false, false));
                        mCurrentPath.addPoint(new PointF(x, y));
                        invalidate();
                    }
                    break;
                    case MotionEvent.ACTION_UP: {
                        addLogEvent(OpenTokConfig.LOG_ACTION_END_DRAWING, OpenTokConfig.LOG_VARIATION_ATTEMPT);
                        upTouch();
                        sendAnnotation(mode.toString(), buildSignalFromPoint(x, y, false, true));
                        try {
                            addAnnotatable(mSession.getConnection().getConnectionId());

                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.toString());
                        }
                        mAnnotationsActive = false;
                        invalidate();
                        addLogEvent(OpenTokConfig.LOG_ACTION_END_DRAWING, OpenTokConfig.LOG_VARIATION_SUCCESS);
                    }
                    break;
                }
                addLogEvent(OpenTokConfig.LOG_ACTION_FREEHAND, OpenTokConfig.LOG_VARIATION_SUCCESS);
            } else {
                if (mode == Mode.Text) {
                    addLogEvent(OpenTokConfig.LOG_ACTION_TEXT, OpenTokConfig.LOG_VARIATION_ATTEMPT);
                    final String myString;

                    mAnnotationsActive = true;

                    EditText editText = new EditText(getContext());
                    editText.setVisibility(VISIBLE);
                    editText.setImeOptions(EditorInfo.IME_ACTION_DONE);

                    // Add whatever you want as size
                    int editTextHeight = 70;
                    int editTextWidth = 200;

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(editTextWidth, editTextHeight);

                    //You could adjust the position
                    params.topMargin = (int) (event.getRawY());
                    params.leftMargin = (int) (event.getRawX());
                    this.addView(editText, params);
                    editText.setVisibility(VISIBLE);
                    editText.setSingleLine();
                    editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    editText.requestFocus();
                    editText.setTextSize(mTextSize);

                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(getContext().INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

                    createTextAnnotatable(editText, x, y);

                    editText.addTextChangedListener(new TextWatcher() {

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before,
                                                  int count) {
                            drawText();
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            // TODO Auto-generated method stub
                        }

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count,
                                                      int after) {
                            // TODO Auto-generated method stub
                        }

                    });

                    editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                                sendAnnotation(mode.toString(), buildSignalFromText(x, y, mCurrentText.getEditText().getText().toString(), false, true));

                                //Create annotatable text and add it to the canvas
                                mAnnotationsActive = false;

                                try {
                                    addAnnotatable(mSession.getConnection().getConnectionId());

                                } catch (Exception e) {
                                    Log.e(LOG_TAG, e.toString());
                                }

                                mCurrentText = null;
                                invalidate();
                                return true;
                            }
                            return false;
                        }
                    });
                    addLogEvent(OpenTokConfig.LOG_ACTION_TEXT, OpenTokConfig.LOG_VARIATION_SUCCESS);
                }
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mAnnotationsActive) {
            if (mCurrentText != null && mCurrentText.getEditText() != null && !mCurrentText.getEditText().getText().toString().isEmpty()) {
                TextPaint textpaint = new TextPaint(mCurrentPaint);
                String text = mCurrentText.getEditText().getText().toString();
                Paint borderPaint = new Paint();
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(5);

                Rect result = new Rect();
                mCurrentPaint.getTextBounds(text, 0, text.length(), result);

                if (text.length() > 10) {
                    String[] strings = text.split("(?<=\\G.{" + 10 + "})");

                    float x = mCurrentText.getX();
                    float y = 340;
                    canvas.drawRect(x, y - result.height() - 20 + (strings.length * 50), x + result.width() + 20, y, borderPaint);

                    for (int i = 0; i < strings.length; i++) {
                        canvas.drawText(strings[i], x, y, mCurrentPaint);
                        y = y + 50;
                    }
                } else {
                    canvas.drawRect(mCurrentText.getX(), 340 - result.height() - 20, mCurrentText.getX() + result.width() + 20, 340, borderPaint);
                    canvas.drawText(mCurrentText.getEditText().getText().toString(), mCurrentText.getX(), 340, mCurrentPaint);
                }
            }
            if (mCurrentPath != null) {
                canvas.drawPath(mCurrentPath, mCurrentPaint);
            }
        }

        for (Annotatable drawing : mAnnotationsManager.getAnnotatableList()) {
            if (drawing.getType().equals(Annotatable.AnnotatableType.PATH)) {
                canvas.drawPath(drawing.getPath(), drawing.getPaint());
            }

            if (drawing.getType().equals(Annotatable.AnnotatableType.TEXT)) {
                canvas.drawText(drawing.getText().getEditText().getText().toString(), drawing.getText().x, drawing.getText().y, drawing.getPaint());
            }
        }

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void onItemSelected(final View v, final boolean selected) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (v.getId() == R.id.done) {
                    addLogEvent(OpenTokConfig.LOG_ACTION_DONE, OpenTokConfig.LOG_VARIATION_ATTEMPT);
                    mode = Mode.Clear;
                    clearAll(false, mSession.getConnection().getConnectionId());
                    AnnotationsView.this.setVisibility(GONE);
                    mListener.onAnnotationsDone();
                    addLogEvent(OpenTokConfig.LOG_ACTION_DONE, OpenTokConfig.LOG_VARIATION_SUCCESS);
                }
                if (v.getId() == R.id.erase) {
                    addLogEvent(OpenTokConfig.LOG_ACTION_ERASE, OpenTokConfig.LOG_VARIATION_ATTEMPT);
                    mode = Mode.Undo;
                    undoAnnotation(false, mSession.getConnection().getConnectionId());
                    addLogEvent(OpenTokConfig.LOG_ACTION_ERASE, OpenTokConfig.LOG_VARIATION_SUCCESS);
                }
                if (v.getId() == R.id.screenshot) {
                    addLogEvent(OpenTokConfig.LOG_ACTION_SCREENCAPTURE, OpenTokConfig.LOG_VARIATION_ATTEMPT);
                    //screenshot capture
                    mode = Mode.Capture;
                    if (videoRenderer != null) {
                        Bitmap bmp = videoRenderer.captureScreenshot();

                        if (bmp != null) {
                            if (mListener != null) {
                                mListener.onScreencaptureReady(bmp);
                            }
                            addLogEvent(OpenTokConfig.LOG_ACTION_SCREENCAPTURE, OpenTokConfig.LOG_VARIATION_SUCCESS);
                        }
                    }
                }
                if (selected) {
                    AnnotationsView.this.setVisibility(VISIBLE);

                    if (v.getId() == R.id.picker_color) {
                        mode = Mode.Color;
                        AnnotationsView.this.mToolbar.bringToFront();
                    } else {
                        if (v.getId() == R.id.type_tool) {
                            //type text
                            mode = Mode.Text;
                        }
                        if (v.getId() == R.id.draw_freehand) {
                            //freehand lines
                            mode = Mode.Pen;
                        }
                    }
                } else {
                    mode = null;
                }

                if (!loaded) {
                    resize();
                    loaded = true;
                }

                if (mode != null) {
                    mListener.onAnnotationsSelected(mode);
                }
            }
        });
    }

    @Override
    public void onColorSelected(int color) {
        addLogEvent(OpenTokConfig.LOG_ACTION_PICKER_COLOR, OpenTokConfig.LOG_VARIATION_ATTEMPT);
        this.mCurrentColor = color;
        mSelectedColor = color;
        addLogEvent(OpenTokConfig.LOG_ACTION_PICKER_COLOR, OpenTokConfig.LOG_VARIATION_SUCCESS);
    }

    @Override
    public void onSignalReceived(final SignalInfo signalInfo, boolean isSelfSignal) {
        Log.i(LOG_TAG, "Signal info: " + signalInfo.mSignalName);
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!loaded) {
                    resize();
                    loaded = true;
                }

                String cid = signalInfo.mSrcConnId;
                String mycid = signalInfo.mDstConnId;

                if (!cid.equals(mycid)) { // Ensure that we only handle signals from other users on the current canvas
                    Log.i(LOG_TAG, "Incoming annotation");
                    AnnotationsView.this.setVisibility(VISIBLE);
                    if (!loaded) {
                        resize();
                        loaded = true;
                    }

                    if (signalInfo.mSignalName != null && signalInfo.mSignalName.contains(Mode.Pen.toString())) {
                        Log.i(LOG_TAG, "New pen annotations is received");
                        penAnnotations(signalInfo.mSrcConnId, signalInfo.mData.toString());
                    } else {
                        if (signalInfo.mSignalName.equalsIgnoreCase(Mode.Undo.toString())) {
                            Log.i(LOG_TAG, "New undo annotations is received");
                            mode = Mode.Undo;
                            undoAnnotation(true, cid);
                        } else {
                            if (signalInfo.mSignalName.equalsIgnoreCase(Mode.Clear.toString())) {
                                Log.i(LOG_TAG, "New clear annotations is received");
                                mode = Mode.Clear;
                                clearAll(true, cid);
                            } else {
                                if (signalInfo.mSignalName.equalsIgnoreCase(Mode.Text.toString())) {
                                    Log.i(LOG_TAG, "New text annotations is received");
                                    try {
                                        textAnnotation(cid, signalInfo.mData.toString());
                                    } catch (Exception e) {
                                        Log.e(LOG_TAG, e.toString());
                                    }
                                } else {
                                    if (signalInfo.mSignalName.equalsIgnoreCase("otAnnotation_requestPlatform")) {
                                        try {
                                            JSONObject jsonObject = new JSONObject();
                                            jsonObject.put("platform", "android");
                                            mSession.sendSignal(new SignalInfo(mSession.getConnection().getConnectionId(), null, "otAnnotation_mobileScreenShare", jsonObject), null);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

}