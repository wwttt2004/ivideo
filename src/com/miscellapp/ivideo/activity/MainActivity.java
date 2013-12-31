package com.miscellapp.ivideo.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.miscellapp.ivideo.DatabaseHelper;
import com.miscellapp.ivideo.R;
import com.miscellapp.ivideo.model.Video;
import com.miscellapp.ivideo.service.DownloadService;
import com.miscellapp.ivideo.util.Constants;
import com.miscellapp.ivideo.util.FileUtils;
import com.miscellapp.ivideo.util.HttpUtils;
import com.miscellapp.ivideo.volley.toolbox.ImageLoader;

import java.util.ArrayList;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener {
    private VideoAdapter mAdapter;
    private DatabaseHelper mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        View emptyView = LayoutInflater.from(this).inflate(R.layout.empty_view, null);
        GridView gridView = (GridView) findViewById(R.id.grid_video);
        ((ViewGroup) gridView.getParent()).addView(emptyView);
        gridView.setEmptyView(emptyView);

        mAdapter = new VideoAdapter(this, gridView);

        mDatabase = DatabaseHelper.getInstance(this);

        new Thread(){
            @Override
            public void run() {
                mHandler.sendEmptyMessage(Constants.MSG_DOWNLOAD_SUCCESS);
            }
        }.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, DownloadService.class));
        LocalBroadcastManager.getInstance(this).registerReceiver(mHandleDownloadReceiver,
                new IntentFilter(Constants.DOWNLOAD_STATUS_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mHandleDownloadReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (Constants.MSG_DOWNLOAD_SUCCESS == msg.what) {
                 ArrayList<Video> videoList = mDatabase.loadAll(DatabaseHelper.TB_NAME_VIDEOS);
                mAdapter.updateList(videoList);
            }
            String mDiskOccupiedSize = FileUtils.getVideoCacheSize();
            ((TextView) findViewById(R.id.title_text)).setText(String.format(getString(R.string.occupied_size), mDiskOccupiedSize));
        }
    };

    private class VideoAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
        private Context mContext;
        private LayoutInflater inflater;
        private ArrayList<Video> dataList;

        public VideoAdapter(Context context, GridView gridView) {
            mContext = context;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            gridView.setAdapter(this);
            gridView.setOnItemClickListener(this);
        }

        public void updateList(ArrayList<Video> list) {
            if (null == dataList) {
                dataList = new ArrayList<Video>();
            }

            dataList.clear();

            if (null != list && list.size() > 0) {
                dataList.addAll(list);
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return null == dataList ? 0 : dataList.size();
        }

        @Override
        public Video getItem(int position) {
            return null == dataList ? null : dataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Video video = getItem(position);
            Intent intent = new Intent(mContext, VideoPlayActivity2.class);
            intent.putExtra("local_path", video.localPath);
            intent.putExtra("id", video.id);
            intent.putExtra("thumb", video.thumbUrl);
            intent.putExtra("title", video.title);
            mContext.startActivity(intent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (null == convertView) {
                convertView = inflater.inflate(R.layout.grid_cell, parent, false);
                holder = new ViewHolder();

                holder.videoImage = (ImageView) convertView.findViewById(R.id.video_image);
                holder.videoTitle = (TextView) convertView.findViewById(R.id.video_title);

                convertView.setTag(holder);
            }

            holder = (ViewHolder) convertView.getTag();

            Video video = getItem(position);

            HttpUtils.getImageLoader().get(video.thumbUrl,
                    ImageLoader.getImageListener(holder.videoImage, R.drawable.pictrue_bg, R.drawable.pictrue_bg));
            holder.videoTitle.setText(video.title);

            return convertView;
        }
    }

    private static class ViewHolder {
        public ImageView videoImage;
        public TextView videoTitle;
    }

    private final BroadcastReceiver mHandleDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int msgCode = intent.getIntExtra(Constants.KEY_MESSAGE_TYPE, Constants.MSG_NO_UPDATE);
//            mDiskOccupiedSize = FileUtils.getVideoCacheSize();

            mHandler.sendEmptyMessage(msgCode);
        }
    };
}
