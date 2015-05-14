package com.android.camera.ui;

import com.android.camera.ListPreference;
//import com.android.internal.telephony.DataConnection.FailCause;

import com.android.camera2.R;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

    public class InLineSettingPopup extends InLineSettingItem {

    private static final String TAG = "InLineSettingPopup";
    private TextView mEntry;

    public InLineSettingPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mEntry = (TextView)findViewById(R.id.current_popup_setting);
    }

    @Override
    public void initialize(ListPreference preference) {
        super.initialize(preference);
    }

    @Override
    protected void updateView() {
        if(mOverrideValue == null) {
            mEntry.setText(mPreference.getEntry());
        } else {
            int index = mPreference.findIndexOfValue(mOverrideValue);
            if(index != -1) {
                mEntry.setText(mPreference.getEntries()[index]);
            } else {
                Log.e(TAG, "Fail to find override value = " + mOverrideValue);
                mPreference.print();
            }
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add(mPreference.getTitle() + mPreference.getEntry());
    }

    }
