package com.android.justmuzic;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import java.util.ArrayList;

public class VideoActivity extends BaseActivity {
    VideoView vv;
    int currentVideoPosition;
    int position;
    ArrayList<String> alPaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        vv = findViewById(R.id.vv);
        alPaths = getIntent().getStringArrayListExtra("paths");
        position = getIntent().getIntExtra("position", 0);
        getSupportActionBar().hide();

        MediaController m = new MediaController(VideoActivity.this);
        vv.setMediaController(m);

        m.setPrevNextListeners(view -> {
            position++;
            if (position == alPaths.size())
                position = 0;
            playVideo(position);
        }, view -> {
            if (position > 0) {
                position--;
                playVideo(position);
            }
        });

        vv.setOnPreparedListener(mp -> {
            int width = mp.getVideoWidth();
            int height = mp.getVideoHeight();
            if (width > height)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        });

        vv.setOnCompletionListener(mediaPlayer -> {
            position++;
            if (position == alPaths.size())
                position = 0;
            playVideo(position);
        });

        playVideo(position);
    }

    private void playVideo(int position) {
        vv.setVideoURI(Uri.parse(alPaths.get(position)));
        vv.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentVideoPosition = vv.getCurrentPosition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vv.seekTo(currentVideoPosition);
    }
}
