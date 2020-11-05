package com.tokbox.accelerator.annotation;


import android.content.Context;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.tokbox.otsdkwrapper.R;

/**
 * Defines the layout for the annotations toolbar
 */
public class AnnotationsToolbar extends LinearLayout {

    private View rootView;
    private ImageButton mFreeHandBtn;
    private ImageButton mEraseBtn;
    private ImageButton mTypeBtn;
    private ImageButton mScreenshotBtn;
    private ImageButton mPickerColorBtn;
    private ImageButton mDoneBtn;

    private Context mContext;
    private RelativeLayout mMainToolbar;
    private LinearLayout mColorToolbar;
    private HorizontalScrollView mColorScrollView;

    private ActionsListener mActionsListener;

    /**
     * Monitors state changes in the AnnotationsToolbar.
     */
    public interface ActionsListener {

        /**
         * Invoked when a new AnnotationsToolbar's item is clicked
         *
         * @param v        View: item
         * @param selected Whether the item has been selected (<code>true</code>) or not (
         *                 <code>false</code>).
         */
        void onItemSelected(View v, boolean selected);

        void onColorSelected(int color);
    }

    /**
     * Constructor
     *
     * @param context Application context
     */
    public AnnotationsToolbar(@NonNull Context context) {
        super(context);

        mContext = context;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        init();
    }

    /**
     * Constructor
     *
     * @param context Application context
     * @param attrs   A collection of attributes
     */
    public AnnotationsToolbar(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        init();
    }

    /**
     * Sets AnnotationsToolbar listener
     *
     * @param listener: ActionsListener
     */
    public void setActionListener(ActionsListener listener) {
        this.mActionsListener = listener;
    }

    private void init() {
        rootView = inflate(mContext, R.layout.annotations_toolbar, this);
        mMainToolbar = rootView.findViewById(R.id.main_toolbar);

        mColorToolbar = rootView.findViewById(R.id.color_toolbar);
        mColorScrollView = rootView.findViewById(R.id.color_view);
        mFreeHandBtn = mMainToolbar.findViewById(R.id.draw_freehand);
        mPickerColorBtn = mMainToolbar.findViewById(R.id.picker_color);
        mTypeBtn = mMainToolbar.findViewById(R.id.type_tool);
        mScreenshotBtn = mMainToolbar.findViewById(R.id.screenshot);
        mEraseBtn = mMainToolbar.findViewById(R.id.erase);
        mDoneBtn = mMainToolbar.findViewById(R.id.done);

        final int mCount = mColorToolbar.getChildCount();

        int[] colors = {R.color.picker_color_blue, R.color.picker_color_purple, R.color.picker_color_red, R.color.picker_color_orange,
                R.color.picker_color_yellow, R.color.picker_color_green, R.color.picker_color_black, R.color.picker_color_gray, R.color.picker_color_white};

        // Loop through all of the children.
        for (int i = 0; i < mCount; ++i) {
            mColorToolbar.getChildAt(i).setOnClickListener(colorClickListener);
            ((ImageButton) mColorToolbar.getChildAt(i)).setColorFilter(getResources().getColor(colors[i]));
        }

        //Init actions
        mFreeHandBtn.setOnClickListener(mActionsClickListener);
        mTypeBtn.setOnClickListener(mActionsClickListener);
        mEraseBtn.setOnClickListener(mActionsClickListener);
        mScreenshotBtn.setOnClickListener(mActionsClickListener);
        mPickerColorBtn.setOnClickListener(mActionsClickListener);
        mDoneBtn.setOnClickListener(mActionsClickListener);

        mDoneBtn.setSelected(false);
    }


    private OnClickListener colorClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            int color = getResources().getColor(R.color.picker_color_orange);

            if (v.getId() == R.id.picker_purple) {
                color = getResources().getColor(R.color.picker_color_purple);
            }
            if (v.getId() == R.id.picker_red) {
                color = getResources().getColor(R.color.picker_color_red);
            }
            if (v.getId() == R.id.picker_orange) {
                color = getResources().getColor(R.color.picker_color_orange);
            }
            if (v.getId() == R.id.picker_blue) {
                color = getResources().getColor(R.color.picker_color_blue);
            }
            if (v.getId() == R.id.picker_green) {
                color = getResources().getColor(R.color.picker_color_green);
            }
            if (v.getId() == R.id.picker_white) {
                color = getResources().getColor(R.color.picker_color_white);
            }
            if (v.getId() == R.id.picker_black) {
                color = getResources().getColor(R.color.picker_color_black);
            }
            if (v.getId() == R.id.picker_yellow) {
                color = getResources().getColor(R.color.picker_color_yellow);
            }
            if (v.getId() == R.id.picker_gray) {
                color = getResources().getColor(R.color.picker_color_gray);
            }

            updateColorPickerSelectedButtons(v, color);
            if (v.isSelected()) {
                v.setSelected(false);
            } else {
                v.setSelected(true);
            }

            if (mActionsListener != null) {
                mActionsListener.onColorSelected(color);
            }
        }

    };


    private OnClickListener mActionsClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (mActionsListener != null) {
                if (v.getId() == R.id.picker_color) {
                    if (mColorScrollView.getVisibility() == View.GONE)
                        mColorScrollView.setVisibility(View.VISIBLE);
                    else {
                        mColorScrollView.setVisibility(View.GONE);
                    }
                } else {
                    mColorScrollView.setVisibility(View.GONE);
                }
                updateSelectedButtons(v);
                if (v.getId() != R.id.screenshot && v.getId() != R.id.erase && v.getId() != R.id.done) {
                    if (v.isSelected()) {
                        v.setSelected(false);
                    } else {
                        v.setSelected(true);
                    }
                    mDoneBtn.setVisibility(VISIBLE);
                } else {
                    restart();
                }
                mActionsListener.onItemSelected(v, v.isSelected());
            }
        }

    };

    private void updateColorPickerSelectedButtons(final View v, final int color) {
        final int mCount = mColorToolbar.getChildCount();

        mPickerColorBtn.setColorFilter(color);

        for (int i = 0; i < mCount; ++i) {
            if (mColorToolbar.getChildAt(i).getId() != v.getId() && mColorToolbar.getChildAt(i).isSelected()) {
                mColorToolbar.getChildAt(i).setSelected(false);
            }
        }

    }

    private void updateSelectedButtons(final View v) {
        final int mCount = mMainToolbar.getChildCount();

        for (int i = 0; i < mCount; ++i) {
            if (mMainToolbar.getChildAt(i).getId() != v.getId() && mMainToolbar.getChildAt(i).isSelected()) {
                mMainToolbar.getChildAt(i).setSelected(false);
            }
        }

    }

    /**
     * Restarts the toolbar actions
     */
    public void restart() {

        int mCount = mMainToolbar.getChildCount();
        for (int i = 0; i < mCount; ++i) {
            mMainToolbar.getChildAt(i).setSelected(false);
        }

        mCount = mColorToolbar.getChildCount();
        for (int i = 0; i < mCount; ++i) {
            mColorToolbar.getChildAt(i).setSelected(false);
        }

        int color = getResources().getColor(R.color.picker_color_orange);
        mPickerColorBtn.setColorFilter(color);
        if (mActionsListener != null) {
            mActionsListener.onColorSelected(color);
        }
        mDoneBtn.setVisibility(GONE);
    }
}