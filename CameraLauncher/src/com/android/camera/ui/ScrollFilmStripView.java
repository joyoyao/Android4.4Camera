package com.android.camera.ui;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.camera.ModuleSwitcherView;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;


public class ScrollFilmStripView extends FilmStripView {

    private ModuleSwitcherView mModuleSwitcherView;


    public ScrollFilmStripView(Context context) {
        super(context);
    }

    public ScrollFilmStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollFilmStripView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(inCameraFullscreen()) {
            mModuleSwitcherView.onTouchEvent(ev);
            return true;
        }
        else
            return super.onTouchEvent(ev);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
        int orientation = getResources().getConfiguration().orientation;
        mModuleSwitcherView.setmConfiguration(orientation);
    }

    // @{ SPRD: bug 258455 begin
    public void setTextViewVisibility(int visibility) {
        if (mModuleSwitcherView != null) {
            mModuleSwitcherView.setViewVisibility(visibility);
            mModuleSwitcherView.setVisibility(visibility);
            mModuleSwitcherView.setEnabled(visibility == VISIBLE);
        }
    } // SPRD: bug 258455 end

    // SPRD: CameraActivity use this method
    public void setModuleSwitcherView(ModuleSwitcherView v) {
        mModuleSwitcherView = v;
        int orientation = getResources().getConfiguration().orientation;
        // SPRD: reset the screen configuration
        mModuleSwitcherView.setmConfiguration(orientation);
    }
}
