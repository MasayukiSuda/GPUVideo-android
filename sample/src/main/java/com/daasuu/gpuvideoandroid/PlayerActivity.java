package com.daasuu.gpuvideoandroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.daasuu.gpuv.egl.filter.GlFilter;
import com.daasuu.gpuv.player.GPUPlayerView;
import com.daasuu.gpuvideoandroid.widget.MovieWrapperView;
import com.daasuu.gpuvideoandroid.widget.PlayerTimer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;

import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private static final String STREAM_URL_MP4_VOD_LONG = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4";

    public static void startActivity(Activity activity) {
        Intent intent = new Intent(activity, PlayerActivity.class);
        activity.startActivity(intent);
    }

    private GPUPlayerView gpuPlayerView;
    private ExoPlayer player;
    private Button button;
    private SeekBar timeSeekBar;
    private SeekBar filterSeekBar;
    private PlayerTimer playerTimer;
    private GlFilter filter;
    private FilterAdjuster adjuster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        setUpViews();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpSimpleExoPlayer();
        setUoGlPlayerView();
        setUpTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
        if (playerTimer != null) {
            playerTimer.stop();
            playerTimer.removeMessages(0);
        }
    }

    private void setUpViews() {
        // play pause
        button = (Button) findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player == null) return;

                if (button.getText().toString().equals(PlayerActivity.this.getString(R.string.pause))) {
                    player.setPlayWhenReady(false);
                    button.setText(R.string.play);
                } else {
                    player.setPlayWhenReady(true);
                    button.setText(R.string.pause);
                }
            }
        });

        // seek
        timeSeekBar = (SeekBar) findViewById(R.id.timeSeekBar);
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player == null) return;

                if (!fromUser) {
                    // We're not interested in programmatically generated changes to
                    // the progress bar's position.
                    return;
                }

                player.seekTo(progress * 1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // do nothing
            }
        });

        filterSeekBar = (SeekBar) findViewById(R.id.filterSeekBar);
        filterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (adjuster != null) {
                    adjuster.adjust(filter, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // do nothing
            }
        });

        // list
        ListView listView = findViewById(R.id.list);
        final List<FilterType> filterTypes = FilterType.createFilterList();
        listView.setAdapter(new FilterAdapter(this, R.layout.row_text, filterTypes));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                filter = FilterType.createGlFilter(filterTypes.get(position), getApplicationContext());
                adjuster = FilterType.createFilterAdjuster(filterTypes.get(position));
                findViewById(R.id.filterSeekBarLayout).setVisibility(adjuster != null ? View.VISIBLE : View.GONE);
                gpuPlayerView.setGlFilter(filter);
            }
        });
    }


    private void setUpSimpleExoPlayer() {
        // SimpleExoPlayer
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(new DefaultTrackSelector(this))
                .build();

        player.addMediaItem(MediaItem.fromUri(Uri.parse(STREAM_URL_MP4_VOD_LONG)));
        player.prepare();
        player.setPlayWhenReady(true);
    }


    private void setUoGlPlayerView() {
        gpuPlayerView = new GPUPlayerView(this);
        gpuPlayerView.setExoPlayer(player);
        gpuPlayerView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ((MovieWrapperView) findViewById(R.id.layout_movie_wrapper)).addView(gpuPlayerView);
        gpuPlayerView.onResume();
    }


    private void setUpTimer() {
        playerTimer = new PlayerTimer();
        playerTimer.setCallback(timeMillis -> {
            long position = player.getCurrentPosition();
            long duration = player.getDuration();

            if (duration <= 0) return;

            timeSeekBar.setMax((int) duration / 1000);
            timeSeekBar.setProgress((int) position / 1000);
        });
        playerTimer.start();
    }


    private void releasePlayer() {
        gpuPlayerView.onPause();
        ((MovieWrapperView) findViewById(R.id.layout_movie_wrapper)).removeAllViews();
        gpuPlayerView = null;
        player.stop();
        player.release();
        player = null;
    }


}
