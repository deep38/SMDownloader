package com.ocean.smdownloader;

import android.content.Context;
import android.icu.text.DecimalFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Algorithms {

    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static final String[] sizeIndicator = new String[]{"Bytes","KB","MB","GB","TB"};

    public static String getSize(float size){
        int time = 0;

        while(size > 1024){
            size/=1024.00;
            time++;
        }
        return df.format(size)+" "+sizeIndicator[time];
    }

    public static String getAudioQuality(long bitrate) {
        return (int) Math.ceil(bitrate / 1000.0f) + " Kbps";
    }

    public static int getProgressPercentage(long loaded, long total) {
        return (int) (((float) loaded / total) * 100);
    }

    public static String getOutOfSizeText(long downloaded, long total) {
        return Algorithms.getSize(downloaded) + "/" + Algorithms.getSize(total);
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
