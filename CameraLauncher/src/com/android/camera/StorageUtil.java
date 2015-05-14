package com.android.camera;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.content.Context;

import com.android.camera.util.CameraUtil;

public class StorageUtil {

    private static final boolean DEBUG = true;
    private static final String TAG = "StorageUtil";

    private static StorageUtil mInstance;
    public static synchronized StorageUtil newInstance() {
        if (mInstance == null) {
            mInstance = new StorageUtil();
        }
        return mInstance;
    }

    private static final String DIRECTORY_DCIM = Environment.DIRECTORY_DCIM;
    private static final String DIRECTORY_CAMERA = "/Camera";
    private static final String DIRECTORY_DEFAULT =
        "/".concat(DIRECTORY_DCIM).concat(DIRECTORY_CAMERA);
    private static final String CAMERA_DEFAULT_STORAGE =
        Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).toString();

    private static final int VAL_DEFAULT_ROOT_DIRECTORY_SIZE = 2;
    public static final String KEY_DEFAULT_INTERNAL = "Internal";
    public static final String KEY_DEFAULT_EXTERNAL = "External";

    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;

    // SPRD: bug 256269 Listener for init over Imp by CameraActivity
    public interface UpdataPathListener {
        public void UpdatPath();
    }

    // default construct
    public StorageUtil() {
        CameraUtil.P(DEBUG, TAG, "create StorageUtil instance");
    }

    private String mVideoStorage;
    private String mCameraStorage;
    private UpdataPathListener mUpdataPathListener;

    // SPRD: bug 256269 set Listener
    public void setmUpdataPathListener(UpdataPathListener mUpdataPathListener) {
        this.mUpdataPathListener = mUpdataPathListener;
    }

    public void initialize(ListPreference preference) {
        if (preference != null) {
            String key = preference.getKey();
            String value = preference.getValue();

            int mode = (!CameraSettings.KEY_VIDEO_STORAGE_PATH.equals(key) ?
                CameraUtil.MODE_CAMERA : CameraUtil.MODE_VIDEO);

            if (isImageStorage(mode) && mCameraStorage == null) {
                mCameraStorage = value;
            }

            if (isVideoStorage(mode) && mVideoStorage == null) {
                mVideoStorage = value;
            }

            boolean needInitialize = (value == null);
            if (needInitialize) {
                value = getDefaultRootDirectory();
                if (isImageStorage(mode)) mCameraStorage = value;
                if (isVideoStorage(mode)) mVideoStorage = value;
            }
            // SPRD: bug 256269 do UpdatPath function in CameraActivity
            mUpdataPathListener.UpdatPath();
            // for test print log
            if (DEBUG) {
                String message =
                    String.format("initialize mCameraStorage=%s, mVideoStorage=%s",
                        new Object[] { mCameraStorage, mVideoStorage });
                CameraUtil.P(DEBUG, TAG, message);
            }
        }
    }

    public void resetStorageByMode(int mode, String path) {
        if (isImageStorage(mode) && !mCameraStorage.equals(path)) {
            mCameraStorage = path;
        }

        if (isVideoStorage(mode) && !mVideoStorage.equals(path)) {
            mVideoStorage = path;
        }
        // SPRD: bug 256269 do UpdatPath function in CameraActivity
//        mUpdataPathListener.UpdatPath();
        syncThumbnailPath();
    }

    public void syncThumbnailPath() {
        if (mUpdataPathListener != null) {
            mUpdataPathListener.UpdatPath();
        }
    }

    public long getAvailableSpace(int mode) {
        String path = null;
        if (isImageStorage(mode)) {
            path = mCameraStorage;
        }
        if (isVideoStorage(mode)) {
            path = mVideoStorage;
        }

        String state = null;
        Map<String, String> roots = supportedRootDirectory();
        String internal = roots.get(KEY_DEFAULT_INTERNAL);
        String external = roots.get(KEY_DEFAULT_EXTERNAL);
        if (internal == null && external == null) {
            return UNAVAILABLE;
        } else if (path != null && internal != null && path.contains(internal)) {
            state = Environment.getExternalStorageState();
        } else if (path != null && external != null && path.contains(external)) {
            state = Environment.getExternalStorageState();
        }
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }

        File dir = new File(path);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }
        try {
            StatFs stat = new StatFs(path);
//            return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong());
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access storage", e);
        }
        return UNKNOWN_SIZE;
    }

    public long getInternalAvailableSpace() {

          String spath = Environment.getExternalStorageDirectory().getAbsolutePath();
          String state = null;

          File dir = new File(spath);
          dir.mkdirs();
          if (!dir.isDirectory() || !dir.canWrite()) {
              return UNAVAILABLE;
          }
          try {
              StatFs stat = new StatFs(spath);
//              return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong());
              return stat.getAvailableBlocks() * (long) stat.getBlockSize();
          } catch (Exception e) {
              Log.i(TAG, "Fail to access storage", e);
          }
          return UNKNOWN_SIZE;
      }

    public String getStorageByMode(int mode) {
        String path = null;

        if (isImageStorage(mode)) {
            path = mCameraStorage;
        }

        if (isVideoStorage(mode)) {
            path = mVideoStorage;
        }

        return path;
    }

    public Map<String, String> supportedRootDirectory() {
        Map<String, String> result = null;
        String
            internal_state = Environment.getExternalStorageState(),
            external_state = Environment.getExternalStorageState();
        boolean
            internal_mounted = (Environment.MEDIA_MOUNTED.equals(internal_state)),
            external_mounted = (Environment.MEDIA_MOUNTED.equals(external_state));
        String
            internal = (internal_mounted ?
                Environment.getExternalStorageDirectory().getAbsolutePath() : null),
            external = (external_mounted ?
                Environment.getExternalStorageDirectory().getAbsolutePath() : null);
        if (DEBUG) {
            String message =
                String.format("supported internal{%s, %s}, external{%s, %s}",
                    new Object[] { internal_mounted, internal, external_mounted, external});
            CameraUtil.P(DEBUG, TAG, message);
        }
        result = new HashMap<String, String>(VAL_DEFAULT_ROOT_DIRECTORY_SIZE);
        result.put(KEY_DEFAULT_INTERNAL, internal);
        result.put(KEY_DEFAULT_EXTERNAL, external);
        return result;
    }

    public String getDefaultRootDirectory() {
        String result = null;
        Map<String, String> roots = supportedRootDirectory();
        String internal = roots.get(KEY_DEFAULT_INTERNAL);
        String external = roots.get(KEY_DEFAULT_EXTERNAL);
        if (internal != null) result = (internal.concat(DIRECTORY_DEFAULT));
        if (external != null) result = (external.concat(DIRECTORY_DEFAULT));
        if (internal == null && external == null) result = CAMERA_DEFAULT_STORAGE;
        return result;
    }

    private boolean isImageStorage(int mode) {
        return (CameraUtil.MODE_VIDEO != mode);
    }

    private boolean isVideoStorage(int mode) {
        return (CameraUtil.MODE_VIDEO == mode);
    }

    public String getStoragePath(Context ctx, int mode) {
        String result = null;
        StoragePathPreference mPreference = StoragePathPreference.getInstance(ctx);
        result = mPreference.readStorage(mode);
        if (Storage.VAL_DEFAULT_CONFIG_PATH.equals(result)) {
            result = getDefaultRootDirectory();
        }
        if (isImageStorage(mode)) {
            mCameraStorage = result;
        }
        if (isVideoStorage(mode)) {
            mVideoStorage = result;
        }
        return result;
    }
}
