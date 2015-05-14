package com.android.camera.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.android.camera2.R;
import com.android.camera.Storage;
import com.android.camera.StoragePathPreference;
import com.android.camera.StorageUtil;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.graphics.Color;

public class StoragePathPopup extends AbstractSettingPopup {

    private static final String TAG = "StoragePathPopup";

    // default construct
    public StoragePathPopup(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    @Override
    public void setOrientation(int orientation, boolean animation) {
        synchronized(mLock) {
            super.setOrientation(orientation, animation);
        }
    }

    // mode picker
    private int mMode = StoragePathPreference.VAL_STORAGE_UNKNOW_MODE;
    // notify UI activity path is changed listener
    private StoragePathPreference.StoragePathChangedListener mListener;

    public void initializeSetup(
            StoragePathPreference.StoragePathChangedListener listener, int mode) {
        if (mMode != mode) mMode = mode;
        if (mListener != listener) mListener = listener;
    }

    public boolean collapseStorageControl() {
        if (View.VISIBLE == getVisibility()) {
            setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    public void displayStoragePopup() {
        processStoragePopup(View.VISIBLE);
        bringToFront();
    }

    public void dismissStoragePopup(boolean isCancle) {
        processStoragePopup(View.GONE);
        if (mListener != null && mTask != null) {
            // anyway we must call intercept thread for next time
            mTask.intercept();
            mListener.storageChanged(mTask.getPath(), isCancle);
        }
    }

    private void processStoragePopup(int state) {
        Log.d(TAG, "processStoragePopup(), state = " + state);
        setVisibility(state);
        if (mTask != null) {
            switch (state) {
                case View.VISIBLE:
                    // initialize data
                    mTask.execute(DataAsyncTask.KEY_INITIALIZE_DATA, null);
                    break;
                case View.GONE:
                case View.INVISIBLE:
                    break;
                default: break;
            }
        }
    }

    private void proxyProgressPanel(int res) {
        Log.d(TAG, "proxyProgressPanel()");
        boolean ensure =
            (mProgressPanel != null && mTask != null
                && mTask.scanning() && DataAsyncTask.KEY_UNKNOW_EVENT != res);
        if (ensure) {
            if (!mHandler.hasMessages(ViewHandler.MSG_NOTICE_STORAGE_SCAN)) {
                TextView tv =
                    (TextView) mProgressPanel.findViewById(R.id.tv_progress_notice);
                tv.setText(res);
                mHandler.sendEmptyMessageDelayed(
                    ViewHandler.MSG_NOTICE_STORAGE_SCAN, ViewHandler.VAL_NOTICE_STORAGE_DELAY);
            }
        }
        if (mProgressPanel != null) {
            mProgressPanel.setVisibility(ensure ? View.VISIBLE : View.GONE);
        }
    }

    private void proxyUpdateViews(int key) {
        if (mControlPanelListener != null) {
            mControlPanelListener.updateViews(key);
        }
    }

    private void proxySendMessage(int what) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(what);
            proxySendMessage(msg);
        }
    }

    private synchronized void proxySendMessage(Message msg) {
        if (mHandler != null)
            mHandler.sendMessage(msg);
    }

    // data list
    private ListView mListView;
    // adapter object
    private DataAdapter mAdapter;
    // item click listener
    private ItemClickListener mItemClickControl;
    // task object
    private DataAsyncTask mTask;
    // handler
    private ViewHandler mHandler;
    // progress bar panel
    private View mProgressPanel;
    // control click listener
    private ControlClickListener mControlPanelListener;
    // lock
    private final Object mLock = new Object();

    @Override
    protected void onFinishInflate() {
        Log.d(TAG, "onFinishInflate");
        super.onFinishInflate();
        mListView = (ListView) findViewById(R.id.storage_path_list);
        mTask = new DataAsyncTask();    // create task
        mAdapter = new DataAdapter();   // create adapter
        mHandler = new ViewHandler();   // create handler
        if (mListView != null) {
            // initialize empty view
            mListView.setEmptyView(findViewById(R.id.storage_path_list_empty));
            // initialize adapter
            mListView.setAdapter(mAdapter);
            // initialize item click listener
            mItemClickControl = new ItemClickListener();
            mListView.setOnItemClickListener(mItemClickControl);
            // initialize progress bar panel
            mProgressPanel = findViewById(R.id.fl_progress_panel);
            proxyProgressPanel(DataAsyncTask.KEY_UNKNOW_EVENT);
            // initialize control panel listener
            mControlPanelListener = new ControlClickListener();
            proxyUpdateViews(ViewHandler.MSG_UI_STATE_INFLATE);
        }
    }

    // fill data
    private class DataAsyncTask implements Runnable {
        // default event
        public static final int KEY_UNKNOW_EVENT        = -1;
        // initialize data
        public static final int KEY_INITIALIZE_DATA     = 0;
        // list view item click data
        public static final int KEY_ITEM_CLICK_DATA     = 1;
        // intercept thread
        public static final int KEY_INTERCEPT_THREAD    = 2;

        // key
        private int mKeyEvent = -1;
        // files filter
        private FilenameFilter filter;
        // current path
        private String cPath;

        // default construct
        /*package*/ DataAsyncTask() {
            filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File f = new File(dir, filename);
                    Log.d(TAG, "filter path = " + f.getAbsolutePath());
                    return
                        (f.exists() && f.isDirectory()
                            && !f.isHidden() && f.canRead() && f.canWrite());
                }
            };
        }

        /*package*/ boolean scanning() {
            return (mKeyEvent != KEY_UNKNOW_EVENT);
        }

        /*package*/ void intercept() {
            mKeyEvent = KEY_INTERCEPT_THREAD;
        }

        @SuppressWarnings("unused")
        private boolean intercepted() {
            return (KEY_INTERCEPT_THREAD == mKeyEvent);
        }

        /*package*/ synchronized void execute(int key, String path) {
            if (mKeyEvent != key) {
                mKeyEvent = key;
                cPath = path;
                Thread t = new Thread(this);
                t.start();
            }
        }

        /*package*/ String getPath() {
            return cPath;
        }

        @Override
        public void run() {
            List<DataItem> result = null;

            // initialize data
            if (KEY_INITIALIZE_DATA == mKeyEvent) {
                // update control panel
                proxySendMessage(ViewHandler.MSG_UI_STATE_INITIALIZE);
                result = initialize();
            }

            // fill data by item click
            if (KEY_ITEM_CLICK_DATA == mKeyEvent) {
                // update control panel
                proxySendMessage(ViewHandler.MSG_UI_STATE_SCANNING);
                result = find();
                proxySendMessage(ViewHandler.MSG_UI_STATE_SCANNED);
            }

            if (mAdapter != null) {
                mAdapter.notifyChanged(result);
            }
            // reset to default event
            mKeyEvent = KEY_UNKNOW_EVENT;
        }

        /*package*/ boolean isRootStorage(String path) {
            boolean result = false;
            if (path != null) {
                StorageUtil util = StorageUtil.newInstance();
                Map<String, String> roots = util.supportedRootDirectory();
                Set<Entry<String, String>> entries = roots.entrySet();
                if (entries != null) {
                    String key = null, val = null;
                    for (Entry<String, String> entry : entries) {
                        key = entry.getKey();
                        val = entry.getValue();
                        if (result =
                                ((key != null && key.equals(path))
                                    || (val != null && val.equals(path)))) {
                            break;
                        }
                    }
                }
            }
            return result;
        }

        private List<DataItem> find() {
            List<DataItem> result =
                new ArrayList<DataItem>(DataAdapter.INIT_SIZE);
            String path = null;
            // get current path
            if ((path = cPath) != null) {
                // fixed bug 199757 start
                StorageUtil util = StorageUtil.newInstance();
                Map<String, String> directory = util.supportedRootDirectory();
                String convert = directory.get(path);
                // fixed bug 199757 end
                if (convert != null) path = convert;
                Log.d(TAG, "fill data from path = " + path);
                // sometime "path" is "internal" or "external", so we must validate "exists" && "directory" && "not hidden"
                File f = new File(path);
                if (f.exists() && f.isDirectory() && !f.isHidden()) {
                    Map<String, String> root = util.supportedRootDirectory();
                    String internal = root.get(Storage.KEY_DEFAULT_INTERNAL);
                    String external = root.get(Storage.KEY_DEFAULT_EXTERNAL);
                    File[] folders = f.listFiles(filter);
                    // fixed bug 199752 start
                    for (int i = 0, len = (folders != null ? folders.length : 0); i < len; i++) {
                        File d = folders[i];
                    // fixed bug 199752 end
                        String c = d.getAbsolutePath();
                        boolean hc = true;  // has children, default true
                        boolean hp = false; // has parent, default false
                        String sp = d.getParentFile().getAbsolutePath();
                        hp = !(sp != null && (sp.equals(internal) || sp.equals(external)));
                        result.add(new DataItem(c, hp, hc));
                    }
                }
            }
            // if "result" is empty, so reset size
            if (result.isEmpty())
                result = new ArrayList<DataItem>(Storage.VAL_DEFAULT_ROOT_DIRECTORY_SIZE);
            return result;
        }

        private List<DataItem> initialize() {
            List<DataItem> result =
                new ArrayList<DataItem>(Storage.VAL_DEFAULT_ROOT_DIRECTORY_SIZE);
            // fixed bug 199757 start
            StorageUtil util = StorageUtil.newInstance();
            Map<String, String> directory = util.supportedRootDirectory();
            String internal = directory.get(Storage.KEY_DEFAULT_INTERNAL);
            String external = directory.get(Storage.KEY_DEFAULT_EXTERNAL);
            // fixed bug 199757 end
            if (internal != null)
                result.add(new DataItem(Storage.KEY_DEFAULT_INTERNAL, false, true));
            if (external != null)
                result.add(new DataItem(Storage.KEY_DEFAULT_EXTERNAL, false, true));
            return result;
        }
    }

    private class ItemClickListener implements OnItemClickListener {

        // default construct
        private ItemClickListener() { }

        @Override
        public void onItemClick(AdapterView<?> group, View v, int pos, long id) {
            Log.d(TAG, "list view onItemClick()");
            DataItem item = null;
            if (mAdapter != null && mTask != null
                    && (item = mAdapter.getItem(pos)) != null) {
                Log.d(TAG,
                    String.format("onItemClick() pos = %d, count = %d",
                            new Object[] { pos, mAdapter.getCount() }));
                String nPath = item.cPath;
                String oPath = mTask.getPath();
                boolean scanning = mTask.scanning();
                Log.d(TAG,
                    String.format("need reload folders? scanning = %b, old.path = %s, new.path = %s",
                        new Object[] { scanning, oPath, nPath }));
                // if current is scanning, so notice user is scanning
                if (!nPath.equals(oPath) && !scanning) {
                    mTask.execute(DataAsyncTask.KEY_ITEM_CLICK_DATA, nPath);
                    proxyProgressPanel(R.string.notice_storage_scanning);
                }
            }
        }
    }

    private class ControlClickListener implements View.OnClickListener {

        private View mDone;
        private View mBack;
        private View mCancel;
        private View mTextView;//add by topwise qiujq for bug152

        // default construct
        /*package*/ ControlClickListener() {
            // initialize image buttons
            mDone = findViewById(R.id.btn_save_path_done);
            mBack = findViewById(R.id.btn_save_path_back);
            mCancel = findViewById(R.id.btn_save_path_cancel);
            mTextView = findViewById(R.id.tv_save_path_title);//add by topwise qiujq for bug152
            mDone.setOnClickListener(this);
            mBack.setOnClickListener(this);
            mCancel.setOnClickListener(this);
            mTextView.setOnClickListener(this);//add by topwise qiujq for bug152
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            String path = null;
            if (mTask != null) path = mTask.getPath();
            Log.d(TAG, "onClick() path = " + path);

            if (R.id.btn_save_path_cancel == id) {
                dismissStoragePopup(true);
            } else if (path != null && mTask != null) {
                switch (id) {
                    case R.id.btn_save_path_done:
                        StoragePathPreference mProxy =
                            StoragePathPreference.getInstance(getContext());
                        mProxy.writeStorage(mMode, path);
                        dismissStoragePopup(false);
                        break;
                    case R.id.btn_save_path_back:
                    case R.id.tv_save_path_title://add by topwise qiujq for bug152
                        File f = new File(path);
                        String p = f.getParent();
                        String c = f.getAbsolutePath();
                        boolean root = mTask.isRootStorage(path);
                        Log.d(TAG,
                            String.format("onClick(back), path = %s, parent = %s, root = %b",
                                new Object[] { c, p, root }));
                        mTask.execute(
                            (root ? DataAsyncTask.KEY_INITIALIZE_DATA : DataAsyncTask.KEY_ITEM_CLICK_DATA),
                            (root ? null : p)); // null is initialize data
                        if (!root) {
                            proxyProgressPanel(R.string.notice_storage_scanning);
                        }
                        break;
                }
            }else if ((R.id.btn_save_path_back == id)  || (R.id.tv_save_path_title == id)){//add by topwise qiujq for bug152
                dismissStoragePopup(false);
            }
        }

        /*package*/ synchronized void updateViews(int key) {
            Log.d(TAG, "proxyUpdateViews() key = " + key);
            switch (key) {
                case ViewHandler.MSG_UI_STATE_SCANNED:
                case ViewHandler.MSG_UI_STATE_INFLATE:
                    boolean enable =
                        (ViewHandler.MSG_UI_STATE_INFLATE != key ?
                            ViewHandler.VAL_BOOLEAN_TRUE : ViewHandler.VAL_BOOLEAN_FALSE);
                    mDone.setEnabled(enable);
//                    mBack.setEnabled(enable);
                    mCancel.setEnabled(enable);
                    break;
                case ViewHandler.MSG_UI_STATE_INITIALIZE:
                    mDone.setEnabled(ViewHandler.VAL_BOOLEAN_FALSE);
//                    mBack.setEnabled(ViewHandler.VAL_BOOLEAN_FALSE);
                    mCancel.setEnabled(ViewHandler.VAL_BOOLEAN_TRUE);
                    break;
                case ViewHandler.MSG_UI_STATE_SCANNING:
                    mDone.setEnabled(ViewHandler.VAL_BOOLEAN_TRUE);
//                    mBack.setEnabled(ViewHandler.VAL_BOOLEAN_FALSE);
                    mCancel.setEnabled(ViewHandler.VAL_BOOLEAN_TRUE);
                    break;
            }
        }
    }

    private class ViewHandler extends Handler {
        /*package*/ static final int MSG_NOTIFY_DATA_CHANGED    = 0;
        /*package*/ static final int MSG_NOTICE_STORAGE_SCAN    = 1;
        /*package*/ static final int MSG_UI_STATE_INFLATE       = 101;
        /*package*/ static final int MSG_UI_STATE_INITIALIZE    = 102;
        /*package*/ static final int MSG_UI_STATE_SCANNING      = 103;
        /*package*/ static final int MSG_UI_STATE_SCANNED       = 104;

        /*package*/ static final boolean VAL_BOOLEAN_TRUE   = true;
        /*package*/ static final boolean VAL_BOOLEAN_FALSE  = false;
        /*package*/ static final long VAL_NOTICE_STORAGE_DELAY = 10 * 1000;
        /*package*/ ViewHandler() { super(); }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case MSG_NOTIFY_DATA_CHANGED:
                    if (mAdapter != null && View.VISIBLE == getVisibility()) {
                        @SuppressWarnings("unchecked")
                        List<DataItem> data = (List<DataItem>) msg.obj;
                        if (mAdapter.fill(data)) {
                            mAdapter.notifyDataSetChanged();
                        }
                        proxyProgressPanel(DataAsyncTask.KEY_UNKNOW_EVENT);
                    }
                    break;
                case MSG_NOTICE_STORAGE_SCAN:
                    proxyProgressPanel(R.string.notice_storage_still_scanning);
                    break;
                case MSG_UI_STATE_INFLATE:
                case MSG_UI_STATE_SCANNED:
                case MSG_UI_STATE_SCANNING:
                case MSG_UI_STATE_INITIALIZE:
                    proxyUpdateViews(what);
                    break;
            }
        }
    }

    // data adapter
    private class DataAdapter extends BaseAdapter {

        private static final int INIT_SIZE = 10;

        // this is data source
        private List<DataItem> mData;

        // default construct
        /*package*/ DataAdapter() {
            mData = Collections.synchronizedList(new ArrayList<DataItem>(INIT_SIZE));
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public DataItem getItem(int pos) {
            DataItem item = null;
            if (pos < getCount()) item = mData.get(pos);
            return item;
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public View getView(int pos, View cvt, ViewGroup group) {
            synchronized(mLock) {
                TextView vText = null;
                DataItem data = null;
    
                // initialize "cvt" object
                if (cvt == null) {
                    LayoutInflater inflater =
                        LayoutInflater.from(StoragePathPopup.this.getContext());
                    cvt = inflater.inflate(R.layout.save_path_item, group, false);
                }
    
                if (cvt != null && (data = getItem(pos)) != null) {
                    vText = (TextView) cvt.findViewById(R.id.item_text);
                    vText.setText(data.cPath);
                    vText.setTextColor(Color.BLACK);
                }
                return cvt;
            }
        }

        public boolean fill(List<DataItem> list) {
            boolean result = (list != null && mData != null);
            if (result) {
                Log.d(TAG,
                    String.format("fill() old.size = %d, new.size = %d",
                        new Object[] { mData.size(), list.size() }));
                mData.clear();
                Collections.sort(list);//SPRD
                mData.addAll(list);
            }
            return result;
        }

        private void notifyChanged(List<DataItem> list) {
            synchronized(mLock) {
                if (mHandler != null) {
                    mHandler.sendMessage(
                        mHandler.obtainMessage(ViewHandler.MSG_NOTIFY_DATA_CHANGED, list));
                }
            }
        }
    }

    // data item
    private class DataItem implements Comparable<DataItem>{//SPRD
        /*package*/ final String cPath; // current path
        /*package*/ final boolean hasParent;
        /*package*/ final boolean hasChildren;

        /*package*/ DataItem(String path, boolean p, boolean c) {
             cPath = path;
             hasParent = p;
             hasChildren = c;
        }

        @Override
        public int compareTo(DataItem mDataItem) {//SPRD
            // TODO Auto-generated method stub
            return this.cPath.compareTo(mDataItem.cPath);
        }
    }

    @Override
    public void reloadPreference() {
        Log.d(TAG, "reloadPreference");
    }
}
