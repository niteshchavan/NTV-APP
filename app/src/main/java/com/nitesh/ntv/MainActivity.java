package com.nitesh.ntv;

import static androidx.core.content.ContextCompat.startActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.exoplayer.LoadControl;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;


public class MainActivity extends AppCompatActivity implements MyAdapter.OnItemClickListener {
    private static final String TAG = "MainActivity";
    private MyAdapter adapter;
    private List<Channel> data;
    private PlayerView playerView;
    private ExoPlayer player;
    private PowerManager.WakeLock wakeLock;
    private MediaItem mediaItem;
    private MediaSource mediaSource;
    private String url;
    public void onFullscreenClicked(View view) {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            // Set PlayerView to fullscreen size
            ViewGroup.LayoutParams params = playerView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            playerView.setLayoutParams(params);
            // Hide system bars for a more immersive experience (optional)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            // Hide the RecyclerView
            findViewById(R.id.recycler_view).setVisibility(View.GONE);
            hideSystemUI();
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            // Reset PlayerView to original size (optional)
            ViewGroup.LayoutParams params = playerView.getLayoutParams();
            // Set your original width and height here (based on your layout)
             params.width = ViewGroup.LayoutParams.MATCH_PARENT;
             params.height = 700;

            playerView.setLayoutParams(params);
            // Show system bars again
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

            // Show the RecyclerView
            findViewById(R.id.recycler_view).setVisibility(View.VISIBLE);
            showSystemUI();
        }
        // Acquire wake lock to keep screen on during playback
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:AudioPlayback");
    }

    private int uiOptions;
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        uiOptions = decorView.getSystemUiVisibility();
        int newUiOptions = uiOptions;
        newUiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(newUiOptions);
    }
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        uiOptions = decorView.getSystemUiVisibility();
        int newUiOptions = uiOptions;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_LOW_PROFILE;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(newUiOptions);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        data = new ArrayList<>();
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter(data, this, this); // Pass MainActivity as listener
        recyclerView.setAdapter(adapter);
        // Add custom divider
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                new LinearLayoutManager(this).getOrientation());
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);
        // Fetch data from URL
        fetchData();
        backpress();

        playerView = findViewById(R.id.video_view);
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        30 * 1000, // minBufferMs
                        5 * 60 * 1000, // maxBufferMs
                        30 * 1000, // bufferForPlaybackMs
                        30 * 1000 // bufferForPlaybackAfterRebufferMs
                )
                .setTargetBufferBytes(-1) // Default value, not overriding
                .setPrioritizeTimeOverSizeThresholds(false) // Default value, not overriding
                .build();


        player = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();
        playerView.setPlayer(player);

        player.setPlayWhenReady(true);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Playback state changed: " + playbackState + ". Restarting player.");
                    restartPlayer();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Playback error: " + error.getMessage());
                restartPlayer();
            }
        });
    }


    private void backpress(){

        // Set up back button handling
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Navigate back to IntroActivity
                Intent intent = new Intent(MainActivity.this, IntroActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

    }

    private void fetchData() {
        String url = "https://raw.githubusercontent.com/niteshchavan/NTV/main/private.json";

        RequestQueue queue = Volley.newRequestQueue(this);
        @SuppressLint("NotifyDataSetChanged") StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray channelsArray = jsonObject.getJSONArray("NTV");
                        data.clear();

                        for (int i = 0; i < channelsArray.length(); i++) {
                            JSONObject channelObject = channelsArray.getJSONObject(i);
                            Channel channel = new Channel();
                            channel.setId(channelObject.getString("id"));
                            channel.setName(channelObject.getString("name"));
                            channel.setImage(channelObject.getString("image"));
                            channel.setUrl(channelObject.getString("url"));
                            data.add(channel);
                        }

                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(MainActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show());

        queue.add(request);
    }


    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onItemClick(Channel channel) {
        player.stop();
        url = channel.getUrl();
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        mediaItem = new MediaItem.Builder()
                .setUri(url)
                .build();
        mediaSource = new DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(mediaItem);
        player.setMediaSource(mediaSource);
        player.prepare();

    }
    @OptIn(markerClass = UnstableApi.class)
    private void restartPlayer() {
        if (player != null) {
            player.setMediaSource(mediaSource);
            player.prepare();
            player.setPlayWhenReady(true);
        }
    }


    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();

        }
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        releaseWakeLock();
//
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();

        }
    }



}
