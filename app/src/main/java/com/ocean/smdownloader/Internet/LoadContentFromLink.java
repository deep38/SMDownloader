package com.ocean.smdownloader.Internet;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.ocean.smdownloader.Download.DownloadTaskData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class LoadContentFromLink extends AsyncTask<String, Integer, String> {

    private ContentLoadListener listener;
    private DownloadTaskData taskData;
    String tag = "LoadContentFromLink";
    public LoadContentFromLink(ContentLoadListener listener, DownloadTaskData taskData) {
        this.listener = listener;
        this.taskData = taskData;

        Log.d(tag, "Initialize");
    }

    @Override
    protected String doInBackground(String... strings) {

        StringBuffer result = new StringBuffer("");
        Log.d(tag, "DoInBackground");
        try {
            if (strings.length > 0) {
                String link = strings[0];
                URL url = new URL(link);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("User-agent", "Android " + Build.VERSION.SDK_INT);
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }

                bufferedReader.close();
                inputStream.close();
            } else {
                throw new Exception("Link not provided");
            }
        } catch (Exception e) {
            result.delete(0, result.length());
            result.append("Error: ").append(e.getMessage());
            Log.e("LoadError", "LoadError");
            e.printStackTrace();
        }
        return result.toString();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(tag, "onPostExecute: " + result.substring(0, 10));
        if (result.startsWith("Error"))
            listener.onFailed(result, taskData);
        else
            listener.onComplete(result, taskData);
    }
}
