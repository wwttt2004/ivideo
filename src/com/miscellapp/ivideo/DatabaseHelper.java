package com.miscellapp.ivideo;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.miscellapp.ivideo.model.Video;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by chenjishi on 13-12-31.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String TB_NAME_LINKS = "links";
    public static final String TB_NAME_VIDEOS = "videos";
    public static final String TB_NAME_HISTORY = "history";

    private static final String COL_ID = "videoId";
    private static final String COL_TITLE = "title";
    private static final String COL_THUMB = "thumb";
    private static final String COL_DURATION = "duration";
    private static final String COL_URL = "url";
    private static final String COL_LOCAL_PATH = "local_path";
    private static final String COL_UPDATE_TIME = "updateTime";
    private static final String COL_WATCHED_TIME = "watchedTime";
    private static final String COL_LOCAL_IMAGE = "local_image";

    private static final String DB_NAME = "ivideo.db";
    private static final int DB_VERSION = 1;
    private static SQLiteDatabase mDatabase = null;
    private static DatabaseHelper mInstance = null;

    private DatabaseHelper(Context c) {
        super(c, DB_NAME, null, DB_VERSION);
    }

    public static DatabaseHelper getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new DatabaseHelper(context);
            mDatabase = mInstance.getWritableDatabase();
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TB_NAME_LINKS + " (" +
                COL_ID + " TEXT," +
                COL_TITLE + " TEXT," +
                COL_THUMB + " TEXT," +
                COL_DURATION + " TEXT," +
                COL_URL + " TEXT," +
                COL_LOCAL_PATH + " TEXT, " +
                COL_UPDATE_TIME + " INTEGER," +
                COL_WATCHED_TIME + " INTEGER," +
                COL_LOCAL_IMAGE + " TEXT," +
                " UNIQUE (" + COL_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TB_NAME_VIDEOS + " (" +
                COL_ID + " TEXT," +
                COL_TITLE + " TEXT," +
                COL_THUMB + " TEXT," +
                COL_DURATION + " TEXT," +
                COL_URL + " TEXT," +
                COL_LOCAL_PATH + " TEXT, " +
                COL_UPDATE_TIME + " INTEGER," +
                COL_WATCHED_TIME + " INTEGER," +
                COL_LOCAL_IMAGE + " TEXT," +
                " UNIQUE (" + COL_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TB_NAME_HISTORY + " (" +
                COL_ID + " TEXT, " +
                COL_TITLE + " TEXT, UNIQUE (" + COL_ID + ") ON CONFLICT REPLACE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TB_NAME_LINKS);
        db.execSQL("DROP TABLE IF EXISTS " + TB_NAME_VIDEOS);
        onCreate(db);
    }

    public void deleteVideo(String id, String tableName) {
        String sql = "DELETE FROM " + tableName + " WHERE " + COL_ID + " = ?";
        mDatabase.execSQL(sql, new String[]{id});
    }

    public void deleteVideoByTitle(String title, String tableName) {
        String sql = "DELETE FROM " + tableName + " WHERE " + COL_TITLE + " = ?";
        mDatabase.execSQL(sql, new String[]{title});
    }

    public boolean isExist(String id, String tableName) {
        boolean result = false;
        Cursor cursor = null;
        String sql = "SELECT * FROM " + tableName + " WHERE " + COL_ID + "=?";

        try {
            cursor = mDatabase.rawQuery(sql, new String[]{id});
            result = cursor.moveToNext();
        } finally {
            if (null != cursor) cursor.close();
        }

        return result;
    }

    public void insertAll(ArrayList<Video> videoList, String tableName) {
        if (null == videoList || videoList.size() == 0) return;

        String sql = "INSERT OR REPLACE INTO " + tableName +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        mDatabase.beginTransaction();
        try {
            for (Video video : videoList) {
                mDatabase.execSQL(sql, new String[]{
                        video.id,
                        video.title,
                        video.thumbUrl,
                        video.duration,
                        video.url,
                        video.localPath,
                        String.valueOf(video.updateTime),
                        String.valueOf(video.watchedTime),
                        video.localImage
                });
            }
            mDatabase.setTransactionSuccessful();
        } catch (Exception e) {
        } finally {
            mDatabase.endTransaction();
        }
    }

    public void insertVideoId(String id, String title) {
        String sql = "INSERT INTO " + TB_NAME_HISTORY +
                " VALUES (?, ?)";

        mDatabase.execSQL(sql, new String[]{id, title});
    }

    public void insert(Video video, String tableName) {
        if (null == video) return;

        String sql = "INSERT OR REPLACE INTO " + tableName +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        mDatabase.execSQL(sql, new String[]{
                video.id,
                video.title,
                video.thumbUrl,
                video.duration,
                video.url,
                video.localPath,
                String.valueOf(video.updateTime),
                String.valueOf(video.watchedTime),
                video.localImage
        });
    }

    public HashMap<String, String> loadHistoryIds() {
        HashMap<String, String> idHashMap = null;
        Cursor cursor = null;

        String sql = "SELECT " + COL_TITLE + ", " + COL_ID + " FROM " + TB_NAME_HISTORY;
        try {
            cursor = mDatabase.rawQuery(sql, null);
            idHashMap = new HashMap<String, String>();
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String id = cursor.getString(1);
                idHashMap.put(id, title);
            }
        } finally {
            if (null != cursor) cursor.close();
        }

        return idHashMap;
    }

    public ArrayList<Video> loadAll(String tableName) {
        ArrayList<Video> videoList = null;
        Cursor cursor = null;

        String sql = "SELECT " + COL_ID + "," +
                COL_TITLE + "," +
                COL_THUMB + "," +
                COL_DURATION + "," +
                COL_URL + "," +
                COL_LOCAL_PATH + "," +
                COL_UPDATE_TIME + "," +
                COL_WATCHED_TIME + "," +
                COL_LOCAL_IMAGE + " FROM " +
                tableName;

        try {
            cursor = mDatabase.rawQuery(sql, null);
            videoList = new ArrayList<Video>();
            while (cursor.moveToNext()) {
                Video video = new Video();
                video.id = cursor.getString(0);
                video.title = cursor.getString(1);
                video.thumbUrl = cursor.getString(2);
                video.duration = cursor.getString(3);
                video.url = cursor.getString(4);
                video.localPath = cursor.getString(5);
                video.updateTime = cursor.getLong(6);
                video.watchedTime = cursor.getLong(7);
                video.localImage = cursor.getString(8);

                videoList.add(video);
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) cursor.close();
        }
        return videoList;
    }
}
