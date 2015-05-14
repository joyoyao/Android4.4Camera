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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.content.res.Configuration;

import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera.FocusOverlayManager.FocusUI;
import com.android.camera.ui.AbstractSettingPopup;
import com.android.camera.ui.CameraControls;
import com.android.camera.ui.CameraRootView;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.CountDownView.OnCountDownFinishedListener;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.FocusIndicator;
import com.android.camera.ui.ModuleSwitcher;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.PieRenderer.PieListener;
import com.android.camera.ui.RenderOverlay;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.StoragePathPopup;
import com.android.camera.ui.ZoomRenderer;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;
import com.android.camera.util.Tuple;
import com.sprd.camera.AlertDialogPopup;    // SPRD: for restore

import java.util.List;

public class PhotoUI implements PieListener,
    PreviewGestures.SingleTapListener,
    PreviewGestures.MultiTapListener,
    FocusUI, TextureView.SurfaceTextureListener,
    LocationManager.Listener, CameraRootView.MyDisplayListener,
    CameraManager.CameraFaceDetectionCallback {

    private static final boolean DEBUG = true;

    private static final String TAG = "CAM_UI";
    private static final int DOWN_SAMPLE_FACTOR = 4;
    private final AnimationManager mAnimationManager;
    private CameraActivity mActivity;
    private PhotoController mController;
    private PreviewGestures mGestures;

    private View mRootView;
    private SurfaceTexture mSurfaceTexture;

    //private PopupWindow mPopup;    // origin
    // SPRD: uui camera setting, show 2nd level settings in a new popup
    private SettingsPopup mPopup;
    private SettingsPopup mSecondLevelPopup;

    private ShutterButton mShutterButton;
    private CountDownView mCountDownView;
    private RotateImageView mFlashButton;
    private RotateImageView mHDRButton;
    private RotateImageView mSwitchButton;

    private FaceView mFaceView;
    private RenderOverlay mRenderOverlay;
    private View mReviewCancelButton;
    private View mReviewDoneButton;
    private View mReviewRetakeButton;
    private ImageView mReviewImage;
    private DecodeImageForReview mDecodeTaskForReview = null;

    private View mMenuButton;
    private PhotoMenu mMenu;
    private ModuleSwitcher mSwitcher;
    private CameraControls mCameraControls;
    private ModuleSwitcherView mModuleSwitcherView;
    private AlertDialog mLocationDialog;

    // Small indicators which show the camera settings in the viewfinder.
    private OnScreenIndicators mOnScreenIndicators;

    private PieRenderer mPieRenderer;
    private ZoomRenderer mZoomRenderer;
    private Toast mNotSelectableToast;

    private int mZoomMax;
    private List<Integer> mZoomRatios;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mSurfaceTextureUncroppedWidth;
    private float mSurfaceTextureUncroppedHeight;

    private ImageView mPreviewThumb;
    private View mFlashOverlay;
    private ImageView mCameraSetting;

    private SurfaceTextureSizeChangedListener mSurfaceTextureSizeListener;
    private TextureView mTextureView;
    private Matrix mMatrix = null;
    private float mAspectRatio = 4f / 3f;
    private View mPreviewCover;
    private final Object mSurfaceTextureLock = new Object();

    private AIDetectionController mAIController;  // SPRD:AIDetectionController

    private boolean mIsHdrSupported = false;
    private boolean mIsFlashSupported = false;
    private boolean mIsFlashDisable = false;
    // SPRD: ZSL is enable at PhotoModule
    private boolean mIsZSLEnable = false;

    public interface SurfaceTextureSizeChangedListener {
        public void onSurfaceTextureSizeChanged(int uncroppedWidth, int uncroppedHeight);
    }

    private OnLayoutChangeListener mLayoutListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            int width = right - left;
            int height = bottom - top;
            if (mPreviewWidth != width || mPreviewHeight != height) {
                mPreviewWidth = width;
                mPreviewHeight = height;
                setTransformMatrix(width, height);
            }
        }
    };

    /* SPRD: uui camera setting, porting this class from VideoUI.java, start @{ */
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
            mMenu.popupDismissed(topLevelOnly);

            // Switch back into full-screen/lights-out mode after popup is dismissed.
            mActivity.setSystemBarsVisibility(false);
        }

        @Override
        public void dismiss() {
            // Called by Framework when touch outside the popup or hit back key
            //dismiss(true);     // @orig
            dismissPopup(true);
        }
    }
    /* uui camera setting, porting SettingsPopup from VideoUI.java, end @} */

    private class DecodeTask extends AsyncTask<Void, Void, Bitmap> {
        private final byte [] mData;
        private int mOrientation;
        private boolean mMirror;

        public DecodeTask(byte[] data, int orientation, boolean mirror) {
            mData = data;
            mOrientation = orientation;
            mMirror = mirror;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            // Decode image in background.
            Bitmap bitmap = CameraUtil.downSample(mData, DOWN_SAMPLE_FACTOR);
            if (mOrientation != 0 || mMirror) {
                Matrix m = new Matrix();
                if (mMirror) {
                    // Flip horizontally
                    m.setScale(-1f, 1f);
                }
                m.preRotate(mOrientation);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,
                        false);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPreviewThumb.setImageBitmap(bitmap);
            //mAnimationManager.startCaptureAnimation(mPreviewThumb);//do not do the animation
        }
    }

    private class DecodeImageForReview extends DecodeTask {
        public DecodeImageForReview(byte[] data, int orientation, boolean mirror) {
            super(data, orientation, mirror);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                return;
            }
            mReviewImage.setImageBitmap(bitmap);
            mReviewImage.setVisibility(View.VISIBLE);
            mDecodeTaskForReview = null;
        }
    }

    public PhotoUI(CameraActivity activity, PhotoController controller, View parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;

        mActivity.getLayoutInflater().inflate(R.layout.photo_module,
                (ViewGroup) mRootView, true);
        mRenderOverlay = (RenderOverlay) mRootView.findViewById(R.id.render_overlay);
        mFlashOverlay = mRootView.findViewById(R.id.flash_overlay);
        mPreviewCover = mRootView.findViewById(R.id.preview_cover);
        // display the view
        mTextureView = (TextureView) mRootView.findViewById(R.id.preview_content);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.addOnLayoutChangeListener(mLayoutListener);
        initIndicators();

        mFlashButton = (RotateImageView) mRootView.findViewById(R.id.btn_flash);
        mHDRButton = (RotateImageView) mRootView.findViewById(R.id.btn_hdr);
        mSwitchButton = (RotateImageView) mRootView.findViewById(R.id.btn_switch);
        mShutterButton = (ShutterButton) mRootView.findViewById(R.id.shutter_button);
        mSwitcher = (ModuleSwitcher) mRootView.findViewById(R.id.camera_switcher);
        mSwitcher.setCurrentIndex(ModuleSwitcher.PHOTO_MODULE_INDEX);
        mSwitcher.setSwitchListener(mActivity);
        mMenuButton = mRootView.findViewById(R.id.menu);
        mMenuButton.setVisibility(View.GONE); // SPRD: don't show original setting button
        mFlashButton.setVisibility(View.GONE);
        mHDRButton.setVisibility(View.GONE);
        ViewStub faceViewStub = (ViewStub) mRootView
                .findViewById(R.id.face_view_stub);
        if (faceViewStub != null) {
            faceViewStub.inflate();
            mFaceView = (FaceView) mRootView.findViewById(R.id.face_view);
            setSurfaceTextureSizeChangedListener(mFaceView);
        }
        mCameraControls = (CameraControls) mRootView.findViewById(R.id.camera_controls);
        mModuleSwitcherView = (ModuleSwitcherView) mRootView.findViewById(R.id.module_switch_textview);
        mActivity.setScrollFilmView(mModuleSwitcherView);
        mModuleSwitcherView.setCurrentIndex(ModuleSwitcher.PHOTO_MODULE_INDEX);
        mAnimationManager = new AnimationManager();
        mCameraSetting = (ImageView) mRootView.findViewById(R.id.btn_camera_setting);
    }

    public void setSurfaceTextureSizeChangedListener(SurfaceTextureSizeChangedListener listener) {
        mSurfaceTextureSizeListener = listener;
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0) {
            Log.e(TAG, "Invalid aspect ratio: " + aspectRatio);
            return;
        }
        if (aspectRatio < 1f) {
            aspectRatio = 1f / aspectRatio;
        }

        if (mAspectRatio != aspectRatio) {
            mAspectRatio = aspectRatio;
            // Update transform matrix with the new aspect ratio.
            if (mPreviewWidth != 0 && mPreviewHeight != 0) {
                setTransformMatrix(mPreviewWidth, mPreviewHeight);
            }
        }
    }

    private void setTransformMatrix(int width, int height) {
        mMatrix = mTextureView.getTransform(mMatrix);
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
            if (mSurfaceTextureSizeListener != null) {
                mSurfaceTextureSizeListener.onSurfaceTextureSizeChanged(
                        (int) mSurfaceTextureUncroppedWidth, (int) mSurfaceTextureUncroppedHeight);
            }
        }
        scaleX = scaledTextureWidth / width;
        scaleY = scaledTextureHeight / height;
        mMatrix.setScale(scaleX, scaleY, (float) width / 2, (float) height / 2);
        mTextureView.setTransform(mMatrix);

        // Calculate the new preview rectangle.
        RectF previewRect = new RectF(0, 0, width, height);
        mMatrix.mapRect(previewRect);
        mController.onPreviewRectChanged(CameraUtil.rectFToRect(previewRect));
    }

    protected Object getSurfaceTextureLock() {
        return mSurfaceTextureLock;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        synchronized (mSurfaceTextureLock) {
            Log.v(TAG, "SurfaceTexture ready.");
            mSurfaceTexture = surface;
            mController.onPreviewUIReady();
            // Workaround for b/11168275, see b/10981460 for more details
            if (mPreviewWidth != 0 && mPreviewHeight != 0) {
                // Re-apply transform matrix for new surface texture
                setTransformMatrix(mPreviewWidth, mPreviewHeight);
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        synchronized (mSurfaceTextureLock) {
            mSurfaceTexture = null;
            mController.onPreviewUIDestroyed();
            Log.w(TAG, "SurfaceTexture destroyed");
            return true;
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public View getRootView() {
        return mRootView;
    }

    private void initIndicators() {
        mOnScreenIndicators = new OnScreenIndicators(mActivity,
                mRootView.findViewById(R.id.on_screen_indicators));
        // SPRD: don't show original setting button
//        mOnScreenIndicators.setVisibility(View.GONE);
    }

    public void onCameraOpened(PreferenceGroup prefGroup, ComboPreferences prefs,
            Camera.Parameters params, OnPreferenceChangedListener listener) {
        if (mPieRenderer == null) {
            mPieRenderer = new PieRenderer(mActivity);
            mPieRenderer.setPieListener(this);
            mRenderOverlay.addRenderer(mPieRenderer);
        }

        if (mMenu == null) {
            mMenu = new PhotoMenu(mActivity, this, mPieRenderer);
            mMenu.setListener(listener);
        }
        mMenu.initialize(prefGroup);

        if (mZoomRenderer == null) {
            mZoomRenderer = new ZoomRenderer(mActivity);
            mRenderOverlay.addRenderer(mZoomRenderer);
        }

        if (mGestures == null) {
            // this will handle gesture disambiguation and dispatching
            //mGestures = new PreviewGestures(mActivity, this, mZoomRenderer, mPieRenderer); // origin
            mGestures = new PreviewGestures(mActivity, this, this,  mZoomRenderer, mPieRenderer); // SPRD: multi-focus feature
            /* SPRD: dev multi focus @{ */
//            String currentFocusMode = prefs.getString(
//                    CameraSettings.KEY_FOCUS_MODE, null);
//            if (mGestures != null && CameraSettings.VAL_MULTI_FOCUS.equals(currentFocusMode)) {
//                mGestures.setFocusMode(true);
//            } else {
//                mGestures.setFocusMode(false);
//            }
            /* @} */
            mRenderOverlay.setGestures(mGestures);
        }
        mGestures.setZoomEnabled(params.isZoomSupported());
        mGestures.setRenderOverlay(mRenderOverlay);
        mRenderOverlay.requestLayout();

        initializeZoom(params);
        updateOnScreenIndicators(params, prefGroup, prefs);
        updateControlsTop(prefGroup, params);
        //SPRD: initialize face detection
        intializeAIDetection(prefs);
    }

    private void intializeAIDetection(ComboPreferences prefs) {
        mAIController = new AIDetectionController(prefs);
    }

    public void onSharedPreferenceChanged(ComboPreferences prefs) {
        if (mAIController != null) {
            mAIController.resetAIController(prefs);
        }
    }

    public void animateCapture(final byte[] jpegData, int orientation, boolean mirror) {
        // Decode jpeg byte array and then animate the jpeg
        DecodeTask task = new DecodeTask(jpegData, orientation, mirror);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void openMenu() {
        if (mPieRenderer != null) {
            // If autofocus is not finished, cancel autofocus so that the
            // subsequent touch can be handled by PreviewGestures
            if (mController.getCameraState() == PhotoController.FOCUSING) {
                    mController.cancelAutoFocus();
            }
            //mPieRenderer.showInCenter();    // origin
            /* SPRD: uui camera setting start @{ */
            mMenu.showUUIPopup();
            /* uui camera setting end @} */
        }
    }

    public void initializeControlByIntent() {
        mPreviewThumb = (ImageView) mRootView.findViewById(R.id.preview_thumb);
        mPreviewThumb.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.gotoGallery();
            }
        });
        // SPRD: mPreviewThumb is invisible.
        mPreviewThumb.setVisibility(View.INVISIBLE);
        mMenuButton = mRootView.findViewById(R.id.menu);
        mMenuButton.setVisibility(View.GONE);
        /*mMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openMenu();
            }
        });*/
        if (mController.isImageCaptureIntent()) {
            hideSwitcher();
            ViewGroup cameraControls = (ViewGroup) mRootView.findViewById(R.id.camera_controls);
            mActivity.getLayoutInflater().inflate(R.layout.review_module_control, cameraControls);

            mReviewDoneButton = mRootView.findViewById(R.id.btn_done);
            mReviewCancelButton = mRootView.findViewById(R.id.btn_cancel);
            mReviewRetakeButton = mRootView.findViewById(R.id.btn_retake);
            mReviewImage = (ImageView) mRootView.findViewById(R.id.review_image);
            mReviewCancelButton.setVisibility(View.VISIBLE);

            mReviewDoneButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onCaptureDone();
                }
            });
            mReviewCancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onCaptureCancelled();
                }
            });

            mReviewRetakeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.onCaptureRetake();
                }
            });
        }
    }

    public void hideUI() {
        mCameraControls.setVisibility(View.INVISIBLE);
        mSwitcher.closePopup();
        hideSwitcher();
    }

    public void showUI() {
        mCameraControls.setVisibility(View.VISIBLE);
        if (!mController.isImageCaptureIntent()) {
            showSwitcher();
        }

    }

    public void hideControlsUI() {
        mPreviewThumb.setVisibility(View.INVISIBLE);
        mCameraSetting.setVisibility(View.INVISIBLE);
    }

    public void showControlsUI() {
        mCameraSetting.setVisibility(View.VISIBLE);
        if (!mController.isImageCaptureIntent()) {
            mPreviewThumb.setVisibility(View.VISIBLE);
        }
    }
    public boolean arePreviewControlsVisible() {
        return (mCameraControls.getVisibility() == View.VISIBLE);
    }

    public void hideSwitcher() {
        mSwitcher.closePopup();
        mSwitcher.setVisibility(View.INVISIBLE);
        mActivity.setModuleVisble(View.INVISIBLE);
    }

    public void showSwitcher() {
        // SPRD: Module switch use Gesture scroll
        //mSwitcher.setVisibility(View.VISIBLE);
        mActivity.setModuleVisble(View.VISIBLE);
    }
    // called from onResume but only the first time
    public  void initializeFirstTime() {
        // Initialize shutter button.
//        mShutterButton.setImageResource(R.drawable.btn_new_shutter);
        mShutterButton.setOnShutterButtonListener(mController);
        mShutterButton.setVisibility(View.VISIBLE);
    }

    // called from onResume every other time
    public void initializeSecondTime(Camera.Parameters params) {
        initializeZoom(params);
        if (mController.isImageCaptureIntent()) {
            hidePostCaptureAlert();
        } else { // SPRD: bug 265085,266416
            if (mMenu != null)
                mMenu.reloadPreferences();
        }
    }

    public void showLocationDialog() {
        if (mLocationDialog != null) return;    // SPRD: avoid show this dialog twice after restore settings
        mLocationDialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.remember_location_title)
                .setMessage(R.string.remember_location_prompt)
                .setPositiveButton(R.string.remember_location_yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int arg1) {
                                mController.enableRecordingLocation(true);
                                mLocationDialog = null;
                            }
                        })
                .setNegativeButton(R.string.remember_location_no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int arg1) {
                                dialog.cancel();
                            }
                        })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mController.enableRecordingLocation(false);
                        mLocationDialog = null;
                    }
                })
                .show();
    }

    public void initializeZoom(Camera.Parameters params) {
        if ((params == null) || !params.isZoomSupported()
                || (mZoomRenderer == null)) return;
        mZoomMax = params.getMaxZoom();
        mZoomRatios = params.getZoomRatios();
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        if (mZoomRenderer != null) {
            mZoomRenderer.setZoomMax(mZoomMax);
            mZoomRenderer.setZoom(params.getZoom());
            mZoomRenderer.setZoomValue(mZoomRatios.get(params.getZoom()));
            mZoomRenderer.setOnZoomChangeListener(new ZoomChangeListener());
        }
    }

    @Override
    public void showGpsOnScreenIndicator(boolean hasSignal) { }

    @Override
    public void hideGpsOnScreenIndicator() { }

    public void overrideSettings(final String ... keyvalues) {
        if (mMenu != null) {
            mMenu.overrideSettings(keyvalues);
        }
    }

    public void updateOnScreenIndicators(Camera.Parameters params,
            PreferenceGroup group, ComboPreferences prefs) {
        if (params == null) return;
        mOnScreenIndicators.updateSceneOnScreenIndicator(params.getSceneMode());
        mOnScreenIndicators.updateExposureOnScreenIndicator(params,
                CameraSettings.readExposure(prefs));
        mOnScreenIndicators.updateFlashOnScreenIndicator(params.getFlashMode());
        int wbIndex = 2;
        ListPreference pref = group.findPreference(CameraSettings.KEY_WHITE_BALANCE);
        // SPRD: When scene mode is not equals "auto", then white balance indicator update to "auto" icon
        if (pref != null && params != null) {
            String sceneMode = params.getSceneMode();
            if (Parameters.SCENE_MODE_AUTO.equals(sceneMode)) {
                wbIndex = pref.getCurrentIndex();
            }
        }
        mOnScreenIndicators.updateWBIndicator(wbIndex);
        boolean location = RecordLocationPreference.get(
                prefs, mActivity.getContentResolver());
        mOnScreenIndicators.updateLocationIndicator(location);
        /* SPRD: dev multi focus @{ */
        String currentFocusMode = prefs.getString(
                CameraSettings.KEY_FOCUS_MODE, null);
        if (mGestures != null && CameraSettings.VAL_MULTI_FOCUS.equals(currentFocusMode)) {
            mGestures.setFocusMode(true);
        } else {
            mGestures.setFocusMode(false);
        }
        /* @} */
    }

    public void updateControlsTop(PreferenceGroup group, Camera.Parameters params) {
        CameraUtil.P(DEBUG, TAG, "updateControlsTop group=" + group);
        int visibility = -1;
        ListPreference preference = null;

        if (group != null) {
            // find preference by "KEY_CAMERA_ID" key
            preference = group.findPreference(CameraSettings.KEY_CAMERA_ID);
            CameraUtil.P(DEBUG, TAG, "updateControlsTop CAMERA_ID preference=" + preference);
            // set mSwitcherButton visibility property
            visibility = ((preference != null) ? View.VISIBLE : View.GONE);
            mSwitchButton.setVisibility(visibility);
        }
        if (params != null) {
            updateHdrButton(params);
            updateFlashButton(params);
        }
    }

    public void setCameraState(int state) {
    }

    public void animateFlash() {
        mAnimationManager.startFlashAnimation(mFlashOverlay);
    }

    public void enableGestures(boolean enable) {
        if (mGestures != null) {
            mGestures.setEnabled(enable);
        }
    }

    // forward from preview gestures to controller
    @Override
    public void onSingleTapUp(View view, int x, int y) {
        mController.onSingleTapUp(view, x, y);
    }

    public boolean onBackPressed() {
        if (mPieRenderer != null && mPieRenderer.showsItems()) {
            mPieRenderer.hide();
            return true;
        }
        // In image capture mode, back button should:
        // 1) if there is any popup, dismiss them, 2) otherwise, get out of
        // image capture
        /* SPRD: uui camera setting, dismiss popup window start @{ */
        if (mPopup != null || mSecondLevelPopup != null) {
           removeTopLevelPopup();
           return true;
        }
        /* uui camera setting end @} */
        if (mController.isImageCaptureIntent()) {
            mController.onCaptureCancelled();
            return true;
        } else if (!mController.isCameraIdle()||mController.isFreezeFrameDisplay()) {
            // ignore backs while we're taking a picture
            return true;
        } else {
            return false;
        }
    }

    public void onPreviewFocusChanged(boolean previewFocused) {
        if (previewFocused) {
            showUI();
        } else {
            hideUI();
        }
        if (mFaceView != null) {
            mFaceView.setBlockDraw(!previewFocused);
        }
        if (mGestures != null) {
            mGestures.setEnabled(previewFocused);
        }
        if (mRenderOverlay != null) {
            // this can not happen in capture mode
            mRenderOverlay.setVisibility(previewFocused ? View.VISIBLE : View.GONE);
        }
        if (mPieRenderer != null) {
            mPieRenderer.setBlockFocus(!previewFocused);
        }
        setShowMenu(previewFocused);
        if (!previewFocused && mCountDownView != null) mCountDownView.cancelCountDown();
    }

    /* SPRD: Add Camera settings start @{ */
    public void showPopup(View view) {
        if (mMenu.getPopupStatus() != PhotoMenu.POPUP_SECOND_LEVEL) {
            hideUI();
            if (mPopup != null) {
                mPopup.dismiss(false);
            }
            mPopup = new SettingsPopup(view);
        } else {
            if (mPopup != null) {
                Log.d(TAG,
                        "before open subwindow ,check parent window: mPopup=" + mPopup.isShowing());
                if (!mPopup.isShowing()) {
                    mPopup.dismiss();
                    return;
                }
            }
            Log.d(TAG, "now is in second level popup");
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
    // @}

    public void showPopup(AbstractSettingPopup popup) {
        showPopup((View) popup);
    }

    /* origin
    public void dismissPopup() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }*/

    /** SPRD: uui camera setting. start @{
     * we show the 2nd level popup in a new popup window,
     * so we have to dismiss them seperately when we want to dismiss
     */
    public void dismissPopup(boolean topLevelOnly) {
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

    public void onShowSwitcherPopup() {
        if (mPieRenderer != null && mPieRenderer.showsItems()) {
            mPieRenderer.hide();
        }
    }

    private void setShowMenu(boolean show) {
        if (mOnScreenIndicators != null) {
            //mOnScreenIndicators.setVisibility(show ? View.VISIBLE : View.GONE); // origin
//            mOnScreenIndicators.setVisibility(View.GONE);  // SPRD: don't show original setting button
        }
        if (mMenuButton != null) {
            //mMenuButton.setVisibility(show ? View.VISIBLE : View.GONE);
            mMenuButton.setVisibility(View.GONE); // SPRD: don't show original setting button
        }
    }

    public boolean collapseCameraControls() {
        // TODO: Mode switcher should behave like a popup and should hide itself when there
        // is a touch outside of it.
        mSwitcher.closePopup();
        // Remove all the popups/dialog boxes
        boolean ret = false;
        /* origin
        if (mPopup != null) {
            dismissPopup();
            ret = true;
        }
        */
        /* SPRD: uui camera setting start @{ */
        if (mPopup != null || mSecondLevelPopup != null) {
            dismissPopup(false);
            ret = true;
        }
        /* uui camera setting end @} */
        onShowSwitcherPopup();
        return ret;
    }

    protected void showCapturedImageForReview(byte[] jpegData, int orientation, boolean mirror) {
        mDecodeTaskForReview = new DecodeImageForReview(jpegData, orientation, mirror);
        mDecodeTaskForReview.execute();
        mOnScreenIndicators.setVisibility(View.GONE);
        mMenuButton.setVisibility(View.GONE);
        CameraUtil.fadeIn(mReviewDoneButton);
        mShutterButton.setVisibility(View.INVISIBLE);
        CameraUtil.fadeIn(mReviewRetakeButton);
        pauseFaceDetection();
    }

    protected void hidePostCaptureAlert() {
        if (mDecodeTaskForReview != null) {
            mDecodeTaskForReview.cancel(true);
        }
        mReviewImage.setVisibility(View.GONE);
        //mOnScreenIndicators.setVisibility(View.VISIBLE);  // origin
        //mMenuButton.setVisibility(View.VISIBLE); // origin
        /* SPRD: don't show original setting button start @{ */
        mMenuButton.setVisibility(View.GONE);
//        mOnScreenIndicators.setVisibility(View.GONE);
        /* don't show original setting button end @} */
        CameraUtil.fadeOut(mReviewDoneButton);
        mShutterButton.setVisibility(View.VISIBLE);
        CameraUtil.fadeOut(mReviewRetakeButton);
        resumeFaceDetection();
    }

    public void setDisplayOrientation(int orientation) {
        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(orientation);
        }
    }

    // shutter button handling

    public boolean isShutterPressed() {
        return mShutterButton.isPressed();
    }

    /**
     * Enables or disables the shutter button.
     */
    public void enableShutter(boolean enabled) {
        if (mShutterButton != null) {
            mShutterButton.setEnabled(enabled);
        }
    }

//add by sprd for bug456 start
    public void enableSwitchCameraButton(boolean enable) {
        if (mSwitchButton != null) {
            mSwitchButton.setEnabled(enable);
        }
    }
//add by sprd for bug456 end

    public void pressShutterButton() {
        if (mShutterButton.isInTouchMode()) {
            mShutterButton.requestFocusFromTouch();
        } else {
            mShutterButton.requestFocus();
        }
        mShutterButton.setPressed(true);
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
            if (mPieRenderer != null) {
                mPieRenderer.setBlockFocus(true);
            }
        }

        @Override
        public void onZoomEnd() {
            if (mPieRenderer != null) {
                mPieRenderer.setBlockFocus(false);
            }
        }
    }

    @Override
    public void onPieOpened(int centerX, int centerY) {
        setSwipingEnabled(false);
        if (mFaceView != null) {
            mFaceView.setBlockDraw(true);
        }
        // Close module selection menu when pie menu is opened.
        mSwitcher.closePopup();
    }

    @Override
    public void onPieClosed() {
        setSwipingEnabled(true);
        if (mFaceView != null) {
            mFaceView.setBlockDraw(false);
        }
    }

    public void setSwipingEnabled(boolean enable) {
        mActivity.setSwipingEnabled(enable);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    // Countdown timer

    /* @{ SPRD: fix bug 273860 start */
    public void instantiateCountDown() {
        if (mCountDownView == null) initializeCountDown();
    }
    /* end }@ */

    private void initializeCountDown() {
        mActivity.getLayoutInflater().inflate(R.layout.count_down_to_capture,
                (ViewGroup) mRootView, true);
        mCountDownView = (CountDownView) (mRootView.findViewById(R.id.count_down_to_capture));
        mCountDownView.setCountDownFinishedListener((OnCountDownFinishedListener) mController);
    }

    public boolean isCountingDown() {
        return mCountDownView != null && mCountDownView.isCountingDown();
    }

    public void cancelCountDown() {
        if (mCountDownView == null) return;
        mCountDownView.cancelCountDown();
    }

    public void startCountDown(int sec, boolean playSound) {
        if (mCountDownView == null) initializeCountDown();
        mCountDownView.startCountDown(sec, playSound);
    }

    public void showPreferencesToast() {
        if (mNotSelectableToast == null) {
            String str = mActivity.getResources().getString(R.string.not_selectable_in_scene_mode);
            mNotSelectableToast = Toast.makeText(mActivity, str, Toast.LENGTH_SHORT);
        }
        mNotSelectableToast.show();
    }

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

    public void onPause() {
        cancelCountDown();

        // Clear UI.
        collapseCameraControls();
        if (mFaceView != null) mFaceView.clear();

        if (mLocationDialog != null && mLocationDialog.isShowing()) {
            mLocationDialog.dismiss();
        }
        mLocationDialog = null;
    }

    public void initDisplayChangeListener() {
        ((CameraRootView) mRootView).setDisplayChangeListener(this);
    }

    public void removeDisplayChangeListener() {
        ((CameraRootView) mRootView).removeDisplayChangeListener();
    }

    // focus UI implementation

    private FocusIndicator getFocusIndicator() {
        return (mFaceView != null && mFaceView.faceExists()) ? mFaceView : mPieRenderer;
    }

    @Override
    public boolean hasFaces() {
        return (mFaceView != null && mFaceView.faceExists());
    }

    public void clearFaces() {
        if (mFaceView != null) mFaceView.clear();
    }

    @Override
    public void clearFocus() {
        FocusIndicator indicator = getFocusIndicator();
        if (indicator != null) indicator.clear();
    }

    @Override
    public void setFocusPosition(int x, int y) {
        mPieRenderer.setFocus(x, y);
    }

    @Override
    public void onFocusStarted() {
        getFocusIndicator().showStart();
    }

    @Override
    public void onFocusSucceeded(boolean timeout) {
        getFocusIndicator().showSuccess(timeout);
    }

    @Override
    public void onFocusFailed(boolean timeout) {
        getFocusIndicator().showFail(timeout);
    }

    @Override
    public void pauseFaceDetection() {
        if (mFaceView != null) mFaceView.pause();
    }

    @Override
    public void resumeFaceDetection() {
        if (mFaceView != null) mFaceView.resume();
    }

    public void onStartFaceDetection(int orientation, boolean mirror) {
        mFaceView.clear();
        mFaceView.setVisibility(View.VISIBLE);
        mFaceView.setDisplayOrientation(orientation);
        mFaceView.setMirror(mirror);
        mFaceView.resume();
    }

    @Override
    public void onFaceDetection(Face[] faces, CameraManager.CameraProxy camera) {
        if (mAIController == null && mAIController.isChooseOff()) {
            if (mFaceView != null)
                mFaceView.clear();
            return;
        }
        if (mAIController.isChooseFace()) {
            if (mFaceView != null)
                mFaceView.setFaces(faces);
        } else if (mAIController.isChooseSmile()) {
            if (faces != null) {
                for (int i = 0, len = faces.length; i < len; i++) {
                    Log.v(TAG, " len=" + len + "     faces[i].score=" + faces[i].score);
                    mAIController.resetSmileScoreCount(faces[i].score >= 90);
                }
            }
        }
    }

    @Override
    public void onDisplayChanged() {
        Log.d(TAG, "Device flip detected.");
        mCameraControls.checkLayoutFlip();
        mController.updateCameraOrientation();
    }

    /* SPRD: uui camera setting start @{ */
    public boolean removeTopLevelPopup() {
        if (mPopup != null || mSecondLevelPopup != null) {
            dismissPopup(true);
            return true;
        }
        return false;
    }

    private void popupDismissed() {
        //mPopup = null;    // origin
        /* SPRD: uui camera setting start @{ */
        // because this method will be called twice in dismissPopup(boolean topLevelOnly) function
        // when showing a second level settings menu, so set null one by one
        if (mSecondLevelPopup != null && mPopup != null) {
            mSecondLevelPopup = null;
        } else {
            mPopup = null;
        }
        /* uui camera setting end @} */
    }
    /* uui camera setting end @} */

    private void updateHdrButton(Camera.Parameters parameters){
        Drawable background = null;
        String value = parameters.getSceneMode();
        if (mHDRButton == null) {
            return;
        }
        if (value == null || !mIsHdrSupported) {
            mHDRButton.setVisibility(View.GONE);
        }else {
            mHDRButton.setVisibility(View.VISIBLE);
            if (Parameters.SCENE_MODE_HDR.equals(value)) {
                background = mActivity.getResources().getDrawable(R.drawable.btn_hdr_on_to_off);
            }else {
                background = mActivity.getResources().getDrawable(R.drawable.btn_hdr_off_to_on);
            }
            mHDRButton.setImageDrawable(background);
            if (!Parameters.SCENE_MODE_AUTO.equals(value)) {
                mIsFlashDisable = true;
            }else {
                // SPRD: flash button enable when ZSL is disable and hdr disable
                mIsFlashDisable = false | mIsZSLEnable;
            }
        }
    }

    private void updateFlashButton(Camera.Parameters parameters) {
        String value = parameters.getFlashMode();
        if (mFlashButton == null) {
            return;
        }
        if (!mIsFlashSupported || value == null) {
            mFlashButton.setVisibility(View.GONE);
        } else {
            mFlashButton.setVisibility(View.VISIBLE);
            if (Parameters.FLASH_MODE_AUTO.equals(value)) {
                mFlashButton.setImageResource(R.drawable.ic_flash_auto_holo_light_uui);
            } else if (Parameters.FLASH_MODE_ON.equals(value)) {
                mFlashButton.setImageResource(R.drawable.ic_flash_on_holo_light_uui);
            } else if (Parameters.FLASH_MODE_OFF.equals(value)) {
                // Should not happen.
                mFlashButton.setImageResource(R.drawable.ic_flash_off_uui);
            }
            mFlashButton.setEnabled(!mIsFlashDisable);
        }
        //add by topwise houyi for bug 51
        if (mOnScreenIndicators != null)
        mOnScreenIndicators.updateFlashOnScreenIndicatorVisible(mIsFlashSupported);
       	//end of houyi add
    }

    // SPRD: "visible" use to set Thumbnail view visibility
    public void updateThumbnail(Bitmap bitmap,boolean visible){
        Log.d(TAG, ".updateThumbnail bitmap = " + bitmap);
        ((ImageView) mPreviewThumb).setImageBitmap(bitmap);
        if (bitmap == null) {
            mPreviewThumb.setVisibility(View.INVISIBLE);
        } else {
            mPreviewThumb.setVisibility(visible?View.VISIBLE:View.INVISIBLE);
        }
    }

    /* SPRD: multi-focus feature start @{ */
    @Override
    public void setMultiFocusPosition(List<Tuple<Integer, Integer>> pointers) {
        mPieRenderer.setMultiFocus(pointers);
    }

    // forward from preview gestures to controller
    @Override
    public void onMultiTapUp(View view, List<Tuple<Integer, Integer>> pointers) {
        mController.onMultiTapUp(view, pointers);
    }
    /* multi-focus feature end @} */

    /* SPRD: enable uui4 style camera setting button start @{ */
    public void onCameraSettingClicked() {
        /* SPRD: 294018 null pointer @{ */
        if(mMenu != null){
            mMenu.showUUIPopup();
        }
        /*@} */
    }
    /* enable uui4 style camera setting button end @} */

    /* SPRD: click 1st level popup title to dismiss start @{ */
    public void onSettingTitleClicked(View v) {
        dismissPopup(true);
    }
    /* click 1st level popup title to dismiss end @} */

    /* SPRD:Add for restore @{ */
    public void reloadPreferences(){
        if (mMenu != null) {
            mMenu.reloadPreferences();
        }
    }
    /* @} */

    /* SPRD: show restore popup @{ */
    public void showPopup(AlertDialogPopup popup) {
        // show the restore popup
        Log.d(TAG, "show restore popup");
        if (mSecondLevelPopup != null) {
            mSecondLevelPopup.dismiss(false);
        }
        mSecondLevelPopup = new SettingsPopup(popup, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }
    /* @} */
    public void setShutterButtonRes(int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mShutterButton.setImageResource(R.drawable.btn_new_shutter_port);
        } else {
            mShutterButton.setImageResource(R.drawable.btn_new_shutter_land);
        }
    }
    public void hideControlTopButton() {
        mSwitchButton.setVisibility(View.INVISIBLE);
        mFlashButton.setVisibility(View.INVISIBLE);
        mHDRButton.setVisibility(View.INVISIBLE);
    }
    public void showControlTopButton() {
        mSwitchButton.setVisibility(View.VISIBLE);
        mFlashButton.setVisibility(View.VISIBLE);
        mHDRButton.setVisibility(View.VISIBLE);
    }
   /* SPRD:fixbug262369 add method to enable control thumbnail is visible  @{ */
   public void setPreviewThumbVisibility(int visiblity) {
      if (mPreviewThumb != null) {
          mPreviewThumb.setVisibility(visiblity);
      }
   }
   /* @}  */

   // SPRD: HDR screen hint supported
   private OnScreenHint mHDRHint;
   public void enableHdrScreenHint(boolean enable, ComboPreferences preference) {
       if (mIsHdrSupported && preference != null) {
           String valueOn = mActivity.getString(R.string.setting_on_value);
           String hdrValue = preference.getString(CameraSettings.KEY_CAMERA_HDR, null);
           CameraUtil.P(DEBUG, TAG, "enableHdrScreenHint, enable=" + enable + " --- hdrValue=" + hdrValue);
           if (valueOn.equals(hdrValue)) {
               if (enable) {
                   String message =
                       mActivity.getString(R.string.notice_progress_text_hdr_mode);
                   if (mHDRHint == null) {
                       mHDRHint = OnScreenHint.makeText(mActivity, message);
                   } else {
                       mHDRHint.setText(message);
                   }
                   mHDRHint.show();
               } else if (mHDRHint != null) {
                   mHDRHint.cancel();
                   mHDRHint = null;
               }
           }
       }
   }

   private OnScreenHint mBurstHint;
   public void enableBurstScreenHint(boolean enable) {
       if (enable) {
           String message =
               mActivity.getString(R.string.notice_progress_text_burst_mode);
           if (mBurstHint == null) {
               mBurstHint = OnScreenHint.makeText(mActivity, message);
           } else {
               mBurstHint.setText(message);
           }
           mBurstHint.show();
       } else if (mBurstHint != null) {
           mBurstHint.cancel();
           mBurstHint = null;
       }
   }

    public void setHdrSupported(boolean supported) {
        mIsHdrSupported = supported;
    }

    public void setFlashSupported(boolean Supported) {
        mIsFlashSupported = Supported;
    }

    public void setZSLEnable(boolean enable) {
        mIsZSLEnable = enable;
    }
}
