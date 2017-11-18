package co.polarr.polarrcamerademo;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import co.polarr.renderer.PolarrRender;

/**
 * Created by Colin on 2017/11/18.
 * Camera Demo
 */

public class CameraRenderView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private Context mContext;

    /**
     * Camera and SurfaceTexture
     */
    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;

    //private final FBORenderTarget mRenderTarget = new FBORenderTarget();
    private final OESTexture mCameraTexture = new OESTexture();
    //    private final Shader mOffscreenShader = new Shader();
    private PolarrRender polarrRender = new PolarrRender();

    private int mWidth, mHeight;
    private boolean updateTexture = false;

    /**
     * OpenGL params
     */
    private float[] mOrientationM = new float[16];

    public CameraRenderView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public CameraRenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
        updateTexture = true;
        requestRender();
    }

    @Override
    public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //load and compile shader

        try {
            polarrRender.initRender(getResources(), 512, 512, null);
            polarrRender.setNeedDrawScreen(true);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;

        polarrRender.updateSize(mWidth, mHeight);

        //generate camera texture------------------------
        mCameraTexture.init();
        polarrRender.setInputTexture(mCameraTexture.getTextureId());

        //set up surfacetexture------------------
        SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
        mSurfaceTexture = new SurfaceTexture(mCameraTexture.getTextureId());
        mSurfaceTexture.setOnFrameAvailableListener(this);
        if (oldSurfaceTexture != null) {
            oldSurfaceTexture.release();
        }


        //set camera para-----------------------------------
        int camera_width = 0;
        int camera_height = 0;

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        mCamera = Camera.open();
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> psize = param.getSupportedPreviewSizes();
        if (psize.size() > 0) {
            int i;
            for (i = 0; i < psize.size(); i++) {
                if (psize.get(i).width < width || psize.get(i).height < height)
                    break;
            }
            if (i > 0)
                i--;
            param.setPreviewSize(psize.get(i).width, psize.get(i).height);

            camera_width = psize.get(i).width;
            camera_height = psize.get(i).height;

        }

        //get the camera orientation and display dimension------------
        if (mContext.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) {
            Matrix.setRotateM(mOrientationM, 0, 90.0f, 0f, 0f, 1f);
        } else {
            Matrix.setRotateM(mOrientationM, 0, 0.0f, 0f, 0f, 1f);
        }

        param.setPictureFormat(PixelFormat.JPEG);
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        //start camera-----------------------------------------
        mCamera.setParameters(param);
        mCamera.startPreview();


        //start render---------------------
        requestRender();
    }

    @Override
    public synchronized void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (mSurfaceTexture == null) {
            return;
        }
        //render the texture to FBO if new frame is available
        if (updateTexture) {
            mSurfaceTexture.updateTexImage();
            updateTexture = false;

            polarrRender.updateOESTexture(mSurfaceTexture, mOrientationM);

            GLES20.glViewport(0, 0, mWidth, mHeight);
            polarrRender.drawFrameOES();
        }
    }

    public void onDestroy() {
        updateTexture = false;
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
        }

        mCamera = null;
    }

    public void updateFilter(final Map<String, Object> state) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.updateStates(state);
            }
        });
    }

    public void takePhoto(final OnCaptureCallback onCaptureCallback) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(readTexture(polarrRender.getOutputId(), mWidth, mHeight));

                onCaptureCallback.onPhoto(bitmap);
            }
        });
    }

    private static ByteBuffer readTexture(int texId, int width, int height) {
        int channels = 4;
        ByteBuffer ib = ByteBuffer.allocate(width * height * channels);
        int[] fFrame = new int[1];
        GLES20.glGenFramebuffers(1, fFrame, 0);
        bindFrameTexture(fFrame[0], texId);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        unBindFrameBuffer();
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        return ib;
    }

    private static void bindFrameTexture(int frameBufferId, int textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    private static void unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public interface OnCaptureCallback {
        void onPhoto(Bitmap bitmap);
    }
}