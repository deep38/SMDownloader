package com.ocean.smdownloader.Download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.annotation.NonNull;

import com.ocean.smdownloader.RuntimeData;

public class DownloadServiceHelper {

    private final Context context;
    private final DownloadServiceListener downloadServiceListener;

    private Intent downloadServiceIntent;
    private ServiceConnection serviceConnection;
    private DownloadService downloadService;

    public DownloadServiceHelper(Context context, @NonNull DownloadServiceListener downloadServiceListener) {
        this.context = context;
        this.downloadServiceListener = downloadServiceListener;
    }

    private void startServiceIfNotRunning() {
        downloadServiceIntent = new Intent(context, DownloadService.class);

        if (!RuntimeData.isDownloadServiceRunning)
            context.startService(downloadServiceIntent);

    }

    public void addDownload(DownloadTaskData data) {
        startServiceIfNotRunning();
        if (downloadService == null) {
            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    downloadService = ((DownloadService.DownloadServiceBinder) iBinder).getService();
                    downloadService.setListeners(null, null);
                    downloadService.addDownload(data);
                    downloadServiceListener.onDownloadAdded();
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            };
            context.bindService(downloadServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            downloadService.addDownload(data);
            downloadServiceListener.onDownloadAdded();
        }
    }

    public void unbindService() {
        if (context != null && serviceConnection != null)
            context.unbindService(serviceConnection);
    }
}
