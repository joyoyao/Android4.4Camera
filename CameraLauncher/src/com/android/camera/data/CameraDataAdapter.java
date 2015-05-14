/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera.data;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.android.camera.Storage;
import com.android.camera.app.PlaceholderManager;
import com.android.camera.ui.FilmStripView.ImageData;

import java.util.ArrayList;
import java.util.Comparator;

import com.android.camera.StorageUtil;
import com.android.camera.ui.ModuleSwitcher;
import com.android.camera.util.CameraUtil;

/**
 * A {@link LocalDataAdapter} that provides data in the camera folder.
 */
public class CameraDataAdapter implements LocalDataAdapter {
    private static final String TAG = "CAM_CameraDataAdapter";

    private static final int DEFAULT_DECODE_SIZE = 1600;
    private static final String[] CAMERA_PATH = { Storage.DIRECTORY + "%" };

    // SPRD: bug 256269 lock for mFristData
    Object lock = new Object();
    private LocalDataList mImages;
    private LocalDataList mSecureImages;
    private LocalDataList mSecureVideos;

    private Listener mListener;
    private Drawable mPlaceHolder;
    // SPRD: bug 256269 the current module
    private int mModule;
    // SPRD: bug the frist data of DataAdapter
    private LocalData mFristData;

    private int mSuggestedWidth = DEFAULT_DECODE_SIZE;
    private int mSuggestedHeight = DEFAULT_DECODE_SIZE;

    private LocalData mLocalDataToDelete;
    private boolean mIsSecureCamera;
    private String mLastImagePath;
    private String mLastVideopath;

    public CameraDataAdapter(Drawable placeHolder,int module) {
        mImages = new LocalDataList();
        mSecureImages = new LocalDataList();
        mSecureVideos = new LocalDataList();
        mPlaceHolder = placeHolder;
        // @{ SPRD: bug 256269
        mModule = module;
        mFristData = null; // SPRD: bug 256269 @}
    }

    // SPRD: bug 256269 set the current camera module
    public void setAdapterModule(int module) {
        mModule = module;
    }

    // SPRD: bug 256269 updata data adapter
    public void UpdataAdapter() {
        getModulePath(mModule);
    }

    // SPRD: bug 256269 updat CAMERA_PATH by current camera module
    public void getModulePath(int module) {
        StorageUtil mStorageUtil = StorageUtil.newInstance();
        String modulePath = mStorageUtil.getStorageByMode(module);
        if(modulePath == null) {
            return;
        }
        if (modulePath.equals("Internal"))
            CAMERA_PATH[0] = Storage.INTERNAL+"%";
        else if(modulePath.equals("External"))
            CAMERA_PATH[0] = Storage.EXTERNAL+"%";
        else
            CAMERA_PATH[0] = modulePath+"%";
    }

    @Override
    public void requestLoad(ContentResolver resolver) {
        QueryTask qtask = new QueryTask();
        qtask.execute(resolver);
    }

    @Override
    public void requestSecureImageLoad(){
        mIsSecureCamera = true;
        LodeSecureImageTask task = new LodeSecureImageTask();
        task.execute();
    }

    @Override
    public LocalData getLocalData(int dataID) {
        if (dataID < 0 || dataID >= mImages.size()) {
            return null;
        }

        return mImages.get(dataID);
    }

    @Override
    public int getTotalNumber() {
        return mImages.size();
    }

    @Override
    public ImageData getImageData(int id) {
        return getLocalData(id);
    }

    @Override
    public void suggestViewSizeBound(int w, int h) {
        if (w <= 0 || h <= 0) {
            mSuggestedWidth  = mSuggestedHeight = DEFAULT_DECODE_SIZE;
        } else {
            mSuggestedWidth = (w < DEFAULT_DECODE_SIZE ? w : DEFAULT_DECODE_SIZE);
            mSuggestedHeight = (h < DEFAULT_DECODE_SIZE ? h : DEFAULT_DECODE_SIZE);
        }
    }

    @Override
    public View getView(Activity activity, int dataID) {
        if (dataID >= mImages.size() || dataID < 0) {
            return null;
        }

        return mImages.get(dataID).getView(
                activity, mSuggestedWidth, mSuggestedHeight,
                mPlaceHolder.getConstantState().newDrawable(), this);
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
        if (mImages != null) {
            mListener.onDataLoaded();
        }
    }

    @Override
    public boolean canSwipeInFullScreen(int dataID) {
        if (dataID < mImages.size() && dataID > 0) {
            return mImages.get(dataID).canSwipeInFullScreen();
        }
        return true;
    }

    @Override
    public void removeData(Context c, int dataID) {
        Log.d(TAG, "removeData dataID = "+dataID);
        if(dataID == 0) {
            mFristData = getLocalData(1);
            resetLastPath(mFristData);
        }
        if (dataID >= mImages.size()) return;
        LocalData d = mImages.remove(dataID);
        resetSecureData();
        // Delete previously removed data first.
        executeDeletion(c);
        mLocalDataToDelete = d;
        mListener.onDataRemoved(dataID, d);
    }

    // TODO: put the database query on background thread
    @Override
    public void addNewVideo(ContentResolver cr, Uri uri) {
        Log.d(TAG, "addNewVideo Uri = "+uri);
        Cursor c = cr.query(uri,
                LocalMediaData.VideoData.QUERY_PROJECTION,
                MediaStore.Images.Media.DATA + " like ? ", CAMERA_PATH,
                LocalMediaData.VideoData.QUERY_ORDER);
        if (c == null || !c.moveToFirst()) {
            return;
        }
        int pos = findDataByContentUri(uri);
        LocalMediaData.VideoData newData = LocalMediaData.VideoData.buildFromCursor(c);
        if (newData == null) {
            return;
        }
        mFristData = newData;
        if (pos != -1) {
            // A duplicate one, just do a substitute.
            updateData(pos, newData);
        } else {
            // A new data.
            insertData(newData);
        }
    }

    // TODO: put the database query on background thread
    @Override
    public void addNewPhoto(ContentResolver cr, Uri uri) {
        Log.d(TAG, "addNewPhoto Uri = "+uri);
        Cursor c = cr.query(uri,
                LocalMediaData.PhotoData.QUERY_PROJECTION,
                MediaStore.Images.Media.DATA + " like ? ", CAMERA_PATH,
                LocalMediaData.PhotoData.QUERY_ORDER);
        if (c == null || !c.moveToFirst()) {
            return;
        }
        int pos = findDataByContentUri(uri);
        LocalMediaData.PhotoData newData = LocalMediaData.PhotoData.buildFromCursor(c);
        if (pos != -1) {
            // a duplicate one, just do a substitute.
            Log.v(TAG, "found duplicate photo");
            updateData(pos, newData);
        } else {
            // a new data.
            insertData(newData);
        }
    }

    @Override
    public int findDataByContentUri(Uri uri) {
        // LocalDataList will return in O(1) if the uri is not contained.
        // Otherwise the performance is O(n), but this is acceptable as we will
        // most often call this to find an element at the beginning of the list.
        return mImages.indexOf(uri);
    }

    @Override
    public boolean undoDataRemoval() {
        if (mLocalDataToDelete == null) return false;
        LocalData d = mLocalDataToDelete;
        mLocalDataToDelete = null;
        insertData(d);
        return true;
    }

    @Override
    public boolean executeDeletion(Context c) {
        if (mLocalDataToDelete == null) return false;

        DeletionTask task = new DeletionTask(c);
        task.execute(mLocalDataToDelete);
        mLocalDataToDelete = null;
        return true;
    }

    @Override
    public void flush() {
        replaceData(new LocalDataList());
    }

    @Override
    public void refresh(ContentResolver resolver, Uri contentUri) {
        Log.d(TAG, "refresh contentUri = "+contentUri);
        int pos = findDataByContentUri(contentUri);
        if (pos == -1) {
            return;
        }

        LocalData data = mImages.get(pos);
        LocalData refreshedData = data.refresh(resolver);
        if (refreshedData != null) {
            updateData(pos, refreshedData);
        }
    }

    @Override
    public void updateData(final int pos, LocalData data) {
        mImages.set(pos, data);
        // SPRD: bug 274023
        mFristData = data;

        if (mListener != null) {
            mListener.onDataUpdated(new UpdateReporter() {
                @Override
                public boolean isDataRemoved(int dataID) {
                    return false;
                }

                @Override
                public boolean isDataUpdated(int dataID) {
                    return (dataID == pos);
                }
            });
        }
    }

    private void resetSecureData() {
        if (mIsSecureCamera) {
            if (mModule == CameraUtil.MODE_VIDEO) {
                mSecureVideos = mImages;
            }else {
                mSecureImages = mImages;
            }
        }
    }

    @Override
    public void insertData(LocalData data) {
        // Since this function is mostly for adding the newest data,
        // a simple linear search should yield the best performance over a
        // binary search.
        Log.d(TAG, "insertData data = "+data);
        int pos = 0;
        Comparator<LocalData> comp = new LocalData.NewestFirstComparator();
        for (; pos < mImages.size()
                && comp.compare(data, mImages.get(pos)) > 0; pos++);
        mImages.add(pos, data);
        resetSecureData();
        // SPRD: bug 274023
        mFristData = data;
        resetLastPath(mFristData);
        if (mListener != null) {
            mListener.onDataInserted(pos, data);
        }
    }

    /** Update all the data */
    private void replaceData(LocalDataList list) {
        if (list.size() == 0 && mImages.size() == 0) {
            return;
        }
        mImages = list;
        if (mListener != null) {
            mListener.onDataLoaded();
        }
    }

    private class QueryTask extends AsyncTask<ContentResolver, Void, LocalDataList> {

        /**
         * Loads all the photo and video data in the camera folder in background
         * and combine them into one single list.
         *
         * @param resolver {@link ContentResolver} to load all the data.
         * @return An {@link ArrayList} of all loaded data.
         */
        @Override
        protected LocalDataList doInBackground(ContentResolver... resolver) {
            LocalDataList l = new LocalDataList();
            // Photos
            Cursor c;
            // SPRD: bug 256269 begin
            // get Data for DataAdapter from database diff by module
            // update fristData;
            Log.d(TAG, "LocalDataList CAMERA_PATH = "+CAMERA_PATH[0]);
            if (mModule == ModuleSwitcher.PHOTO_MODULE_INDEX ||
                    mModule == ModuleSwitcher.WIDE_ANGLE_PANO_MODULE_INDEX) {
                Log.d(TAG, "QueryTask mModule == " + mModule);
                    c = resolver[0].query(
                            LocalMediaData.PhotoData.CONTENT_URI,
                            LocalMediaData.PhotoData.QUERY_PROJECTION,
                            MediaStore.Images.Media.DATA + " like ? ", CAMERA_PATH,
                            LocalMediaData.PhotoData.QUERY_ORDER);
                    if (c != null && c.moveToFirst()) {
                        // build up the list.
                        while (true) {
                            LocalData data = LocalMediaData.PhotoData.buildFromCursor(c);
                            if (data != null) {
                                l.add(data);
                            } else {
                                Log.e(TAG, "Error loading data:"
                                        + c.getString(LocalMediaData.PhotoData.COL_DATA));
                            }
                            if (c.isLast()) {
                                break;
                            }
                            c.moveToNext();
                        }
                     }
                    if (c != null) {
                        c.close();
                     }
                } else if (mModule == ModuleSwitcher.VIDEO_MODULE_INDEX) {
                Log.d(TAG, ".QueryTask  -----	mModule == " + mModule);
                Log.d(TAG, ".QueryTask  -----   VideoData.CONTENT_URI == " + LocalMediaData.VideoData.CONTENT_URI);
                Log.d(TAG, ".QueryTask  -----   VideoData.QUERY_ORDER == " + LocalMediaData.VideoData.QUERY_ORDER);
                    c = resolver[0].query(
                            LocalMediaData.VideoData.CONTENT_URI,
                            LocalMediaData.VideoData.QUERY_PROJECTION,
                            MediaStore.Video.Media.DATA + " like ? ", CAMERA_PATH,
                            LocalMediaData.VideoData.QUERY_ORDER);
                    if (c != null && c.moveToFirst()) {
                        // build up the list.
//                        c.moveToFirst();
                        if (!c.isLast())
                            Log.d(TAG, ".QueryTask  -----  c.isLast() ");
                        while (true) {
                            LocalData data = LocalMediaData.VideoData.buildFromCursor(c);
                            if (data != null) {
                                l.add(data);
                            } else {
                                Log.e(TAG, "Error loading data:"
                                        + c.getString(LocalMediaData.VideoData.COL_DATA));
                            }
                            if (!c.isLast()) {
                                c.moveToNext();
                            } else {
                                break;
                            }
                        }
                     }
                    if (c != null) {
                        c.close();
                     }
                 }

                if (l.size() != 0) {
                    l.sort(new LocalData.NewestFirstComparator());
                }
                if (l != null && l.size() > 0)
                    mFristData = l.get(0);
                 // SPRD: bug 256269 end
            return l;
        }

        @Override
        protected void onPostExecute(LocalDataList l) {
            Log.d(TAG, "onPostExecute this = " + this.toString());
            // SPRD: Even if data list is empty, we also must be replace data to "mImages" list
            replaceData(l);
            mFristData = (l.size() > 0 ? l.get(0) : null);
            resetLastPath(mFristData);
        }
    }

    private class DeletionTask extends AsyncTask<LocalData, Void, Void> {
        Context mContext;

        DeletionTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(LocalData... data) {
            for (int i = 0; i < data.length; i++) {
                if (!data[i].isDataActionSupported(LocalData.ACTION_DELETE)) {
                    Log.v(TAG, "Deletion is not supported:" + data[i]);
                    continue;
                }
                data[i].delete(mContext);
            }
            return null;
        }
    }

    // SPRD: bug 256269 get the first Data of DataAdapter
    @Override
    public LocalData getFirstData() {
        Log.d(TAG, "getFirstData is started mFristData = "+mFristData);
        return mFristData;
    }

    private class LodeSecureImageTask extends AsyncTask<Void, Void, LocalDataList>{

        @Override
        protected LocalDataList doInBackground(Void... params){
            LocalDataList dataList = new LocalDataList();
            if (mModule == CameraUtil.MODE_VIDEO) {
                 dataList = mSecureVideos;
            }else {
                 dataList = mSecureImages;
            }
            return dataList;
        }

        @Override
        protected void onPostExecute(LocalDataList dataList) {
            replaceData(dataList);
            mFristData = (dataList.size() > 0 ? dataList.get(0) : null);
        }
    }

    @Override
    public String getLastVideoPath() {
            return mLastVideopath;
    }

    @Override
    public String getImageLastPath() {
            return mLastImagePath;
    }

    @Override
    public void setLastThumbnailPath(String imagepath, String videopath) {
         mLastImagePath = imagepath;
         mLastVideopath = videopath;
    }

    private void resetLastPath(LocalData data) {
        if (data == null) {
            if (mModule == CameraUtil.MODE_VIDEO) {
                mLastVideopath = null;
            }else {
                mLastImagePath = null;
            }
            return;
        }
        String type= data.getMimeType();
        if (LocalData.MIME_TYPE_JPEG.equals(type)) {
            mLastImagePath = data.getPath();
        }else {
            mLastVideopath = data.getPath();
        }
    }
}
