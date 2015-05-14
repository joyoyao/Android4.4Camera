package com.android.camera.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.android.camera.CameraHolder;
import com.android.camera2.R;

public class VideoControlsTop extends RotatableLayout {
    private static final String TAG = "Video_Controls_Top";

    private View mSwitchButton;
    private View mRecordingTime;
    private View mFlashButton;
    private View mTimeLapseLabel;
    public VideoControlsTop(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMeasureAllChildren(true);
    }

    public VideoControlsTop(Context context) {
        super(context);
        setMeasureAllChildren(true);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mSwitchButton = findViewById(R.id.btn_switch);
        if(CameraHolder.instance().getCameraInfo().length < 2){
            mSwitchButton.setVisibility(View.INVISIBLE);
        }
        mRecordingTime = findViewById(R.id.recording_time);
        mTimeLapseLabel = findViewById(R.id.time_lapse_label);
        mFlashButton = findViewById(R.id.btn_flash);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    public void golayout(int l,int t,int r,int b) {
        this.layout(l,t,r,b);
        int orientation = getResources().getConfiguration().orientation;
        int size = getResources().getDimensionPixelSize(
                R.dimen.camera_controls_size);
        int rotation = getUnifiedRotation();
        // As l,t,r,b are positions relative to parents, we need to convert them
        // to child's coordinates
        r = r - l;
        b = b - t;
        l = 0;
        t = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            v.layout(l, t, r, b);
        }
        Rect shutter = new Rect();
        if (rotation == 0) {
            topRight(mSwitchButton, l, t, r, b, 0);
            topLeft(mFlashButton, l, t, r, b, 0);
        } else if (rotation == 90) {
            topRight(mSwitchButton, l, t, r, b, 150);
            topLeft(mFlashButton, l, t, r, b, 0);
        } else if (rotation == 180) {
            bottomRight(mSwitchButton, l, t, r, b, 0);
            bottomLeft(mFlashButton, l, t, r, b, 0);
        } else {
            topRight(mSwitchButton, l, t, r, b, 0);
            topLeft(mFlashButton, l, t, r, b, 150);
        }
        center(mRecordingTime, l, t + 25, r, b, orientation, rotation, shutter);
        /** SPRD: fixbug255454 set RecordingTime view at the top of TimeLapseLabel view */
        center(mTimeLapseLabel, l, t + mRecordingTime.getMeasuredHeight() + 25, r, b
                - mRecordingTime.getMeasuredHeight(), orientation, rotation, shutter);
        if (size > 0) {
            // restrict controls to size
            switch (rotation) {
                case 0:
                case 180:
                    l = (l + r - size) / 2;
                    r = l + size;
                    break;
                case 90:
                case 270:
                    t = (t + b - size) / 2;
                    b = t + size;
                    break;
            }
        }
    }

    private void center(View v, int l, int t, int r, int b, int orientation, int rotation, Rect result) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        int tw = lp.leftMargin + v.getMeasuredWidth() + lp.rightMargin;
        int th = lp.topMargin + v.getMeasuredHeight() + lp.bottomMargin;
        switch (rotation) {
        case 0:
        case 90:
        case 270:
            // phone portrait; controls top
            result.left = (r + l) / 2 - tw / 2 + lp.leftMargin +
                ((rotation == 0)?0:((rotation == 90)?-75:75));
            result.right = (r + l) / 2 + tw / 2 - lp.rightMargin +
                ((rotation == 0)?0:((rotation == 270)?75:-75));
            result.top = t + lp.topMargin;
            result.bottom = t + th - lp.bottomMargin;
            break;

        case 180:
            // phone upside down: controls bottom
            result.left = (r + l) / 2 - tw / 2 + lp.leftMargin;
            result.right = (r + l) / 2 + tw / 2 - lp.rightMargin;
            result.bottom = b - lp.bottomMargin;
            result.top = b - th + lp.topMargin;
            break;

        }
        v.layout(result.left, result.top, result.right, result.bottom);
    }

    private void topRight(View v, int l, int t, int r, int b, int m) {
        // first 0 0 720 1280
        // after 592 32 688 128
        // layout using the specific margins; the rotation code messes up the
        // others
        int mt = 0;
        int mr = m;
        v.layout(r - v.getMeasuredWidth() - mr, t + mt, r - mr,
                t + mt + v.getMeasuredHeight());
        // v.layout(0,500,96,596);
    }

    private void topLeft(View v, int l, int t, int r, int b , int m) {
        // need to do
        int mt = 0;
        int mr = m;
        v.layout(l + mr, t + mt, l + mr + v.getMeasuredWidth(),
                t + mt + v.getMeasuredHeight());
    }

    private void bottomLeft(View v, int l, int t, int r, int b , int m) {
        int mt = 0;
        int mr = m;
        v.layout(l + mr, b - v.getMeasuredHeight() - mr,
                l + mr + v.getMeasuredWidth(), b - mr);
    }

    private void bottomRight(View v, int l, int t, int r, int b , int m) {
        int mt = 0;
        int mr = m;
        v.layout(r - v.getMeasuredWidth() - mr, b - v.getMeasuredHeight() - mr,
                r - mr, b - mr);
    }

}
