package com.lesson.draghelperlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;


/**
 * Created by Administrator on 2017/7/10.
 * lisen
 * 内部布局可回弹
 *
 */

public class DragHelperLayout extends RelativeLayout{


    private static  String TAG = "DragHelperLayout";
    private static final int NORMAL = 0;
    private static final int PULL_DOWN = 1;
    private static final int PULL_UP = 2;
    private int state = NORMAL;
    /**
     * 可回弹的控件在xml中的位置
     */

    private int dragViewIndex = 0;

    private View dragView;

    private Rect normal = new Rect();

    private int height;

    public DragHelperLayout(Context context) {
        this(context, null);
    }

    public DragHelperLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragHelperLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs,R.styleable.DragHelperLayout,defStyleAttr,0);
        dragViewIndex = typedArray.getInt(R.styleable.DragHelperLayout_dragViewIndex,0);
        typedArray.recycle();
    }

    @Override
    protected void onFinishInflate() {

        if (dragViewIndex >= getChildCount()){
            throw new RuntimeException("dragViewIndex must be <= getChildCount()!!!");
        }

        if (getChildCount() > 0){
            dragView = getChildAt(dragViewIndex);
        }
        super.onFinishInflate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        height = getMeasuredHeight();
    }



    private int lastY;
    private int touchLastY;
    private boolean isAnima = false;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isAnima){
            return true;
        }
        boolean intercept = false;
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchLastY = y;
                lastY = y;
                intercept = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (listener != null){
                    if ( Math.abs(lastY) < Math.abs(ev.getY()) && listener.isCanPullDown() ){
                        //可以下拉并且是向下滑动
                        intercept = true;
                        state = PULL_DOWN;
                    }else if (  Math.abs(lastY) > Math.abs(ev.getY()) && listener.isCanPullUp()){
                        //可以上啦并且是向上滑动
                        intercept = true;
                        state = PULL_UP;
                    }else{
                        if (normal.contains(normal.left,dragView.getTop()+10,normal.right,dragView.getBottom() - 10)){
                            //如果慢慢的回滑，将布局恢复，如果直接调用animation()；会造成时间分发失败
                            dragView.layout(normal.left,normal.top,normal.right,normal.bottom);
                            normal.setEmpty();
                        }else{
                            //此处是为了优化用户体验，不至于在内部控件被拖拽后快速反方向滑动早成的闪烁
                            if (!normal.isEmpty()){
                                animation();
                            }
                        }
                        intercept = false;
                        state = NORMAL;
                    }
                }else{
                    intercept = false;
                    state = NORMAL;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!normal.isEmpty()){
                    animation();
                }
                intercept = false;
                break;
        }
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isAnima){
            return super.onTouchEvent(ev);
        }
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:

                break;
            case MotionEvent.ACTION_UP:
                y = 0;
                state = NORMAL;
                if (!normal.isEmpty()) {
                    animation();
                }
                break;
            case MotionEvent.ACTION_MOVE:

                int dy = Math.abs(touchLastY) - Math.abs(y);
                if (normal.isEmpty()) {
                    normal.set(dragView.getLeft(), dragView.getTop(), dragView.getRight(), dragView.getBottom());
                }
                dragView.layout(dragView.getLeft(), dragView.getTop() - dy / 2, dragView.getRight(), dragView.getBottom() - dy / 2);
                if (state == PULL_DOWN && dragView.getTop() <= normal.top
                        ||state == PULL_UP && dragView.getBottom() >= normal.bottom){//被拖拽后回滑，重新分发事件
                    ev.setAction(MotionEvent.ACTION_CANCEL);
                    MotionEvent motionEvent = MotionEvent.obtain(ev);
                    motionEvent.setAction(MotionEvent.ACTION_DOWN);
                    return dispatchTouchEvent(motionEvent);
                }
                break;
        }
        touchLastY = y;
        return true;
    }


    private void animation() {

        //整个控件被拖出屏幕后，动画会无法执行，先刷新一下父布局就可以解决
        if (dragView.getTop() >= height || dragView.getBottom() <= 0){
            invalidate();
        }

        TranslateAnimation ta = new TranslateAnimation(0, 0, 0, normal.bottom - dragView.getBottom());
        ta.setInterpolator(new DecelerateInterpolator(0.6f));
        ta.setDuration(400);
        ta.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnima = true;

            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                isAnima = false;
                dragView.clearAnimation();
                // 设置回到正常的布局位置
                dragView.layout(normal.left, normal.top, normal.right, normal.bottom);
                normal.setEmpty();
            }
        });
        dragView.startAnimation(ta);
    }


    public interface DragListener {
        boolean isCanPullDown();
        boolean isCanPullUp();
    }
    private DragListener listener;

    public void setListener(DragListener listener) {
        this.listener = listener;
    }


}

