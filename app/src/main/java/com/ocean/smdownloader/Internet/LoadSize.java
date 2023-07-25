package com.ocean.smdownloader.Internet;

import android.os.AsyncTask;
import android.util.Log;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class LoadSize extends AsyncTask<String, Integer, Long> {

    private SizeLoadListener listener;
    private Object object;

    private String error;

    public LoadSize(SizeLoadListener listener, Object object) {
        this.listener = listener;
        this.object = object;
    }

    @Override
    protected Long doInBackground(String... strings) {
        try {
            String link = strings[0];
            URL url = new URL(link);
            if (!isCancelled()) {
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

                if (!isCancelled())
                    return urlConnection.getContentLengthLong();
            }
        } catch (Exception e) {
            error = e.getMessage();
            Log.e("LoadSize", error);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Long size) {
        super.onPostExecute(size);
        if (size == null)
            listener.onFailed(error, object);
        else
            listener.onLoad(size, object);
    }
}
