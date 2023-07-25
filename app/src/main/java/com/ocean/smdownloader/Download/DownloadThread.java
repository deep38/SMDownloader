package com.ocean.smdownloader.Download;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.ocean.smdownloader.Algorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class DownloadThread extends AsyncTask<String, Integer, Integer> {

    public static final String ERROR_FILE_NOT_FOUND = "FileNotFound";
    public static final String ERROR_UNABLE_RESOLVE_HOST = "Unable to resolve host";

    private final DownloadThreadListener listener;
    private final int threadNo;
    private final DownloadTaskData taskData;
    private final long start;
    private final long end;

    private long downloaded;
    private boolean isDownloadingSecondary;
    private String link;
    private String outputFilePath;
    private String error;
    private int responseCode;
    public boolean isCancelled;

    public DownloadThread(DownloadThreadListener listener, int threadNo, DownloadTaskData taskData, long start, long end, long downloaded) {
        this.listener = listener;
        this.threadNo = threadNo;
        this.taskData = taskData;
        this.start = start;
        this.end = end;
        this.downloaded = downloaded;

        init();
    }

    private void init() {
        if (DownloadTask.isPrimaryTaskCompleted(taskData) && DownloadTask.hasSecondaryTask(taskData)) {
            isDownloadingSecondary = true;
            link = taskData.getSecondaryLink();
            outputFilePath = taskData.getSecondaryFilePath();
        } else {
            isDownloadingSecondary = false;
            link = taskData.getPrimaryLink();
            outputFilePath = taskData.getPrimaryFilePath();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Integer doInBackground(String... strings) {

        try {

            URL url = new URL(link);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Range", "bytes=" + start + "-" + end);
            InputStream inputStream = urlConnection.getInputStream();
            responseCode = urlConnection.getResponseCode();

            RandomAccessFile outputFile = new RandomAccessFile(outputFilePath, "rw");
            outputFile.seek(start);

            byte[] bytes = new byte[512];
            int data;

            while ((data = inputStream.read(bytes, 0, bytes.length)) != -1) {

                if (isCancelled || isCancelled()) {
                    inputStream.close();
                    outputFile.close();
                    urlConnection.disconnect();
                    return DownloadStates.STATE_PAUSE;
                }
                outputFile.write(bytes, 0, data);
                downloaded += data;
                publishProgress(Algorithms.getProgressPercentage(downloaded, taskData.getTotalSize()));
            }

            urlConnection.disconnect();
            inputStream.close();
            outputFile.close();

            return DownloadStates.STATE_COMPLETE;

        } catch (FileNotFoundException e) {
            error = ERROR_FILE_NOT_FOUND + " : " + e.getMessage();
            return DownloadStates.STATE_ERROR;
        } catch (IOException e) {
            error = e.getMessage();
            return DownloadStates.STATE_ERROR;
        }

    }

    public long getDownloaded() {
        return downloaded;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        listener.onProgress(taskData, threadNo, values[0], downloaded, isDownloadingSecondary);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        listener.onPause(taskData, threadNo, downloaded, isDownloadingSecondary);
    }

    @Override
    protected void onPostExecute(Integer state) {
        super.onPostExecute(state);

        if (state == DownloadStates.STATE_COMPLETE) {
            listener.onComplete(taskData, threadNo, downloaded, isDownloadingSecondary);
        } else if (isCancelled() || state == DownloadStates.STATE_PAUSE) {
            listener.onPause(taskData, threadNo, downloaded, isDownloadingSecondary);
        } else if (state == DownloadStates.STATE_ERROR) {
            listener.onError(taskData, threadNo, error, downloaded, isDownloadingSecondary);
        }
    }
}
