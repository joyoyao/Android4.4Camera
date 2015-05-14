package com.android.camera.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.camera2.R;
import android.util.Log;

public class VideoControls extends RotatableLayout {
    private static final String TAG = "VIDEO_Controls";

    private View mBackgroundView;
    private View mShutterControl;
    private View mSwitcher;
    private View mMenu;
    private View mIndicators;
    private View mPreview;
    private View mCameraSetting;
    private View mVideoCapture;

    public VideoControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMeasureAllChildren(true);
    }

    public VideoControls(Context context) {
        super(context);
        setMeasureAllChildren(true);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mBackgroundView = findViewById(R.id.blocker);
        mSwitcher = findViewById(R.id.camera_switcher);
        mShutterControl = findViewById(R.id.video_shutter_control);
        mMenu = findViewById(R.id.menu);
        mIndicators = findViewById(R.id.on_screen_indicators);
        mPreview = findViewById(R.id.preview_thumb);
        mCameraSetting = findViewById(R.id.btn_camera_setting);
        mVideoCapture = findViewById(R.id.video_capture);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int orientation = getResources().getConfiguration().orientation;
        int size = getResources().getDimensionPixelSize(
                R.dimen.camera_controls_size);
        int rotation = getUnifiedRotation();
        adjustBackground();
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
            bottomLeft(mPreview, l, t, r, b);
            bottomLeft(mVideoCapture, l, t, r, b);
            bottomRight(mCameraSetting, l, t, r, b);
            topRight(mIndicators, l, t, r, b, 90, 0);
        } else if (rotation == 90) {
            bottomRight(mPreview, l, t, r, b);
            bottomRight(mVideoCapture, l, t, r, b);
            topRight(mCameraSetting, l, t, r, b);
            topLeft(mIndicators, l, t, r, b, 0, 90);
        } else if (rotation == 180) {
            topRight(mPreview, l, t, r, b);
            topRight(mVideoCapture, l, t, r, b);
            topLeft(mCameraSetting, l, t, r, b);
            bottomRight(mIndicators, l, t, r, b, 90, 0);
        } else {
            topLeft(mPreview, l, t, r, b);
            topLeft(mVideoCapture, l, t, r, b);
            bottomLeft(mCameraSetting, l, t, r, b);
            topRight(mIndicators, l, t, r, b, 0, 90);
        }
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
        center(mShutterControl, l, t, r, b, orientation, rotation, shutter);
        center(mBackgroundView, l, t, r, b, orientation, rotation, new Rect());
        toLeft(mSwitcher, shutter, rotation);
        toRight(mMenu, shutter, rotation);
        // toRight(mIndicators, shutter, rotation);
        View retake = findViewById(R.id.btn_retake);
        if (retake != null) {
            center(retake, shutter, rotation);
            View cancel = findViewById(R.id.btn_cancel);
            toLeft(cancel, shutter, rotation);
            View done = findViewById(R.id.btn_done);
            toRight(done, shutter, rotation);
        }
    }

    private void center(View v, int l, int t, int r, int b, int orientation,
            int rotation, Rect result) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v
                .getLayoutParams();
        int tw = lp.leftMargin + v.getMeasuredWidth() + lp.rightMargin;
        int th = lp.topMargin + v.getMeasuredHeight() + lp.bottomMargin;
        switch (rotation) {
            case 0:
                // phone portrait; controls bottom
                result.left = (r + l) / 2 - tw / 2 + lp.leftMargin;
                result.right = (r + l) / 2 + tw / 2 - lp.rightMargin;
                result.bottom = b - lp.bottomMargin;
                result.top = b - th + lp.topMargin;
                break;
            case 90:
                // phone landscape: controls right
                result.right = r - lp.rightMargin;
                result.left = r - tw + lp.leftMargin;
                result.top = (b + t) / 2 - th / 2 + lp.topMargin;
                result.bottom = (b + t) / 2 + th / 2 - lp.bottomMargin;
                break;
            case 180:
                // phone upside down: controls top
                result.left = (r + l) / 2 - tw / 2 + lp.leftMargin;
                result.right = (r + l) / 2 + tw / 2 - lp.rightMargin;
                result.top = t + lp.topMargin;
                result.bottom = t + th - lp.bottomMargin;
                break;
            case 270:
                // reverse landscape: controls left
                result.left = l + lp.leftMargin;
                result.right = l + tw - lp.rightMargin;
                result.top = (b + t) / 2 - th / 2 + lp.topMargin;
                result.bottom = (b + t) / 2 + th / 2 - lp.bottomMargin;
                break;
        }
        v.layout(result.left, result.top, result.right, result.bottom);
    }

    private void center(View v, Rect other, int rotation) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v
                .getLayoutParams();
        int tw = lp.leftMargin + v.getMeasuredWidth() + lp.rightMargin;
        int th = lp.topMargin + v.getMeasuredHeight() + lp.bottomMargin;
        int cx = (other.left + other.right) / 2;
        int cy = (other.top + other.bottom) / 2;
        v.layout(cx - tw / 2 + lp.leftMargin, cy - th / 2 + lp.topMargin, cx
                + tw / 2 - lp.rightMargin, cy + th / 2 - lp.bottomMargin);
    }

    private void toLeft(View v, Rect other, int rotation) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v
                .getLayoutParams();
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
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v
                .getLayoutParams();
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
        // layout using the specific margins; the rotation code messes up the
        // others
        int mt = getContext().getResources().getDimensionPixelSize(
                R.dimen.capture_margin_top);
        int mr = getContext().getResources().getDimensionPixelSize(
                R.dimen.capture_margin_right);
        v.layout(r - v.getMeasuredWidth() - mr, t + mt, r - mr,
                t + mt + v.getMeasuredHeight());
        // v.layout(0,500,96,596);
    }

    private void topRight(View v, int l, int t, int r, int b, int mt, int mr) {

        v.layout(r - v.getMeasuredWidth() - mr, t + mt, r - mr,
                t + mt + v.getMeasuredHeight());
        // v.layout(0,500,96,596);
    }

    private void topLeft(View v, int l, int t, int r, int b, int mt, int mr) {
        // need to do

        v.layout(l + mr, t + mt, l + mr + v.getMeasuredWidth(),
                t + mt + v.getMeasuredHeight());
    }

    private void bottomRight(View v, int l, int t, int r, int b, int mt, int mr) {

        v.layout(r - v.getMeasuredWidth() - mr, b - v.getMeasuredHeight() - mt,
                r - mr, b - mt);
    }

    private void topLeft(View v, int l, int t, int r, int b) {
        // need to do
        int mt = getContext().getResources().getDimensionPixelSize(
                R.dimen.capture_margin_top);
        int mr = getContext().getResources().getDimensionPixelSize(
                R.dimen.capture_margin_right);
        v.layout(l + mr, t + mt, l + mr + v.getMeasuredWidth(),
                t + mt + v.getMeasuredHeight());
    }

    private void bottomLeft(View v, int l, int t, int r, int b) {
        int mt = getContext().getResources().getDimensionPixelSize(
                R.dimen.capture_margin_top);
        int mr = getContext().getResources().getDimensionPixelSize(
                R.dimen.capture_margin_right);
        v.layout(l + mr, b - v.getMeasuredHeight() - mr,
                l + mr + v.getMeasuredWidth(), b - mr);
    }

    private void bottomRight(View v, int l, int t, int r, int b) {
        int mt = getContext().getResources().getDimensionPixelSize(
                R.dimen.capture_margin_top);
        int mr = getContext().getResources().getDimensionPixelSize(
                R.dimen.capture_margin_right);
        v.layout(r - v.getMeasuredWidth() - mr, b - v.getMeasuredHeight() - mr,
                r - mr, b - mr);
    }

    private void adjustBackground() {
        int rotation = getUnifiedRotation();
        // remove current drawable and reset rotation
        mBackgroundView.setBackgroundDrawable(null);
        mBackgroundView.setRotationX(0);
        mBackgroundView.setRotationY(0);
        // if the switcher background is top aligned we need to flip the
        // background
        // drawable vertically; if left aligned, flip horizontally
        switch (rotation) {
            case 180:
                mBackgroundView.setRotationX(180);
                break;
            case 270:
                mBackgroundView.setRotationY(180);
                break;
            default:
                break;
        }
        // mBackgroundView.setBackgroundResource(R.drawable.switcher_bg);
        mBackgroundView.setBackgroundColor(Color.rgb(0, 0, 0));
        mBackgroundView.setAlpha(0.3f);
    }

}
