package co.polarr.polarrrenderdemo;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import co.polarr.renderer.render.GLRenderView;

/**
 * Created by Colin on 2017/6/30.
 * customized render view with additional functions.
 */

public class CustomRenderView extends GLRenderView {
    // judgement if or not is move, pixel distance
    private static final float IS_MOVE_DIS = 10;

    // image offset
    private PointF mOffset = new PointF();
    private float mZoom = 1f;

    // one point touch
    private PointF mLastTouch;
    private int mStartEventId;

    // start 2nd touch distance
    private float mTouchStartDis;

    // is moving or not
    private boolean mIsMoving;
    // is showing original
    private boolean mIsOriginal;

    public CustomRenderView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // check inited
        if (getCurrentBitmap() == null) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                mLastTouch = new PointF(event.getX(0), event.getY(0));
                mStartEventId = event.getPointerId(0);

                mZoom = getZoom();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() > 1) {
                    float x = event.getX(0) - event.getX(1);
                    float y = event.getY(0) - event.getY(1);
                    mTouchStartDis = (float) Math.sqrt(x * x + y * y);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mIsMoving) {
                    mIsMoving = false;

                    mStartEventId = -1;
                }

                if (mIsOriginal) {
                    mIsOriginal = false;
                    showOriginal(false);
                }

                checkEdge();

                return true;
            case MotionEvent.ACTION_MOVE:
                boolean needUpdate = false;
                if (event.getPointerId(0) == mStartEventId) {
                    float dX = event.getX(0) - mLastTouch.x;
                    float dY = event.getY(0) - mLastTouch.y;
                    mLastTouch = new PointF(event.getX(0), event.getY(0));

                    if (!mIsMoving && (Math.abs(dX) >= IS_MOVE_DIS || Math.abs(dY) >= IS_MOVE_DIS)) {
                        mIsMoving = true;
                    }

                    if (mIsMoving) {
                        if (mIsOriginal) {
                            mIsOriginal = false;
                            showOriginal(false);
                        }

                        mOffset.x += dX;
                        mOffset.y += dY;
                        setPosition(mOffset.x, mOffset.y);
                        needUpdate = true;
                    }
                }

                if (event.getPointerCount() > 1) {
                    float x = event.getX(0) - event.getX(1);
                    float y = event.getY(0) - event.getY(1);
                    float detlaDis = (float) Math.sqrt(x * x + y * y);
                    float scale = detlaDis / mTouchStartDis;
                    mZoom *= scale;
                    mTouchStartDis = detlaDis;

                    mZoom = Math.max(getMinZoom(), mZoom);

                    setZoom(mZoom);
                    needUpdate = true;
                } else if (!mIsMoving) {
                    if (!mIsOriginal) {
                        mIsOriginal = true;
                        showOriginal(true);
                    }
                }

                if (needUpdate) {
                    requestRender();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void checkEdge() {
        float dZoom = mZoom - getMinZoom();
        float dx = dZoom * mWidth / 2;
        float dy = dZoom * mHeight / 2;
        boolean needUpdate = false;
        if (mOffset.x > dx) {
            mOffset.x = dx;
            needUpdate = true;
        } else if (mOffset.x < -dx) {
            mOffset.x = -dx;
            needUpdate = true;
        }

        if (mOffset.y > dy) {
            mOffset.y = dy;
            needUpdate = true;
        } else if (mOffset.y < -dy) {
            mOffset.y = -dy;
            needUpdate = true;
        }

        if (needUpdate) {
            setPosition(mOffset.x, mOffset.y);
            requestRender();
        }
    }
}
