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

import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.LayoutInflater;

import com.android.camera.ui.AbstractSettingPopup;
import com.android.camera.ui.CountdownTimerPopup;
import com.android.camera.ui.ListPrefSettingPopup;
import com.android.camera.ui.MoreSettingPopup;
import com.android.camera.ui.MoreSettingPopup.Listener;
import com.android.camera.ui.PieItem;
import com.android.camera.ui.PieItem.OnClickListener;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.StoragePathPopup;
import com.android.camera.util.CameraUtil;
import com.sprd.camera.AlertDialogPopup;
import com.android.camera2.R;

public class PhotoMenu extends PieController
        implements CountdownTimerPopup.Listener,
        ListPrefSettingPopup.Listener, Listener,
        StoragePathPreference.StoragePathChangedListener {
    private static String TAG = "PhotoMenu";
    private static final boolean DEBUG = true;

    private final String mSettingOff;

    private PhotoUI mUI;
    private AbstractSettingPopup mPopup;
    private CameraActivity mActivity;
    /* SPRD: Add for camera setting container	@{ */
    private String[] mOtherKeys;
    public static final int POPUP_NONE = 0;
    public static final int POPUP_FIRST_LEVEL = 1;
    public static final int POPUP_SECOND_LEVEL = 2;
    private int mPopupStatus;
    ListPreference ctpref;
    ListPreference beeppref;
    /* @} */

    public PhotoMenu(CameraActivity activity, PhotoUI ui, PieRenderer pie) {
        super(activity, pie);
        mUI = ui;
        mSettingOff = activity.getString(R.string.setting_off_value);
        mActivity = activity;
    }

    public void initialize(PreferenceGroup group) {
        super.initialize(group);// * SPRD: Add for camera setting container
        String CAMERA_CONTINUOUS_CAPTURE_ITEM = CameraSettings.KEY_CAMERA_CONTINUOUS_CAPTURE;
        mPopup = null;
        mPopupStatus = POPUP_NONE;
        PieItem item = null;
        final Resources res = mActivity.getResources();
        Locale locale = res.getConfiguration().locale;
        // The order is from left to right in the menu.
        if (mActivity.isImageCaptureIntent()) {
            CAMERA_CONTINUOUS_CAPTURE_ITEM = null;
        }
        // SPRD: Add camera and video initialize storage
        ListPreference prefStorage =
            group.findPreference(CameraSettings.KEY_CAMERA_STORAGE_PATH);
        StorageUtil utilStorage = StorageUtil.newInstance();
        String patch = utilStorage.getStoragePath(mActivity, CameraUtil.MODE_CAMERA);
        if(patch != null){
            prefStorage.setStorageValue(patch);
        }
        // SPRD: bug 256269
        utilStorage.setmUpdataPathListener(mActivity);
        utilStorage.initialize(prefStorage);

        // HDR+ (GCam).
        if (group.findPreference(CameraSettings.KEY_CAMERA_HDR_PLUS) != null) {
            item = makeSwitchItem(CameraSettings.KEY_CAMERA_HDR_PLUS, true);
            mRenderer.addItem(item);
        }

        // HDR.
        if (group.findPreference(CameraSettings.KEY_CAMERA_HDR) != null) {
            item = makeSwitchItem(CameraSettings.KEY_CAMERA_HDR, true);
            mRenderer.addItem(item);
        }
        /* Bug 262393 Exposure item change IconListPreference to ListPreference @{ */
        // Exposure compensation.
        //if (group.findPreference(CameraSettings.KEY_EXPOSURE) != null) {
         //   item = makeItem(CameraSettings.KEY_EXPOSURE);
         //   item.setLabel(res.getString(R.string.pref_exposure_label));
         //   mRenderer.addItem(item);
       // }
        /* @} */
        // More settings.
        PieItem more = makeItem(R.drawable.ic_settings_holo_light);
        more.setLabel(res.getString(R.string.camera_menu_more_label));
        mRenderer.addItem(more);

        // Flash.
        if (group.findPreference(CameraSettings.KEY_FLASH_MODE) != null) {
            item = makeItem(CameraSettings.KEY_FLASH_MODE);
            item.setLabel(res.getString(R.string.pref_camera_flashmode_label));
            mRenderer.addItem(item);
        }
        // Camera switcher.
        if (group.findPreference(CameraSettings.KEY_CAMERA_ID) != null) {
            item = makeSwitchItem(CameraSettings.KEY_CAMERA_ID, false);
            final PieItem fitem = item;
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(PieItem item) {
                    // Find the index of next camera.
                    ListPreference pref = mPreferenceGroup
                            .findPreference(CameraSettings.KEY_CAMERA_ID);
                    if (pref != null) {
                        int index = pref.findIndexOfValue(pref.getValue());
                        CharSequence[] values = pref.getEntryValues();
                        index = (index + 1) % values.length;
                        pref.setValueIndex(index);
                        mListener.onCameraPickerClicked(index);
                    }
                    updateItem(fitem, CameraSettings.KEY_CAMERA_ID);
                }
            });
            mRenderer.addItem(item);
        }
        // Location.
        if (group.findPreference(CameraSettings.KEY_RECORD_LOCATION) != null) {
            item = makeSwitchItem(CameraSettings.KEY_RECORD_LOCATION, true);
            more.addItem(item);
            if (mActivity.isSecureCamera()) {
                // Prevent location preference from getting changed in secure camera mode
                item.setEnabled(false);
            }
        }

        // Countdown timer.
        //final ListPreference ctpref = group.findPreference(CameraSettings.KEY_TIMER); // @orign
        //final ListPreference beeppref = group.findPreference(CameraSettings.KEY_TIMER_SOUND_EFFECTS); //@origin
        /* SPRD: announce ctpref and beeppref as globle to use them in onPreferenceClicked @{ */
        ctpref = group.findPreference(CameraSettings.KEY_TIMER);
        beeppref = group.findPreference(CameraSettings.KEY_TIMER_SOUND_EFFECTS);
        /* @} */
        item = makeItem(R.drawable.ic_timer);
        item.setLabel(res.getString(R.string.pref_camera_timer_title).toUpperCase(locale));
        item.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(PieItem item) {
                CountdownTimerPopup timerPopup = (CountdownTimerPopup) mActivity.getLayoutInflater().inflate(
                        R.layout.countdown_setting_popup, null, false);
                timerPopup.initialize(ctpref, beeppref);
                timerPopup.setSettingChangedListener(PhotoMenu.this);
                //mUI.dismissPopup();    // origin
                mUI.dismissPopup(false); // SPRD: uui camera setting
                mPopup = timerPopup;
                mUI.showPopup(mPopup);
            }
        });
        more.addItem(item);
        // Image size.
        item = makeItem(R.drawable.ic_imagesize);
        final ListPreference sizePref = group.findPreference(CameraSettings.KEY_PICTURE_SIZE);
        item.setLabel(res.getString(R.string.pref_camera_picturesize_title).toUpperCase(locale));
        item.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(PieItem item) {
                ListPrefSettingPopup popup = (ListPrefSettingPopup) mActivity.getLayoutInflater().inflate(
                        R.layout.list_pref_setting_popup, null, false);
                popup.initialize(sizePref);
                popup.setSettingChangedListener(PhotoMenu.this);
                //mUI.dismissPopup();    // origin
                mUI.dismissPopup(false); // SPRD: uui camera setting
                mPopup = popup;
                mUI.showPopup(mPopup);
            }
        });
        more.addItem(item);
        // White balance.
        if (group.findPreference(CameraSettings.KEY_WHITE_BALANCE) != null) {
            item = makeItem(CameraSettings.KEY_WHITE_BALANCE);
            item.setLabel(res.getString(R.string.pref_camera_whitebalance_label));
            more.addItem(item);
        }

        /* SPRD:Add for camera setting container   @{ */
        mOtherKeys = new String[] {
            CameraSettings.KEY_GENERAL_SETTINGS,
            CameraSettings.KEY_PICTURE_SIZE,
            CameraSettings.KEY_CAMERA_JPEG_QUALITY,
            CameraSettings.KEY_CAMERA_SHARPNESS,
            CameraSettings.KEY_SCENE_MODE,
            CAMERA_CONTINUOUS_CAPTURE_ITEM,
            CameraSettings.KEY_TIMER,
            CameraSettings.KEY_WHITE_BALANCE,
            CameraSettings.KEY_CAMERA_COLOR_EFFECT,
            CameraSettings.KEY_FREEZE_FRAME_DISPLAY,
            CameraSettings.KEY_RECORD_LOCATION,
            CameraSettings.KEY_CAMERA_STORAGE_PATH,
            CameraSettings.KEY_ADVANCED_SETTINGS,
            CameraSettings.KEY_FOCUS_MODE,
            CameraSettings.KEY_CAMERA_AI_DETECT,
            CameraSettings.KEY_CAMERA_ANTIBANDING,
            CameraSettings.KEY_CAMERA_VIDEO_ZSL,
            CameraSettings.KEY_EXPOSURE,
            CameraSettings.KEY_CAMERA_METERING,
            CameraSettings.KEY_CAMERA_ISO,
            CameraSettings.KEY_CAMERA_BRIGHTNESS,
            CameraSettings.KEY_CAMERA_VIDEO_CONTRAST,
            CameraSettings.KEY_CAMERA_SATURATION,

        };
        item = makeItem(R.drawable.ic_settings_holo_light);
        item.setLabel(mActivity.getResources().getString(R.string.camera_menu_settings_label));
        item.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(PieItem item) {
                if (mPopup == null || mPopupStatus != POPUP_FIRST_LEVEL) {
                    initializePopup();
                    mPopupStatus = POPUP_FIRST_LEVEL;
                }
                mUI.showPopup(mPopup);
            }
        });
        more.addItem(item);
        /* @} */

    }

    /* SPRD:Add for camera setting container   @{ */
    protected void initializePopup() {
        LayoutInflater inflater = (LayoutInflater)
            mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        MoreSettingPopup popup = (MoreSettingPopup)
            inflater.inflate(R.layout.more_setting_popup, null, false);
        popup.setSettingChangedListener(this);
        popup.initialize(mPreferenceGroup, mOtherKeys);
        mPopup = popup;
    }
    /* @} */

    @Override
    /* SPRD: Hit the cancel button in 2nd level setting menu @{ */
    public void onCancelButtonClicked() {
        if (mPopup != null) {
            if (mPopupStatus == POPUP_SECOND_LEVEL) {
                mUI.dismissSencondLevelPopup();
            }
        }
    }
    /* @} */

    // Hit when an item in a popup gets selected
    public void onListPrefChanged(ListPreference pref) {
        if (mPopup != null) {
            if (mPopupStatus == POPUP_SECOND_LEVEL) {// SPRD :Add for camera setting container
                //mUI.dismissPopup();    // origin
                /* SPRD: uui camera setting start @{ */
                mUI.dismissPopup(false);
                /* uui camera setting feature end @} */
            }
        }
        onSettingChanged(pref);
    }

    public void popupDismissed() {
        if (mPopupStatus == POPUP_SECOND_LEVEL) {// SPRD :Add for camera setting container
            initializePopup();
            mPopupStatus = POPUP_FIRST_LEVEL;// SPRD :Add for camera setting container
            mUI.showPopup(mPopup);
        }
    }

    public void popupDismissed(boolean topPopupOnly) {
        // if the 2nd level popup gets dismissed
        if (mPopupStatus == POPUP_SECOND_LEVEL) {
            initializePopup();
            updateSettingsMutex();
            mPopupStatus = POPUP_FIRST_LEVEL;
            if (topPopupOnly) mUI.showPopup(mPopup);
        }
    }

    // Return true if the preference has the specified key but not the value.
    private static boolean notSame(ListPreference pref, String key, String value) {
        return (key.equals(pref.getKey()) && !value.equals(pref.getValue()));
    }

    private void setPreference(String key, String value) {
        ListPreference pref = mPreferenceGroup.findPreference(key);
        if (pref != null && !value.equals(pref.getValue())) {
            pref.setValue(value);
            reloadPreferences();
        }
    }
    /* SPRD:Add for restore   @{ */
    @Override
    public void onRestorePreferencesClicked() {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        AlertDialogPopup popup = (AlertDialogPopup) inflater.inflate(
                R.layout.alert_dialog_popup, null, false);
        mPopupStatus = POPUP_SECOND_LEVEL;
        mListener.onRestorePreferencesClicked(popup);
    }
    /* @} */
    @Override
    public void onSettingChanged(ListPreference pref) {
        // Reset the scene mode if HDR is set to on. Reset HDR if scene mode is
        // set to non-auto.
        if (notSame(pref, CameraSettings.KEY_CAMERA_HDR, mSettingOff)) {
            setPreference(CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_AUTO);
        } else if (notSame(pref, CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_AUTO)) {
            setPreference(CameraSettings.KEY_CAMERA_HDR, mSettingOff);
        }
        super.onSettingChanged(pref);
    }

    /* SPRD:Add for camera setting container   @{ */
    @Override
    public void onPreferenceClicked(ListPreference pref) {
        if (mPopupStatus != POPUP_FIRST_LEVEL) return;

        LayoutInflater inflater = (LayoutInflater)
            mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        String key = pref.getKey();
        AbstractSettingPopup basic = null;
        if (CameraSettings.KEY_CAMERA_STORAGE_PATH.equals(key)) {
            basic = (AbstractSettingPopup)
                inflater.inflate(R.layout.storage_path_popup, null, false);
            StoragePathPopup storagePopup = (StoragePathPopup) basic;
            storagePopup.initializeSetup(this, CameraUtil.MODE_CAMERA);
        /* SPRD: add this branch to show the original timer setting ui instead of a list @{ */
        } else if (CameraSettings.KEY_TIMER.equals(key)) {
            basic = (AbstractSettingPopup)
                    inflater.inflate(R.layout.countdown_setting_popup, null, false);
            CountdownTimerPopup timerPopup = (CountdownTimerPopup) basic;
            timerPopup.initialize(ctpref, beeppref);
            timerPopup.setSettingChangedListener(PhotoMenu.this);
        /* @} */
        } else {
            basic = (AbstractSettingPopup)
                inflater.inflate(R.layout.list_pref_setting_popup, null, false);
            ListPrefSettingPopup listPopup = (ListPrefSettingPopup) basic;
            listPopup.initialize(pref);
            listPopup.setSettingChangedListener(this);
        }
        // SPRD: uui camera setting, commented the 'dismissPopup' to show
        // the first level settings behind the second level settings
        //mUI.dismissPopup(true);
        //mUI.dismissPopup();    // origin
        mPopup = basic;

        mPopupStatus = POPUP_SECOND_LEVEL;
        mUI.showPopup(mPopup);
    }
    /* @} */
    /* SPRD: uui camera setting start @{ */
    // call this method to directly show popup.
    public void showUUIPopup() {
        if (mPopup == null || mPopupStatus != POPUP_FIRST_LEVEL) {
            initializePopup();
            mPopupStatus = POPUP_FIRST_LEVEL;
        }
        mUI.showPopup(mPopup);
    }

    public int getPopupStatus() {
        return mPopupStatus;
    }

    // Add for restore preferences
    @Override
    public void reloadPreferences() {
        super.reloadPreferences();
        if (mPopup != null) {
            mPopup.reloadPreference();
        }
    }

    @Override
    public void overrideSettings(final String ... keyvalues) {
        super.overrideSettings(keyvalues);
        if (mPopup == null || mPopupStatus != POPUP_FIRST_LEVEL) {
            mPopupStatus = POPUP_FIRST_LEVEL;
            initializePopup();
        }
        ((MoreSettingPopup) mPopup).overrideSettings(keyvalues);
    }
    /* uui camera setting end @} */

    @Override
    public void storageChanged(String path, boolean isCancle) {
        CameraUtil.P(DEBUG, TAG, "storageChanged path=" + path);
        mUI.dismissPopup(false);
        if(!isCancle) {
            StorageUtil util = StorageUtil.newInstance();
            util.resetStorageByMode(CameraUtil.MODE_CAMERA, path);
        }
        mActivity.updateStorageSpaceAndHint();
    }

    /* SPRD: added 20140107 of 263964 enabled item not enable sometimes @{ */
    public void updateSettingsMutex() {
        mActivity.updateSettingsMutex();
    }
    /* @} */
}
