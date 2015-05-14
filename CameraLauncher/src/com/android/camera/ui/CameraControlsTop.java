package com.android.camera.ui;


import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera.CameraInfo;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.util.Log;

import com.android.camera.CameraHolder;
import com.android.camera2.R;
public class CameraControlsTop extends RotatableLayout {


    private static final String TAG = "CAM_Controls_Top";

    private View mSwitchButton;
    private View mHDRButton;
    private View mFlashButton;
    public CameraControlsTop(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMeasureAllChildren(true);
    }

    public CameraControlsTop(Context context) {
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
        mHDRButton = findViewById(R.id.btn_hdr);
        mFlashButton = findViewById(R.id.btn_flash);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    public void golayout(int l, int t, int r, int b) {
        this.layout(l,t,r,b);
        int orientation = getResources().getConfiguration().orientation;
        int size = getResources().getDimensionPixelSize(R.dimen.camera_controls_size);
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
        if(rotation == 0 ){
        topRight(mSwitchButton, l, t, r, b);
        topLeft(mFlashButton, l, t, r, b);
        }else if (rotation == 90){
        topLeft(mSwitchButton, l, t, r, b);
        bottomLeft(mFlashButton, l, t, r, b);
        }else if (rotation == 180){
        bottomLeft(mSwitchButton, l, t, r, b);
        bottomRight(mFlashButton, l, t, r, b);
        }else {
        bottomRight(mSwitchButton, l, t, r, b);
        topRight(mFlashButton, l, t, r, b);
        }
        center(mHDRButton, l, t, r, b, orientation, rotation, shutter);
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
            // phone portrait; controls top
            result.left = (r + l) / 2 - tw / 2 + lp.leftMargin;
            result.right = (r + l) / 2 + tw / 2 - lp.rightMargin;
            result.top = t + lp.topMargin;
            result.bottom = t + th - lp.bottomMargin;

            break;
        case 90:
            // phone landscape: controls left
            result.left = l + lp.leftMargin;
            result.right = l + tw - lp.rightMargin;
            result.top = (b + t) / 2 - th / 2 + lp.topMargin;
            result.bottom = (b + t) / 2 + th / 2 - lp.bottomMargin;

            break;
        case 180:
            // phone upside down: controls bottom
            result.left = (r + l) / 2 - tw / 2 + lp.leftMargin;
            result.right = (r + l) / 2 + tw / 2 - lp.rightMargin;
            result.bottom = b - lp.bottomMargin;
            result.top = b - th + lp.topMargin;
            break;
        case 270:
            // reverse landscape: controls right
            result.right = r - lp.rightMargin;
            result.left = r - tw + lp.leftMargin;
            result.top = (b + t) / 2 - th / 2 + lp.topMargin;
            result.bottom = (b + t) / 2 + th / 2 - lp.bottomMargin;
            break;
        }
        v.layout(result.left, result.top, result.right, result.bottom);
    }

    private void center(View v, Rect other, int rotation) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        int tw = lp.leftMargin + v.getMeasuredWidth() + lp.rightMargin;
        int th = lp.topMargin + v.getMeasuredHeight() + lp.bottomMargin;
        int cx = (other.left + other.right) / 2;
        int cy = (other.top + other.bottom) / 2;
        v.layout(cx - tw / 2 + lp.leftMargin,
                cy - th / 2 + lp.topMargin,
                cx + tw / 2 - lp.rightMargin,
                cy + th / 2 - lp.bottomMargin);
    }

    private void toLeft(View v, Rect other, int rotation) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        int tw = lp.leftMargin + v.getMeasuredWidth() + lp.rightMargin;
        int th = lp.topMargin + v.getMeasuredHeight() + lp.bottomMargin;
        int cx = (other.left + other.right) / 2;
        int cy = (other.top + other.bottom) / 2;
        int l = 0, r = 0, t = 0, b = 0;
        switch (rotation) {
        case 0:
            // portrait, to left of anchor at bottom
            l = other.left - tw + lp.leftMargin;
            r = other.left - lp.rightMargin;
            t = cy - th / 2 + lp.topMargin;
            b = cy + th / 2 - lp.bottomMargin;
            break;
        case 90:
            // phone landscape: below anchor on right
            l = cx - tw / 2 + lp.leftMargin;
            r = cx + tw / 2 - lp.rightMargin;
            t = other.bottom + lp.topMargin;
            b = other.bottom + th - lp.bottomMargin;
            break;
        case 180:
            // phone upside down: right of anchor at top
            l = other.right + lp.leftMargin;
            r = other.right + tw - lp.rightMargin;
            t = cy - th / 2 + lp.topMargin;
            b = cy + th / 2 - lp.bottomMargin;
            break;
        case 270:
            // reverse landscape: above anchor on left
            l = cx - tw / 2 + lp.leftMargin;
            r = cx + tw / 2 - lp.rightMargin;
            t = other.top - th + lp.topMargin;
            b = other.top - lp.bottomMargin;
            break;
        }
        v.layout(l, t, r, b);
    }

    private void toRight(View v, Rect other, int rotation) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        int tw = lp.leftMargin + v.getMeasuredWidth() + lp.rightMargin;
        int th = lp.topMargin + v.getMeasuredHeight() + lp.bottomMargin;
        int cx = (other.left + other.right) / 2;
        int cy = (other.top + other.bottom) / 2;
        int l = 0, r = 0, t = 0, b = 0;
        switch (rotation) {
        case 0:
            l = other.right + lp.leftMargin;
            r = other.right + tw - lp.rightMargin;
            t = cy - th / 2 + lp.topMargin;
            b = cy + th / 2 - lp.bottomMargin;
            break;
        case 90:
            l = cx - tw / 2 + lp.leftMargin;
            r = cx + tw / 2 - lp.rightMargin;
            t = other.top - th + lp.topMargin;
            b = other.top - lp.bottomMargin;
            break;
        case 180:
            l = other.left - tw + lp.leftMargin;
            r = other.left - lp.rightMargin;
            t = cy - th / 2 + lp.topMargin;
            b = cy + th / 2 - lp.bottomMargin;
            break;
        case 270:
            l = cx - tw / 2 + lp.leftMargin;
            r = cx + tw / 2 - lp.rightMargin;
            t = other.bottom + lp.topMargin;
            b = other.bottom + th - lp.bottomMargin;
            break;
        }
        v.layout(l, t, r, b);
    }

    private void topRight(View v, int l, int t, int r, int b) {
        //first 0 0 720 1280
        //after 592 32 688 128
        // layout using the specific margins; the rotation code messes up the others
        int mt = 0;
        int mr = 0;
        v.layout(r - v.getMeasuredWidth() - mr, t + mt, r - mr, t + mt + v.getMeasuredHeight());
//        v.layout(0,500,96,596);
    }
    private void topLeft(View v, int l, int t, int r, int b){
        //need to do
        int mt = 0;
        int mr = 0;
        v.layout(l + mr, t + mt, l + mr + v.getMeasuredWidth(), t + mt + v.getMeasuredHeight());
    }
    private void bottomLeft(View v, int l, int t, int r, int b){
        int mt = 0;
        int mr = 0;
        v.layout(l + mr,b - v.getMeasuredHeight() - mr, l + mr + v.getMeasuredWidth(), b - mr);
    }
    private void bottomRight(View v, int l, int t, int r, int b){
        int mt = 0;
        int mr = 0;
        v.layout(r - v.getMeasuredWidth() - mr,b - v.getMeasuredHeight() - mr,  r - mr, b - mr);
    }

}
