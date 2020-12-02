package com.opentok.accelerator.core.screensharing;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.opentok.accelerator.core.GlobalLogLevel;
import com.opentok.accelerator.core.utils.LogWrapper;

/**
 * Represents a headless fragment to enable the screensharing by default
 */
public class ScreenSharingFragment extends Fragment {

    private static final String LOG_TAG = ScreenSharingFragment.class.getSimpleName();
    private static final short LOCAL_LOG_LEVEL = 0xFF;
    private static final LogWrapper LOG =
            new LogWrapper((short) (GlobalLogLevel.INSTANCE.getMaxLogLevel() & LOCAL_LOG_LEVEL));
    public static void setLogLevel(short logLevel) {
        LOG.setLogLevel(logLevel);
    }


    private static final String ERROR = "ScreenSharing error";
    private static final int REQUEST_MEDIA_PROJECTION = 1;

    public ViewGroup getScreen() {
        return mScreen;
    }

    private ViewGroup mScreen;

    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;

    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;

    private int mResultCode;
    private Intent mResultData;

    public void setListener(ScreenSharingListener listener) {
        this.mListener = listener;
    }

    private ScreenSharingListener mListener;

    public ScreenSharingCapturer getScreenCapturer() {
        return mScreenCapturer;
    }

    private ScreenSharingCapturer mScreenCapturer;

    public interface ScreenSharingListener {

        /**
         * Invoked when the ScreenCapturer is ready
         */
        void onScreenCapturerReady();

        /**
         * Invoked when an error happened
         */
        void onError(String error);

    }

    public static ScreenSharingFragment newInstance() {
        ScreenSharingFragment fragment = new ScreenSharingFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.densityDpi;
        mMediaProjectionManager = (MediaProjectionManager)
                activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        //run permissions
        startActivityForResult(
                mMediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                LOG.i(LOG_TAG, "User cancelled screensharing permission");
                mListener.onError(ERROR + ": User cancelled screensharing permission");
                return;
            }
            Activity activity = getActivity();
            if (activity == null) {
                mListener.onError(ERROR + ": User cancelled screensharing permission");
                return;
            }

            LOG.i(LOG_TAG, "Starting screen capture");
            mResultCode = resultCode;
            mResultData = data;
            startScreenCapture();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setUpVirtualDisplay() {

        mScreen = (ViewGroup) getActivity().getWindow().getDecorView().getRootView();

        // display metrics
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mDensity = metrics.densityDpi;
        Display mDisplay = getActivity().getWindowManager().getDefaultDisplay();

        Point size = new Point();
        mDisplay.getRealSize(size);
        mWidth = size.x;
        mHeight = size.y;

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;

        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture", mWidth, mHeight, mDensity, flags, mImageReader.getSurface(), null, null);

        size.set(mWidth, mHeight);

        //create ScreenCapturer
        mScreenCapturer = new ScreenSharingCapturer(getActivity(), mScreen, mImageReader);

        mListener.onScreenCapturerReady();
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startScreenCapture() {

        if (mMediaProjection != null) {
            LOG.i(LOG_TAG, "mMediaProjection != null");

            setUpVirtualDisplay();
        } else if (mResultCode != 0 && mResultData != null) {
            LOG.i(LOG_TAG, "mResultCode != 0 && mResultData != null");
            setUpMediaProjection();
            setUpVirtualDisplay();
        } else {
            LOG.i(LOG_TAG, "Requesting confirmation");
            startActivityForResult(
                    mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;

        tearDownMediaProjection();
    }

}