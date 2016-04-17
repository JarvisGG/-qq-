package com.myproject18.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Created by Administrator on 2016/4/17.
 */
public class GooView extends View {

    private Paint mPaint;

    public OnStateChangeListener getmStateChangeListener() {
        return mStateChangeListener;
    }

    public void setmStateChangeListener(OnStateChangeListener mStateChangeListener) {
        this.mStateChangeListener = mStateChangeListener;
    }

    public interface OnStateChangeListener {
        void onDisappear();

        void onReset(boolean isOutOfRange);
    }

    private OnStateChangeListener mStateChangeListener;

    PointF[] mStickPoints = new PointF[]{
            new PointF(250f, 250f),
            new PointF(250f, 350f)
    };
    PointF[] mDragPoints = new PointF[]{
            new PointF(50f, 250f),
            new PointF(50f, 350f)
    };
    PointF mControlPoint = new PointF(150f, 300f);

    PointF mStickCenter = new PointF(150f, 150f);
    PointF mDragCenter = new PointF(100f, 100f);

    float mStickRadius = 12f;
    float mDragRadius = 16f;

    // 范围
    float mFarthest = 80f;
    boolean isOutOfRange = false;
    boolean isDisappear = false;

    public GooView(Context context) {
        super(context);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
    }

    public GooView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
    }

    public GooView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
    }

    int mStatusBarHeight;

    /**
     * 更新拖拽圆坐标，同时重绘
     * @param x
     * @param y
     */
    private void updateDragCenter(float x, float y) {
        mDragCenter.set(x, y);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mStatusBarHeight = getStatusBarHeight(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN:
                float x = event.getRawX();
                float y = event.getRawY();
                isOutOfRange = false;
                isDisappear = false;
                updateDragCenter(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                float rawX = event.getRawX();
                float rawy = event.getRawY();

                updateDragCenter(rawX, rawy);

                float distance_move = getDistanceBetween2Points(mDragCenter, mStickCenter);
                if (distance_move > mFarthest){
                    isOutOfRange = true;
                    invalidate();
                }

                break;
            case MotionEvent.ACTION_UP:
                if(!isOutOfRange) {
                    onViewReset();
                } else {
                    float distance_up = getDistanceBetween2Points(mDragCenter, mStickCenter);
                    if(distance_up < mFarthest) {
                        updateDragCenter(mStickCenter.x, mStickCenter.y);
                        isDisappear = false;

                        if (mStateChangeListener != null) {
                            mStateChangeListener.onReset(isOutOfRange);
                        }
                    } else {
                        isDisappear = true;
                        invalidate();

                        if (mStateChangeListener != null) {
                            mStateChangeListener.onDisappear();
                        }
                    }

                }
                break;
            default:
                break;
        }

        return true;
    }

    private void onViewReset() {
        ValueAnimator mAnim = ValueAnimator.ofFloat(1.0f);
        final PointF startP = new PointF(mDragCenter.x, mDragCenter.y);
        final PointF endP = new PointF(mStickCenter.x, mStickCenter.y);

        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float franction = animation.getAnimatedFraction();
                PointF point = getPointsByPercent(startP, endP, franction);
                updateDragCenter(point.x, point.y);
            }
        });
        mAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(mStateChangeListener != null) {
                    mStateChangeListener.onReset(isOutOfRange);
                }
            }
        });
        mAnim.setInterpolator(new OvershootInterpolator(4.0f));
        mAnim.setDuration(500);
        mAnim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // 平移画布
        canvas.save();
        canvas.translate(0, -mStatusBarHeight);

        // 画最大范围参考圆
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(mStickCenter.x, mStickCenter.y, mFarthest, mPaint);
        mPaint.setStyle(Paint.Style.FILL);
        if(!isDisappear) {
            if (!isOutOfRange) {
                // 计算坐标
                // 1.根据两圆心间距，计算固定圆半径
                float distance = getDistanceBetween2Points(mDragCenter, mStickCenter);
                float mTempStickRadius = getRadiusByDistance(distance);

                // 2.计算四个附着点坐标
                float offsetY = mStickCenter.y - mDragCenter.y;
                float offsetX = mStickCenter.x - mDragCenter.x;
                Double LineK = null;
                if (offsetX != 0) {
                    LineK = (double) offsetY / offsetX;
                }
                mDragPoints = getIntersectionPoints(mDragCenter, mDragRadius, LineK);
                mStickPoints = getIntersectionPoints(mStickCenter, mTempStickRadius, LineK);

                // 3.计算控制点
                mControlPoint = getPointsByPercent(mDragCenter, mStickCenter, 0.618f);


                // 化连接部分
                Path path = new Path();
                path.moveTo(mStickPoints[0].x, mStickPoints[0].y);
                path.quadTo(mControlPoint.x, mControlPoint.y, mDragPoints[0].x, mDragPoints[0].y);
                path.lineTo(mDragPoints[1].x, mDragPoints[1].y);
                path.quadTo(mControlPoint.x, mControlPoint.y, mStickPoints[1].x, mStickPoints[1].y);
                path.close();
                canvas.drawPath(path, mPaint);

                // 画固定圆
                canvas.drawCircle(mStickCenter.x, mStickCenter.y, mTempStickRadius, mPaint);
            }
            // 画拖拽圆
            canvas.drawCircle(mDragCenter.x, mDragCenter.y, mDragRadius, mPaint);
        }

        canvas.restore();
    }

    public static PointF[] getIntersectionPoints(PointF pMiddle, float radius, Double lineK) {
        PointF[] points = new PointF[2];
        float radian, xOffset = 0, yOffset = 0;
        if(lineK != null) {
            radian = (float) Math.atan(lineK);
            xOffset = (float) (Math.sin(radian) * radius);
            yOffset = (float) (Math.cos(radian) * radius);
        } else {
            xOffset = radius;
            yOffset = 0;
        }
        points[0] = new PointF(pMiddle.x + xOffset, pMiddle.y - yOffset);
        points[1] = new PointF(pMiddle.x - xOffset, pMiddle.y + yOffset);
        return points;
    }

    public static PointF getPointsByPercent(PointF p1, PointF p2, float percent) {
        return new PointF(evaluateValue(percent, p1.x, p2.x), evaluateValue(percent, p1.y, p2.y));
    }

    public static float evaluateValue(float fraction, Number start, Number end) {
        return start.floatValue() + (end.floatValue() - start.floatValue()) * fraction;
    }

    public static float getDistanceBetween2Points(PointF p1, PointF p2) {
        float distance = (float) Math.sqrt(Math.pow(p1.y - p2.y, 2) + Math.pow(p1.x - p2.x, 2));
        return distance;
    }

    /**
     * g
     * @param distance
     * @return
     */
    public float getRadiusByDistance(float distance) {
        float mFarthest = 80f;

        distance = Math.min(distance, mFarthest);
        float percent = distance / mFarthest;
        // 原始半径缩放20%，最小20% percent, mStickRadius, mStickRadius * 0.2f

        FloatEvaluator floatEvaluator = new FloatEvaluator();
        return floatEvaluator.evaluate(percent, mStickRadius, mStickRadius * 0.4f);
    }

    public static int getStatusBarHeight(View v) {
        if(v == null) {
            return 0;
        }
        Rect frame = new Rect();
        v.getWindowVisibleDisplayFrame(frame);
        return frame.top;
    }
}
