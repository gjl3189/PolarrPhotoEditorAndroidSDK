package co.polarr.polarrrenderdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.AttributeSet;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import co.polarr.renderer.PolarrRender;
import co.polarr.renderer.entities.BrushItem;
import co.polarr.renderer.entities.MagicEraserPath;
import co.polarr.renderer.filters.Basic;

/**
 * Created by Colin on 2017/10/17.
 */

public class DemoView extends GLSurfaceView {
    private static final String TAG = "DEBUG";
    private PolarrRender polarrRender;
    private DemoRender render = new DemoRender();
    private int inputTexture;
    private int outputTexture;

    // benchmark
    private long lastTraceTime = 0;
    private int outWidth;
    private int outHeight;

    public DemoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(3);
        setRenderer(render);
//        setRenderMode(RENDERMODE_WHEN_DIRTY);

        polarrRender = new PolarrRender();
    }

    public void initMagicEraser() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.magicEraserInit();
            }
        });
    }

    public void undoMagicEraser() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.magicEraserUndo();
            }
        });
    }

    public void redoMagicEraser() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.magicEraserRedo();
            }
        });
    }

    public void resetMagicEraser() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.magicEraserReset();
            }
        });
    }

    public void renderMagicEraser(final MagicEraserPath path) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.magicEraserStep(path);
            }
        });
    }

    public void updateBrushPoints(final BrushItem brushItem) {
        polarrRender.updateBrushPoints(brushItem);

    }

    public void addBrushPathPoint(final BrushItem brushItem, final PointF point) {
        polarrRender.addBrushPathPoint(brushItem, point);
    }

    private class DemoRender implements Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            inputTexture = genInputTexture();
            BenchmarkUtil.MemStart("initRender");
            BenchmarkUtil.MemStart("AllSDK");
            BenchmarkUtil.TimeStart("initRender");
            polarrRender.initRender(getResources(), getWidth(), getHeight(), false);
            BenchmarkUtil.TimeEnd("initRender");
            BenchmarkUtil.MemEnd("initRender");
            polarrRender.setInputTexture(inputTexture);

            outputTexture = genOutputTexture(getWidth(), getHeight());
            polarrRender.setOutputTexture(outputTexture);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
//            BenchmarkUtil.TimeStart("updateSize");
            // already updated by importImage.
//            polarrRender.updateSize(width, height);
//            BenchmarkUtil.TimeEnd("updateSize");
//            updateSize(outputTexture, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            polarrRender.drawFrame();
            GLES20.glViewport(0, 0, getWidth(), getHeight());
//            demoCopyTexture(polarrRender.getOutputId(), outputTexture, outWidth, outHeight);
            // demo draw screen
            Basic filter = Basic.getInstance(getResources());
            filter.setInputTextureId(outputTexture);
            Matrix.scaleM(filter.getMatrix(), 0, 1, -1, 1);
            filter.draw();

            if (System.currentTimeMillis() - lastTraceTime > 2000) {
                BenchmarkUtil.MemEnd("AllSDK");
                lastTraceTime = System.currentTimeMillis();
            }
        }
    }

    private void demoCopyTexture(int src_id, int dest_id, int width, int height) {
        int[] fbo = new int[1];
        GLES20.glGenFramebuffers(1, fbo, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, src_id, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, dest_id);
        GLES20.glCopyTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDeleteFramebuffers(1, fbo, 0);
    }

    public void importImage(final Bitmap bitmap) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);

//                demoCopyTexture(inputTexture, outputTexture, outWidth, outHeight);
//                Bitmap bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
//                bitmap.copyPixelsFromBuffer(readTexture(outputTexture, outWidth, outHeight));
//                bitmap.recycle();

                outHeight = bitmap.getHeight();
                outWidth = bitmap.getWidth();

                updateSize(outputTexture, outWidth, outHeight);
                polarrRender.updateSize(outWidth, outHeight);
                bitmap.recycle();

                polarrRender.updateInputTexture();

//                demoCopyTexture(inputTexture, outputTexture, outWidth, outHeight);
//                bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
//                bitmap.copyPixelsFromBuffer(readTexture(outputTexture, outWidth, outHeight));
//                bitmap.recycle();
            }
        });
    }

    private static ByteBuffer readTexture(int texId, int width, int height) {
        int channels = 4;
        ByteBuffer ib = ByteBuffer.allocate(width * height * channels);
        int[] fFrame = new int[1];
        GLES20.glGenFramebuffers(1, fFrame, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fFrame[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texId, 0);

        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        return ib;
    }

    private int genInputTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        int texture = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        return texture;
    }

    private int genOutputTexture(int width, int height) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        int texture = textures[0];
        updateSize(texture, width, height);

        return texture;
    }

    private void updateSize(int texture, int width, int height) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, width, height,
                0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void updateStatesWithJson(final String statesString) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.updateStates(statesString);
            }
        });
    }

    public void updateStates(final Map<String, Object> statesMap) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                BenchmarkUtil.TimeStart("updateStates");
                polarrRender.updateStates(statesMap);
                BenchmarkUtil.TimeEnd("updateStates");
            }
        });
    }

    public void autoEnhance(final Map<String, Object> statesMapToUpdate, final float percent) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                BenchmarkUtil.TimeStart("autoEnhanceGlobal");
                Map<String, Object> changedStates = polarrRender.autoEnhanceGlobal(percent);
                BenchmarkUtil.TimeEnd("autoEnhanceGlobal");

                if (statesMapToUpdate != null) {
                    statesMapToUpdate.putAll(changedStates);

                    updateStates(statesMapToUpdate);
                }
            }
        });
    }

    public void autoEnhanceFace0(final Map<String, Object> faceStates) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                BenchmarkUtil.TimeStart("autoEnhanceFace");
                polarrRender.autoEnhanceFace(faceStates, 0);
                BenchmarkUtil.TimeEnd("autoEnhanceFace");
                polarrRender.updateStates(faceStates);
            }
        });
    }

    public void releaseRender() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.releaseGLRes();
                polarrRender.releaseNonGLRes();
                polarrRender = null;
            }
        });
    }
}