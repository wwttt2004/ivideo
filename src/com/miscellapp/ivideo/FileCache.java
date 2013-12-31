package com.miscellapp.ivideo;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by chenjishi on 13-12-31.
 */
public class FileCache {
    public static void init(Context context) {
        mkDirs(getVideoDirectory(context));
        mkDirs(getImageCacheDirectory(context));
        mkDirs(getDataCacheDirectory(context));
        mkDirs(getTempDirectory(context));
    }

    public static String getImageCacheDirectory(Context context) {
        return getRootDirectory(context) + "cache/";
    }

    public static String getTempDirectory(Context context) {
        return getSDCardDirectory() + "temp/";
    }

    public static String getDataCacheDirectory(Context context) {
        return getRootDirectory(context) + "data/";
    }

    public static void mkDirs(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) file.mkdirs();
    }

    public static String getVideoDirectory(Context context) {
        return getSDCardDirectory() + "video/";
    }

    public static String getSDCardDirectory() {
        String path = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            path = Environment.getExternalStorageDirectory() + "/ivideo/";
        }

        return path;
    }

    public static String getRootDirectory(Context context) {
        String rootPath = null;
        File cacheDir = context.getCacheDir();
        if (cacheDir.exists()) {
            rootPath = cacheDir + "/ivideo/";
        } else {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                rootPath = Environment.getExternalStorageDirectory() + "/ivideo/";
            }
        }

        return rootPath;
    }
}
