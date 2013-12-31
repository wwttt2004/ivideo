package com.miscellapp.ivideo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import com.miscellapp.ivideo.R;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

/**
 * Created with IntelliJ IDEA.
 * User: chenjishi
 * Date: 13-11-21
 * Time: 下午11:44
 * To change this template use File | Settings | File Templates.
 */
public class VideoPlayActivity2 extends Activity implements MediaController.OnHiddenListener,
        MediaController.OnShownListener {
    private VideoView mVideoView;
    private MediaController mMediaController;

    private String mVideoId;
    private String mVideoPath;
    private String mThumbUrl;
    private String mVideoTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this)) return;
        setContentView(R.layout.videoview);

        mMediaController = new MediaController(this);
        mMediaController.setOnHiddenListener(this);
        mMediaController.setOnShownListener(this);
        mVideoView = (VideoView) findViewById(R.id.surface_view);
        mVideoView.setMediaController(mMediaController);

        mVideoPath = getIntent().getStringExtra("local_path");
        mVideoId = getIntent().getStringExtra("id");
        mThumbUrl = getIntent().getStringExtra("thumb");
        mVideoTitle = getIntent().getStringExtra("title");

        play();
    }

    private void play() {
        if (!TextUtils.isEmpty(mVideoPath)) {
            mVideoView.setVideoPath(mVideoPath);
            mVideoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
            mVideoView.setMediaController(mMediaController);
        } else {
            Toast.makeText(this, "无法播放", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onHidden() {
    }



    @Override
    protected void onResume() {
        super.onResume();
        if (-1L != lastTimeWatched && null != mVideoView) {
            mVideoView.resume();
            mVideoView.seekTo(lastTimeWatched);
        }
    }

    private long lastTimeWatched = -1L;
    @Override
    protected void onPause() {
        super.onPause();

        lastTimeWatched = mVideoView.getCurrentPosition();
        mVideoView.pause();
    }


    @Override
    public void onShown() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mVideoView)
            mVideoView.stopPlayback();
    }
}
