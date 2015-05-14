/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Images.ImageColumns;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import java.io.FileDescriptor;
import android.util.Log;

import com.android.camera.ui.ModuleSwitcher;
import com.android.camera.util.CameraUtil;

public class Thumbnail {
    private static String  TAG = "Thumbmail";
    public static Bitmap createVideoThumbnailBitmap(FileDescriptor fd, int targetWidth) {
        return createVideoThumbnailBitmap(null, fd, targetWidth);
    }

    private static class Media {
        public Media(long id, int orientation, long dateTaken, Uri uri) {
            this.id = id;
            this.orientation = orientation;
            this.dateTaken = dateTaken;
            this.uri = uri;
        }

        public final long id;
        public final int orientation;
        public final long dateTaken;
        public final Uri uri;
    }
    public static Bitmap getLastThumbnailFromContentResolver(
            ContentResolver resolver) {
        /*
         * First: Camera & Panorama
         * Second: Video
         */
        Media lastMedia = getLastImageThumbnail(resolver);
        if (lastMedia == null) return null;

        Bitmap bitmap = null;

            bitmap = Images.Thumbnails.getThumbnail(
                resolver, lastMedia.id, Images.Thumbnails.MINI_KIND, null);


            return bitmap;
    }

    // @{ SPRD: bug 256269 get video thumbnail by id begin
    public static Bitmap getLastThumbnailFromContentResolver(
            ContentResolver resolver, long id, int orientation, int module) {

        Bitmap bitmap = null;
        if (module == ModuleSwitcher.VIDEO_MODULE_INDEX) {
            Log.d(TAG, ".getLastThumbnailFromContentResolver id == "+id);
            bitmap = Video.Thumbnails.getThumbnail(resolver, id, Video.Thumbnails.MINI_KIND, null);
        } else {
            bitmap = Images.Thumbnails
                    .getThumbnail(resolver, id, Images.Thumbnails.MINI_KIND, null);
            if (bitmap != null) {
                bitmap = rotateImage(bitmap, orientation);
            }
        }
        return bitmap;
    } // SPRD: bug 256269 get video thumbnail by id end @}

    private static Media getLastImageThumbnail(ContentResolver resolver) {
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
//        int i = 1216519567;
        Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
        String[] projection = new String[] {ImageColumns._ID, ImageColumns.ORIENTATION,
                ImageColumns.DATE_TAKEN};
        String selection = ImageColumns.MIME_TYPE + "='image/jpeg' AND " +
//        ImageColumns.BUCKET_ID + '=' + i;
                ImageColumns.BUCKET_ID + '=' + Storage.getConvertBucket(CameraUtil.MODE_CAMERA);
//        String order = ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";
        String order = ImageColumns._ID + " DESC," + ImageColumns.DATE_TAKEN + " DESC";

        Cursor cursor = null;
        try {
            cursor = resolver.query(query, projection, selection, null, order);
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return new Media(id, cursor.getInt(1), cursor.getLong(2),
                                 ContentUris.withAppendedId(baseUri, id));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static Bitmap createVideoThumbnailBitmap(String filePath, int targetWidth) {
        return createVideoThumbnailBitmap(filePath, null, targetWidth);
    }

    private static Bitmap createVideoThumbnailBitmap(String filePath, FileDescriptor fd,
            int targetWidth) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (filePath != null) {
                retriever.setDataSource(filePath);
            } else {
                retriever.setDataSource(fd);
            }
            bitmap = retriever.getFrameAtTime(-1);
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        if (bitmap == null) return null;

        // Scale down the bitmap if it is bigger than we need.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > targetWidth) {
            float scale = (float) targetWidth / width;
            int w = Math.round(scale * width);
            int h = Math.round(scale * height);
            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        }
        return bitmap;
    }

    /* SPRD: 新增方法用于旋转图片  @{
    *
    * @param bitmap orientation
    **/
    private static Bitmap rotateImage(Bitmap bitmap, int orientation) {
        if (orientation != 0) {
            // We only rotate the thumbnail once even if we get OOM.
            Matrix m = new Matrix();
            m.setRotate(orientation, bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f);
            try {
                    Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                    // If the rotated bitmap is the original bitmap, then it should not be recycled.
                    if (rotated != bitmap) bitmap.recycle();
                    return rotated;
            } catch (Throwable t) {
                    Log.w(TAG, "Failed to rotate thumbnail", t);
            }
        }
        return bitmap;
    }
    /* @} */
    /* SPRD: 新增方法用于旋转图片  @{
    *
    * @param bitmap orientation
    * */
    public static Bitmap proxyRotateImage(Bitmap bitmap, int orientation) {
         return rotateImage(bitmap, orientation);
    }
     /* @} */

}
