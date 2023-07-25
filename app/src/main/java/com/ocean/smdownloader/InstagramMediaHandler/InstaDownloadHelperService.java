package com.ocean.smdownloader.InstagramMediaHandler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.ocean.smdownloader.Download.DownloadServiceHelper;
import com.ocean.smdownloader.Download.DownloadServiceListener;
import com.ocean.smdownloader.Download.DownloadStates;
import com.ocean.smdownloader.Download.DownloadTaskData;
import com.ocean.smdownloader.Download.MediaFileHandler;
import com.ocean.smdownloader.Download.MediaFileHandlerListener;
import com.ocean.smdownloader.Internet.BitmapLoadListener;
import com.ocean.smdownloader.Internet.LoadBitmapFromLink;
import com.ocean.smdownloader.Internet.LoadSize;
import com.ocean.smdownloader.Internet.SizeLoadListener;
import com.ocean.smdownloader.MainActivity;
import com.ocean.smdownloader.MySharedPreferences;
import com.ocean.smdownloader.R;
import com.ocean.smdownloader.RuntimeData;
import com.ocean.smdownloader.Sources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class InstaDownloadHelperService extends Service implements SizeLoadListener, BitmapLoadListener, DownloadServiceListener, MediaFileHandlerListener {

    private final String NOTIFICATION_CHANNEL_ID = "INSTA_MULTI_DOWNLOAD";
    private final String NOTIFICATION_CHANNEL_NAME = "For Downloading Multiple Items From Instagram";
    private final int NOTIFICATION_ID = -3;

    InstaMultiDownloadBinder binder = new InstaMultiDownloadBinder();

    private int downloadTasksCount;
    private DownloadServiceHelper downloadServiceHelper;
    private MediaFileHandler mediaFileHandler;
    private MySharedPreferences sharedPreferences;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(NOTIFICATION_ID, getNotification());
        RuntimeData.isInstaDownloadHelperServiceRunning = true;

        downloadServiceHelper = new DownloadServiceHelper(this, this);
        mediaFileHandler = new MediaFileHandler(this, this);
        sharedPreferences = new MySharedPreferences(this);

        return super.onStartCommand(intent, flags, startId);
    }

    private Notification getNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(false)
                .setContentTitle("Download")
                .setContentText("Downloading multiple items from instagram")
                .setContentIntent(MainActivity.getPendingIntent(this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            createNotificationChannel();
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }

        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void addDownload(InstaDownloadTaskData task) {
        downloadTasksCount++;
        if (task.getData().getQualities().get(task.getQualityIndex()).getSize() <= 0) {
            new LoadSize(this, task).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, task.getData().getQualities().get(task.getQualityIndex()).getLink());
        } else {
            startDownload(task);
        }

        if (task.getData().getThumbnail() == null)
            new LoadBitmapFromLink(this, task).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, task.getData().getThumbnailLink());
        else
            saveThumbnail(task.getData().getName(), task.getData().getThumbnail());
    }

    public void addMultipleDownload(ArrayList<InstaDownloadTaskData> tasks) {
        for (InstaDownloadTaskData task : tasks) {
            addDownload(task);
        }
    }

    private void startDownload(InstaDownloadTaskData taskData) {
        InstaMediaDetailData data = taskData.getData();

        String outPath = mediaFileHandler.getPathForFile(data.getName(), data.getType(), null, false, taskData.getData().getQualities().get(taskData.getQualityIndex()).getSize());
        if (outPath != null) {
            data.setName(outPath.substring(outPath.lastIndexOf(File.separator) + 1));

            downloadServiceHelper.addDownload(new DownloadTaskData(
                    sharedPreferences.incrementDownloadCounts(), data.getName(),
                    data.getType(), Sources.INSTA, saveThumbnail(data.getName(), data.getThumbnail()),
                    taskData.getData().getQualities().get(taskData.getQualityIndex()).getLink(),
                    null,
                    outPath,
                    null,
                    0,
                    0,
                    taskData.getData().getQualities().get(taskData.getQualityIndex()).getSize(),
                    0,
                    DownloadStates.STATE_PAUSE,
                    DownloadStates.STATE_NOT_AVAILABLE
            ));
        } else {
            Toast.makeText(this, "Error while creating file: " + data.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private String saveThumbnail(String name, Bitmap bitmap) {
        if (bitmap == null)
            return null;

        File thumbnailFolder = new File(getExternalCacheDir() + "/.thumb");
        String thumbnailFilePath;

        if (thumbnailFolder.exists() || thumbnailFolder.mkdirs()) {
            thumbnailFilePath = thumbnailFolder + File.separator + name + MediaFileHandler.getExtension(MediaFileHandler.TYPE_IMAGE);

            try {
                FileOutputStream outputStream = new FileOutputStream(thumbnailFilePath);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                return thumbnailFilePath;

            } catch (FileNotFoundException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
        } else {
            Toast.makeText(this, "Failed to save thumbnail", Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    @Override
    public void onLoad(long size, Object object) {
        InstaDownloadTaskData taskData = (InstaDownloadTaskData) object;
        if (size > 0) {
            taskData.getData().getQualities().get(taskData.getQualityIndex()).setSize(size);
            startDownload(taskData);
        } else {
            new LoadSize(this, taskData).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, taskData.getData().getQualities().get(taskData.getQualityIndex()).getLink());
        }
    }

    @Override
    public void onFailed(String error, Object object) {

    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Object object) {
        ((InstaDownloadTaskData) object).getData().setThumbnail(bitmap);
        saveThumbnail(((InstaDownloadTaskData) object).getData().getName(), bitmap);
    }

    @Override
    public void onFileCreated(String primaryFilePath, String secondaryFilePath, boolean isOverwrite) {

    }

    @Override
    public void onCanceled() {

    }

    @Override
    public void onError(String fileName, String error) {

    }

    @Override
    public void onDownloadAdded() {
        if (--downloadTasksCount <= 0) {
            downloadServiceHelper.unbindService();
            stopSelf();
            stopForeground(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RuntimeData.isInstaDownloadHelperServiceRunning = false;
    }

    class InstaMultiDownloadBinder extends Binder {
        public InstaDownloadHelperService getService() {
            return InstaDownloadHelperService.this;
        }
    }
}
