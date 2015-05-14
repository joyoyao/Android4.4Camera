package com.android.camera.ui;

import com.android.camera.StoragePathPreference;
import com.android.camera.StorageUtil;
import com.android.camera2.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class InLineStoragePath extends InLineSettingItem {

    /*
     * the value is current ModePicker, please
     * @see "layout/in_line_setting_storage_camera_path", @value "ModePicker.MODE_CAMERA"
     * @see "layout/in_line_setting_storage_video_path", @value "ModePicker.MODE_VIDEO"
     */
    public static final String VAL_ATTR_MODE_PICKER = "modePicker";

    // ModePicker value, default is -1
    private int mMode = StoragePathPreference.VAL_STORAGE_UNKNOW_MODE;
    // storage path view
    private TextView mTextView;

    // default construct
    public InLineStoragePath(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        // initialize mMode value
        for (int i = 0, len = attrs.getAttributeCount(); i < len; i++) {
            String attrName = attrs.getAttributeName(i);
            String attrValue = attrs.getAttributeValue(i);
            if (VAL_ATTR_MODE_PICKER.equals(attrName) && attrValue != null) {
                mMode = Integer.parseInt(attrValue);
                break;
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTextView = (TextView) findViewById(R.id.tv_storage_path);
    }

    @Override
    protected void updateView() {
        if (mTextView != null) {
            StorageUtil util = StorageUtil.newInstance();
            util.getStoragePath(getContext(), mMode);
            String text = util.getStorageByMode(mMode);
            mTextView.setText(text);
        }
    }

}
