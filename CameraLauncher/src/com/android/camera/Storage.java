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

package com.android.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.android.camera.data.LocalData;
import com.android.camera.exif.ExifInterface;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;

public class Storage {
    private static final String TAG = "CameraStorage";

    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    // SPRD: Internal root path
    public static final String INTERNAL = Environment.getExternalStorageDirectory().toString();

    // SPRD: External root path
    public static final String EXTERNAL = Environment.getExternalStorageDirectory().toString();

    public static final String DIRECTORY = DCIM + "/Camera";
    public static final String JPEG_POSTFIX = ".jpg";

    // Match the code in MediaProvider.computeBucketValues().
    public static final String BUCKET_ID =
            String.valueOf(DIRECTORY.toLowerCase().hashCode());

    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long LOW_STORAGE_THRESHOLD_BYTES = 20 * 1024 *1024;

    /* SPRD: porting uui setting start @{ */
    public static final String DIRECTORY_CAMERA = "/Camera";
    public static final String DIRECTORY_STORAGE =
        "/".concat(Environment.DIRECTORY_DCIM).concat(DIRECTORY_CAMERA);
    public static final String VAL_DEFAULT_CONFIG_PATH = "DEFAULT_PATH";

    public static final int VAL_DEFAULT_ROOT_DIRECTORY_SIZE = 2;
    public static final String KEY_DEFAULT_INTERNAL = StorageUtil.KEY_DEFAULT_INTERNAL;
    public static final String KEY_DEFAULT_EXTERNAL = StorageUtil.KEY_DEFAULT_EXTERNAL;
    /* porting uui setting end @} */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void setImageSize(ContentValues values, int width, int height) {
        // The two fields are available since ICS but got published in JB
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            values.put(MediaColumns.WIDTH, width);
            values.put(MediaColumns.HEIGHT, height);
        }
    }

    public static void writeFile(String path, byte[] jpeg, ExifInterface exif) {
        if (exif != null) {
            try {
                exif.writeExif(jpeg, path);
            } catch (Exception e) {
                Log.e(TAG, "Failed to write data", e);
            }
        } else {
            writeFile(path, jpeg);
        }
    }

    public static void writeFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        }
    }

    // Save the image and add it to the MediaStore.
    public static Uri addImage(ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif, byte[] jpeg, int width,
            int height, int mode) {
        return addImage(resolver, title, date, location, orientation, exif, jpeg, width, height,
                LocalData.MIME_TYPE_JPEG, mode);
    }

    // Save the image with a given mimeType and add it the MediaStore.
    public static Uri addImage(ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif, byte[] jpeg, int width,
            int height, String mimeType, int mode) {
        String path = generateFilepath(title, mode);
        writeFile(path, jpeg, exif);
        return addImage(resolver, title, date, location, orientation,
                jpeg.length, path, width, height, mimeType);
    }

    // Get a ContentValues object for the given photo data
    public static ContentValues getContentValuesForData(String title,
            long date, Location location, int orientation, int jpegLength,
            String path, int width, int height, String mimeType) {

        ContentValues values = new ContentValues(11);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, title + JPEG_POSTFIX);
        values.put(ImageColumns.DATE_TAKEN, date);
        values.put(ImageColumns.MIME_TYPE, mimeType);
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.SIZE, jpegLength);

        setImageSize(values, width, height);

        if (location != null) {
            values.put(ImageColumns.LATITUDE, location.getLatitude());
            values.put(ImageColumns.LONGITUDE, location.getLongitude());
        }
        return values;
    }

    // Add the image to media store.
    public static Uri addImage(ContentResolver resolver, String title,
            long date, Location location, int orientation, int jpegLength,
            String path, int width, int height, String mimeType) {
        // Insert into MediaStore.
        ContentValues values =
                getContentValuesForData(title, date, location, orientation, jpegLength, path,
                        width, height, mimeType);

         return insertImage(resolver, values);
    }

    // Overwrites the file and updates the MediaStore, or inserts the image if
    // one does not already exist.
    public static void updateImage(Uri imageUri, ContentResolver resolver, String title, long date,
            Location location, int orientation, ExifInterface exif, byte[] jpeg, int width,
            int height, String mimeType) {
        String path = generateFilepath(title, CameraUtil.MODE_CAMERA);
        writeFile(path, jpeg, exif);
        updateImage(imageUri, resolver, title, date, location, orientation, jpeg.length, path,
                width, height, mimeType);
    }

    // Updates the image values in MediaStore, or inserts the image if one does
    // not already exist.
    public static void updateImage(Uri imageUri, ContentResolver resolver, String title,
            long date, Location location, int orientation, int jpegLength,
            String path, int width, int height, String mimeType) {

        ContentValues values =
                getContentValuesForData(title, date, location, orientation, jpegLength, path,
                        width, height, mimeType);

        // Update the MediaStore
        int rowsModified = resolver.update(imageUri, values, null, null);

        if (rowsModified == 0) {
            // If no prior row existed, insert a new one.
            Log.w(TAG, "updateImage called with no prior image at uri: " + imageUri);
            insertImage(resolver, values);
        } else if (rowsModified != 1) {
            // This should never happen
            throw new IllegalStateException("Bad number of rows (" + rowsModified
                    + ") updated for uri: " + imageUri);
        }
    }

    public static void deleteImage(ContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
        } catch (Throwable th) {
            Log.e(TAG, "Failed to delete image: " + uri);
        }
    }

    public static String generateFilepath(String title, int mode) {
        StorageUtil util = StorageUtil.newInstance();
        String directory = util.getStorageByMode(mode);
        Log.e(TAG,"directory="+directory+" mode="+mode);
//        return DIRECTORY + '/' + title + ".jpg";
        return directory + '/' + title + ".jpg";
    }

//    public static long getAvailableSpace() {
//        String state = Environment.getExternalStorageState();
//        Log.d(TAG, "External storage state=" + state);
//        if (Environment.MEDIA_CHECKING.equals(state)) {
//            return PREPARING;
//        }
//        if (!Environment.MEDIA_MOUNTED.equals(state)) {
//            return UNAVAILABLE;
//        }
//
//        File dir = new File(DIRECTORY);
//        dir.mkdirs();
//        if (!dir.isDirectory() || !dir.canWrite()) {
//            return UNAVAILABLE;
//        }
//
//        try {
//            StatFs stat = new StatFs(DIRECTORY);
//            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
//        } catch (Exception e) {
//            Log.i(TAG, "Fail to access external storage", e);
//        }
//        return UNKNOWN_SIZE;
//    }

    /**
     * OSX requires plugged-in USB storage to have path /DCIM/NNNAAAAA to be
     * imported. This is a temporary fix for bug#1655552.
     */
    public static void ensureOSXCompatible() {
        File nnnAAAAA = new File(DCIM, "100ANDRO");
        if (!(nnnAAAAA.exists() || nnnAAAAA.mkdirs())) {
            Log.e(TAG, "Failed to create " + nnnAAAAA.getPath());
        }
    }

    private static Uri insertImage(ContentResolver resolver, ContentValues values) {
        Uri uri = null;
        try {
            uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }
        return uri;
    }

      /* SPRD: porting uui setting @{ */
//    public static synchronized String getDisplayStroagePath(Context ctx, int mode) {
//        String result = getStoragePath(ctx, mode);
//        if (result != null) {
//            Map<String, String> roots = getSupportedRootDirectory(false);
//            Set<Entry<String, String>> entries = roots.entrySet();
//            for (Entry<String, String> item : entries) {
//                if (result.equals(item.getValue())) {
//                    result = item.getKey();
//                    break;
//                }
//            }
//        }
//        return result;
//    }
//
//    public synchronized static Map<String, String>
//            getSupportedRootDirectory(boolean mounted) {
//
//        String
//            internal_state = Environment.getInternalStoragePathState(),
//            external_state = Environment.getExternalStoragePathState();
//        boolean
//            internal_monted = (Environment.MEDIA_MOUNTED.equals(internal_state)),
//            external_monted = (Environment.MEDIA_MOUNTED.equals(external_state));
//        String
//            internal = (internal_monted ?
//                Environment.getInternalStoragePath().getAbsolutePath() : null),
//            external = (external_monted ?
//                Environment.getExternalStoragePath().getAbsolutePath() : null);
//        Log.d(TAG,
//            String.format("getSupportedRootDirectory internal{%s, %s}, external{%s, %s}",
//                new Object[] { internal_state, internal, external_state, external}));
//
//        Map<String, String> result = new HashMap<String, String>(VAL_DEFAULT_ROOT_DIRECTORY_SIZE);
//        result.put(KEY_DEFAULT_INTERNAL, internal);
//        result.put(KEY_DEFAULT_EXTERNAL, external);
//        return result;
//    }

    public synchronized static String getConvertBucket(int mode) {
        String result = null;
        StorageUtil util = StorageUtil.newInstance();
        result = util.getStorageByMode(mode);
//        result = (CameraUtil.MODE_VIDEO != mode ? mStorageCameraPath : mStorageVideoPath);
        Log.d(TAG, String.format("getConvertBucket() mode = %d, path = %s", new Object[] { mode, result }));
        if (result != null) result = String.valueOf(result.toLowerCase().hashCode());
        return result;
    }

    // Match the code in MediaProvider.computeBucketValues().
//    private static String mStorageCameraPath = null;
//    private static String mStorageVideoPath = null;
//    public synchronized static String getStoragePath(Context ctx, int mode) {
//        String result = null;
//        StoragePathPreference mPreference = StoragePathPreference.getInstance(ctx);
//        result = mPreference.readStorage(mode);
//        /*
//         * we must execute get default path or convert path method, because sometimes:
//         * 1、result equals default_storage_path         --> get default
//         * 2、result equals "internal" or "external"     --> convert path
//         * 3、result is NULL                             --> get default
//         */
//        result =
//            (result == null ?
//                getDefaultStoragePath(ctx, mode) :
//                    getConvertStoragePath(result, ctx, mode));
//        switch (mode) {
//            case CameraUtil.MODE_VIDEO:
//                mStorageVideoPath = result;
//                break;
//            case CameraUtil.MODE_CAMERA:
//                mStorageCameraPath = result;
//                break;
//        }
//        Log.d(TAG, "getStoragePath() result = " + result);
//        return result;
//    }

//    public synchronized static String getDefaultStoragePath(Context ctx, int mode) {
//        String result = null, internal = null, external = null;
//        Map<String, String> roots = getSupportedRootDirectory(true);
//        // internal path
//        internal = roots.get(KEY_DEFAULT_INTERNAL);
//        // external path
//        external = roots.get(KEY_DEFAULT_EXTERNAL);
//        // check internal path is valid
//        if (internal != null) result = internal;
//        // check external path is valid
//        if (external != null) result = external;
//        // if internal & external is invalid, so result is default
//        if (result != null) result = result.concat(DIRECTORY_STORAGE);
//        else result = DIRECTORY;
//        return result;
//    }

//    private synchronized static String
//            getConvertStoragePath(String path, Context ctx, int mode) {
//        String result = path;
//        if (path != null) {
//            String key = null;
//            if (KEY_DEFAULT_INTERNAL.equals(path)) {
//                key = KEY_DEFAULT_INTERNAL;
//            } else if (KEY_DEFAULT_EXTERNAL.equals(path)) {
//                key = KEY_DEFAULT_EXTERNAL;
//            }
//
//            if (key != null) {
//                Map<String, String> roots = getSupportedRootDirectory(false);
//                result = roots.get(key);
//            } else if (VAL_DEFAULT_CONFIG_PATH.equals(path)) {
//                result = getDefaultStoragePath(ctx, mode);
//            }
//        }
//        return result;
//    }
    /* porting uui setting end @} */
}
