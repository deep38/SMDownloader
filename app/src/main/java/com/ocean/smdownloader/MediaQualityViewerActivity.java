package com.ocean.smdownloader;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.view.View;
import android.widget.Toast;

import com.ocean.smdownloader.InstagramMediaHandler.InstaQualityViewer;
import com.ocean.smdownloader.YTMediaHandler.YTMediaQualityViewer;

public class MediaQualityViewerActivity extends AppCompatActivity {

    View ytMediaQualityView;
    View instaMediaQualityView;

    YTMediaQualityViewer ytMediaQualityViewer;
    InstaQualityViewer instaMediaQualityViewer;
    MySharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_quality_viewer);

        sharedPreferences = new MySharedPreferences(this);

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED)
            AppCompatDelegate.setDefaultNightMode(MainActivity.THEME_STATES[sharedPreferences.getThemeState()]);

        ytMediaQualityView = findViewById(R.id.ytMediaQualityView);
        instaMediaQualityView = findViewById(R.id.insta_media_quality_view);

        Intent intent = getIntent();
        String link = intent.getStringExtra(Intent.EXTRA_TEXT);

        if (link == null) {
            Toast.makeText(this, "No link provided", Toast.LENGTH_SHORT).show();
            finish();
        } else if (isLinkOfYoutube(link)) {
            instaMediaQualityView.setVisibility(View.GONE);
            ytMediaQualityViewer = new YTMediaQualityViewer(this, this, link, ytMediaQualityView);
        } else if (isLinkOfInstagram(link)) {
            ytMediaQualityView.setVisibility(View.GONE);
            instaMediaQualityViewer = new InstaQualityViewer(this, this, instaMediaQualityView, link);
        } else {
            Toast.makeText(this, "Invalid link", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean isLinkOfYoutube(String link) {
        return link.startsWith("https://youtu.be/") || link.startsWith("https://youtube.com") || link.startsWith("https://m.youtube.com/");
    }

    private boolean isLinkOfInstagram(String link) {
        return link.startsWith("https://www.instagram.com/");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RequestManger.EXTERNAL_STORAGE_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ytMediaQualityViewer != null) {
                    if (Environment.isExternalStorageManager())
                        ytMediaQualityViewer.startPendingDownload();
                    else
                        ytMediaQualityViewer.requestManger.onStoragePermissionGranted();
                } else  if (instaMediaQualityViewer != null) {
                    if (Environment.isExternalStorageManager())
                        instaMediaQualityViewer.startPendingDownload();
                    else
                        instaMediaQualityViewer.requestManger.onStoragePermissionGranted();
                }
            } else {
                if (ytMediaQualityViewer != null)
                    ytMediaQualityViewer.startPendingDownload();
                else if (instaMediaQualityViewer != null)
                    instaMediaQualityViewer.startPendingDownload();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestManger.MANAGE_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager() && new RequestManger(this).checkStoragePermission()) {
                if (ytMediaQualityViewer != null)
                    ytMediaQualityViewer.startPendingDownload();
                else if (instaMediaQualityViewer != null)
                    instaMediaQualityViewer.startPendingDownload();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (ytMediaQualityViewer != null)
            ytMediaQualityViewer.cancelAllTask();

        if (instaMediaQualityViewer != null)
            instaMediaQualityViewer.cancelAllTask();

    }
}