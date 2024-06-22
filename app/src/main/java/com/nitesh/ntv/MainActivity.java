package com.nitesh.ntv;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.ui.PlayerView;


public class MainActivity extends AppCompatActivity implements MyAdapter.OnItemClickListener {
    private static final String TAG = "MainActivity";
    private MyAdapter adapter;
    private List<Channel> data;
    private PlayerView playerView;
    private ExoPlayer player;


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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        data = new ArrayList<>();

        // Initialize player view
        playerView = findViewById(R.id.video_view);

        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter(data, this, this); // Pass MainActivity as listener
        recyclerView.setAdapter(adapter);
        // Add divider
        // Add custom divider
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                new LinearLayoutManager(this).getOrientation());
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);
        // Fetch data from URL
        fetchData();
    }

    private void fetchData() {
        String url = "https://raw.githubusercontent.com/niteshchavan/NTV/main/ntv.json";

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
        try {
            DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
            HlsMediaSource hlsMediaSource =
                    new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(channel.getUrl()));
            player.setMediaSource(hlsMediaSource);
            player.prepare();
            player.play();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ExoPlayer: ", e);
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release ExoPlayer when activity is destroyed
        player.release();
    }


}
