/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.camera.ui;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.camera.ListPreference;
import com.android.camera.PreferenceGroup;
import com.android.camera.CameraSettings;
import com.android.camera.util.CameraUtil;
import com.android.camera2.R;

/* A popup window that contains several camera settings. */
public class MoreSettingPopup extends AbstractSettingPopup
        implements InLineSettingItem.Listener,
        AdapterView.OnItemClickListener {

    private static final String TAG = "CAM_MoreSettingPopup";

    private Listener mListener;
    private ArrayList<ListPreference> mListItem = new ArrayList<ListPreference>();

    // Keep track of which setting items are disabled
    // e.g. White balance will be disabled when scene mode is set to non-auto
    private boolean[] mEnabled;

    PreferenceGroup mPreferenceGroup;

    static public interface Listener {
        public void onSettingChanged(ListPreference pref);
        public void onPreferenceClicked(ListPreference pref);
        public void onRestorePreferencesClicked();//SPRD:Add for restore
    }

    private class MoreSettingAdapter extends ArrayAdapter<ListPreference> {
        LayoutInflater mInflater;
        String mOnString;
        String mOffString;
        MoreSettingAdapter() {
            super(MoreSettingPopup.this.getContext(), 0, mListItem);
            Context context = getContext();
            mInflater = LayoutInflater.from(context);
            mOnString = context.getString(R.string.setting_on);
            mOffString = context.getString(R.string.setting_off);
        }

        private int getSettingLayoutId(ListPreference pref) {
            if (pref == null) return R.layout.in_line_setting_restore;//SPRD:Add for restore
            if (isOnOffPreference(pref)) {
                //return R.layout.in_line_setting_check_box;
                return R.layout.in_line_setting_switch;
            }
            String tag = pref.getKey();
            Log.d(TAG, "in getSettingLayoutId the key of the preference=" + tag);
            /* SPRD: uui camera setting popup start @{ */
            // just for TAG_GENERAL_SETTINGS,TAG_ADVANCED_SETINGS
            if (isSettingTagPrefernce(pref)) {
                return R.layout.in_line_setting_tag;
            }
            /* uui camera setting popup end @} */
            // SPRD: key for (camera | video) storage path 
            if (CameraSettings.KEY_CAMERA_STORAGE_PATH.equals(tag)) {
                return R.layout.in_line_setting_storage_camera_path;
            } else if (CameraSettings.KEY_VIDEO_STORAGE_PATH.equals(tag)) {
                return R.layout.in_line_setting_storage_video_path;
            }
            return R.layout.in_line_setting_menu;
        }

        /* SPRD: uui camera setting popup start @{ */
        private boolean isSettingTagPrefernce(ListPreference pref) {
            String tag = pref.getKey();
            Log.d(TAG, "in isSettingTagPrefernce the key of the preference=" + tag);
            if (CameraSettings.KEY_GENERAL_SETTINGS.equals(tag)
                    || CameraSettings.KEY_ADVANCED_SETTINGS.equals(tag)) {
                return true;
            }
            return false;
        }
        /* uui camera setting popup end @} */

        private boolean isOnOffPreference(ListPreference pref) {
            CharSequence[] entries = pref.getEntries();
            if (entries.length != 2) return false;
            String str1 = entries[0].toString();
            String str2 = entries[1].toString();
            return ((str1.equals(mOnString) && str2.equals(mOffString)) ||
                    (str1.equals(mOffString) && str2.equals(mOnString)));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListPreference pref = mListItem.get(position);

            int viewLayoutId = getSettingLayoutId(pref);
            InLineSettingItem view =
                (InLineSettingItem) mInflater.inflate(viewLayoutId, parent, false);

            view.initialize(pref); // no init for restore one
            view.setSettingChangedListener(MoreSettingPopup.this);
            if (pref != null) view.setTag(pref.getKey());

            if (position >= 0 && position < mEnabled.length) {
                view.setEnabled(mEnabled[position]);
                /* SPRD: 20140102 of Bug 261937 SceneMode is not auto, white balance and focus mode can not be used but still high light @{ */
                if (!mEnabled[position]) {
                    view.setTitleDisableColor(pref);
                }
                /* @} */
            } else {
                Log.w(TAG, "Invalid input: enabled list length, " + mEnabled.length
                        + " position " + position);
            }
            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            if (position >= 0 && position < mEnabled.length) {
                return mEnabled[position];
            }
            return true;
        }
    }

    public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }

    public MoreSettingPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(PreferenceGroup group, String[] keys) {
        // Prepare the setting items.
        for (int i = 0; i < keys.length; ++i) {
            ListPreference pref = group.findPreference(keys[i]);
            if (pref != null) {
                String key = pref.getKey();
                if (CameraSettings.KEY_RECORD_LOCATION.equals(key)
                        && !CameraUtil.isSupportedGps()) {
                    continue;
                }
                mListItem.add(pref);
            }
        }
        mPreferenceGroup = group;
        mListItem.add(null);//SPRD:Add for restore
        ArrayAdapter<ListPreference> mListItemAdapter = new MoreSettingAdapter();
        ((ListView) mSettingList).setAdapter(mListItemAdapter);
        ((ListView) mSettingList).setOnItemClickListener(this);
        ((ListView) mSettingList).setSelector(android.R.color.transparent);
        // Initialize mEnabled
        mEnabled = new boolean[mListItem.size()];
        for (int i = 0; i < mEnabled.length; i++) {
            mEnabled[i] = true;
        }
    }

    // When preferences are disabled, we will display them grayed out. Users
    // will not be able to change the disabled preferences, but they can still see
    // the current value of the preferences
    public void setPreferenceEnabled(String key, boolean enable) {
        int count = mEnabled == null ? 0 : mEnabled.length;
        for (int j = 0; j < count; j++) {
            ListPreference pref = mListItem.get(j);
            if (pref != null && key.equals(pref.getKey())) {
                mEnabled[j] = enable;
                break;
            }
        }
    }

    public void onSettingChanged(ListPreference pref) {
        if (mListener != null) {
            modifyMutexChange(pref);
            mListener.onSettingChanged(pref);
        }
    }

    private void modifyMutexChange(ListPreference pref) {
        if (pref != null && CameraSettings.KEY_FREEZE_FRAME_DISPLAY.equals(pref.getKey())
                && CameraSettings.VALUE_ON.equals(pref.getValue())) {
            ListPreference continuousPref = mPreferenceGroup
                    .findPreference(CameraSettings.KEY_CAMERA_CONTINUOUS_CAPTURE);
            if (continuousPref != null) {
                continuousPref.setValue(CameraSettings.DETAULT_PICTURE_NUMBER);
            }
        }
    }

    /*package*/ View findViewByKey(String key) {
        View v = null;
        if (mSettingList != null && key != null) {
            for (int i = 0, len = mSettingList.getChildCount(); i < len; i++) {
                v = mSettingList.getChildAt(i);
                if (v instanceof InLineSettingItem) {
                    Object objKey = v.getTag();
                    if (objKey == null) continue;
                    String tag = objKey.toString();
                    if (key.equals(tag)) {
                        break;
                    }
                }
            }
        }
        return v;
    }

    // Scene mode can override other camera settings (ex: flash mode).
    public void overrideSettings(final String ... keyvalues) {
        int count = mEnabled == null ? 0 : mEnabled.length;
        for (int i = 0; i < keyvalues.length; i += 2) {
            String key = keyvalues[i];
            String value = keyvalues[i + 1];
            for (int j = 0; j < count; j++) {
                ListPreference pref = mListItem.get(j);
                if (pref != null && key.equals(pref.getKey())) {
                    // Change preference
                    if (value != null) pref.setValue(value);
                    // If the preference is overridden, disable the preference
                    boolean enable = value == null;
                    mEnabled[j] = enable;
                    View v = findViewByKey(key);
                    if (v != null) {
                        v.setEnabled(enable);
                    }
                }
            }
        }
        reloadPreference();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (mListener != null) {
            /* SPRD:Add for restore   @{ */
            if ((position == mListItem.size() - 1) && (mListener != null)) {
                 mListener.onRestorePreferencesClicked();
            } else {
                ListPreference pref = mListItem.get(position);
                //mListener.onPreferenceClicked(pref); // origin
                /* SPRD: uui camera setting start @{ */
                String tag = pref.getKey();
                Log.d(TAG, "in isSettingTagPrefernce the key of the preference=" + tag);
                if (!CameraSettings.KEY_GENERAL_SETTINGS.equals(tag)
                        && !CameraSettings.KEY_ADVANCED_SETTINGS.equals(tag)) {
                    mListener.onPreferenceClicked(pref);
                }
                /* uui camera setting end @} */
            }
            /* @} */
        }
    }

    @Override
    public void reloadPreference() {
        int count = mSettingList.getChildCount();
        for (int i = 0; i < count; i++) {
            ListPreference pref = mListItem.get(i);
            if (pref != null) {
                InLineSettingItem settingItem =
                        (InLineSettingItem) mSettingList.getChildAt(i);
                settingItem.reloadPreference();
            }
        }
    }
}
