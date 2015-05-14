/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.util.Log;

import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GcamHelper;
import com.android.camera2.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides utilities and keys for Camera settings.
 */
public class CameraSettings {
	public static final int NOT_FOUND = -1;

	public static final String KEY_VERSION = "pref_version_key";
	public static final String KEY_LOCAL_VERSION = "pref_local_version_key";
	public static final String KEY_RECORD_LOCATION = "pref_camera_recordlocation_key";
	public static final String KEY_VIDEO_QUALITY = "pref_video_quality_key";
	public static final String KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL = "pref_video_time_lapse_frame_interval_key";
	public static final String KEY_PICTURE_SIZE = "pref_camera_picturesize_key";
	public static final String KEY_JPEG_QUALITY = "pref_camera_jpegquality_key";
	public static final String KEY_FOCUS_MODE = "pref_camera_focusmode_key";
	public static final String KEY_FLASH_MODE = "pref_camera_flashmode_key";
	public static final String KEY_VIDEOCAMERA_FLASH_MODE = "pref_camera_video_flashmode_key";
	public static final String KEY_WHITE_BALANCE = "pref_camera_whitebalance_key";
	public static final String KEY_SCENE_MODE = "pref_camera_scenemode_key";
	public static final String KEY_EXPOSURE = "pref_camera_exposure_key";
	public static final String KEY_TIMER = "pref_camera_timer_key";
	public static final String KEY_TIMER_SOUND_EFFECTS = "pref_camera_timer_sound_key";
	public static final String KEY_VIDEO_EFFECT = "pref_video_effect_key";
	public static final String KEY_CAMERA_ID = "pref_camera_id_key";
	public static final String KEY_CAMERA_HDR = "pref_camera_hdr_key";
	public static final String KEY_CAMERA_HDR_PLUS = "pref_camera_hdr_plus_key";
	public static final String KEY_CAMERA_FIRST_USE_HINT_SHOWN = "pref_camera_first_use_hint_shown_key";
	public static final String KEY_VIDEO_FIRST_USE_HINT_SHOWN = "pref_video_first_use_hint_shown_key";
	public static final String KEY_PHOTOSPHERE_PICTURESIZE = "pref_photosphere_picturesize_key";
	public static final String KEY_STARTUP_MODULE_INDEX = "camera.startup_module";
	public static final String EXPOSURE_DEFAULT_VALUE = "0";
	public static final int CURRENT_VERSION = 5;
	public static final int CURRENT_LOCAL_VERSION = 2;

	public static final String KEY_CAMERA_ANTIBANDING = "pref_camera_antibanding_key"; /*
																						 * SPRD
																						 * :
																						 * New
																						 * Feature
																						 * About
																						 * Antibanding
																						 */
	public static final String KEY_CAMERA_ISO = "pref_camera_iso_key"; /*
																		 * SPRD:
																		 * New
																		 * Feature
																		 * About
																		 * ISO
																		 */
	public static final String KEY_CAMERA_COLOR_EFFECT = "pref_camera_color_effect_key"; /*
																						 * SPRD
																						 * :
																						 * New
																						 * Feature
																						 * About
																						 * Color
																						 * Effect
																						 */
	public static final String KEY_CAMERA_VIDEO_ZSL = "pref_camera_video_zsl_key"; /*
																					 * SPRD
																					 * :
																					 * New
																					 * Feature
																					 * About
																					 * ZSL
																					 */
	public static final String KEY_CAMERA_STORAGE_PATH = "pref_camera_storage_path"; // SPRD:
																						// New
																						// feature
																						// about
																						// camera
																						// storage
																						// path
	public static final String KEY_VIDEO_STORAGE_PATH = "pref_video_storage_path"; // SPRD:
																					// New
																					// feature
																					// about
																					// video
																					// storage
																					// path
	// private static final String PROP_CMCC_KEY = "ro.operator";// SPRD:Add for
	// ZSL
	// private static final boolean VAL_CHECK_CMCC =
	// "cmcc".equals(SystemProperties.get(PROP_CMCC_KEY));// SPRD:Add for ZSL
	/* SPRD: uui camera setting start @{ */
	public static final String KEY_GENERAL_SETTINGS = "general_settings";
	public static final String KEY_ADVANCED_SETTINGS = "advanced_settings";
	public static final String TAG_GENERAL_SETTINGS = "setting_1";
	public static final String TAG_ADVANCED_SETINGS = "setting_2";
	/* uui camera setting end @} */

	private static final String TAG = "CameraSettings";

	/* SPRD: switch value. @{ */
	public static final String VALUE_NONE = "none";
	public static final String VALUE_ON = "on";
	public static final String VALUE_OFF = "off";
	/* @} */
	public static final String VALUE_TRUE = "true";

	public static final String KEY_CAPTURE_MODE = "capture-mode";
	public static final String KEY_HAL_FLASH_SUPPORTED = "flash-mode-supported";

	public static final String KEY_VIDEO_COLOR_EFFECT = "pref_video_color_effect_key";
	public static final String KEY_CAMERA_VIDEO_CONTRAST = "pref_camera_video_contrast_key"; /*
																							 * SPRD
																							 * :
																							 * New
																							 * Feature
																							 * About
																							 * CONTRAST
																							 */
	public static final String KEY_CAMERA_METERING = "pref_camera_metering_key"; /*
																				 * SPRD
																				 * :
																				 * New
																				 * Feature
																				 * About
																				 * Camera_metering
																				 */
	public static final String KEY_CAMERA_BRIGHTNESS = "pref_camera_brightness_key"; /*
																					 * SPRD
																					 * :
																					 * New
																					 * Feature
																					 * About
																					 * Camera
																					 * Brightness
																					 */
	public static final String KEY_CAMERA_JPEG_QUALITY = "pref_camera_jpeg_quality_key"; /*
																						 * SPRD
																						 * :
																						 * New
																						 * Feature
																						 * About
																						 * Camera_jpeg_quality
																						 */
	public static final String KEY_CAMERA_SHARPNESS = "pref_camera_sharpness_key"; /*
																					 * SPRD
																					 * :
																					 * New
																					 * Feature
																					 * About
																					 * Camera
																					 * Sharpness
																					 */
	public static final String KEY_CAMERA_SATURATION = "pref_camera_saturation_key"; /*
																					 * SPRD
																					 * :
																					 * New
																					 * Feature
																					 * About
																					 * Camera
																					 * Saturation
																					 */
	public static final String KEY_FREEZE_FRAME_DISPLAY = "pref_freeze_frame_display_key"; /*
																							 * SPRD
																							 * :
																							 * New
																							 * Feature
																							 * About
																							 * Camera
																							 * FreezePictureDisplay
																							 */
	public static final String KEY_CAMERA_CONTINUOUS_CAPTURE = "pref_camera_burst_key"; /*
																						 * SPRD
																						 * :
																						 * New
																						 * Feature
																						 * About
																						 * continuous
																						 * capture
																						 */
	public static final String KEY_VIDEO_ENCODE_TYPE = "pref_video_encode_type"; /*
																				 * SPRD
																				 * :
																				 * New
																				 * Feature
																				 * About
																				 * Camera
																				 * VideoEncodeType
																				 */
	public static final String VAL_VIDEO_ENCODE_TYPE_H264 = "h264";
	public static final String VAL_VIDEO_ENCODE_TYPE_MPEG = "mpeg";
	public static final String KEY_VIDEO_SLOW_MOTION = "pref_video_slow_motion_key"; /*
																					 * SPRD
																					 * :
																					 * New
																					 * Feature
																					 * About
																					 * Camera
																					 * SlowMotion
																					 */
	public static final String SLOWMOTION_DEFAULT_VALUE = "1";
	public static final String VAL_MULTI_FOCUS = "auto-multi";
	public static final String KEY_CAMERA_AI_DETECT = "pref_camera_ai_detect_key";/*
																				 * SPRD
																				 * :
																				 * New
																				 * Feature
																				 * face
																				 * capture
																				 */
	public static final int VALUE_INT_ON = 1;
	public static final int VALUE_INT_OFF = 0;
	public static final String DETAULT_PICTURE_NUMBER = "1";

	private final Context mContext;
	private final Parameters mParameters;
	private final CameraInfo[] mCameraInfo;
	private final int mCameraId;

	public CameraSettings(Activity activity, Parameters parameters,
			int cameraId, CameraInfo[] cameraInfo) {
		mContext = activity;
		mParameters = parameters;
		mCameraId = cameraId;
		mCameraInfo = cameraInfo;
	}

	public PreferenceGroup getPreferenceGroup(int preferenceRes) {
		PreferenceInflater inflater = new PreferenceInflater(mContext);
		PreferenceGroup group = (PreferenceGroup) inflater
				.inflate(preferenceRes);
		if (mParameters != null)
			initPreference(group);
		return group;
	}

	public static String getSupportedHighestVideoQuality(int cameraId,
			String defaultQuality) {
		// When launching the camera app first time, we will set the video
		// quality
		// to the first one (i.e. highest quality) in the supported list
		List<String> supported = getSupportedVideoQuality(cameraId);
		if (supported == null) {
			Log.e(TAG, "No supported video quality is found");
			return defaultQuality;
		}
		return supported.get(0);
	}

	public static void initialCameraPictureSize(Context context,
			Parameters parameters) {
		// When launching the camera app first time, we will set the picture
		// size to the first one in the list defined in "arrays.xml" and is also
		// supported by the driver.
		List<Size> supported = parameters.getSupportedPictureSizes();
		if (supported == null)
			return;
		for (String candidate : context.getResources().getStringArray(
				R.array.pref_camera_picturesize_entryvalues)) {
			if (setCameraPictureSize(candidate, supported, parameters)) {
				SharedPreferences.Editor editor = ComboPreferences.get(context)
						.edit();
				editor.putString(KEY_PICTURE_SIZE, candidate);
				editor.apply();
				return;
			}
		}
		Log.e(TAG, "No supported picture size found");
	}

	public static void removePreferenceFromScreen(PreferenceGroup group,
			String key) {
		removePreference(group, key);
	}

	public static boolean setCameraPictureSize(String candidate,
			List<Size> supported, Parameters parameters) {
		int index = candidate.indexOf('x');
		if (index == NOT_FOUND)
			return false;
		int width = Integer.parseInt(candidate.substring(0, index));
		int height = Integer.parseInt(candidate.substring(index + 1));
		for (Size size : supported) {
			if (size.width == width && size.height == height) {
				parameters.setPictureSize(width, height);
				return true;
			}
		}
		return false;
	}

	public static int getMaxVideoDuration(Context context) {
		int duration = 0; // in milliseconds, 0 means unlimited.
		try {
			duration = context.getResources().getInteger(
					R.integer.max_video_recording_length);
		} catch (Resources.NotFoundException ex) {
		}
		return duration;
	}

	private void initPreference(PreferenceGroup group) {
		ListPreference videoQuality = group.findPreference(KEY_VIDEO_QUALITY);
		ListPreference timeLapseInterval = group
				.findPreference(KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
		ListPreference pictureSize = group.findPreference(KEY_PICTURE_SIZE);
		ListPreference whiteBalance = group.findPreference(KEY_WHITE_BALANCE);
		ListPreference sceneMode = group.findPreference(KEY_SCENE_MODE);
		ListPreference flashMode = group.findPreference(KEY_FLASH_MODE);
		ListPreference focusMode = group.findPreference(KEY_FOCUS_MODE);
		ListPreference exposure = (ListPreference) group
				.findPreference(KEY_EXPOSURE);
		IconListPreference cameraIdPref = (IconListPreference) group
				.findPreference(KEY_CAMERA_ID);
		ListPreference videoFlashMode = group
				.findPreference(KEY_VIDEOCAMERA_FLASH_MODE);
		ListPreference videoEffect = group.findPreference(KEY_VIDEO_EFFECT);
		ListPreference cameraHdr = group.findPreference(KEY_CAMERA_HDR);
		ListPreference cameraHdrPlus = group
				.findPreference(KEY_CAMERA_HDR_PLUS);
		ListPreference prefAntibanding = group
				.findPreference(KEY_CAMERA_ANTIBANDING); /*
														 * SPRD: Init The
														 * prefAntibanding View
														 */
		ListPreference cameraISO = group.findPreference(KEY_CAMERA_ISO); /*
																		 * SPRD:
																		 * Init
																		 * The
																		 * ISO
																		 * View
																		 */
		IconListPreference cameraColorEffect = (IconListPreference) group
				.findPreference(KEY_CAMERA_COLOR_EFFECT); /*
														 * SPRD: Init The
														 * cameraColorEffect
														 * View
														 */
		ListPreference prefZSL = group.findPreference(KEY_CAMERA_VIDEO_ZSL); // SPRD:
																				// Add
																				// for
																				// ZSL
		IconListPreference videoColorEffect = (IconListPreference) group
				.findPreference(KEY_VIDEO_COLOR_EFFECT);
		ListPreference cameraContrast = group
				.findPreference(KEY_CAMERA_VIDEO_CONTRAST); /*
															 * SPRD:Init The
															 * CONTRAST View
															 */
		ListPreference metering = group.findPreference(KEY_CAMERA_METERING); /*
																			 * SPRD:
																			 * Init
																			 * The
																			 * metering
																			 * View
																			 */
		ListPreference prefBrightness = group
				.findPreference(KEY_CAMERA_BRIGHTNESS); /*
														 * SPRD:Init The
														 * brightness View
														 */
		ListPreference sharpness = group.findPreference(KEY_CAMERA_SHARPNESS); /*
																				 * SPRD
																				 * :
																				 * Init
																				 * The
																				 * Sharpness
																				 * View
																				 */
		ListPreference saturation = group.findPreference(KEY_CAMERA_SATURATION); /*
																				 * SPRD
																				 * :
																				 * Init
																				 * The
																				 * Saturation
																				 * View
																				 */
		IconListPreference freeze = (IconListPreference) group
				.findPreference(KEY_FREEZE_FRAME_DISPLAY); /*
															 * SPRD: Init The
															 * freeze frame
															 * display View
															 */
		ListPreference preBurst = group
				.findPreference(KEY_CAMERA_CONTINUOUS_CAPTURE);/*
																 * SPRD: Init
																 * CONTINUOUS_CAPTURE
																 */
		ListPreference prefSlowMotion = group
				.findPreference(KEY_VIDEO_SLOW_MOTION); /*
														 * SPRD: Init The
														 * SlowMotion View
														 */
		ListPreference preAiDetect = group.findPreference(KEY_CAMERA_AI_DETECT); /*
																				 * SPRD
																				 * :
																				 * Init
																				 * The
																				 * face
																				 * capture
																				 */
		// Since the screen could be loaded from different resources, we need
		// to check if the preference is available here
		if (videoQuality != null) {
			filterUnsupportedOptions(group, videoQuality,
					getSupportedVideoQuality(mCameraId));
		}

		if (pictureSize != null) {
			filterUnsupportedOptions(
					group,
					pictureSize,
					sizeListToStringList(mParameters.getSupportedPictureSizes()));
			filterSimilarPictureSize(group, pictureSize);
		}
		if (whiteBalance != null) {
			filterUnsupportedOptions(group, whiteBalance,
					mParameters.getSupportedWhiteBalance());
		}
		if (sceneMode != null) {
			filterUnsupportedOptions(group, sceneMode,
					mParameters.getSupportedSceneModes());
		}
		if (flashMode != null) {
			filterUnsupportedOptions(group, flashMode,
					mParameters.getSupportedFlashModes());
		}
		if (focusMode != null) {
			if (CameraUtil.isFocusAreaSupported(mParameters)) {
				filterUnsupportedOptions(group, focusMode,
						mParameters.getSupportedFocusModes());
			} else {
				// Remove the focus mode if we can use tap-to-focus.
				removePreference(group, focusMode.getKey());
			}
		}
		if (videoFlashMode != null) {
			filterUnsupportedOptions(group, videoFlashMode,
					mParameters.getSupportedFlashModes());
		}

		if (exposure != null)
			buildExposureCompensation(group, exposure);

		if (cameraIdPref != null)
			buildCameraId(group, cameraIdPref);

		if (timeLapseInterval != null) {
			resetIfInvalid(timeLapseInterval);
		}
		if (videoEffect != null) {
			filterUnsupportedOptions(group, videoEffect,
					mParameters.getSupportedColorEffects());
		}
		if (cameraHdr != null
				&& (!ApiHelper.HAS_CAMERA_HDR || !CameraUtil
						.isCameraHdrSupported(mParameters))) {
			removePreference(group, cameraHdr.getKey());
		}

		int frontCameraId = CameraHolder.instance().getFrontCameraId();
		boolean isFrontCamera = (frontCameraId == mCameraId);
		if (cameraHdrPlus != null
				&& (!ApiHelper.HAS_CAMERA_HDR_PLUS
						|| !GcamHelper.hasGcamCapture() || isFrontCamera)) {
			removePreference(group, cameraHdrPlus.getKey());
		}

		/* SPRD: Judge Hardware Support Antibanding Feature. */
		if (prefAntibanding != null) {
			filterUnsupportedOptions(group, prefAntibanding,
					mParameters.getSupportedAntibanding());
		}

		/* SPRD: Judge Hardware Support ISO Feature. */
		if (/*
			 * !android.os.TopwiseProp.getDefaultSettingBoolean(
			 * "default_camera_ISO_support")
			 */true && (cameraISO != null)) {
			removePreference(group, cameraISO.getKey());
		} else if (cameraISO != null) {
			filterUnsupportedOptions(group, cameraISO,
					null);
		}

		/* SPRD: Judge Hardware Support Color Effect Feature. */
		if (cameraColorEffect != null) {
			filterUnsupportedOptions(group, cameraColorEffect,
					mParameters.getSupportedColorEffects());
		}

		/* SPRD:Add for ZSL @{ */
		if (prefZSL != null) {
			boolean zslSupported = false;
			// if current not support ZSL, then remove ZSL item in preferences
			if (!zslSupported) {
				filterUnsupportedOptions(group, prefZSL, null);
			}
		}
		/* @} */

		/* SPRD: Judge Hardware Support Contrast Feature. */
		if (cameraContrast != null) {
			filterUnsupportedOptions(group, cameraContrast,
					null);
		}

		/* SPRD: Judge Hardware Support Camera_metering Feature. */
		// edit by topwise houyi for bug 236
		if (metering != null) {
			// Log.d(TAG,"houyi cameraContrast="+mParameters.getSupportedMetering());
			filterUnsupportedOptions(group, metering,
					null);
		}

		/* SPRD: Judge Hardware Support Camera Brightness Feature. */
		if (prefBrightness != null) {
			filterUnsupportedOptions(group, prefBrightness,
					null);
		}

		/* SPRD: Judge Hardware Support Camera Sharpness Feature. */
		if (sharpness != null) {
			filterUnsupportedOptions(group, sharpness,
					null);
		}

		/* SPRD: Judge Hardware Support Camera ColorEffects Feature. */
		if (videoColorEffect != null) {
			filterUnsupportedOptions(group, videoColorEffect,
					mParameters.getSupportedColorEffects());
		}

		if (saturation != null) {
			filterUnsupportedOptions(group, saturation,
					null);
		}
		/* SPRD: Judge Hardware Support Camera continuous captrue Feature. */
		if (preBurst != null) {
			String mValue = preBurst.getValue();
			if (mValue == null) {
				mParameters.set(KEY_CAPTURE_MODE, "1");
			} else {
				mParameters.set(KEY_CAPTURE_MODE, mValue);
			}
		}

		if (prefSlowMotion != null) {
			filterUnsupportedOptions(group, prefSlowMotion,
					null);
		}

		/* SPRD: Judge Hardware Support Camera detecte faces Feature. */
		if (preAiDetect != null) {
			boolean aiSupported = (mParameters.getMaxNumDetectedFaces() > 0);
			if (!aiSupported) {
				filterUnsupportedOptions(group, preAiDetect, null);
			}
		}
	}

	private void buildExposureCompensation(PreferenceGroup group,
			ListPreference exposure) {
		int max = mParameters.getMaxExposureCompensation();
		int min = mParameters.getMinExposureCompensation();
		if (max == 0 && min == 0) {
			removePreference(group, exposure.getKey());
			return;
		}
		float step = mParameters.getExposureCompensationStep();

		// show only integer values for exposure compensation
		int maxValue = Math.min(3, (int) Math.floor(max * step));
		int minValue = Math.max(-3, (int) Math.ceil(min * step));
		CharSequence entries[] = new CharSequence[maxValue - minValue + 1];
		CharSequence entryValues[] = new CharSequence[maxValue - minValue + 1];
		for (int i = minValue; i <= maxValue; ++i) {
			entryValues[i - minValue] = Integer.toString(Math.round(i / step));
			StringBuilder builder = new StringBuilder();
			if (i > 0)
				builder.append('+');
			entries[i - minValue] = builder.append(i).toString();
		}
		exposure.setEntries(entries);
		exposure.setEntryValues(entryValues);
	}

	private void buildCameraId(PreferenceGroup group,
			IconListPreference preference) {
		int numOfCameras = mCameraInfo.length;
		if (numOfCameras < 2) {
			removePreference(group, preference.getKey());
			return;
		}

		CharSequence[] entryValues = new CharSequence[numOfCameras];
		for (int i = 0; i < numOfCameras; ++i) {
			entryValues[i] = "" + i;
		}
		preference.setEntryValues(entryValues);
	}

	private static boolean removePreference(PreferenceGroup group, String key) {
		for (int i = 0, n = group.size(); i < n; i++) {
			CameraPreference child = group.get(i);
			if (child instanceof PreferenceGroup) {
				if (removePreference((PreferenceGroup) child, key)) {
					return true;
				}
			}
			if (child instanceof ListPreference
					&& ((ListPreference) child).getKey().equals(key)) {
				group.removePreference(i);
				return true;
			}
		}
		return false;
	}

	private void filterUnsupportedOptions(PreferenceGroup group,
			ListPreference pref, List<String> supported) {

		// Remove the preference if the parameter is not supported or there is
		// only one options for the settings.
		if (supported == null || supported.size() <= 1) {
			removePreference(group, pref.getKey());
			return;
		}

		pref.filterUnsupported(supported);
		if (pref.getEntries().length <= 1) {
			removePreference(group, pref.getKey());
			return;
		}

		resetIfInvalid(pref);
	}

	private void filterSimilarPictureSize(PreferenceGroup group,
			ListPreference pref) {
		pref.filterDuplicated();
		if (pref.getEntries().length <= 1) {
			removePreference(group, pref.getKey());
			return;
		}
		resetIfInvalid(pref);
	}

	private void resetIfInvalid(ListPreference pref) {
		// Set the value to the first entry if it is invalid.
		String value = pref.getValue();
		if (pref.findIndexOfValue(value) == NOT_FOUND) {
			pref.setValueIndex(0);
		}
	}

	private static List<String> sizeListToStringList(List<Size> sizes) {
		ArrayList<String> list = new ArrayList<String>();
		for (Size size : sizes) {
			list.add(String.format(Locale.ENGLISH, "%dx%d", size.width,
					size.height));
		}
		return list;
	}

	public static void upgradeLocalPreferences(SharedPreferences pref) {
		int version;
		try {
			version = pref.getInt(KEY_LOCAL_VERSION, 0);
		} catch (Exception ex) {
			version = 0;
		}
		if (version == CURRENT_LOCAL_VERSION)
			return;

		SharedPreferences.Editor editor = pref.edit();
		if (version == 1) {
			// We use numbers to represent the quality now. The quality
			// definition is identical to
			// that of CamcorderProfile.java.
			editor.remove("pref_video_quality_key");
		}
		editor.putInt(KEY_LOCAL_VERSION, CURRENT_LOCAL_VERSION);
		editor.apply();
	}

	public static void upgradeGlobalPreferences(SharedPreferences pref) {
		upgradeOldVersion(pref);
		upgradeCameraId(pref);
	}

	private static void upgradeOldVersion(SharedPreferences pref) {
		int version;
		try {
			version = pref.getInt(KEY_VERSION, 0);
		} catch (Exception ex) {
			version = 0;
		}
		if (version == CURRENT_VERSION)
			return;

		SharedPreferences.Editor editor = pref.edit();
		if (version == 0) {
			// We won't use the preference which change in version 1.
			// So, just upgrade to version 1 directly
			version = 1;
		}
		if (version == 1) {
			// Change jpeg quality {65,75,85} to {normal,fine,superfine}
			String quality = pref.getString(KEY_JPEG_QUALITY, "85");
			if (quality.equals("65")) {
				quality = "normal";
			} else if (quality.equals("75")) {
				quality = "fine";
			} else {
				quality = "superfine";
			}
			editor.putString(KEY_JPEG_QUALITY, quality);
			version = 2;
		}
		if (version == 2) {
			editor.putString(
					KEY_RECORD_LOCATION,
					pref.getBoolean(KEY_RECORD_LOCATION, false) ? RecordLocationPreference.VALUE_ON
							: RecordLocationPreference.VALUE_NONE);
			version = 3;
		}
		if (version == 3) {
			// Just use video quality to replace it and
			// ignore the current settings.
			editor.remove("pref_camera_videoquality_key");
			editor.remove("pref_camera_video_duration_key");
		}

		editor.putInt(KEY_VERSION, CURRENT_VERSION);
		editor.apply();
	}

	private static void upgradeCameraId(SharedPreferences pref) {
		// The id stored in the preference may be out of range if we are running
		// inside the emulator and a webcam is removed.
		// Note: This method accesses the global preferences directly, not the
		// combo preferences.
		int cameraId = readPreferredCameraId(pref);
		if (cameraId == 0)
			return; // fast path

		int n = CameraHolder.instance().getNumberOfCameras();
		if (cameraId < 0 || cameraId >= n) {
			writePreferredCameraId(pref, 0);
		}
	}

	public static int readPreferredCameraId(SharedPreferences pref) {
		return Integer.parseInt(pref.getString(KEY_CAMERA_ID, "0"));
	}

	public static void writePreferredCameraId(SharedPreferences pref,
			int cameraId) {
		Editor editor = pref.edit();
		editor.putString(KEY_CAMERA_ID, Integer.toString(cameraId));
		editor.apply();
	}

	public static int readExposure(ComboPreferences preferences) {
		String exposure = preferences.getString(CameraSettings.KEY_EXPOSURE,
				EXPOSURE_DEFAULT_VALUE);
		try {
			return Integer.parseInt(exposure);
		} catch (Exception ex) {
			Log.e(TAG, "Invalid exposure: " + exposure);
		}
		return 0;
	}

	public static String readPreferredBurst(ComboPreferences preferences) {
		return preferences.getString(
				CameraSettings.KEY_CAMERA_CONTINUOUS_CAPTURE,
				CameraSettings.VALUE_OFF);
	}

	public static void restorePreferences(Context context,
			ComboPreferences preferences, Parameters parameters) {
		int currentCameraId = readPreferredCameraId(preferences);

		// Clear the preferences of both cameras.
		int backCameraId = CameraHolder.instance().getBackCameraId();
		if (backCameraId != -1) {
			preferences.setLocalId(context, backCameraId);
			Editor editor = preferences.edit();
			editor.clear();
			editor.apply();
		}
		int frontCameraId = CameraHolder.instance().getFrontCameraId();
		if (frontCameraId != -1) {
			preferences.setLocalId(context, frontCameraId);
			Editor editor = preferences.edit();
			editor.clear();
			editor.apply();
		}

		// Switch back to the preferences of the current camera. Otherwise,
		// we may write the preference to wrong camera later.
		preferences.setLocalId(context, currentCameraId);

		upgradeGlobalPreferences(preferences.getGlobal());
		upgradeLocalPreferences(preferences.getLocal());

		// Write back the current camera id because parameters are related to
		// the camera. Otherwise, we may switch to the front camera but the
		// initial picture size is that of the back camera.
		initialCameraPictureSize(context, parameters);
		writePreferredCameraId(preferences, currentCameraId);
	}

	private static ArrayList<String> getSupportedVideoQuality(int cameraId) {
		ArrayList<String> supported = new ArrayList<String>();
		// Check for supported quality
		if (CamcorderProfile.hasProfile(cameraId,
				CamcorderProfile.QUALITY_1080P)) {
			supported.add(Integer.toString(CamcorderProfile.QUALITY_1080P));
		}
		if (CamcorderProfile
				.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
			supported.add(Integer.toString(CamcorderProfile.QUALITY_720P));
		}
		if (CamcorderProfile
				.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
			supported.add(Integer.toString(CamcorderProfile.QUALITY_480P));
		}
		/** SPRD: 255888 FrontCamera Don't Show VideoSize In Recording. @{ */
		if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_CIF)) {
			supported.add(Integer.toString(CamcorderProfile.QUALITY_CIF));
		}
		if (CamcorderProfile
				.hasProfile(cameraId, CamcorderProfile.QUALITY_QCIF)) {
			supported.add(Integer.toString(CamcorderProfile.QUALITY_QCIF));
		}
		if (CamcorderProfile
				.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA)) {
			supported.add(Integer.toString(CamcorderProfile.QUALITY_QVGA));
		}
		/** }@ */
		return supported;
	}

	// fixed bug 188360 start
	public static int getRecorderAudioSoruceBySlowMotion(
			ComboPreferences preferences) {
		int result = MediaRecorder.AudioSource.CAMCORDER;
		if (preferences != null) {
			String sSlowMotion = preferences.getString(
					CameraSettings.KEY_VIDEO_SLOW_MOTION,
					SLOWMOTION_DEFAULT_VALUE);
			try {
				if (Integer.parseInt(sSlowMotion) > 1) {
					//result = MediaRecorder.AudioSource.RECORD_NO_AUDIO;
				}
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return result;
	}
	// fixed bug 188360 end
}
