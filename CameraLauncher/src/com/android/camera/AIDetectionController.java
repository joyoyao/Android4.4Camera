/**
 * 
 */

package com.android.camera;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * @author SPRD
 */
public class AIDetectionController {

    private static final String TAG = "CAM_PhotoModule_AIDetection";

    private static final String VAL_OFF = "off";
    private static final String VAL_FACE = "face";
    private static final String VAL_SMILE = "smile";
    private int sSmileScoreCount = 0;
    private static int SMILE_STEP_INCRMENT = 1;
    private static int SMILE_STEP_DECREASE = -1;
    private static int SMILE_STEP_MAX = 10;
    public static int SMILE_SCORE_X = 7;
    private String strValue = VAL_FACE;

    AIDetectionController(SharedPreferences pref) {
        getChooseValue(pref);
    }

    private String getChooseValue(SharedPreferences pref) {
        if (pref != null) {
            strValue = pref.getString(CameraSettings.KEY_CAMERA_AI_DETECT, VAL_FACE);
        }
        Log.d(TAG, " getChooseValue strValue=" + strValue);
        return strValue;
    }

    /* package */boolean isChooseOff() {
        return (VAL_OFF.equals(strValue));
    }

    /* package */boolean isChooseFace() {
        return (VAL_FACE.equals(strValue));
    }

    /* package */boolean isChooseSmile() {
        return (VAL_SMILE.equals(strValue));
    }

    private void setSmileScoreCount(int num) {
        Log.d(TAG, " setSmileScoreCount sSmileScoreCount=" + sSmileScoreCount + "   num=" + num);
        sSmileScoreCount += num;
        if (sSmileScoreCount < 0)
            sSmileScoreCount = 0;
        if (sSmileScoreCount > SMILE_STEP_MAX) {
            sSmileScoreCount = 0;
        }

    }

    /* package */void resetAIController(SharedPreferences pref) {
        getChooseValue(pref);
    }

    /* package */void resetSmileScoreCount(boolean isIncrement) {
        setSmileScoreCount(isIncrement ? SMILE_STEP_INCRMENT : SMILE_STEP_DECREASE);
    }

}
