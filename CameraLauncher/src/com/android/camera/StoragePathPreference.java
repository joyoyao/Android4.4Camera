package com.android.camera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;

public class StoragePathPreference extends CameraPreference {

    public static interface StoragePathChangedListener {
        // SPRD: "isCancle" is for resetStorageByMode
        public void storageChanged(String path, boolean isCancle);
    }

    private static final String TAG = "StoragePathPreference";
    private static final String PREF_NAME = "_preferences_storage_path";

    // this is singleton
    private static StoragePathPreference mInstance;
    public synchronized static StoragePathPreference getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new StoragePathPreference(ctx, null);
        }
        return mInstance;
    }

    public static final int VAL_STORAGE_UNKNOW_MODE = -1;
    /*
     * saved paths
     * { (CAMERA, 1), (VIDEO, 2) }
     */
    public static final int KEY_STORAGE_PATH_CAMERA  = 1;
    public static final int KEY_STPRAGE_PATH_VIDEO   = 2;

    private SharedPreferences mPreference;
    public static final String
        KEY_PREF_STORAGE_PATH_DEFAULT = "key_pref_storage_path";
    private static final String
        KEY_PREF_STORAGE_PATH = (KEY_PREF_STORAGE_PATH_DEFAULT.concat("_"));

    // default construct
    private StoragePathPreference(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        String prefName = ctx.getPackageName().concat(PREF_NAME);
        mPreference = ctx.getSharedPreferences(prefName, Activity.MODE_PRIVATE);
    }

    public synchronized String readStorage(int k) {
        String key =
            ((k != VAL_STORAGE_UNKNOW_MODE) ?
                (KEY_PREF_STORAGE_PATH + k) : KEY_PREF_STORAGE_PATH_DEFAULT);
        String result = mPreference.getString(key, Storage.VAL_DEFAULT_CONFIG_PATH);
        Log.d(TAG,
            String.format("readStorage() key = %s, result = %s", new Object[] { key, result }));
        return result;
    }

    public synchronized void writeStorage(int k, String path) {
        String key =
            ((k != VAL_STORAGE_UNKNOW_MODE) ?
                (KEY_PREF_STORAGE_PATH + k) : KEY_PREF_STORAGE_PATH_DEFAULT);
        if (Storage.KEY_DEFAULT_INTERNAL.equals(path)){
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else if(Storage.KEY_DEFAULT_EXTERNAL.equals(path)) {
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        if (path == null) path = Storage.DIRECTORY;
        String oPath = readStorage(k); // read old path
        Log.d(TAG,
            String.format("writeStorage() key = %s, nPath = %s, oPath = %s", new Object[] { key, path, oPath }));
        if (!path.equals(oPath)) {
            Editor editor = mPreference.edit();
            editor.putString(key, path);
            editor.putString(KEY_PREF_STORAGE_PATH_DEFAULT, path); // reset last time value
            editor.commit();
        }
    }

    public void clear() {
        if (mPreference != null) {
            Editor editor = mPreference.edit();
            editor.clear();
            editor.apply();
        }
    }

    @Override
    public void reloadValue() { /* ignore */ }

}
