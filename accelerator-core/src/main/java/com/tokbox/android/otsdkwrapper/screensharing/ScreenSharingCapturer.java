package com.tokbox.android.otsdkwrapper.screensharing;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.view.View;

import com.opentok.android.BaseVideoCapturer;
import com.tokbox.android.otsdkwrapper.GlobalLogLevel;
import com.tokbox.android.otsdkwrapper.utils.LogWrapper;

import java.io.FileOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a Custom Capturer to capture the full screen of the device
 */
public class ScreenSharingCapturer extends BaseVideoCapturer{
    private static final String LOG_TAG = ScreenSharingCapturer.class.getSimpleName();
    private static final short LOCAL_LOG_LEVEL = 0xFF;
    private static final LogWrapper LOG =
            new LogWrapper((short)(GlobalLogLevel.sMaxLogLevel & LOCAL_LOG_LEVEL));
    public static void setLogLevel(short logLevel) {
        LOG.setLogLevel(logLevel);
    }

    private boolean capturing = false;
    private View contentView;

    private int fps = 15;
    private int width = 0;
    private int height = 0;
    private int[] frame;

    private Bitmap bmp;
    private Canvas canvas;

    private Handler mHandler = new Handler();

    private ImageReader mImageReader;

    Bitmap lastBmp;

    private final Lock mImageReaderLock = new ReentrantLock(true /*fair*/);

    private Runnable newFrame = new Runnable() {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            if (capturing) {
            frame = null;
                  if (frame == null || width != bmp.getWidth()
                        || height != bmp.getHeight()){
                    if (lastBmp != null){
                        width = lastBmp.getWidth();
                        height = lastBmp.getHeight();
                        frame = new int[width * height];

                        lastBmp.getPixels(frame, 0, width, 0, 0, width, height);
                        provideIntArrayFrame(frame, ARGB, width, height, 0, false);
                    }
                    mHandler.postDelayed(newFrame, 1000 / fps);
                }
            }
        }
    };


    /* Constructor
     * @param context Application context
     * @param view Screensharing content view
     * @param imageReader to access to the image data rendered in the screensharing
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public ScreenSharingCapturer(Context context, View view, ImageReader imageReader) {
        this.contentView = view;
        this.mImageReader = imageReader;
        this.mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);

        this.width = contentView.getWidth();
        this.height = contentView.getHeight();

    }

    @Override
    public void init() {

    }

    @Override
    public int startCapture() {
        LOG.d(LOG_TAG, "Start Screensharing Capturer");
        capturing = true;
        mHandler.postDelayed(newFrame, 1000 / fps);
        return 0;
    }

    @Override
    public int stopCapture() {
        LOG.d(LOG_TAG, "Stop Screensharing Capturer");
        capturing = false;
        mHandler.removeCallbacks(newFrame);
        return 0;
    }

    @Override
    public boolean isCaptureStarted() {
        return capturing;
    }

    @Override
    public CaptureSettings getCaptureSettings() {

        CaptureSettings settings = new CaptureSettings();
        settings.fps = fps;
        settings.width = width;
        settings.height = height;
        settings.format = ARGB;
        return settings;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {

        @Override
        public void onImageAvailable(ImageReader reader) {
                Image mImage = null;
                FileOutputStream fos = null;

                try {
                    mImage = mImageReader.acquireLatestImage();

                    if (mImage != null) {
                        int imgWidth = mImage.getWidth();
                        int imgHeight = mImage.getHeight();

                        Image.Plane[] planes = mImage.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * imgWidth;

                        Buffer buffer2 = planes[0].getBuffer().rewind();
                        bmp = Bitmap.createBitmap(imgWidth + rowPadding / pixelStride, imgHeight, Bitmap.Config.ARGB_8888);

                        bmp.copyPixelsFromBuffer(buffer2);
                        lastBmp = bmp.copy(bmp.getConfig(), true);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bmp != null) {
                        bmp.recycle();
                    }

                    if (mImage != null) {
                        mImage.close();
                    }
                }
        }
    }
}
