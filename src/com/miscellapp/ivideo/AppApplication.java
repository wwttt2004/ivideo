package com.miscellapp.ivideo;

import android.app.Application;
import com.miscellapp.ivideo.util.HttpUtils;

/**
 * Created by chenjishi on 13-12-30.
 */
public class AppApplication extends Application {
    private static AppApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;
        HttpUtils.init(this);
        FileCache.init(this);
        DatabaseHelper.getInstance(this);
    }

    public static AppApplication getInstance() {
        return mInstance;
    }
}
