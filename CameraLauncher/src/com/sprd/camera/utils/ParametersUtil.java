/*Created by Spreadst*/

package com.sprd.camera.utils;

import com.android.camera.CameraSettings;

public class ParametersUtil {

    public static final String convertControllerValue(int value) {
        String result = CameraSettings.VALUE_OFF; // default set to device is
                                                  // off
        if (value == 1) {
            result = CameraSettings.VALUE_ON;
        }
        return result;
    }

    public static final int convertControllerValue(String value) {
        int result = 0; // default set to device is off
        if (CameraSettings.VALUE_ON.equals(value)) {
            result = 1;
        }
        return result;
    }
}
