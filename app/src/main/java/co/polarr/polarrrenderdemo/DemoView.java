package co.polarr.polarrrenderdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;

import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import co.polarr.renderer.PolarrRender;
import co.polarr.renderer.filters.Basic;

/**
 * Created by Colin on 2017/10/17.
 */

public class DemoView extends GLSurfaceView {
    private PolarrRender polarrRender;
    private DemoRender render = new DemoRender();

    public DemoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);
        setRenderer(render);

        polarrRender = new PolarrRender();
    }

    private class DemoRender implements Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            polarrRender.initRender(getResources(), getWidth(), getHeight(), null);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            polarrRender.updateSize(width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            polarrRender.drawFrame();

            // demo draw screen
            Basic filter = Basic.getInstance(getResources());
            filter.setInputTextureId(polarrRender.getOutputId());
            filter.draw();
        }
    }

    public void importImage(final Bitmap bitmap) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                int inputTexture = polarrRender.getTextureId();
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, 0);

                polarrRender.updateInputTexture();
            }
        });
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
                polarrRender.updateStates(statesMap);
            }
        });
    }

    public void releaseRender() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                polarrRender.release();
            }
        });
    }
}
