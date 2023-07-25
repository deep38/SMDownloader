package com.ocean.smdownloader.YTMediaHandler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.ocean.smdownloader.Download.DownloadServiceHelper;
import com.ocean.smdownloader.Download.DownloadServiceListener;
import com.ocean.smdownloader.Download.DownloadTaskData;
import com.ocean.smdownloader.Internet.ContentLoadListener;
import com.ocean.smdownloader.Internet.LoadContentFromLink;
import com.ocean.smdownloader.MainActivity;
import com.ocean.smdownloader.MySharedPreferences;
import com.ocean.smdownloader.R;
import com.ocean.smdownloader.RuntimeData;

public class YTMediaDownloadHandlerService extends Service implements ContentLoadListener, DownloadServiceListener {

    private int runningTasks = 0;

    private final String NOTIFICATION_CHANNEL_ID = "YT_DOWNLOAD_HANDLER";

    private final ServiceBinder binder = new ServiceBinder();
    private WebView webView;

    DownloadServiceHelper downloadServiceHelper;
    MySharedPreferences mySharedPreferences;
    NotificationManager notificationManager;

    class ServiceBinder extends Binder {
        public YTMediaDownloadHandlerService getService() {
            return YTMediaDownloadHandlerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mySharedPreferences = new MySharedPreferences(this);
        notificationManager = getSystemService(NotificationManager.class);

        int NOTIFICATION_ID = -2;
        startForeground(NOTIFICATION_ID, getNotification());
        RuntimeData.isYTDownloadHelperServiceRunning = true;

        return START_STICKY;
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(false)
                .setContentIntent(MainActivity.getPendingIntent(this))
                .setContentTitle("Link retriever service is running");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            createNotificationChannel();
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }

        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            String NOTIFICATION_CHANNEL_NAME = "YouTube Download Handler";
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private Notification getTaskNotification(int icon, String title, String contentText) {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(icon)
                .setOngoing(false)
                .setContentTitle(title)
                .setContentText(contentText)
                .setContentIntent(MainActivity.getPendingIntent(this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            createNotificationChannel();
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }

        return builder.build();
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public void loadLinkAndDownload(String baseJsLink, DownloadTaskData taskData) {
        if (taskData.getPrimaryLink() != null) {
            runningTasks++;
            new LoadContentFromLink(this, taskData).execute(baseJsLink);
            notificationManager.notify(taskData.getId(), getTaskNotification(android.R.drawable.stat_sys_download, taskData.getName(), "Retrieving link..."));
        } else {
            Toast.makeText(this, "YTMediaDownloadHandler: Null Link", Toast.LENGTH_SHORT).show();
        }
    }

    // BASE JS LOADING

    @Override
    public void onComplete(String result, DownloadTaskData taskData) {
        notificationManager.notify(taskData.getId(), getTaskNotification(android.R.drawable.stat_sys_download, taskData.getName(), "Link retrieved successfully"));
        try {
            String decipherFunction = YTMediaRetrieverAlgorithms.getScriptForDecipher(result);

            String[] primaryLinkCipherData = taskData.getPrimaryLink().split("&", 3);
            String primaryLinkCipherSig = primaryLinkCipherData[0].replaceFirst("s=", "");

            if (taskData.getSecondaryLink() != null) {
                String[] secondaryLinkCipherData = taskData.getSecondaryLink().split("&", 3);
                String secondaryLinkCipherSig = secondaryLinkCipherData[0].replaceFirst("s=", "");

                webView.evaluateJavascript(decipherFunction + "(\"" + secondaryLinkCipherSig + "\");", sig ->
                        taskData.setSecondaryLink(secondaryLinkCipherData[2].replaceFirst("url=", "") + "&sig=" + sig.substring(1, sig.length() - 1)));

            }

            webView.evaluateJavascript(decipherFunction + "(\"" + primaryLinkCipherSig + "\");", sig -> {
                taskData.setPrimaryLink(primaryLinkCipherData[2].replaceFirst("url=", "") + "&sig=" + sig.substring(1, sig.length() - 1));

                downloadServiceHelper = new DownloadServiceHelper(this, this);
                downloadServiceHelper.addDownload(taskData);

            });
        } catch (Exception e) {
            runningTasks--;
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            notificationManager.notify(taskData.getId(), getTaskNotification(android.R.drawable.stat_notify_error, taskData.getName(), e.getMessage()));
            if (runningTasks <= 0 ) {
                if(downloadServiceHelper != null)
                    downloadServiceHelper.unbindService();
                stopSelf();
                stopForeground(true);
            }
        }
    }

    @Override
    public void onDownloadAdded() {
        Log.d("Download", "Added");
        runningTasks--;
        if (runningTasks <= 0 ) {
            downloadServiceHelper.unbindService();
            stopSelf();
            stopForeground(true);
        }
    }

    @Override
    public void onFailed(String error, DownloadTaskData taskData) {
        runningTasks--;
        notificationManager.notify(taskData.getId(), getTaskNotification(android.R.drawable.stat_sys_download, taskData.getName(), error));

        if (runningTasks <= 0 ) {
            downloadServiceHelper.unbindService();
            stopSelf();
            stopForeground(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RuntimeData.isYTDownloadHelperServiceRunning = false;
    }
}
