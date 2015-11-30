package com.asha;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.ScrollView;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by hzqiujiadi on 15/11/20.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class ChromeLikeSwipeLayout extends ViewGroup {
    private static final String TAG = "ChromeLikeSwipeLayout";
    private static final int sThreshold = dp2px(120);

    private View mTarget; // the target of the gesture
    private ChromeLikeView mChromeLikeView;
    private boolean mBeginDragging;
    private int mTopOffset;
    private int mTouchSlop;
    private float mTouchDownActor;
    private boolean mIsBusy;
    private IOnItemSelectedListener mOnItemSelectedListener;
    private LinkedList<IOnExpandViewListener> mExpandListeners = new LinkedList<>();
    private static final int sThreshold2 = dp2px(400);


    public ChromeLikeSwipeLayout(Context context) {
        super(context);
        init();
    }

    public ChromeLikeSwipeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChromeLikeSwipeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChromeLikeSwipeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        mChromeLikeView = new ChromeLikeView(getContext());
        mChromeLikeView.setRippleListener(new ChromeLikeView.IOnRippleListener() {
            @Override
            public void onRippleAnimFinished(int index) {
                mIsBusy = false;
                launchResetAnim(false);
                mBeginDragging = false;
                if ( mOnItemSelectedListener != null ){
                    mOnItemSelectedListener.onItemSelected(index);
                }
            }
        });
        addOnExpandViewListener(mChromeLikeView);
        addView(mChromeLikeView);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        float getY = event.getY();
        int action = event.getAction();
        if ( canChildDragDown() ) return false;

        switch ( action & MotionEvent.ACTION_MASK  ) {

            case MotionEvent.ACTION_DOWN:
                if ( mBeginDragging ){
                    //Log.d(TAG, String.format("onInterceptTouchEvent ACTION_DOWN %d %d",mTopOffset,sThreshold));
                    float diff;
                    if ( mTopOffset < 0 ){
                        diff = 0;
                    } else if( mTopOffset > sThreshold ){
                        diff = sThreshold;
                    } else {
                        diff = mTopOffset;
                    }
                    mTouchDownActor = getY - diff;
                    return true;
                }
                mTouchDownActor = getY;
                mBeginDragging = false;
                mChromeLikeView.onActionDown(event);
                break;
            case MotionEvent.ACTION_UP:
                //Log.d(TAG, String.format("onInterceptTouchEvent ACTION_UP"));
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.e(TAG, String.format("onInterceptTouchEvent ACTION_MOVE moving:%f",(getY - mTouchDownActor)));
                if ( !mBeginDragging && getY - mTouchDownActor > mTouchSlop ) {
                    mBeginDragging = true;
                }
                //Log.d(TAG, String.format("onInterceptTouchEvent ACTION_MOVE mBeginDragging=%b",mBeginDragging));
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //Log.d(TAG, String.format("onInterceptTouchEvent ACTION_POINTER_DOWN"));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //Log.d(TAG, String.format("onInterceptTouchEvent ACTION_POINTER_UP"));
                break;
        }
        //mBeginDragging = true;
        //Log.d(TAG, String.format("onInterceptTouchEvent return %b", mBeginDragging));

        return mBeginDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean isExpanded = mTopOffset >= sThreshold;
        //first point
        float getY = event.getY();

        switch ( event.getAction() ) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_CANCEL:
                mChromeLikeView.onActionUpOrCancel(event,isExpanded);
                break;
            case MotionEvent.ACTION_UP:
                mChromeLikeView.onActionUpOrCancel(event,isExpanded);
                executeAction();

                break;
            case MotionEvent.ACTION_MOVE:
                mChromeLikeView.onActionMove(event,isExpanded);
                mTopOffset = calculateTopOffset(getY - mTouchDownActor);
                ensureTarget();
                View child = mTarget;
                int currentTop = child.getTop();
                if ( mBeginDragging ) {
                    if ( !isExpanded )
                        notifyOnExpandListeners( currentTop * 1.0f / sThreshold, true);
                    childOffsetTopAndBottom(currentTop,mTopOffset);
                }
                invalidate();
                //requestLayout();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //Log.d(TAG,String.format("ACTION_POINTER_DOWN"));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //Log.d(TAG,String.format("ACTION_POINTER_UP"));
                break;
        }
        return true;
    }

    private int calculateTopOffset(float original){
        float basic = original * 0.6f;
        if ( basic > sThreshold ){
            basic = sThreshold + (basic - sThreshold) * 0.3f;
        }
        return (int) basic;
    }


    private void childOffsetTopAndBottom(int currentTop, int offset){
        int target;
        if ( currentTop <= sThreshold2 ) {
            if ( offset < 0 ){
                target = 0 - currentTop;
            } else if ( offset < sThreshold2 ) {
                target = offset - currentTop;
            } else {
                target = sThreshold2 - currentTop;
            }
        } else {
            target = sThreshold2 - currentTop;
        }
        mTarget.offsetTopAndBottom( target );

        mChromeLikeView.offsetTopAndBottom( target );
        requestLayout();
    }

    private void executeAction() {

        if ( mTopOffset >= sThreshold ){
            mIsBusy = true;
        } else {
            if ( mIsBusy ) return;
            launchResetAnim(true);
            mBeginDragging = false;
        }
    }

    private void launchResetAnim(final boolean isFromCancel ){
        ensureTarget();

        final int from = mTarget.getTop();
        final int to = 0;
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                float step = (to - from) * interpolatedTime + from;
                notifyOnExpandListeners( mTarget.getTop() * 1.0f / sThreshold ,isFromCancel);
                childOffsetTopAndBottom( mTarget.getTop(), Math.round(step) );
            }
        };
        animation.setDuration(300);
        animation.setInterpolator(new DecelerateInterpolator());
        mTarget.clearAnimation();
        mTarget.startAnimation(animation);
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        boolean touchAlwaysTrue =  child instanceof ScrollView
                || child instanceof AbsListView
                || child instanceof TouchAlwaysTrueLayout
                || child instanceof ChromeLikeView;

        if ( !touchAlwaysTrue ) child = TouchAlwaysTrueLayout.wrap(child);
        super.addView(child,index,params);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        View child = mTarget;
        int childLeft = getPaddingLeft();
        int childTop = child.getTop();
        int childWidth = width - getPaddingLeft() - getPaddingRight();
        int childHeight = height - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

        child = mChromeLikeView;
        childLeft = getPaddingLeft();
        childTop = mTarget.getTop() - child.getMeasuredHeight();
        childWidth = width - getPaddingLeft() - getPaddingRight();
        childHeight = child.getMeasuredHeight();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                width,
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));

        mChromeLikeView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mTarget.getTop(), MeasureSpec.EXACTLY));
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
        //super.requestDisallowInterceptTouchEvent(b);
    }


    private boolean canChildDragDown()
    {
        ensureTarget();
        boolean result = ViewCompat.canScrollVertically(mTarget,-1) ;
        //Log.e(TAG,"canChildDragDown:" + result + ",scrollY:" + mTarget.getScrollY() );
        return result ;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mChromeLikeView)) {
                    mTarget = child;
                    mChromeLikeView.bringToFront();
                    break;
                }
            }
        }
    }

    private void setConfig(Config config){
        mChromeLikeView.setIcons(config.mIcons);
        if ( config.mBackgroundResId != Config.DEFAULT )
            mChromeLikeView.setBackgroundResource(config.mBackgroundResId);
        if ( config.mBackgroundColor != Config.DEFAULT )
            mChromeLikeView.setBackgroundColor(config.mBackgroundColor);
        if ( config.mCircleColor != Config.DEFAULT )
            mChromeLikeView.setCircleColor( config.mCircleColor );
        mOnItemSelectedListener = config.mOnItemSelectedListener;
    }

    public void notifyOnExpandListeners(float fraction, boolean isFromCancel){
        fraction = fraction < 1 ? fraction : 1;
        for ( IOnExpandViewListener listener : mExpandListeners )
            listener.onExpandView(fraction,isFromCancel);
    }

    public void addOnExpandViewListener(IOnExpandViewListener listener){
        mExpandListeners.add(listener);
    }

    public void removeOnExpandViewListener(IOnExpandViewListener listener){
        mExpandListeners.remove(listener);
    }

    public void removeAllOnExpandViewListener(){
        mExpandListeners.clear();
    }

    public interface IOnExpandViewListener {
        void onExpandView(float fraction, boolean isFromCancel);
    }

    public static Config makeConfig(){
        return new Config();
    }

    public static class Config{
        private List<Integer> mIcons;
        private IOnItemSelectedListener mOnItemSelectedListener;
        private int mCircleColor = DEFAULT;
        private int mBackgroundResId = DEFAULT;
        private int mBackgroundColor = DEFAULT;
        private static final int DEFAULT = -1;

        private Config(){

        }

        public Config addIcon(@DrawableRes int drawableResId){
            if ( mIcons == null ) mIcons = new LinkedList<>();
            mIcons.add(drawableResId);
            return this;
        }

        public Config background(@DrawableRes int backgroundResId){
            this.mBackgroundResId = backgroundResId;
            return this;
        }

        public Config backgroundColor(@ColorInt int color){
            this.mBackgroundColor = color;
            return this;
        }

        public Config circleColor(@ColorInt int color){
            this.mCircleColor = color;
            return this;
        }

        public Config listenItemSelected(IOnItemSelectedListener listener){
            this.mOnItemSelectedListener = listener;
            return this;
        }

        public void setTo(ChromeLikeSwipeLayout chromeLikeSwipeLayout){
            chromeLikeSwipeLayout.setConfig(this);
        }

    }

    public static int dp2px(float valueInDp) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (valueInDp * scale + 0.5f);
    }

    public interface IOnItemSelectedListener{
        void onItemSelected(int index);
    }

}