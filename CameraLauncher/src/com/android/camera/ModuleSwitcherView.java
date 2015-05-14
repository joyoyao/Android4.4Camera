
package com.android.camera;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;

import com.android.camera.ui.RotatableLayout;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.PhotoSphereHelper;
import com.android.camera2.R;

public class ModuleSwitcherView extends RotatableLayout {

    private static String TAG = "CAM_ModuleSwitcherView";
    private static final float SCROLL_GAT = 100.0f;
    private float SCROLL_RAT = 100 / 100;
    private float TEXT_VIEW_WITH = 60.0F;
    public int FOOT_HIGHT = 130;

    public static final int PHOTO_MODULE_INDEX = 0;
    public static final int VIDEO_MODULE_INDEX = 1;
    public static final int WIDE_ANGLE_PANO_MODULE_INDEX = 2;
    public static final int LIGHTCYCLE_MODULE_INDEX = 3;
    public static final int GCAM_MODULE_INDEX = 4;

    public static boolean isEnableParomara =  MemeoryTotal.isGreateMemory();

    private static final int[] TEXT_IDS = {
            R.string.switch_camera_text, R.string.switch_video_text, R.string.switch_panorama_text,
            R.string.switch_sphere_text, R.string.switch_gcam_text,
    };
    private static int[] FRAMT_MODULE_INDEX = {
            VIDEO_MODULE_INDEX, PHOTO_MODULE_INDEX, WIDE_ANGLE_PANO_MODULE_INDEX,
            LIGHTCYCLE_MODULE_INDEX,GCAM_MODULE_INDEX};
    public static final int MODULE_NUMBER_MAX = TEXT_IDS.length;
    private static final int MODULE_NUMBER_SURPORT = (MODULE_NUMBER_MAX-2)-
        (isEnableParomara?0:1);
    private float[] mTextLength = new float[MODULE_NUMBER_SURPORT];

    private CameraActivity mActivity;
    private GestureDetector mGestureDetector;
    private int mOrientation;
    private int mCameraMoudleIndex;
    // SPRD: bug 258455
    private boolean mVisibility;

    private boolean mIsLayoutFinish;

    private int mLastIndex;
    private int mNextIndex;

    private float mLastPosation;

    private boolean mScrolling;
    private boolean mStopScroll;
    private boolean mModuleChange;
    private int mConfiguration = 1;

    private boolean mIsLeft;
    private MotionEvent mScrollStart;
    private MotionEvent mScrollEv1;
    private String[] mDrawIds;


    private ArrayList<TextView> mTextViewList;
    private TextView mDelTextView;

    private String mLocalLanguage = "en";
    private float mSystemTextScale = (float) 1.0;

    private GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            if (mStopScroll || mModuleChange)
                return super.onScroll(e1, e2, distanceX, distanceY);
            if (!mScrolling) {
                mScrollStart = MotionEvent.obtain(e2);
                mScrolling = true;
                mScrollEv1 = MotionEvent.obtain(e2);
            } else {
                if (mConfiguration == 1) {
                    if (Math.abs(e2.getX() - mScrollStart.getX()) >= 100) {
                        scrollEnd(e2.getX() - mScrollStart.getX());
                    }
                    else if (true) {
                        if (Math.abs(e2.getX() - mScrollEv1.getX()) >= SCROLL_RAT) {
                            scrollIng(e2.getX() - mScrollEv1.getX(), false);
                            mScrollEv1 = MotionEvent.obtain(e2);
                        }
                    }
                } else if (mConfiguration == 2) {
                    if (Math.abs(e2.getY() - mScrollStart.getY()) >= 100) {
                        scrollEnd(e2.getY() - mScrollStart.getY());
                    }
                    else if (true) {
                        if (Math.abs(e2.getY() - mScrollEv1.getY()) >= SCROLL_RAT) {
                            scrollIng(e2.getY() - mScrollEv1.getY(), true);
                            mScrollEv1 = MotionEvent.obtain(e2);
                        }
                    }
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    };

    public interface ModuleSwitchViewListener {

        public void onModuleViewSelected(int i);

        public void onShowSwitcherViewPopup();
    }

    private ModuleSwitchViewListener mListener;

    public ModuleSwitcherView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }

    public ModuleSwitcherView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ModuleSwitcherView(Context context) {
        super(context);
        initialize(context);
    }

    private void initialize(Context context) {

        mActivity = (CameraActivity) context;
        // SPRD: bug 258455
        mVisibility = true;
        initializeDrawables(context);
        setSwitchListener(mActivity);
        mScrolling = false;
        mStopScroll = false;
        mScrollStart = null;
        mScrollEv1 = null;
        mIsLeft = true;
        mModuleChange = false;

        TEXT_VIEW_WITH =  getContext().getResources().
                getDimensionPixelSize(R.dimen.module_textview_with);
        FOOT_HIGHT =  getContext().getResources().
                getDimensionPixelSize(R.dimen.module_margin_bottom);
        SCROLL_RAT = SCROLL_GAT / TEXT_VIEW_WITH;

        Configuration c = mActivity.getResources().getConfiguration();
        mLocalLanguage = c.locale.getLanguage();
        mSystemTextScale = c.fontScale;
        Log.d(TAG, "  mLocalLanguage =" + mLocalLanguage + "   mSystemTextScale="
                + mSystemTextScale);
        mGestureDetector = new GestureDetector(mSimpleOnGestureListener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // SPRD: bug 258455
        if(!mVisibility) return false;
        if (event.getAction() == MotionEvent.ACTION_CANCEL
                || event.getAction() == MotionEvent.ACTION_UP) {
            mStopScroll = false;
            if (mScrolling) {
                if (mConfiguration == 1)
                    scrollEnd(event.getX() - mScrollStart.getX());
                else if ((mConfiguration == 2))
                    scrollEnd(event.getY() - mScrollStart.getY());
            }
        }
        mGestureDetector.onTouchEvent(event);
        return false;
    }

    public void setOrientation(int orientation) {
        Log.d(TAG, "setOrientation orientation = " + orientation);
        if (!mScrolling)
            mOrientation = orientation;
    }

    public void scrollEnd(float scrollX) {
        Log.d(TAG, "scrolleEnd Start");
        if (mModuleChange) {
            return;
        }
        mScrolling = false;
        mScrollStart = null;
        mScrollEv1 = null;
        mIsLeft = (scrollX <= 0);
        if (!isEnableSwitch(scrollX <= 0)) {
            // SPRD: reset position of text view before return scrollEnd
            scrollBack(mConfiguration == 2);
            return;
        }
        if (mOrientation == 90 || mOrientation == 180) {
            mIsLeft = !mIsLeft;
        }
        if (Math.abs(scrollX) >= SCROLL_GAT) {
            mModuleChange = true;
            translateAnimat(scrollX);
            return;
        }
        scrollBack(mConfiguration == 2);
    }

    public void scrollIng(float scrollX, boolean lunch) {
        if (mConfiguration == 1 && lunch)
            return;
        if (mConfiguration == 2 && !lunch)
            return;
        if (mConfiguration == 1)
            scrollTextView(scrollX, false);
        else if (mConfiguration == 2)
            scrollTextView(scrollX, true);
    }

    public void onModuleSelected(int ix) {
        if (mListener != null)
            mListener.onModuleViewSelected(ix);
    }

    public void initializeDrawables(Context context) {
        int numDrawIds = TEXT_IDS.length;

        if (!PhotoSphereHelper.hasLightCycleCapture(context)) {
            --numDrawIds;
        }
        // Always decrement one because of GCam.
        --numDrawIds;

        String[] drawids = new String[numDrawIds];
        int[] moduleids = new int[numDrawIds];
        int ix = 0;
        for (int i = 0; i < TEXT_IDS.length; i++) {
            if (i == LIGHTCYCLE_MODULE_INDEX
                    && !PhotoSphereHelper.hasLightCycleCapture(context)) {
                continue; // not enabled, so don't add to UI
            }
            if (i == GCAM_MODULE_INDEX) {
                continue; // don't add to UI
            }
            moduleids[ix] = FRAMT_MODULE_INDEX[i];
            drawids[ix++] = mActivity.getString(TEXT_IDS[FRAMT_MODULE_INDEX[i]]);
        }
        setIds(moduleids, drawids);
    }

    public void setIds(int[] moduleids, String[] drawids) {
        mDrawIds = drawids;
    }

    public void setCurrentIndex(int i) {
        if (i < 0 || i > MODULE_NUMBER_SURPORT-1)
            return;
        mLastIndex = framtModuleIndex(i) - 1;
        mNextIndex = framtModuleIndex(i) + 1;
        if (mCameraMoudleIndex != i)
            mCameraMoudleIndex = i;
        scrollTextViewBound(mConfiguration == 2);
    }

    public void setSwitchListener(ModuleSwitchViewListener l) {
        mListener = l;
    }

    @Override
    protected void onFinishInflate() {
        // TODO Auto-generated method stub
        super.onFinishInflate();
        mTextViewList = new ArrayList<TextView>();
        mTextViewList.add((TextView) findViewById(R.id.last_switch_textview));
        mTextViewList.add((TextView) findViewById(R.id.current_switch_textview));
        if(MODULE_NUMBER_SURPORT >= 3) {
            mTextViewList.add((TextView) findViewById(R.id.next_switch_textview));
        }
        for (int i = 0; i < mTextViewList.size(); i++) {
            mTextViewList.get(i).setText(mDrawIds[i]);
            if (mLocalLanguage.endsWith("en") && mSystemTextScale > 1.0) {
                mTextViewList.get(i).setTextScaleX(1 / mSystemTextScale);
            }
        }
        mDelTextView = (TextView)findViewById(R.id.del_switch_textview);
        mDelTextView.setText("●");
        mDelTextView.setVisibility(VISIBLE);
        //start by liweiping 20140609 for bug141
        for(TextView v : mTextViewList){
            v.setOnClickListener(myOncClickListener);
        }
        //end by liweiping 20140609 for bug141
    }
    //start by liweiping 20140609 for bug141
    OnClickListener myOncClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            TextView tv = (TextView) v;
            String title = tv.getText().toString(); 
            if(title.equals(getResources().getString(TEXT_IDS[0]))){
                onModuleSelected(FRAMT_MODULE_INDEX[1]);
            }else if(title.equals(getResources().getString(TEXT_IDS[1]))){
                onModuleSelected(FRAMT_MODULE_INDEX[0]);
            }else if(title.equals(getResources().getString(TEXT_IDS[2]))){
                onModuleSelected(FRAMT_MODULE_INDEX[2]);
            }
        }
    };
    //end by liweiping 20140609 for bug141

    private float midX = 0;
    private float midY = 0;
    public void onLayoutTextView(boolean changed, int l, int t, int r, int b) {
        if (mModuleChange) {
            return;
        }
        this.layout(l, t, r, b);
        // SPRD: bug 258455
        clearMove();
        int orientation = getResources().getConfiguration().orientation;
        int rotation = CameraUtil.getDisplayRotation((Activity) getContext());
        Log.d(TAG, "onLayoutTextView rotation == " + rotation
                + " && mConfiguration = " + mConfiguration);
        r = r - l;
        b = b - t;
        l = 0;
        t = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            v.layout(l, t, r, b);
        }
        setOrientation(rotation);

        Rect shutter = new Rect();
        boolean lunch = (r > b) ? true : false;

        int laignText = (mSystemTextScale == (float)1.3)?3:(
                (mSystemTextScale == (float)1.15)?2:(
                        (mSystemTextScale == (float)1.0)?1:0));
        int laignLanguage = (mLocalLanguage.equals("en"))?(
                (laignText == 3)?0:((laignText == 2)?3:(
                        (laignText == 1)?6:5))):0;

        center(mDelTextView, l, t, r, b, orientation, rotation, shutter,FOOT_HIGHT);
        midX = shutter.left + shutter.width()/2;
        if(rotation == 270) {
            midX = midX + shutter.width()/2+ laignText+laignLanguage;
        } else if(rotation == 90) {
            midX = midX - shutter.width()/2- laignText-laignLanguage;
        }
        midY = shutter.top + shutter.height()/2;
        for (int i = 0; i < mTextViewList.size(); i++) {
            center(mTextViewList.get(i), l, t, r, b, orientation, rotation, shutter,FOOT_HIGHT);
            if (rotation == 90) {
                mTextLength[i] = midY+(0-i) * TEXT_VIEW_WITH - mTextViewList.
                        get(i).getHeight() / 2;
            } else if (rotation == 0) {
                mTextLength[i] = midX+(i) * TEXT_VIEW_WITH - mTextViewList.
                        get(i).getWidth() / 2;
            } else if (rotation == 270) {
                mTextLength[i] = midY+(i) * TEXT_VIEW_WITH - mTextViewList.
                        get(i).getHeight() / 2;
            } else if (rotation == 180) {
                mTextLength[i] = midX + (0 - i) * TEXT_VIEW_WITH - mTextViewList.
                        get(i).getWidth() / 2;
            }
        }
        updatTextView(rotation);
        scrollTextViewBound(lunch);
    }

    public void updatTextView(int orentation) {

        switch (orentation) {
            case 0:
                for (int i = 0; i < mTextViewList.size(); i++) {
                    mTextViewList.get(i).setX(mTextLength[i]);
                    mTextViewList.get(i).setY(midY+5);
                }
                break;
            case 180:
                for (int i = 0; i < mTextViewList.size(); i++) {
                    mTextViewList.get(i).setX(mTextLength[i]);
                    mTextViewList.get(i).setY(midY -mTextViewList.
                            get(0).getHeight()-5);
                }
                break;
            case 90:
                for (int i = 0; i < mTextViewList.size(); i++) {
                    mTextViewList.get(i).setY(mTextLength[i]);
                    mTextViewList.get(i).setX(midX + 5 -
                            (float) ((mTextViewList.get(i).getWidth() -
                            mTextViewList.get(0).getWidth()) / 2));
                }
                break;
            case 270:
                for (int i = 0; i < mTextViewList.size(); i++) {
                    mTextViewList.get(i).setY(mTextLength[i]);
                    mTextViewList.get(i).setX(midX-mTextViewList.
                            get(i).getWidth() - 5 + (float)((mTextViewList.get(i).getWidth() -
                                    mTextViewList.get(0).getWidth()) / 2));
                }
                break;
        }
    }

    public void scrollTextView(float length, boolean lunch) {
        length = isEnableScrollText(length,(length<=0));
        moveTextView(lunch, length / SCROLL_RAT);

    }

    public void scrollBack(boolean lunch) {
        if(mTextViewList.size() <= 0) return;
        float length = 0;
        length = (lunch)?mLastPosation - mTextViewList.get(0).getY():
                mLastPosation - mTextViewList.get(0).getX();
        moveTextView(lunch, length);
    }

    public boolean isEnableSwitch(boolean left) {
        left = (mOrientation == 90 || mOrientation == 180)?!left:left;
        if (mLastIndex < 0 && !left) {
            return false;
        }
        if (mNextIndex > MODULE_NUMBER_SURPORT-1 && left)
            return false;
        return true;
    }

    public float isEnableScrollText(float length, boolean left) {
        if(length == 0 )return 0;
        left = (mOrientation == 90 || mOrientation == 180)?!left:left;
        float bound = (mOrientation == 0 || mOrientation == 180)?
                mTextViewList.get(0).getX():mTextViewList.get(0).getY();
        if (mLastIndex < 0 && !left) {
            if(mOrientation == 0 || mOrientation == 270 ) {
                if(mLastPosation <= bound+length) {
                    scrollEnd(0);
                    return 0;
                }
            } else if(mOrientation == 90 || mOrientation == 180) {
                if(mLastPosation >= bound+length) {
                    scrollEnd(0);
                    return 0;
                }
            }
        }
        if (mNextIndex > MODULE_NUMBER_SURPORT-1 && left) {
            if(mOrientation == 0 || mOrientation == 270 ) {
                if(mLastPosation >= bound+length) {
                    scrollEnd(0);
                    return 0;
                }
            } else if (mOrientation == 90 || mOrientation == 180) {
                if(mLastPosation <= bound+length) {
                    scrollEnd(0);
                    return 0;
                }
            }
        }
        return length;
    }
    public void scrollTextViewBound(boolean lunch) {
        // SPRD: bug 258455
        if (mTextLength == null)
            return;
        if(mTextViewList.size() <=0) return;
        float length = mTextLength[FRAMT_MODULE_INDEX[mCameraMoudleIndex]]
                - (lunch ? mTextViewList.get(0).getY() : mTextViewList.get(0)
                        .getX()-(float)((mTextViewList.get(FRAMT_MODULE_INDEX[mCameraMoudleIndex]).getWidth() -
                                mTextViewList.get(0).getWidth()) / 2));

        moveTextView(lunch, -length);
        mLastPosation = (lunch)?mTextViewList.get(0).getY() :
                mTextViewList.get(0).getX();
    }

    public void moveTextView(boolean lunch, float length) {
        Log.d(TAG, "movoTextView lunch == " + lunch +
                " && length == " + length);
        if (lunch) {
            for (int i = 0; i < mTextViewList.size(); i++)
                mTextViewList.get(i).setY(mTextViewList.get(i)
                        .getY() + length);
        } else {
            for (int i = 0; i < mTextViewList.size(); i++) {
                mTextViewList.get(i).setX(mTextViewList.get(i).
                        getX() + length);
            }
        }
    }

    public void clearMove() {
        for (int i = 0; i < mTextViewList.size(); i++) {
            mTextViewList.get(i).setY(midY);
            mTextViewList.get(i).setX(midX);
        }
    }

    public boolean isLayoutFinish() {
        if (mIsLayoutFinish)
            return true;
        TextView tmp = (TextView) mActivity
                .findViewById(R.id.last_switch_textview);
        if (tmp != null)
            mIsLayoutFinish = true;
        if (mIsLayoutFinish)
            return true;
        return false;
    }

    public void setmConfiguration(int configuration) {
        mConfiguration = configuration;
    }

    // @{ SPRD: bug 258455 begin
    public void setViewVisibility(int visibal) {
        mVisibility = !(visibal == INVISIBLE);
        if (mTextViewList != null && mTextViewList.size() > 0) {
            for (int i = 0; i < mTextViewList.size(); i++)
                mTextViewList.get(i).setVisibility(visibal);
        }
        if(mDelTextView != null)
            mDelTextView.setVisibility(visibal);
    } // SPRD: bug 258455 end @}

    public void setViewVisibility(int visibal,boolean enable) {
        if (mTextViewList != null && mTextViewList.size() > 0) {
            for (int i = 0; i < mTextViewList.size(); i++)
                mTextViewList.get(i).setVisibility(visibal);
        }
        if(mDelTextView != null)
            mDelTextView.setVisibility(visibal);
    }

    private static class MemeoryTotal {
        private static final long MINX_MEMORY_FOR_PANORAMA = 256 * 1024 * 1024;

        private static MemeoryTotal mMemeoryTotal;
        private static boolean isGreateMemory = false;

        private MemeoryTotal() {
            long startTime = SystemClock.uptimeMillis();
            long iMemorySize = 0;
            try {
                Class ownerProcess = Class.forName("android.os.Process");
                Method getTotalMemory = ownerProcess.getMethod("getTotalMemory", null);
                iMemorySize = (Long) getTotalMemory.invoke(null, null);
                isGreateMemory = iMemorySize > MINX_MEMORY_FOR_PANORAMA;
            } catch (Exception e) {
                Log.d(TAG, "invokeStaticGetTotalMemory exception e = " + e);
            }
            Log.d(TAG, "take up time : "+(SystemClock.uptimeMillis() - startTime));
        }

        protected static boolean isGreateMemory() {
            if (mMemeoryTotal == null) {
                mMemeoryTotal = new MemeoryTotal();

            }
            return isGreateMemory;
        }
    }

    private int framtModuleIndex(int moduleindex) {
        for(int i=0;i<=MODULE_NUMBER_SURPORT;i++) {
            if(FRAMT_MODULE_INDEX[i] == moduleindex)
                return i;
        }
        return 0;
    }
    private void center(View v, int l, int t, int r, int b, int orientation, int rotation, Rect result,int foot) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        int tw = lp.leftMargin + v.getMeasuredWidth() + lp.rightMargin;
        int th = lp.topMargin + v.getMeasuredHeight() + lp.bottomMargin;
        switch (rotation) {
        case 0:
            // phone portrait; controls bottom
            result.left = (r + l) / 2 - tw / 2 + lp.leftMargin;
            result.right = (r + l) / 2 + tw / 2 - lp.rightMargin;
            result.bottom = b - lp.bottomMargin - foot;
            result.top = b - th + lp.topMargin - foot;
            break;
        case 90:
            // phone landscape: controls right
            result.right = r - 8 - lp.rightMargin -foot;
            result.left = r - 8 - tw + lp.leftMargin - foot;
            result.top = (b + t) / 2 - th / 2 + lp.topMargin;
            result.bottom = (b + t) / 2 + th / 2 - lp.bottomMargin;
            break;
        case 180:
            // phone upside down: controls top
            result.left = (r + l) / 2 - tw / 2 + lp.leftMargin;
            result.right = (r + l) / 2 + tw / 2 - lp.rightMargin;
            result.top = t + lp.topMargin +foot;
            result.bottom = t + th - lp.bottomMargin + foot;
            break;
        case 270:
            // reverse landscape: controls left
            result.left = l + 8 + lp.leftMargin + foot;
            result.right = l + tw + 8 - lp.rightMargin + foot;
            result.top = (b + t) / 2 - th / 2 + lp.topMargin;
            result.bottom = (b + t) / 2 + th / 2 - lp.bottomMargin;
            break;
        }
        v.layout(result.left, result.top, result.right, result.bottom);
        v.setRotation(360-rotation);
    }

    private void translateAnimat(float length) {
        final boolean sIsLeft = (mOrientation == 90 || mOrientation == 180) ? (length > 0)
                : (length <= 0);
        float lengthOf = mTextLength[0]
                - ((mConfiguration == 2) ? mTextViewList.get(
                        sIsLeft ? mNextIndex : mLastIndex).getY()
                        : mTextViewList.get(sIsLeft ? mNextIndex : mLastIndex)
                                .getX())
                - (mTextViewList.get(sIsLeft ? mNextIndex : mLastIndex)
                        .getWidth() - mTextViewList.get(0).getWidth()) / 2;
        final ArrayList<TranslateAnimation> animatList = new ArrayList<TranslateAnimation>();
        for (int i = 0; i < mTextViewList.size(); i++) {
            animatList.add((mConfiguration == 2) ? (new TranslateAnimation(0,
                    0, 0, lengthOf)) : (new TranslateAnimation(0, lengthOf, 0,
                    0)));
            animatList.get(i).setDuration(200);
        }
        if (animatList.size() > 0 && animatList.get(0) != null) {
            animatList.get(0).setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    setCurrentIndex((sIsLeft) ? FRAMT_MODULE_INDEX[mNextIndex]
                            : FRAMT_MODULE_INDEX[mLastIndex]);
                    onModuleSelected(mCameraMoudleIndex);
                    mStopScroll = true;
                    mModuleChange = false;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            for (int i = 0; i < mTextViewList.size(); i++) {
                mTextViewList.get(i).startAnimation(animatList.get(i));
            }
        }
    }

}
