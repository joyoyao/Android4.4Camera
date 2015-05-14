/**
 * Created by Spreadst.
 */

package com.sprd.camera;

import com.android.camera.CameraSettings;
import com.sprd.camera.utils.ParametersUtil;

import android.content.SharedPreferences;
import android.hardware.Camera.Parameters;
import android.util.Log;

public class ZSLController {
    private static final String TAG = "CameraZSLController";
    private static final String KEY_DEVICE_ZSL = "zsl";

    private String mLastSwitcher;

    // +1 construct & initialize properties
    public ZSLController(SharedPreferences pref) {
        // initialize mLastSwitcher value
        mLastSwitcher = getSwitcherValue(pref);
    }

    public boolean switchOn() {
        return (CameraSettings.VALUE_ON.equals(mLastSwitcher));
    }

    public boolean switchOff() {
        return !switchOn();
    }

    public void proxyResetSwitcherEnforcement(
            SharedPreferences pref, Parameters paras) {
        String valZSL = getSwitcherValue(pref);
        String devZSL = (valZSL != null) ? valZSL : CameraSettings.VALUE_OFF;
        setSwitcherValue(devZSL);
        if (paras != null) {
           // paras.set(KEY_DEVICE_ZSL, ParametersUtil.convertControllerValue(devZSL));
        }
    }

    public void proxyResetSwitcherEnforcement(
            SharedPreferences pref, Parameters paras, boolean hdrOn) {
        String valZSL = getSwitcherValue(pref);
        String devZSL = (valZSL != null && !hdrOn) ? valZSL : CameraSettings.VALUE_OFF;
        setSwitcherValue(devZSL);
        if (paras != null) {
           // paras.set(KEY_DEVICE_ZSL, ParametersUtil.convertControllerValue(devZSL));
        }
    }

    public boolean proxyResetSwitcherValue(SharedPreferences pref) {
        return setSwitcherValue(getSwitcherValue(pref));
    }

    public boolean proxyResetSwitcherValue(SharedPreferences pref, Parameters paras) {
        boolean result = false;
        String valZSL = getSwitcherValue(pref);
        result = (setSwitcherValue(valZSL) && paras != null );
        if (result) {
//            String devZSL =
//                    ParametersUtil.convertControllerValue(paras.getInt(KEY_DEVICE_ZSL));
//            if (result = (!valZSL.equals(devZSL)))
//                paras.set(KEY_DEVICE_ZSL, ParametersUtil.convertControllerValue(valZSL));
        }
        return result;
    }

    public boolean proxyResetSwitcherValue(
            SharedPreferences pref, Parameters paras, boolean hdrOn) {
        boolean result = false;
        String valZSL = getSwitcherValue(pref);
        result = (setSwitcherValue(valZSL) && paras != null);
        if (result) {
            String devZSL = CameraSettings.VALUE_OFF;
            if (hdrOn) {
                //paras.set(KEY_DEVICE_ZSL, ParametersUtil.convertControllerValue(devZSL));
            } else {
//                devZSL = ParametersUtil.convertControllerValue(paras.getInt(KEY_DEVICE_ZSL));
//                if (!valZSL.equals(devZSL))
//                    paras.set(KEY_DEVICE_ZSL, ParametersUtil.convertControllerValue(valZSL));
            }
        }
        return result;
    }

    private boolean setSwitcherValue(String switcher) {
        boolean result = (switcher != null);
        Log.d(TAG,
            String.format("reset switcher value=%s, mLastSwitcher=%s",
                new Object[] { switcher, mLastSwitcher }));
        if (result) {
            mLastSwitcher = switcher;
        }
        return result;
    }

    private String getSwitcherValue(SharedPreferences pref) {
        String result = null;
        if (pref != null) {
            result = pref.getString(
                CameraSettings.KEY_CAMERA_VIDEO_ZSL, CameraSettings.VALUE_OFF);
        }
        Log.d(TAG, "return getSwitcherValue()=" + result);
        return result;
    }
}
