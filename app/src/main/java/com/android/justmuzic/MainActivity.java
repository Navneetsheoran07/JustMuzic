package com.android.justmuzic;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MainActivity extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener, ActionBar.TabListener, ViewPager.OnPageChangeListener {
    ListView lvMusic, lvVideo;
    ViewPager vp;
    TextView tvSongName, tvTimeElapsed, tvTotalTime;
    SeekBar sbMusicBar;
    Button btPrevious, btRewind, btPlayPause, btForward, btNext;
    ArrayList<String> alMusicTitles, alMusicPaths, alVideoTitles, alVideoPaths;
    int currentSongPosition;
    MediaPlayer mp;
    CountDownTimer musicPlayTimer, backPressTimer;
    boolean flagBackPressed;
    CustomListAdapter adapterMusic, adapterVideo;

    @Override
    public void onBackPressed() {
        if (!flagBackPressed) {
            showToast("Press Back Again to Exit");
            flagBackPressed = true;
            backPressTimer.start();
        } else
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backPressTimer = new CountDownTimer(3000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                flagBackPressed = false;
            }
        };
        vp = findViewById(R.id.vp);
        lvMusic = findViewById(R.id.lv1);
        lvVideo = findViewById(R.id.lv2);
        tvSongName = findViewById(R.id.songname);
        tvTimeElapsed = findViewById(R.id.timeelap);
        tvTotalTime = findViewById(R.id.timetot);
        sbMusicBar = findViewById(R.id.seekbar);
        btPrevious = findViewById(R.id.prev);
        btRewind = findViewById(R.id.rew);
        btPlayPause = findViewById(R.id.play);
        btForward = findViewById(R.id.fastforw);
        btNext = findViewById(R.id.next);

        btPrevious.setOnClickListener(this);
        btRewind.setOnClickListener(this);
        btPlayPause.setOnClickListener(this);
        btForward.setOnClickListener(this);
        btNext.setOnClickListener(this);
        sbMusicBar.setOnSeekBarChangeListener(this);

        setViewPager();
        showProgressDialog();
        new Thread(() -> getMusicVideoFiles()).start();
    }

    private void setViewPager() {
        CustomPagerAdapter cp = new CustomPagerAdapter();
        vp.setOffscreenPageLimit(vp.getChildCount() - 1);
        vp.setAdapter(cp);

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getSupportActionBar().addTab(getSupportActionBar().newTab().setText("Music Files").setTabListener(this));
        getSupportActionBar().addTab(getSupportActionBar().newTab().setText("Video Files").setTabListener(this));
        getSupportActionBar().addTab(getSupportActionBar().newTab().setText("Music Player").setTabListener(this));

        vp.setOnPageChangeListener(this);
    }

    private void getMusicVideoFiles() {
        alMusicTitles = new ArrayList<>();
        alMusicPaths = new ArrayList<>();
        alVideoTitles = new ArrayList<>();
        alVideoPaths = new ArrayList<>();

        Cursor cr = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.DISPLAY_NAME);
        while (cr.moveToNext()) {
            alMusicTitles.add(cr.getString(cr.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)));
            alMusicPaths.add(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cr.getLong(cr.getColumnIndex(MediaStore.Audio.Media._ID))).toString());
        }
        cr.close();


        cr = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.Media.DISPLAY_NAME);
        while (cr.moveToNext()) {
            alVideoTitles.add(cr.getString(cr.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)));
            alVideoPaths.add(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cr.getLong(cr.getColumnIndex(MediaStore.Video.Media._ID))).toString());
        }
        dismissProgressDialog();
        runOnUiThread(() -> {
            adapterMusic = new CustomListAdapter(MainActivity.this, R.layout.row, alMusicTitles.toArray(new String[alMusicTitles.size()]));
            lvMusic.setAdapter(adapterMusic);
            lvMusic.setOnItemClickListener(MainActivity.this);
            adapterVideo = new CustomListAdapter(MainActivity.this, R.layout.row, alVideoTitles.toArray(new String[alVideoTitles.size()]));
            lvVideo.setAdapter(adapterVideo);
            lvVideo.setOnItemClickListener(MainActivity.this);
            try {
                if (alMusicPaths.size() > 0)
                    initMediaPlayer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == lvMusic) {
            vp.setCurrentItem(2);
            currentSongPosition = position;
            try {
                initMediaPlayer();
                playsong();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (parent == lvVideo) {
            Intent in = new Intent(MainActivity.this, VideoActivity.class);
            in.putExtra("paths", alVideoPaths);
            in.putExtra("position", position);
            startActivity(in);
        }
    }

    private void playsong() {
        if (mp != null) {
            mp.start();
            btPlayPause.setBackgroundResource(R.drawable.pausere);
            if (musicPlayTimer != null) musicPlayTimer.cancel();
            musicPlayTimer = new CountDownTimer(2 * mp.getDuration(), 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    sbMusicBar.setProgress(mp.getCurrentPosition());
                    tvTotalTime.setText(getMinSecFromMillis(mp.getDuration()));
                    tvTimeElapsed.setText(getMinSecFromMillis(mp.getCurrentPosition()));
                }

                @Override
                public void onFinish() {
                    musicPlayTimer.cancel();
                }
            }.start();
        }
    }

    private void initMediaPlayer() throws IOException {
        if (mp != null) mp.stop();
        mp = new MediaPlayer();
        mp.setOnCompletionListener(this);
        mp.setDataSource(this, Uri.parse(alMusicPaths.get(currentSongPosition)));
        mp.prepare();
        tvSongName.setText(alMusicTitles.get(currentSongPosition));
        sbMusicBar.setMax(mp.getDuration());
    }

    private String getMinSecFromMillis(long millis) {
        String result = null;
        try {
            long sec = Math.round(millis / 1000f);
            SimpleDateFormat sdf1 = new SimpleDateFormat("s");
            SimpleDateFormat sdf2 = new SimpleDateFormat("mm:ss");
            result = sdf2.format(sdf1.parse("" + sec));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void onClick(View v) {
        if (v == btPlayPause) {
            if (mp != null && !mp.isPlaying())
                playsong();
            else
                onPause();
        } else if (v == btRewind) {
            if (mp != null && mp.getCurrentPosition() > 10000)
                mp.seekTo(mp.getCurrentPosition() - 10000);
        } else if (v == btPrevious) {
            if (currentSongPosition > 0) {
                try {
                    currentSongPosition--;
                    initMediaPlayer();
                    playsong();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (v == btForward) {
            if (mp != null && (mp.getDuration() - mp.getCurrentPosition()) > 10000)
                mp.seekTo(mp.getCurrentPosition() + 10000);
        } else if (v == btNext) {
            currentSongPosition++;
            if (currentSongPosition == alMusicTitles.size())
                currentSongPosition = 0;
            try {
                initMediaPlayer();
                playsong();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mp != null) {
            mp.seekTo(seekBar.getProgress());
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        currentSongPosition++;
        if (currentSongPosition == alMusicTitles.size())
            currentSongPosition = 0;
        try {
            initMediaPlayer();
            playsong();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        vp.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mp != null && mp.isPlaying()) {
            mp.pause();
            btPlayPause.setBackgroundResource(R.drawable.playgreen);
        }
    }

    private class CustomListAdapter extends ArrayAdapter {
        int siz;

        public CustomListAdapter(@NonNull Context context, int resource, @NonNull Object[] objects) {
            super(context, resource, objects);
            siz = objects.length;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            View v = getLayoutInflater().inflate(R.layout.row, null);
            ImageView iv = v.findViewById(R.id.iv);
            TextView tv = v.findViewById(R.id.tv);

            if (siz == alMusicTitles.size()) {
                iv.setImageResource(R.drawable.musicicon);
                tv.setText(alMusicTitles.get(position));
            } else if (siz == alVideoTitles.size()) {
                iv.setImageResource(R.drawable.vidicon);
                tv.setText(alVideoTitles.get(position));
            }
            return v;
        }
    }

    private class CustomPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return vp.getChildCount();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            int[] ids = {R.id.SongList, R.id.VideoList, R.id.Music_Player};
            return findViewById(ids[position]);
        }
    }
}