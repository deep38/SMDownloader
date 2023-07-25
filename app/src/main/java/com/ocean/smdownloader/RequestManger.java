package com.ocean.smdownloader;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.annotation.RequiresApi;

public class RequestManger {
    public final static int EXTERNAL_STORAGE_CODE = 11;
    public final static int MANAGE_STORAGE_REQUEST_CODE = 1;

    private final Activity activity;

    public RequestManger(Activity activity) {
        this.activity = activity;
    }
    
    public void requestPermission(String[] permissions, int requestCode) {
        activity.requestPermissions(permissions, requestCode);
    }

    public void requestStoragePermission() {
        activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_CODE);
    }

    public boolean checkStoragePermission() {
        return activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public void onStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestManageExternalStorage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public boolean requestManageExternalStorage() {
        if (!Environment.isExternalStorageManager()) {
            Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
            activity.startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);

            return false;
        }

        return true;
    }
}
