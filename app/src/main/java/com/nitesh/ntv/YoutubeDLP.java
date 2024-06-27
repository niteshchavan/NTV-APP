package com.nitesh.ntv;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.BuildConfig;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.yausername.aria2c.Aria2c;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class YoutubeDLP extends AppCompatActivity {

    private boolean updating = false;
    private static ExoPlayer player;
    private WebView webView;
    private String matchedUrl;
    private PlayerView playerView;
    private ImageButton BtnStartStream;
    private ImageButton showPlayer;
    private ImageButton hidePlayer;
    private ImageButton BtnUpdate;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private PowerManager.WakeLock wakeLock;
    private ImageButton BtnStartDownload;
    private ImageButton BtnStopDownload;
    private ImageButton hd_Play;
    private ProgressBar pbLoading;
    private ProgressBar dwLoading;
    private boolean downloading = false;
    private final String processId = "MyDlProcess";




    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_youtube_dl);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //library initialization
        try {
            YoutubeDL.getInstance().init(this);
            FFmpeg.getInstance().init(this);
            Aria2c.getInstance().init(this);
        } catch (YoutubeDLException e) {
            Log.e(TAG, "failed to initialize youtubedl-android", e);
            Toast.makeText(YoutubeDLP.this, "Failed to initialize YouTubeDL", Toast.LENGTH_LONG).show();
            return; // Exit if initialization fails
        }

        //Initialize the wake lock in the
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.player_view);
        BtnStartStream = findViewById(R.id.btnStartStream);
        BtnUpdate = findViewById(R.id.btnUpdate);
        hidePlayer = findViewById(R.id.hideplayer);
        showPlayer = findViewById(R.id.showplayer);
        webView = findViewById(R.id.webview);
        BtnStartDownload = findViewById(R.id.btnStartDownload);
        BtnStopDownload = findViewById(R.id.btnStopDownload);
        hd_Play = findViewById(R.id.hd_play);
        dwLoading = findViewById(R.id.dw_status);
        pbLoading = findViewById(R.id.pb_status);
        BtnStopDownload.setVisibility(View.GONE);
        player = new ExoPlayer.Builder(this).build();
        playerView.setVisibility(View.GONE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String head = request.getRequestHeaders().toString();
                String regex = "\\bhttps?://m\\.youtube\\.com/watch\\?v=[a-zA-Z0-9_-]{11}\\b";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(head);

                if (matcher.find()) {
                    matchedUrl = matcher.group();

                }

                String url = request.getUrl().toString();

                if (url.contains("youtube.com") || url.contains("i.ytimg.com") || url.contains("accounts.google.com") ||
                        url.contains("gstatic.com") || url.contains("ggpht.com") || url.contains("googleapis.com")) {
                    return super.shouldInterceptRequest(view, request);
                }
                return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream("".getBytes()));
            }
        });
        webView.loadUrl("https://m.youtube.com/");

        BtnUpdate.setOnClickListener(v -> updateYoutubeDL());
        BtnStartStream.setOnClickListener(v -> {
            playerView.setVisibility(View.VISIBLE);
            startStream(matchedUrl);
        });

        // Set up back button handling
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {

                if (webView.canGoBack()) {
                    // Go back in the web view history
                    playerView.setVisibility(View.GONE);
                    webView.goBack();
                }else {
                    player.release();
                    Intent intent = new Intent(YoutubeDLP.this, IntroActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }


            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                handleIncomingLink(uri);
            }
        }

    }
    private void handleIncomingLink(Uri uri) {
        // Extract the full URI as a string
        String fullUrl = uri.toString();
        playerView.setVisibility(View.VISIBLE);
        startStream(fullUrl);

    }

    private void updateYoutubeDL() {
        if (updating) {
            Toast.makeText(YoutubeDLP.this, "Update is already in progress", Toast.LENGTH_LONG).show();
            return;
        }

        updating = true;

        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel._STABLE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    updating = false;
                    switch (status) {
                        case DONE:
                            Toast.makeText(YoutubeDLP.this, "Update successful", Toast.LENGTH_LONG).show();
                            break;
                        case ALREADY_UP_TO_DATE:
                            Toast.makeText(YoutubeDLP.this, "Already up to date", Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(YoutubeDLP.this, status.toString(), Toast.LENGTH_LONG).show();
                            break;
                    }
                }, e -> {
                    Log.e(TAG, "Failed to update YouTubeDL", e);
                    Toast.makeText(YoutubeDLP.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updating = false;
                });
        compositeDisposable.add(disposable);
    }

    private void startStream(String url) {
        //Log.d(TAG, "startStream " + url);

        pbLoading.setVisibility(View.VISIBLE);
        Disposable disposable = Observable.fromCallable(() -> {
                    YoutubeDLRequest request = new YoutubeDLRequest(url);
                    request.addOption("-f", "18");
                    return YoutubeDL.getInstance().getInfo(request);
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(streamInfo -> {
                    pbLoading.setVisibility(View.GONE);
                    String videoUrl = streamInfo.getUrl();
                    if (TextUtils.isEmpty(videoUrl)) {
                        Toast.makeText(YoutubeDLP.this, "Failed to get stream URL", Toast.LENGTH_LONG).show();
                    } else {
                        setupVideoView(videoUrl);
                    }
                }, e -> {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Failed to get stream info", e);
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(YoutubeDLP.this, "Streaming failed. Failed to get stream info", Toast.LENGTH_LONG).show();
                });
        compositeDisposable.add(disposable);
    }
    private void HdStream(String url) {

        pbLoading.setVisibility(View.VISIBLE);

        Disposable disposable = Observable.fromCallable(() -> {
                    YoutubeDLRequest request = new YoutubeDLRequest(url);
                    // best stream containing video+audio
                    request.addOption("-f", "best");
                    return YoutubeDL.getInstance().getInfo(request);
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(streamInfo -> {
                    pbLoading.setVisibility(View.GONE);
                    String videoUrl = streamInfo.getUrl();
                    if (TextUtils.isEmpty(videoUrl)) {
                        Toast.makeText(YoutubeDLP.this, "failed to get stream url", Toast.LENGTH_LONG).show();
                    } else {
                        setupVideoView(videoUrl);
                    }
                }, e -> {
                    if(BuildConfig.DEBUG) Log.e(TAG,  "failed to get stream info", e);
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(YoutubeDLP.this, "streaming failed. failed to get stream info", Toast.LENGTH_LONG).show();
                });
        compositeDisposable.add(disposable);
    }

    private void setupVideoView(String videoUrl) {
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);


        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                updatePlayerState(playWhenReady);
            }
        });
        updatePlayerState(true);
    }
    public void hd_play(View view) {
        HdStream(matchedUrl);
        Toast.makeText(YoutubeDLP.this, "Playing HD", Toast.LENGTH_LONG).show();
        playerView.setVisibility(View.VISIBLE);
    }
    public void btnStartDownload(View view) {
        BtnStartDownload.setVisibility(View.GONE);
        BtnStopDownload.setVisibility(View.VISIBLE);
        startDownload(matchedUrl);
        Toast.makeText(YoutubeDLP.this, "Downloading", Toast.LENGTH_LONG).show();
    }

    public void btnStopDownload(View view) {
        BtnStartDownload.setVisibility(View.VISIBLE);
        BtnStopDownload.setVisibility(View.GONE);
        try {
            YoutubeDL.getInstance().destroyProcessById(processId);
            Toast.makeText(YoutubeDLP.this, "Stopped", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
    private void updatePlayerState(boolean playWhenReady) {
        if (playWhenReady) {
            player.play();
        } else {
            player.pause();
        }
    }

    public boolean isStoragePermissionGranted() {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return false;
        }
    }


    private void startDownload(String url) {
        if (downloading) {
            Toast.makeText(YoutubeDLP.this, "cannot start download. a download is already in progress", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isStoragePermissionGranted()) {
            Toast.makeText(YoutubeDLP.this, "grant storage permission and retry", Toast.LENGTH_LONG).show();
            return;
        }


        YoutubeDLRequest request = getYoutubeDLRequest(url);

        showStart();

        downloading = true;
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, processId))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    dwLoading.setVisibility(View.GONE);
                    BtnStartDownload.setVisibility(View.VISIBLE);
                    BtnStopDownload.setVisibility(View.GONE);
                    Toast.makeText(YoutubeDLP.this, "Download successful", Toast.LENGTH_LONG).show();
                    downloading = false;
                }, e -> {
                    if (BuildConfig.DEBUG) Log.e(TAG, "failed to download", e);
                    dwLoading.setVisibility(View.GONE);
                    Toast.makeText(YoutubeDLP.this, "Download failed" + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.d("geterror",e.toString());
                    downloading = false;
                });
        compositeDisposable.add(disposable);

    }

    private @NonNull YoutubeDLRequest getYoutubeDLRequest(String url) {
        YoutubeDLRequest request = new YoutubeDLRequest(url);
        File youtubeDLDir = getDownloadLocation();

        request.addOption("--no-mtime");
        request.addOption("--downloader", "libaria2c.so");
        request.addOption("--external-downloader-args", "aria2c:\"--summary-interval=1\"");
        request.addOption("-f", "best");
        //request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best");
        request.addOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");
        return request;
    }

    @NonNull
    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File youtubeDLDir = new File(downloadsDir, "youtubedl-android");
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir();
        return youtubeDLDir;
    }
    private void showStart() {

        dwLoading.setVisibility(View.VISIBLE);
    }
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
            findViewById(R.id.webview).setVisibility(View.GONE);
            findViewById(R.id.btnStartStream).setVisibility(View.GONE);
            findViewById(R.id.btnUpdate).setVisibility(View.GONE);
            findViewById(R.id.showplayer).setVisibility(View.VISIBLE);

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
            findViewById(R.id.webview).setVisibility(View.VISIBLE);
            findViewById(R.id.btnStartStream).setVisibility(View.VISIBLE);
            findViewById(R.id.btnUpdate).setVisibility(View.VISIBLE);
            findViewById(R.id.showplayer).setVisibility(View.VISIBLE);
            showSystemUI();
        }
        // Acquire wake lock to keep screen on during playback
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:AudioPlayback");
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            webView.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);

            //SHOW Hide Image Buttons
            showPlayer.setVisibility(View.GONE);
            BtnStartStream.setVisibility(View.GONE);
            BtnUpdate.setVisibility(View.GONE);
            BtnStartDownload.setVisibility(View.GONE);
            BtnStopDownload.setVisibility(View.GONE);
            hd_Play.setVisibility(View.GONE);
            hidePlayer.setVisibility(View.GONE);
            ViewGroup.LayoutParams layoutParams = playerView.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                marginLayoutParams.topMargin = 0;
                playerView.setLayoutParams(marginLayoutParams);
            }
            hideSystemUI();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            webView.setVisibility(View.VISIBLE);
            //SHOW Hide Image Buttons
            showPlayer.setVisibility(View.VISIBLE);
            hidePlayer.setVisibility(View.VISIBLE);
            BtnStartStream.setVisibility(View.VISIBLE);
            BtnUpdate.setVisibility(View.VISIBLE);
            BtnStartDownload.setVisibility(View.VISIBLE);
            hd_Play.setVisibility(View.VISIBLE);
            BtnStopDownload.setVisibility(View.GONE);
            ViewGroup.LayoutParams layoutParams = playerView.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = 700; // Set an appropriate height for portrait mode
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                marginLayoutParams.topMargin = 100;
                playerView.setLayoutParams(marginLayoutParams);
            }
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
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();

        }
    }
    public void showplayer(View view) {
        if (playerView.getVisibility() == View.VISIBLE) {
            playerView.setVisibility(View.GONE);
            showPlayer.setVisibility(View.GONE);
            hidePlayer.setVisibility(View.VISIBLE);
        } else if (playerView.getVisibility() == View.GONE) {
            playerView.setVisibility(View.VISIBLE);
            hidePlayer.setVisibility(View.GONE);
            showPlayer.setVisibility(View.VISIBLE);
        }
    }
    public void hideplayer(View view) {
        if (playerView.getVisibility() == View.VISIBLE) {
            playerView.setVisibility(View.GONE);
            showPlayer.setVisibility(View.GONE);
            hidePlayer.setVisibility(View.VISIBLE);
        } else if (playerView.getVisibility() == View.GONE) {
            playerView.setVisibility(View.VISIBLE);
            hidePlayer.setVisibility(View.GONE);
            showPlayer.setVisibility(View.VISIBLE);
        }
    }

}
