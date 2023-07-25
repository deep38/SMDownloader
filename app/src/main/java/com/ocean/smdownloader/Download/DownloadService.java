package com.ocean.smdownloader.Download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import android.util.Log;
import android.widget.Toast;

import com.ocean.smdownloader.Algorithms;
import com.ocean.smdownloader.MainActivity;
import com.ocean.smdownloader.MySharedPreferences;
import com.ocean.smdownloader.R;
import com.ocean.smdownloader.RuntimeData;

import java.io.File;
import java.util.HashMap;

public class DownloadService extends Service implements DownloadTaskListener, MediaMuxerTaskListener {
    public static final String ACTION_SERVICE_START = "ACTION_SERVICE_START";

    private final String NOTIFICATION_CHANNEL_ID = "DOWNLOAD_MEDIA";

    private DownloadTaskListener downloadTaskListener;
    private MediaMuxerTaskListener mediaMuxerTaskListener;
    private HashMap<Integer, DownloadTaskData> notifications;
    private long lastProgressUpdateTime = 0;

    DownloadTask downloadTask;
    MySharedPreferences mySharedPreferences;
    NotificationManager notificationManager;
    DownloadTasksDatabase downloadTasksDatabase;
    PendingIntent mainActivityPendingIntent;
    /****************************** TASK CONTROL RECEIVER *****************************************/

    private final BroadcastReceiver taskControllerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case DownloadControllerIntent.ACTION_TASK_PAUSE:
                    pauseTask(notifications.get(intent.getIntExtra(DownloadControllerIntent.EXTRA_TASK_ID, 0)));
                    break;
                case DownloadControllerIntent.ACTION_TASK_RESUME:
                    resumeTask(notifications.get(intent.getIntExtra(DownloadControllerIntent.EXTRA_TASK_ID, 0)));
                    break;
                case DownloadControllerIntent.ACTION_SERVICE_STOP:
                    stopService();
                    break;
            }
        }
    };

    /**********************************************************************************************/

    @Override
    public IBinder onBind(Intent intent) {
        return new DownloadServiceBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int SERVICE_NOTIFICATION_ID = -1;

        downloadTask = new DownloadTask(this, this);
        notificationManager = getSystemService(NotificationManager.class);
        mySharedPreferences = new MySharedPreferences(this);
        downloadTasksDatabase = new DownloadTasksDatabase(this);
        mainActivityPendingIntent = PendingIntent.getActivity(this, -1, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        RuntimeData.isDownloadServiceRunning = true;
        startForeground(SERVICE_NOTIFICATION_ID, getServiceNotification());

        notifications = new HashMap<>();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadControllerIntent.ACTION_TASK_PAUSE);
        intentFilter.addAction(DownloadControllerIntent.ACTION_TASK_RESUME);
        intentFilter.addAction(DownloadControllerIntent.ACTION_SERVICE_STOP);

        registerReceiver(taskControllerReceiver, intentFilter);

        return START_STICKY;
    }

    private Notification getServiceNotification() {
        Intent stopServiceIntent = new Intent();
        stopServiceIntent.setAction(DownloadControllerIntent.ACTION_SERVICE_STOP);
        PendingIntent stopServicePendingIntent = PendingIntent.getBroadcast(this, 1, stopServiceIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(false)
                .setContentTitle("Download")
                .setContentText(getString(R.string.app_name) + " is running")
                .setContentIntent(MainActivity.getPendingIntent(this))
                .addAction(new NotificationCompat.Action(0, "Stop all", stopServicePendingIntent));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            createNotificationChannel();
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }

        return builder.build();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        RuntimeData.isDownloadServiceRunning = true;
        Intent intent = new Intent();
        intent.setAction(ACTION_SERVICE_START);
        sendBroadcast(intent);
    }

    private Notification getDownloadNotification(DownloadTaskData downloadTaskData, String contentText, int smallIcon, int progress) {
        int status;

        if (downloadTaskData.getSecondaryStatus() != DownloadStates.STATE_NOT_AVAILABLE && downloadTaskData.getPrimaryStatus() == DownloadStates.STATE_COMPLETE) {
            status = downloadTaskData.getSecondaryStatus();
        } else {
            status = downloadTaskData.getPrimaryStatus();

        }
//        Log.d("DownloadService", "Notification: " + DownloadTaskList.taskList.indexOf(downloadTaskData) + " " + downloadTaskData);
        Intent taskControllerIntent = new Intent();
        taskControllerIntent.setAction(status == DownloadStates.STATE_RUNNING ? DownloadControllerIntent.ACTION_TASK_PAUSE : DownloadControllerIntent.ACTION_TASK_RESUME);
        taskControllerIntent.putExtra(DownloadControllerIntent.EXTRA_TASK_ID, downloadTaskData.getId());
        PendingIntent taskControllerPendingIntent = PendingIntent.getBroadcast(this, downloadTaskData.getId(), taskControllerIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(downloadTaskData.getName())
                .setContentIntent(MainActivity.getPendingIntent(this))
                .setOngoing(false);

        if (contentText != null)
            builder.setContentText(contentText);

        if (status == DownloadStates.STATE_RUNNING || status == DownloadStates.STATE_PAUSE)
            builder.addAction(new NotificationCompat.Action(0, status == DownloadStates.STATE_RUNNING ? "Pause" : "Resume", taskControllerPendingIntent));

        if (progress >= 0 && progress < 100)
            builder.setProgress(100, progress, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);

        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {

        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            String NOTIFICATION_CHANNEL_NAME = "Download Service";
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void addDownload(DownloadTaskData taskData) {

        if (downloadTasksDatabase.insertData(taskData)) {
            downloadTask.startDownload(taskData);
            notifications.put(taskData.getId(), taskData);

            DownloadTasks.list.add(0, taskData);
            DownloadTasks.newAddedTasks++;
            DownloadTasks.downloadedStartIndex++;

            if (downloadTaskListener != null)
                downloadTaskListener.onStart(taskData);
        } else {
            Toast.makeText(this, "Failed to insert data in database", Toast.LENGTH_SHORT).show();
        }
    }

    public void setListeners(DownloadTaskListener downloadTaskListener, MediaMuxerTaskListener mediaMuxerTaskListener) {
        this.downloadTaskListener = downloadTaskListener;
        this.mediaMuxerTaskListener = mediaMuxerTaskListener;
    }

    private void stopService() {
        downloadTask.pauseAllDownloads();
    }

    /****************************** NEW DOWNLOAD LISTENER *****************************************/

    @Override
    public void onStart(DownloadTaskData taskData) {
//        Log.d("DownloadService", "onStart: " + taskData);

        if (DownloadTask.isPrimaryTaskCompleted(taskData) && DownloadTask.hasSecondaryTask(taskData)) {
            taskData.setSecondaryStatus(DownloadStates.STATE_RUNNING);
            downloadTasksDatabase.updateSecondaryStatus(taskData.getId(), DownloadStates.STATE_RUNNING);
        } else {
            taskData.setPrimaryStatus(DownloadStates.STATE_RUNNING);
            downloadTasksDatabase.updatePrimaryStatus(taskData.getId(), DownloadStates.STATE_RUNNING);
        }
        notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, null, android.R.drawable.stat_sys_download, 0));
        //taskDataHashMap.put(taskData.getId(), taskData);

        if (downloadTaskListener != null)
            downloadTaskListener.onResume(taskData);

    }

    @Override
    public void onResume(DownloadTaskData taskData) {}
    @Override
    public void restart(DownloadTaskData taskData) {}

    @Override
    public void onPause(DownloadTaskData taskData, boolean isSecondaryTask) {
//        Log.d("DownloadService", "onPause: " + taskData);

        if (isSecondaryTask) {
//            Log.d("DownloadService", "onPause secondary " + taskData.getSecondaryDownloadedSize());
            taskData.setSecondaryStatus(DownloadStates.STATE_PAUSE);
            downloadTasksDatabase.updateSecondaryStatus(taskData.getId(), DownloadStates.STATE_PAUSE);
            downloadTasksDatabase.updateSecondaryDownloadedSize(taskData.getId(), taskData.getSecondaryDownloadedSize());
        } else {
//            Log.d("DownloadService", "onPause primary " + taskData.getPrimaryDownloadedSize());
            taskData.setPrimaryStatus(DownloadStates.STATE_PAUSE);
            downloadTasksDatabase.updatePrimaryStatus(taskData.getId(), DownloadStates.STATE_PAUSE);
            downloadTasksDatabase.updatePrimaryDownloadedSize(taskData.getId(), taskData.getPrimaryDownloadedSize());
        }

        notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, "Paused", R.drawable.ic_pause, -1));

        if (downloadTaskListener != null)
            downloadTaskListener.onPause(taskData, isSecondaryTask);
    }

    @Override
    public void onZeroTaskRemain(boolean removeNotification) {
        if (removeNotification) {
            for (int id : notifications.keySet()) {
                notificationManager.cancel(id);
            }
        }
        unregisterReceiver(taskControllerReceiver);
        stopSelf();
        stopForeground(true);
    }

    @Override
    public void onProgress(DownloadTaskData downloadTaskData, int progress) {
        //Log.d("DownloadService", "onProgress: " + System.currentTimeMillis() + ", " + lastProgressUpdateTime);
        if ((System.currentTimeMillis() - lastProgressUpdateTime) >= 1000) {
            Log.d("DownloadService", "onProgress: " + progress);
            notificationManager.notify(downloadTaskData.getId(), getDownloadNotification(downloadTaskData, Algorithms.getSize(downloadTaskData.getTotalDownloadedSize()) + "/" + Algorithms.getSize(downloadTaskData.getTotalSize()), android.R.drawable.stat_sys_download, progress));

            if (downloadTaskListener != null)
                downloadTaskListener.onProgress(downloadTaskData, progress);

            lastProgressUpdateTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onComplete(DownloadTaskData taskData, boolean isSecondaryTask) {

        if (isSecondaryTask) {
            taskData.setSecondaryStatus(DownloadStates.STATE_COMPLETE);
            downloadTasksDatabase.updateSecondaryDownloadedSize(taskData.getId(), taskData.getSecondaryDownloadedSize());
            downloadTasksDatabase.updateSecondaryStatus(taskData.getId(), DownloadStates.STATE_COMPLETE);

            String outputFilePath = taskData.getPrimaryFilePath().substring(0, taskData.getPrimaryFilePath().lastIndexOf(File.separator));
            outputFilePath = outputFilePath.substring(0, outputFilePath.lastIndexOf(File.separator) + 1);
            outputFilePath += taskData.getName();

            new MediaMuxerTask(this, taskData, taskData.getPrimaryFilePath(), taskData.getSecondaryFilePath(), outputFilePath).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, "Merging...", android.R.drawable.stat_notify_sync, -1));
        } else {
            taskData.setPrimaryStatus(DownloadStates.STATE_COMPLETE);
            downloadTasksDatabase.updatePrimaryDownloadedSize(taskData.getId(), taskData.getPrimaryDownloadedSize());
            downloadTasksDatabase.updatePrimaryStatus(taskData.getId(), DownloadStates.STATE_COMPLETE);
            if (DownloadTask.isCompleted(taskData)) {
                Toast.makeText(this, "Download complete: " + taskData.getName(), Toast.LENGTH_SHORT).show();
                notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, "Download Complete", R.drawable.ic_done, 100));
                DownloadTasks.list.remove(taskData);
                DownloadTasks.downloadedStartIndex--;
                DownloadTasks.list.add(DownloadTasks.downloadedStartIndex, taskData);
                DownloadTasks.downloadedStartIndex++;
                if (downloadTaskListener != null)
                    downloadTaskListener.onComplete(taskData, false);
            }
        }
    }

    @Override
    public void onError(DownloadTaskData taskData, String error, boolean isSecondaryTask) {
        if (isSecondaryTask) {
//            Log.d("DownloadService", "onPause secondary " + taskData.getSecondaryDownloadedSize());
            taskData.setSecondaryStatus(DownloadStates.STATE_ERROR);
            downloadTasksDatabase.updateSecondaryStatus(taskData.getId(), DownloadStates.STATE_ERROR);
            downloadTasksDatabase.updateSecondaryDownloadedSize(taskData.getId(), taskData.getSecondaryDownloadedSize());
        } else {
//            Log.d("DownloadService", "onPause primary " + taskData.getPrimaryDownloadedSize());
            taskData.setPrimaryStatus(DownloadStates.STATE_ERROR);
            downloadTasksDatabase.updatePrimaryStatus(taskData.getId(), DownloadStates.STATE_ERROR);
            downloadTasksDatabase.updatePrimaryDownloadedSize(taskData.getId(), taskData.getPrimaryDownloadedSize());
        }

        notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, "Error: link expired", android.R.drawable.stat_notify_error, -1));

        if (downloadTaskListener != null)
            downloadTaskListener.onError(taskData, error, isSecondaryTask);
    }

    /****************************** MUXER LISTENERS ***********************************************/

    @Override
    public void onMuxStart(DownloadTaskData taskData) {
        notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, "Merging...", R.drawable.ic_media_merg, 0));
        if (mediaMuxerTaskListener != null)
            mediaMuxerTaskListener.onMuxStart(taskData);
    }

    @Override
    public void onMuxProgress(DownloadTaskData taskData, int progress) {
        notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, "Merging...", R.drawable.ic_media_merg, progress));
        if (mediaMuxerTaskListener != null)
            mediaMuxerTaskListener.onMuxProgress(taskData, progress);
    }

    @Override
    public void onMuxComplete(DownloadTaskData taskData) {
        if (mediaMuxerTaskListener != null)
            mediaMuxerTaskListener.onMuxComplete(taskData);

        Toast.makeText(this, "Download complete: " + taskData.getName(), Toast.LENGTH_SHORT).show();

        File primaryFile = new File(taskData.getPrimaryFilePath());
        File secondaryFile = new File(taskData.getSecondaryFilePath());
        File folder = new File(taskData.getPrimaryFilePath().substring(0, taskData.getPrimaryFilePath().lastIndexOf("/")));
        File noMediaFile = new File(folder.getAbsolutePath() + File.separator + ".nomedia");

        noMediaFile.delete();
        primaryFile.delete();
        secondaryFile.delete();
        folder.delete();

        String outputFilePath = taskData.getPrimaryFilePath().substring(0, taskData.getPrimaryFilePath().lastIndexOf(File.separator));
        outputFilePath = outputFilePath.substring(0, outputFilePath.lastIndexOf(File.separator) + 1);
        outputFilePath += taskData.getName();
        taskData.setPrimaryFilePath(outputFilePath);
        downloadTasksDatabase.updatePrimaryFilePath(taskData.getId(), outputFilePath);

        notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, "Completed", R.drawable.ic_done, -1));

        DownloadTasks.list.remove(taskData);
        DownloadTasks.downloadedStartIndex--;
        DownloadTasks.list.add(DownloadTasks.downloadedStartIndex, taskData);
        DownloadTasks.downloadedStartIndex++;

    }

    @Override
    public void onMuxFailed(DownloadTaskData taskData, String error) {
        notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, error, android.R.drawable.stat_notify_error, -1));
        if (mediaMuxerTaskListener != null)
            mediaMuxerTaskListener.onMuxFailed(taskData, error);
    }

    /************************************ DOWNLOAD CONTROLS ***************************************/

    public void pauseTask(DownloadTaskData taskData) {
        downloadTask.pauseDownload(taskData);
    }

    public void resumeTask(DownloadTaskData taskData) {
        if (!Algorithms.isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        File primaryFile = new File(taskData.getPrimaryFilePath());
        File secondaryFile = (DownloadTask.hasSecondaryTask(taskData) && taskData.getSecondaryDownloadedSize() > 0) ? new File(taskData.getSecondaryFilePath()) : null;
        if (primaryFile.exists() && (secondaryFile == null || secondaryFile.exists())) {
            notifications.put(taskData.getId(), taskData);
            downloadTask.startDownload(taskData);
        } else {
            notificationManager.notify(taskData.getId(), getDownloadNotification(taskData, "File not found", android.R.drawable.stat_notify_error, -1));
            if (downloadTaskListener != null) {
                downloadTaskListener.onError(taskData, "File not found", false);
                downloadTaskListener.restart(taskData);
            }
        }
    }
    /**********************************************************************************************/


    @Override
    public void onDestroy() {
        super.onDestroy();
        RuntimeData.isDownloadServiceRunning = false;
        if (RuntimeData.isMainActivityRunning)
            downloadTasksDatabase.close();
    }

    public class DownloadServiceBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

}