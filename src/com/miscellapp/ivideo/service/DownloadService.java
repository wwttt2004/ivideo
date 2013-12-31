package com.miscellapp.ivideo.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.miscellapp.ivideo.DatabaseHelper;
import com.miscellapp.ivideo.FileCache;
import com.miscellapp.ivideo.PrefsUtil;
import com.miscellapp.ivideo.model.Video;
import com.miscellapp.ivideo.util.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import static com.miscellapp.ivideo.util.Utils.isWifiConnected;
import static com.miscellapp.ivideo.util.Utils.nullSafe;
import static com.miscellapp.ivideo.util.Utils.isEmpty;

/**
 * Created with IntelliJ IDEA.
 * User: chenjishi
 * Date: 13-11-25
 * Time: 下午4:03
 * To change this template use File | Settings | File Templates.
 */
public class DownloadService extends Service {
    private static final String URL_56 = "http://fun.56.com/";
    private static final String URL_YOUKU = "http://fun.youku.com/";

    private static final long DAY = 24 * 60 * 60 * 1000;
    private static final long TWO_HUNDRED_MB = 200 * 1024 * 1024;

    private static final int SOCKET_READ_TIMEOUT = 5000;
    private static final int SOCKET_CONNECT_TIMEOUT = 5000;
    private static final int BUFFER_SIZE = 20 * 1024;

    private DatabaseHelper dataBase;

    private ArrayList<Video> videoList;
    private Video currentVideo;
    private boolean downloading = false;

    private boolean stopFlag;

    @Override
    public void onCreate() {
        super.onCreate();
        dataBase = DatabaseHelper.getInstance(this);
        stopFlag = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isWifiConnected(this) && !downloading) {
            new Thread() {
                @Override
                public void run() {
                    clearInvalidFile();
                    checkVideoUpdate();
                }
            }.start();
        }

        return START_STICKY;
    }

    private void clearInvalidFile() {
        //delete invalid links in database, sometimes video fetched no id, so delete it!
        ArrayList<Video> links = dataBase.loadAll(DatabaseHelper.TB_NAME_LINKS);
        for (Video link : nullSafe(links)) {
            String id = link.id;
            if (isEmpty(id)) {
                dataBase.deleteVideoByTitle(link.title, DatabaseHelper.TB_NAME_LINKS);
            }
        }

        //delete invalid database video
        ArrayList<Video> savedVideos = dataBase.loadAll(DatabaseHelper.TB_NAME_VIDEOS);
        for (Video video : nullSafe(savedVideos)) {
            String filePath = video.localPath;
            File file = new File(filePath);
            if (!file.exists()) dataBase.deleteVideo(video.id, DatabaseHelper.TB_NAME_VIDEOS);
        }

        String videoCachePath = FileCache.getVideoDirectory(this);
        File[] flists = new File(videoCachePath).listFiles();

        if (null == flists || flists.length == 0) return;

        //delete files not in database
        for (File f : flists) {
            String name = f.getName();
            if (!isEmpty(name)) {
                boolean isExistInDB = DatabaseHelper.getInstance(this).isExist(name,
                        DatabaseHelper.TB_NAME_VIDEOS);
                if (!isExistInDB) f.delete();
            }
        }
    }

    private boolean isListEmpty(ArrayList<Video> list) {
        return null == list || list.size() == 0;
    }

    private void checkVideoUpdate() {
        videoList = dataBase.loadAll(DatabaseHelper.TB_NAME_LINKS);

        if (isListEmpty(videoList)) {
            videoList = new ArrayList<Video>();

            String html = HttpUtils.getSync(URL_56);
            ArrayList<Video> videos = VideoParser.parse56Video(html);
            if (!isListEmpty(videos)) videoList.addAll(videos);

            html = HttpUtils.getSync(URL_YOUKU);
            videos = VideoParser.parseYouKuVideo(html);
            if (!isListEmpty(videos)) videoList.addAll(videos);

            if (!isListEmpty(videoList)) {
                HashMap<String, String> historyIds = dataBase.loadHistoryIds();

                ArrayList<Video> tmpList = new ArrayList<Video>();

                if (null != historyIds && historyIds.size() > 0) {
                    for (Video v : videoList) {
                        String key = v.id;
                        String value = historyIds.get(key);

                        if (!isEmpty(value)) tmpList.add(v);
                    }
                }

                if (tmpList.size() > 0) videoList.removeAll(tmpList);

                if (videoList.size() == 0) {
                    stopSelf();
                } else {
                    dataBase.insertAll(videoList, DatabaseHelper.TB_NAME_LINKS);
                }
            } else {
                stopSelf();
            }
        }

        if (!isListEmpty(videoList)) {
            long lastTime = PrefsUtil.getLongPreferences(PrefsUtil.KEY_VIDEO_UPDATE_TIME);
            if (System.currentTimeMillis() >= lastTime) {
                ArrayList<Video> downloadList = dataBase.loadAll(DatabaseHelper.TB_NAME_VIDEOS);
                if (null != downloadList && downloadList.size() > 0) {
                    for (Video video : downloadList) {
                        dataBase.deleteVideo(video.id, DatabaseHelper.TB_NAME_VIDEOS);
                        FileUtils.deleteFile(video.localPath);
                    }
                }
                PrefsUtil.saveLongPreference(PrefsUtil.KEY_VIDEO_UPDATE_TIME, System.currentTimeMillis() + DAY);
            }

            initTask();
        }
    }

    private void initTask() {
        long size = FileUtils.getCachedVideoSize(this);
        if (size < TWO_HUNDRED_MB) {
            videoList.clear();
            videoList = dataBase.loadAll(DatabaseHelper.TB_NAME_LINKS);
            if (!isListEmpty(videoList)) {
                startDownload();
            } else {
                stopSelf();
            }
        } else {
            stopSelf();
        }
    }

    private void startDownload() {
        downloading = true;
        currentVideo = getNext();
        while (null != currentVideo && !stopFlag) {
            String fileName = FileCache.getVideoDirectory(this) + currentVideo.id;
            File file = new File(fileName);

            String videoUrl;
            if (currentVideo.url.contains("youku")) {
                videoUrl = VideoUrlParser.getYoukuVideo(currentVideo.id);
            } else {
                videoUrl = VideoUrlParser.get56Video(currentVideo.id);
            }

            currentVideo.url = videoUrl;
            boolean state = downloadFile(currentVideo.url, file);
            if (state) {
                currentVideo.localPath = fileName;

                dataBase.insert(currentVideo, DatabaseHelper.TB_NAME_VIDEOS);
                dataBase.insertVideoId(currentVideo.id, currentVideo.title);
                sendStatusBroadcast(Constants.MSG_DOWNLOAD_SUCCESS);
            }
            dataBase.deleteVideo(currentVideo.id, DatabaseHelper.TB_NAME_LINKS);

            videoList.remove(0);
            currentVideo = getNext();

            long size = FileUtils.getCachedVideoSize(this);
            if (size > TWO_HUNDRED_MB) {
                stopFlag = true;
            }
        }

        if (null == currentVideo || stopFlag) {
            stopSelf();
        }
    }

    private boolean downloadFile(String videoUrl, File file) {
        Log.i("test", "##videoUrl " + videoUrl);
        if (isEmpty(videoUrl)) return false;

        HttpURLConnection conn = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            URL url = new URL(videoUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(SOCKET_CONNECT_TIMEOUT);
            conn.setReadTimeout(SOCKET_READ_TIMEOUT);
            conn.connect();

            bis = new BufferedInputStream(conn.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(file));

            byte buf[] = new byte[BUFFER_SIZE];
            int len;
            while ((len = bis.read(buf)) != -1) bos.write(buf, 0, len);
        } catch (IOException e) {
            if (file.exists()) file.delete();
            return false;
        } finally {
            try {
                if (null != bis) bis.close();
                if (null != bos) bos.close();
                if (null != conn) conn.disconnect();
            } catch (IOException e) {
            }
        }
        return true;
    }

    private Video getNext() {
        if (null != videoList && videoList.size() > 0) {
            return videoList.get(0);
        } else {
            return null;
        }
    }

    private void sendStatusBroadcast(int code) {
        Intent intent = new Intent(Constants.DOWNLOAD_STATUS_ACTION);
        intent.putExtra(Constants.KEY_VIDEO_SIZE, null == videoList ? 0 : videoList.size());
        intent.putExtra(Constants.KEY_MESSAGE_TYPE, code);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopFlag = true;
        downloading = false;
    }
}
