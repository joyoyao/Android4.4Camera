/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.res.Configuration;

import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera.ui.AbstractSettingPopup;
import com.android.camera.ui.CameraControls;
import com.android.camera.ui.CameraRootView;
import com.android.camera.ui.ModuleSwitcher;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.RenderOverlay;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.StoragePathPopup;
import com.android.camera.ui.VideoControls;
import com.android.camera.ui.ZoomRenderer;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.android.camera.util.Tuple;
import com.sprd.camera.AlertDialogPopup;

import java.util.List;

public class VideoUI implements PieRenderer.PieListener,
        PreviewGestures.SingleTapListener,
        PreviewGestures.MultiTapListener,
        CameraRootView.MyDisplayListener,
        SurfaceTextureListener, SurfaceHolder.Callback {

    private static final boolean DEBUG = true;
    private static final String TAG = "CAM_VideoUI";
    private static final int UPDATE_TRANSFORM_MATRIX = 1;
    // module fields
    private CameraActivity mActivity;
    private View mRootView;
    private TextureView mTextureView;
    // An review image having same size as preview. It is displayed when
    // recording is stopped in capture intent.
    private RotateImageView mFlashButton ;
    private RotateImageView mSwitchButton;
    private ImageView mReviewImage;
    private TextView  mReviewImageLoading;
    private View mReviewCancelButton;
    private View mReviewDoneButton;
    private View mReviewRetakeButton;    // SPRD: add retake feature in a video capture intent
    private View mReviewPlayButton;
    private RelativeLayout mVideoShutterControl;
    private ModuleSwitcherView mModuleSwitcherView;
    private ShutterButton mShutterButton;
    private ImageView mPauseButton;
    private ImageView mStopButton;
    private ModuleSwitcher mSwitcher;
    private TextView mRecordingTimeView;
    private LinearLayout mLabelsLinearLayout;
    private View mTimeLapseLabel;
    private RenderOverlay mRenderOverlay;
    private PieRenderer mPieRenderer;
    private VideoMenu mVideoMenu;
    private VideoControls mCameraControls;
    private SettingsPopup mPopup;
    private SettingsPopup mSecondLevelPopup;
    private ZoomRenderer mZoomRenderer;
    private PreviewGestures mGestures;
    private View mMenuButton;
    private OnScreenIndicators mOnScreenIndicators;
    private RotateLayout mRecordingTimeRect;
    private boolean mRecordingStarted = false;
    // SPRD: param for "updateThumbnail()" to set Thumbnail visibility
    private boolean mShowPreviewThumb = true;
    private SurfaceTexture mSurfaceTexture;
    private VideoController mController;
    private int mZoomMax;
    private List<Integer> mZoomRatios;
    // SPRD: bug 256269
    private ImageView mPreviewThumb;
    private View mVideoCapture;
    private View mFlashOverlay;
    private View mCameraSetting;
    private int mNewOrientation = 1;

    private View mPreviewCover;
    private SurfaceView mSurfaceView = null;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mSurfaceTextureUncroppedWidth;
    private float mSurfaceTextureUncroppedHeight;
    private float mAspectRatio = 4f / 3f;
    private Matrix mMatrix = null;
    private final AnimationManager mAnimationManager;
    private boolean mPauseRecording = false ;
    private boolean mSwitchButtonShow = false;
    private boolean mFlashButtonShow = false;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TRANSFORM_MATRIX:
                    setTransformMatrix(mPreviewWidth, mPreviewHeight);
                    break;
                default:
                    break;
            }
        }
    };
    private OnLayoutChangeListener mLayoutListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            int width = right - left;
            int height = bottom - top;
            // Full-screen screennail
            int w = width;
            int h = height;
            if (CameraUtil.getDisplayRotation(mActivity) % 180 != 0) {
                w = height;
                h = width;
            }
            if (mPreviewWidth != width || mPreviewHeight != height) {
                mPreviewWidth = width;
                mPreviewHeight = height;
                onScreenSizeChanged(width, height, w, h);
                setShutterButtonRes((width > height)? Configuration.ORIENTATION_LANDSCAPE :
                        Configuration.ORIENTATION_PORTRAIT);
                showRecordingUI(mRecordingStarted);
            }
        }
    };

    public void showPreviewCover() {
        Log.d(TAG, "show mPreviewCover");
        mPreviewCover.setVisibility(View.VISIBLE);
    }

    public void hidePreviewCover() {
        if (mPreviewCover.getVisibility() != View.GONE) {
            Log.d(TAG, "hide mPreviewCover");
            mPreviewCover.setVisibility(View.GONE);
        }
    }

    private class SettingsPopup extends PopupWindow {
        public SettingsPopup(View popup) {
            this(popup, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }

        // create a popup with particular width and height
        public SettingsPopup(View popup, int width, int height) {
            super(width, height);
            setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setOutsideTouchable(true);
            setFocusable(true);
            if (popup instanceof StoragePathPopup) {
                StoragePathPopup view = (StoragePathPopup) popup;
                view.displayStoragePopup();
            } else {
                popup.setVisibility(View.VISIBLE);
            }
            setContentView(popup);
            showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        }

        public void dismiss(boolean topLevelOnly) {
            super.dismiss();
            popupDismissed();
            showUI();
            mVideoMenu.popupDismissed(topLevelOnly);

            // Switch back into full-screen/lights-out mode after POPUP is dismissed.
            mActivity.setSystemBarsVisibility(false);
        }

        @Override
        public void dismiss() {
            // Called by Framework when touch outside the popup or hit back key
            //dismiss(true);  // @orig
            dismissPopup(true);
        }
    }

    public VideoUI(CameraActivity activity, VideoController controller, View parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;
        mActivity.getLayoutInflater().inflate(R.layout.video_module, (ViewGroup) mRootView, true);
        mPreviewCover = mRootView.findViewById(R.id.preview_cover);
        mTextureView = (TextureView) mRootView.findViewById(R.id.preview_content);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.addOnLayoutChangeListener(mLayoutListener);
        mFlashOverlay = mRootView.findViewById(R.id.flash_overlay);
        mFlashButton = (RotateImageView) mRootView.findViewById(R.id.btn_flash);
        mSwitchButton = (RotateImageView) mRootView.findViewById(R.id.btn_switch);
        mVideoShutterControl = (RelativeLayout) mRootView.findViewById(R.id.video_shutter_control);
        mShutterButton = (ShutterButton) mRootView.findViewById(R.id.shutter_button);
        mPauseButton = (ImageView) mRootView.findViewById(R.id.btn_video_pause);
        mStopButton = (ImageView) mRootView.findViewById(R.id.btn_video_stop);
        mSwitcher = (ModuleSwitcher) mRootView.findViewById(R.id.camera_switcher);
        mSwitcher.setCurrentIndex(ModuleSwitcher.VIDEO_MODULE_INDEX);
        mSwitcher.setSwitchListener(mActivity);
        mModuleSwitcherView = (ModuleSwitcherView) mRootView.findViewById(R.id.module_switch_textview);
        mActivity.setScrollFilmView(mModuleSwitcherView);
        mModuleSwitcherView.setCurrentIndex(ModuleSwitcher.VIDEO_MODULE_INDEX);
        mFlashButton.setVisibility(View.GONE);
        initializeMiscControls();
        initializeControlByIntent();
        initializeOverlay();
        mAnimationManager = new AnimationManager();
    }


    public void initializeSurfaceView() {
        mSurfaceView = new SurfaceView(mActivity);
        ((ViewGroup) mRootView).addView(mSurfaceView, 0);
        mSurfaceView.getHolder().addCallback(this);
    }

    private void initializeControlByIntent() {
        mMenuButton = mRootView.findViewById(R.id.menu);
        mMenuButton.setVisibility(View.GONE); // SPRD: don't show original setting button
        /*mMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPieRenderer != null) {
                    mPieRenderer.showInCenter();
                }
            }
        });*/

        mCameraControls = (VideoControls) mRootView.findViewById(R.id.video_controls);
        mOnScreenIndicators = new OnScreenIndicators(mActivity,
                mRootView.findViewById(R.id.on_screen_indicators));
        mOnScreenIndicators.setVisibility(View.GONE);  // SPRD: don't show original setting button
        mOnScreenIndicators.resetToDefault();
        if (mController.isVideoCaptureIntent()) {
            hideSwitcher();
            mActivity.getLayoutInflater().inflate(R.layout.review_module_control,
                    (ViewGroup) mCameraControls);
            // Cannot use RotateImageView for "done" and "cancel" button because
            // the tablet layout uses RotateLayout, which cannot be cast to
            // RotateImageView.
            mReviewDoneButton = mRootView.findViewById(R.id.btn_done);
            mReviewCancelButton = mRootView.findViewById(R.id.btn_cancel);
            mReviewRetakeButton = mRootView.findViewById(R.id.btn_retake); // SPRD: add retake feature in a video capture intent
            mReviewPlayButton = mRootView.findViewById(R.id.btn_play);
            mReviewImageLoading = (TextView) mRootView.findViewById(R.id.review_image_loading);
            mReviewCancelButton.setVisibility(View.VISIBLE);
            mReviewDoneButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onReviewDoneClicked(v);
                }
            });
            mReviewCancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onReviewCancelClicked(v);
                }
            });
            /* SPRD: add retake feature in a video capture intent @{ */
            mReviewRetakeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onReviewRetakeClicked(v);
                }
            });
            /* @} */
            mReviewPlayButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onReviewPlayClicked(v);
                }
            });
        }
    }

    public void setPreviewSize(int width, int height) {
        if (width == 0 || height == 0) {
            Log.w(TAG, "Preview size should not be 0.");
            return;
        }
        if (width > height) {
            mAspectRatio = (float) width / height;
        } else {
            mAspectRatio = (float) height / width;
        }
        mHandler.sendEmptyMessage(UPDATE_TRANSFORM_MATRIX);
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    public void onScreenSizeChanged(int width, int height, int previewWidth, int previewHeight) {
        setTransformMatrix(width, height);
    }

    private void setTransformMatrix(int width, int height) {
        mMatrix = mTextureView.getTransform(mMatrix);
        int orientation = CameraUtil.getDisplayRotation(mActivity);
        float scaleX = 1f, scaleY = 1f;
        float scaledTextureWidth, scaledTextureHeight;
        if (width > height) {
            scaledTextureWidth = Math.max(width,
                    (int) (height * mAspectRatio));
            scaledTextureHeight = Math.max(height,
                    (int)(width / mAspectRatio));
        } else {
            scaledTextureWidth = Math.max(width,
                    (int) (height / mAspectRatio));
            scaledTextureHeight = Math.max(height,
                    (int) (width * mAspectRatio));
        }

        if (mSurfaceTextureUncroppedWidth != scaledTextureWidth ||
                mSurfaceTextureUncroppedHeight != scaledTextureHeight) {
            mSurfaceTextureUncroppedWidth = scaledTextureWidth;
            mSurfaceTextureUncroppedHeight = scaledTextureHeight;
        }
        scaleX = scaledTextureWidth / width;
        scaleY = scaledTextureHeight / height;
        mMatrix.setScale(scaleX, scaleY, (float) width / 2, (float) height / 2);
        mTextureView.setTransform(mMatrix);

        if (mSurfaceView != null && mSurfaceView.getVisibility() == View.VISIBLE) {
            LayoutParams lp = (LayoutParams) mSurfaceView.getLayoutParams();
            lp.width = (int) mSurfaceTextureUncroppedWidth;
            lp.height = (int) mSurfaceTextureUncroppedHeight;
            lp.gravity = Gravity.CENTER;
            mSurfaceView.requestLayout();
        }
    }

    /**
     * Starts a flash animation
     */
    public void animateFlash() {
        // SPRD: remove animation from customer requirement
        mAnimationManager.startFlashAnimation(mFlashOverlay);
    }

    /**
     * Starts a capture animation
     */
    public void animateCapture() {
        Bitmap bitmap = null;
        if (mTextureView != null) {
            bitmap = mTextureView.getBitmap((int) mSurfaceTextureUncroppedWidth / 2,
                    (int) mSurfaceTextureUncroppedHeight / 2);
        }
        animateCapture(bitmap);
    }

    /**
     * Starts a capture animation
     * @param bitmap the captured image that we shrink and slide in the animation
     */
    public void animateCapture(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "No valid bitmap for capture animation.");
            return;
        }
        ((ImageView) mPreviewThumb).setImageBitmap(bitmap);
        //mAnimationManager.startCaptureAnimation(mPreviewThumb);//do not do the animation
    }

    /**
     * Cancels on-going animations
     */
    public void cancelAnimations() {
        mAnimationManager.cancelAnimations();
    }

    public void hideUI() {
        mCameraControls.setVisibility(View.INVISIBLE);
        mSwitcher.closePopup();
        hideSwitcher();
    }

    public void showUI() {
        mCameraControls.setVisibility(View.VISIBLE);
        if (!mController.isVideoCaptureIntent() && !mRecordingStarted) {
            showSwitcher();
        }
    }

    public boolean arePreviewControlsVisible() {
        return (mCameraControls.getVisibility() == View.VISIBLE);
    }

    public void hideSwitcher() {
        mSwitcher.closePopup();
        // mSwitcher.setVisibility(View.INVISIBLE);
        mActivity.setModuleVisble(View.INVISIBLE);
    }

    public void showSwitcher() {
        // mSwitcher.setVisibility(View.VISIBLE);
        mActivity.setModuleVisble(View.VISIBLE);
    }

    public boolean collapseCameraControls() {
        boolean ret = false;
        if (mPopup != null || mSecondLevelPopup != null) {
            dismissPopup(false);
            ret = true;
        }
        return ret;
    }

    public boolean removeTopLevelPopup() {
        if (mPopup != null || mSecondLevelPopup != null) {
            dismissPopup(true);
            return true;
        }
        return false;
    }

    public void enableCameraControls(boolean enable) {
        if (mGestures != null) {
            mGestures.setZoomOnly(!enable);
        }
        if (mPieRenderer != null && mPieRenderer.showsItems()) {
            mPieRenderer.hide();
        }
        /* SPRD: avoid goto gallery when in recording @{ */
        if (mPreviewThumb != null) {
            mPreviewThumb.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        }
        /* @} */

        /* SPRD: CameraSeting is enable when in recording @{ */
        if (mCameraSetting != null) {
            mCameraSetting.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        }
        /* @} */

        /* SPRD: The button of Camera switch disabled when in recording @{ */
        if (mSwitchButton != null && mSwitchButtonShow) {
            mSwitchButton.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        }
        /* @} */
        if (mFlashButton != null && mFlashButtonShow) {
            mFlashButton.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void initDisplayChangeListener() {
        ((CameraRootView) mRootView).setDisplayChangeListener(this);
    }

    public void removeDisplayChangeListener() {
        ((CameraRootView) mRootView).removeDisplayChangeListener();
    }

    public void overrideSettings(final String... keyvalues) {
        if (mVideoMenu != null) {
            mVideoMenu.overrideSettings(keyvalues);
        }

    }

    /* SPRD:Add for restore @{ */
    public void reloadPreferences(){
        if (mVideoMenu != null) {
            mVideoMenu.reloadPreferences();
        }
    }
    /* @} */

    public void setOrientationIndicator(int orientation, boolean animation) {
        // We change the orientation of the linearlayout only for phone UI
        // because when in portrait the width is not enough.
        // if (mLabelsLinearLayout != null) {
        // if (((orientation / 90) & 1) == 0) {
        // mLabelsLinearLayout.setOrientation(LinearLayout.VERTICAL);
        // } else {
        // mLabelsLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        // }
        // }
        //mRecordingTimeRect.setOrientation(0, animation);
    }

    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    public void hideSurfaceView() {
        mSurfaceView.setVisibility(View.GONE);
        mTextureView.setVisibility(View.VISIBLE);
        setTransformMatrix(mPreviewWidth, mPreviewHeight);
    }

    public void showSurfaceView() {
        mSurfaceView.setVisibility(View.VISIBLE);
        mTextureView.setVisibility(View.GONE);
        setTransformMatrix(mPreviewWidth, mPreviewHeight);
    }

    private void initializeOverlay() {
        mRenderOverlay = (RenderOverlay) mRootView.findViewById(R.id.render_overlay);
        if (mPieRenderer == null) {
            mPieRenderer = new PieRenderer(mActivity);
            mVideoMenu = new VideoMenu(mActivity, this, mPieRenderer);
            mPieRenderer.setPieListener(this);
        }
        mRenderOverlay.addRenderer(mPieRenderer);
        if (mZoomRenderer == null) {
            mZoomRenderer = new ZoomRenderer(mActivity);
        }
        mRenderOverlay.addRenderer(mZoomRenderer);
        if (mGestures == null) {
            //mGestures = new PreviewGestures(mActivity, this, mZoomRenderer, mPieRenderer); // origin
            mGestures = new PreviewGestures(mActivity, this, this, mZoomRenderer, mPieRenderer);
            mRenderOverlay.setGestures(mGestures);
        }
        mGestures.setRenderOverlay(mRenderOverlay);

        // SPRD: bug 256269
        mPreviewThumb = (ImageView) mRootView.findViewById(R.id.preview_thumb);
        if (!mController.isVideoCaptureIntent()) {
            mPreviewThumb.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Do not allow navigation to filmstrip during video
                    // recording
                    if (!mRecordingStarted) {
                        mActivity.gotoGallery();
                    }
                }
            });
        }
        mPreviewThumb.setVisibility(View.INVISIBLE);

        mCameraSetting = mRootView.findViewById(R.id.btn_camera_setting);
        mVideoCapture = mRootView.findViewById(R.id.video_capture);
    }

    public void setPrefChangedListener(OnPreferenceChangedListener listener) {
        mVideoMenu.setListener(listener);
    }

    private void initializeMiscControls() {
        mReviewImage = (ImageView) mRootView.findViewById(R.id.review_image);
        // SPRD: bug 262381 Set mnewOrientation's value when init
        mNewOrientation = mActivity.getResources().getConfiguration().orientation;
//        mShutterButton.setImageResource(R.drawable.btn_new_shutter_video_port);
        mShutterButton.setOnShutterButtonListener(mController);
        mShutterButton.setVisibility(View.VISIBLE);
        mShutterButton.requestFocus();
        mShutterButton.enableTouch(true);
        mRecordingTimeView = (TextView) mRootView.findViewById(R.id.recording_time);
        //mRecordingTimeRect = (RotateLayout) mRootView.findViewById(R.id.recording_time_rect);
        mTimeLapseLabel = mRootView.findViewById(R.id.time_lapse_label);
        // The R.id.labels can only be found in phone layout.
        // That is, mLabelsLinearLayout should be null in tablet layout.
        //mLabelsLinearLayout = (LinearLayout) mRootView.findViewById(R.id.labels);
    }

    public void updateOnScreenIndicators(Parameters param, ComboPreferences prefs) {
      mOnScreenIndicators.updateFlashOnScreenIndicator(param.getFlashMode());
      boolean location = RecordLocationPreference.get(
              prefs, mActivity.getContentResolver());
      mOnScreenIndicators.updateLocationIndicator(location);

    }

    public void updateControlsTop(PreferenceGroup group){
        ListPreference pref = group.findPreference(
                CameraSettings.KEY_CAMERA_ID);
        if(pref != null){
            mSwitchButton.setVisibility(View.VISIBLE);
            mSwitchButtonShow = true;
            int cameraId = Integer.parseInt((String)pref.getValue());
           // mFlashButton.setVisibility(cameraId == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT ? View.INVISIBLE : View.VISIBLE);
            //add by topwise houyi for bug 51
            if(mFlashButtonShow) {
            if(cameraId != android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT)
                CameraUtil.fadeIn(mFlashButton);
            else //SPRD: bug 272036
                CameraUtil.fadeOut(mFlashButton);
            }
           // mHDRButton.setVisibility(newCameraId == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT ? View.VISIBLE : View.GONE);
        }else{
            // SPRD: bug 272036
        	//add by topwise houyi for bug 51
        	if(mFlashButtonShow)
            CameraUtil.fadeIn(mFlashButton);
            mSwitchButton.setVisibility(View.GONE);
            mSwitchButtonShow = false;
        }

    }

    public void setAspectRatio(double ratio) {
      //  mPreviewFrameLayout.setAspectRatio(ratio);
    }

    public void showTimeLapseUI(boolean enable) {
        if (mTimeLapseLabel != null) {
            mTimeLapseLabel.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    private void openMenu() {
        /* orign
        if (mPieRenderer != null) {
            mPieRenderer.showInCenter();
        }
        */
        /* SPRD: uui camera setting start @{ */
        mVideoMenu.showUUIPopup();
        /* uui camera setting end @} */
    }

    public void dismissPopup(boolean topLevelOnly) {
        // In review mode, we do not want to bring up the camera UI
        if (mController.isInReviewMode()) return;
        /* origin
        if (mPopup != null) {
            mPopup.dismiss(topLevelOnly);
        }
        */
        /* SPRD: uui camera setting start @{ */
        try {
            if (mPopup != null) {
                if (mSecondLevelPopup != null) {
                    // dismiss the second level settings popup
                    mSecondLevelPopup.dismiss(false);
                    /** SPRD: make the mask invisible before dismiss the 2nd level popup @{ */
                    View mask = mPopup.getContentView().findViewById(R.id.mask);
                    mask.setBackgroundColor(Color.parseColor("#00000000"));
//                    mPopup.update();
                    /** @} */
                    if (!topLevelOnly) {
                        // dismiss first level settings popup
                        mPopup.dismiss(false);
                    }
                } else {
                    // only dismiss the first level popup
                    mPopup.dismiss(false);
                }
            } else if (mSecondLevelPopup != null) {    // in case of unexpected conditions
                mSecondLevelPopup.dismiss(false);
            }
        } catch (NullPointerException e) {
            String message =
                "dismiss popup topLevelOnly=" + topLevelOnly +
                ", mPopup=" + mPopup + ", mSecondLevelPopup=" + mSecondLevelPopup;
            Log.d(TAG, message, e);
        }
        /* uui camera setting end @} */
    }

    public void dismissSencondLevelPopup() {
        if (mSecondLevelPopup != null) {
            if (mPopup != null) {
                View mask = mPopup.getContentView().findViewById(R.id.mask);
                mask.setBackgroundColor(Color.parseColor("#00000000"));
            }
            // dismiss the second level settings popup
            mSecondLevelPopup.dismiss(false);
        }
    }

    private void popupDismissed() {
        if (mSecondLevelPopup != null && mPopup != null) {
            mSecondLevelPopup = null;
        } else {
            mPopup = null;
        }
    }

    /* SPRD: show restore popup @{ */
    public void showPopup(AlertDialogPopup popup) {
        showPopup((View) popup);
    }
    /* @} */

    public void showPopup(AbstractSettingPopup popup) {
        showPopup((View) popup);
    }

    public void showPopup(View view) {
        if (mVideoMenu.getPopupStatus() != VideoMenu.POPUP_SECOND_LEVEL) {
            hideUI();
            if (mPopup != null) {
                mPopup.dismiss(false);
            }
            mPopup = new SettingsPopup(view);
            Log.d(TAG, "show first level popup widow");
        } else {
            if (mPopup != null) {
                Log.d(TAG,
                        "before open subwindow ,check parent window: mPopup=" + mPopup.isShowing());
                if (!mPopup.isShowing()) {
                    mPopup.dismiss();
                    return;
                }
            }
            Log.d(TAG, "show second level popup window");
            if (mSecondLevelPopup != null) {
                mSecondLevelPopup.dismiss(false);
            }
            if (view instanceof StoragePathPopup) {
                // SPRD: show storage path popup in full screen
                mSecondLevelPopup = new SettingsPopup(
                        view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            } else {
                mSecondLevelPopup = new SettingsPopup(
                        view, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                /** SPRD: make the 1st level popup window turn grey before show the 2nd level popup @{ */
                mPopup.getContentView().findViewById(R.id.mask).setBackgroundColor(Color.parseColor("#9D000000"));
//                mPopup.update();
                /** @} */
            }
        }
    }

    public void onShowSwitcherPopup() {
        hidePieRenderer();
    }

    public boolean hidePieRenderer() {
        if (mPieRenderer != null && mPieRenderer.showsItems()) {
            mPieRenderer.hide();
            return true;
        }
        return false;
    }

    // disable preview gestures after shutter is pressed
    public void setShutterPressed(boolean pressed) {
        if (mGestures == null) return;
        mGestures.setEnabled(!pressed);
    }

    public void enableShutter(boolean enable) {
        if (mShutterButton != null) {
            mShutterButton.setEnabled(enable);
        }
    }

    // PieListener
    @Override
    public void onPieOpened(int centerX, int centerY) {
        setSwipingEnabled(false);
        // Close module selection menu when pie menu is opened.
        mSwitcher.closePopup();
    }

    @Override
    public void onPieClosed() {
        setSwipingEnabled(true);
    }

    public void setSwipingEnabled(boolean enable) {
        mActivity.setSwipingEnabled(enable);
    }

    public void showPreviewBorder(boolean enable) {
       // TODO: mPreviewFrameLayout.showBorder(enable);
    }

    // SingleTapListener
    // Preview area is touched. Take a picture.
    @Override
    public void onSingleTapUp(View view, int x, int y) {
        mController.onSingleTapUp(view, x, y);
    }

    public void showRecordingUI(boolean recording) {
        if (recording == false) mPauseRecording = false;
        if (mPauseButton != null) {
            mPauseButton.setVisibility(recording ? View.VISIBLE : View.GONE);
            mPauseButton.setImageResource(
                    mPauseRecording ? R.drawable.icn_play_uui : R.drawable.btn_new_video_pause);
        }
        Log.d(TAG, "showRecordingUI    recording="+recording);
        mRecordingStarted = recording;
        //mMenuButton.setVisibility(recording ? View.GONE : View.VISIBLE); // origin
        //mOnScreenIndicators.setVisibility(recording ? View.GONE : View.VISIBLE);  // origin
        /* SPRD: don't show original setting button start @{ */
        mMenuButton.setVisibility(View.GONE);
        mOnScreenIndicators.setVisibility(View.GONE);
        /* don't show original setting button end @} */
        if (recording) {
            mPauseButton.setVisibility(View.VISIBLE);
            mStopButton.setVisibility(View.VISIBLE);
            mShutterButton.setVisibility(View.GONE);
            hideSwitcher();
            mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
        } else {
            mPauseButton.setVisibility(View.GONE);
            mStopButton.setVisibility(View.GONE);
            mShutterButton
                    .setImageResource(mNewOrientation == Configuration.ORIENTATION_PORTRAIT ? R.drawable.btn_new_shutter_video_port
                            : R.drawable.btn_new_shutter_video_land);
            if (!mController.isVideoCaptureIntent()) {
                // SPRD: Module switch use Gesture scroll
                // showSwitcher();
                // SPRD: bug 280809
                mShutterButton.setVisibility(View.VISIBLE);
            }
            mRecordingTimeView.setVisibility(View.GONE);
        }
    }

    public void showReviewImage(Bitmap bitmap) {
        mReviewImage.setImageBitmap(bitmap);
        mReviewImage.setVisibility(View.VISIBLE);
    }

    public void showReviewControls() {
        if(mReviewImage.getVisibility() != View.VISIBLE){
            CameraUtil.fadeIn(mReviewImageLoading);
            CameraUtil.fadeOut(mReviewPlayButton);
            CameraUtil.fadeOut(mReviewDoneButton);
            CameraUtil.fadeOut(mReviewRetakeButton);
            CameraUtil.fadeOut(mReviewRetakeButton);
            mReviewImage.setImageBitmap(null);
        } else {
            CameraUtil.fadeOut(mReviewImageLoading);
            CameraUtil.fadeIn(mReviewPlayButton) ;
            CameraUtil.fadeIn(mReviewDoneButton);
            CameraUtil.fadeIn(mReviewRetakeButton);
            CameraUtil.fadeIn(mReviewRetakeButton);
        }
        CameraUtil.fadeOut(mFlashButton);
        CameraUtil.fadeOut(mSwitchButton);
        mShutterButton.setVisibility(View.INVISIBLE);
        mActivity.findViewById(R.id.module_switch_textview).setVisibility(View.INVISIBLE);
        mReviewImage.setVisibility(View.VISIBLE);
        mMenuButton.setVisibility(View.GONE);
        mOnScreenIndicators.setVisibility(View.GONE);
    }

    public void hideReviewUI() {
        mReviewImage.setVisibility(View.GONE);
        mShutterButton.setEnabled(true);
        //mMenuButton.setVisibility(View.VISIBLE);  // origin
        //mOnScreenIndicators.setVisibility(View.VISIBLE);  // origin
        /* SPRD: don't show original setting button start @{ */
        mMenuButton.setVisibility(View.GONE);
        mOnScreenIndicators.setVisibility(View.GONE);
        /* don't show original setting button end @} */
        CameraUtil.fadeOut(mReviewDoneButton);
        CameraUtil.fadeOut(mReviewPlayButton);
        CameraUtil.fadeIn(mShutterButton);
    }

    private void setShowMenu(boolean show) {
        if (mOnScreenIndicators != null) {
            //mOnScreenIndicators.setVisibility(show ? View.VISIBLE : View.GONE);  // origin
            mOnScreenIndicators.setVisibility(View.GONE); // SPRD: don't show original setting button
        }
        if (mMenuButton != null) {
            //mMenuButton.setVisibility(show ? View.VISIBLE : View.GONE);  // origin
            mMenuButton.setVisibility(View.GONE);  // SPRD: don't show original setting button
        }
    }

    public void onPreviewFocusChanged(boolean previewFocused) {
        if (previewFocused) {
            showUI();
        } else {
            hideUI();
        }
        if (mGestures != null) {
            mGestures.setEnabled(previewFocused);
        }
        if (mRenderOverlay != null) {
            // this can not happen in capture mode
            mRenderOverlay.setVisibility(previewFocused ? View.VISIBLE : View.GONE);
        }
        setShowMenu(previewFocused);
    }

    public void initializePopup(PreferenceGroup pref) {
        mVideoMenu.initialize(pref);
    }

    public void initializeZoom(Parameters param) {
        if (param == null || !param.isZoomSupported()) {
            mGestures.setZoomEnabled(false);
            return;
        }
        mGestures.setZoomEnabled(true);
        mZoomMax = param.getMaxZoom();
        mZoomRatios = param.getZoomRatios();
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        mZoomRenderer.setZoomMax(mZoomMax);
        mZoomRenderer.setZoom(param.getZoom());
        mZoomRenderer.setZoomValue(mZoomRatios.get(param.getZoom()));
        mZoomRenderer.setOnZoomChangeListener(new ZoomChangeListener());
    }

    public void clickShutter() {
        mShutterButton.performClick();
    }

    public void pressShutter(boolean pressed) {
        mShutterButton.setPressed(pressed);
    }

    public View getShutterButton() {
        return mShutterButton;
    }

    public void setRecordingTime(String text) {
        mRecordingTimeView.setText(text);
    }

    public void setRecordingTimeTextColor(int color) {
        mRecordingTimeView.setTextColor(color);
    }

    public boolean isVisible() {
        if (DEBUG) {
            int visibility = mCameraControls.getVisibility();
            StringBuffer buff = new StringBuffer("isVisible mCameraControls visibility is ");
            switch (visibility) {
                case View.VISIBLE :
                    buff.append("visible return true");
                    break;
                default :
                    buff.append("invisible return false");
                    break;
            }
            CameraUtil.P(DEBUG, TAG, buff.toString());
        }
        return mCameraControls.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onDisplayChanged() {
        mCameraControls.checkLayoutFlip();
        mController.updateCameraOrientation();
    }

    private class ZoomChangeListener implements ZoomRenderer.OnZoomChangedListener {
        @Override
        public void onZoomValueChanged(int index) {
            int newZoom = mController.onZoomChanged(index);
            if (mZoomRenderer != null) {
                mZoomRenderer.setZoomValue(mZoomRatios.get(newZoom));
            }
        }

        @Override
        public void onZoomStart() {
        }

        @Override
        public void onZoomEnd() {
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    // SurfaceTexture callbacks
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurfaceTexture = surface;
        mController.onPreviewUIReady();
        setTransformMatrix(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurfaceTexture = null;
        mController.onPreviewUIDestroyed();
        Log.d(TAG, "surfaceTexture is destroyed");
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    // SurfaceHolder callbacks
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "Surface changed. width=" + width + ". height=" + height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "Surface created");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "Surface destroyed");
        mController.stopPreview();
    }

    public void updateControlTopButton(Camera.Parameters mParameters) {
        updateFlashButton(mParameters);
    }

    private void updateFlashButton(Camera.Parameters mParameters) {
        String value = mParameters.getFlashMode();
        boolean isSupportFlash = CameraSettings.VALUE_TRUE.equals(
                mParameters.get(CameraSettings.KEY_HAL_FLASH_SUPPORTED));
        if (mFlashButton == null) {
            return;
        }
        mFlashButtonShow = isSupportFlash;
        if (!isSupportFlash || value == null) {
            mFlashButton.setVisibility(View.GONE);
        } else {
            if (Parameters.FLASH_MODE_AUTO.equals(value)) {
                mFlashButton.setImageResource(R.drawable.ic_flash_auto_holo_light_uui);
            } else if (Parameters.FLASH_MODE_ON.equals(value)
                    || Parameters.FLASH_MODE_TORCH.equals(value)) {
                mFlashButton.setImageResource(R.drawable.ic_flash_on_holo_light_uui);
            } else if (Parameters.FLASH_MODE_OFF.equals(value)) {
                // Should not happen.
                mFlashButton.setImageResource(R.drawable.ic_flash_off_uui);
            }
        }
    }

    /* SPRD: multi-focus feature start @{ */
    @Override
    public void onMultiTapUp(View view, List<Tuple<Integer, Integer>> pointers) {
        // do nothing if multi-touched screen in video module
    }
    /* multi-focus feature end @} */

    /* SPRD: enable uui4 style camera setting button start @{ */
    public void onCameraSettingClicked() {
        mVideoMenu.showUUIPopup();
    }
    /* enable uui4 style camera setting button end @} */

    /* SPRD: click 1st level popup title to dismiss start @{ */
    public void onSettingTitleClicked(View v) {
        dismissPopup(true);
    }
    /* click 1st level popup title to dismiss end @} */

    public void onPauseClicked(boolean mPauseRecorderRecording ){
        // reset pause button icon
        mPauseRecording = mPauseRecorderRecording;
        mPauseButton
                .setImageResource(mPauseRecorderRecording ? R.drawable.icn_play_uui
                        : R.drawable.btn_new_video_pause);
    }

    public void onStopClicked() {

    }

    public void setShutterButtonRes(int orientation) {
        updateShutterLayout();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mShutterButton.setImageResource(R.drawable.btn_new_shutter_video_port);
        } else {
            mShutterButton.setImageResource(R.drawable.btn_new_shutter_video_land);
        }
        mNewOrientation = orientation;
    }

    /* SPRD: update thumbnail button @{ */
    public void updateThumbnail(Bitmap bitmap) {
        Log.d(TAG, ".updateThumbnail bitmap = " + bitmap);
        ((ImageView) mPreviewThumb).setImageBitmap(bitmap);
        if (bitmap == null) {
            mPreviewThumb.setVisibility(View.INVISIBLE);
        } else {
            mPreviewThumb.setVisibility(mShowPreviewThumb?View.VISIBLE:View.INVISIBLE);
        }
    }
    /* @} */

    /* SPRD: add retake feature in a video capture intent @{ */
    protected void hidePostCaptureAlert() {
        mReviewDoneButton.setVisibility(View.GONE);
        mReviewImage.setVisibility(View.GONE);
        mReviewRetakeButton.setVisibility(View.GONE);
        mOnScreenIndicators.setVisibility(View.GONE);
        mActivity.findViewById(R.id.module_switch_textview).setVisibility(View.INVISIBLE);

        CameraUtil.fadeOut(mReviewPlayButton);

        mShutterButton.setVisibility(View.VISIBLE);
        mShutterButton.setEnabled(true);

        // CameraUtil.fadeIn(mFlashButton);
        CameraUtil.fadeIn(mCameraSetting);
    }
    /* @} */
    public void hideControlsUI() {
        mShowPreviewThumb = false;
        mPreviewThumb.setVisibility(View.INVISIBLE);
        mCameraSetting.setVisibility(View.INVISIBLE);
    }

    public void showControlsUI() {
        /* @{ SPRD: fix bug 272156 start */
        mCameraSetting.setVisibility(View.VISIBLE);
        enableSwitchCameraButton(true);
        if (!mController.isVideoCaptureIntent()) {
//            mCameraSetting.setVisibility(View.VISIBLE);
        /* end }@ */
            mShowPreviewThumb = true;
            mPreviewThumb.setVisibility(View.VISIBLE);
        }
    }

    public void showVideoCaptureIcon(boolean enable) {
        if (mVideoCapture != null) {
            mVideoCapture.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void enableSwitchCameraButton(boolean enable) {
        if (mSwitchButton != null) {
            mSwitchButton.setEnabled(enable);
        }
    }

    // @{ SPRD: bug 274432 reload video_shutter_control layout begin
    public void updateShutterLayout() {
        int tmpShutterButtonVisble = mShutterButton.getVisibility();
        mVideoShutterControl.removeAllViews();
        mActivity.getLayoutInflater().inflate(R.layout.video_shutter,
                (ViewGroup) mVideoShutterControl, true);
        mShutterButton = (ShutterButton) mRootView.findViewById(R.id.shutter_button);
        mShutterButton.setVisibility(tmpShutterButtonVisble);
        mStopButton = (ImageView) mRootView.findViewById(R.id.btn_video_stop);
        mPauseButton = (ImageView) mRootView.findViewById(R.id.btn_video_pause);
        mShutterButton.setOnShutterButtonListener(mController);
        Log.d(TAG, "  updateShutterLayout end");
    } // SPRD: bug 274432 reload video_shutter_control layout end @}
}
