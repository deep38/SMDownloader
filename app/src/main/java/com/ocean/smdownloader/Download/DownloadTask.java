package com.ocean.smdownloader.Download;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ocean.smdownloader.Algorithms;
import com.ocean.smdownloader.MySharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class DownloadTask implements DownloadThreadListener {

    private final Context context;
    private final DownloadTaskListener listener;
    private final MySharedPreferences sharedPreferences;
    private final HashMap<Integer, ArrayList<ThreadData>> downloadTaskThreads;
    private final HashMap<Integer, Integer> runningThreads;
    private final ArrayList<Integer> failedTasks;
    private final ArrayList<DownloadTaskData> pausedTasks;
    private final ArrayList<DownloadTaskData> runningTasks;
    private final int totalThreads;

    private long lastProgressUpdateTime;
    private boolean stopAllTask;

    public DownloadTask(Context context, DownloadTaskListener listener) {
        this.context = context;
        this.listener = listener;

        sharedPreferences = new MySharedPreferences(context);
        downloadTaskThreads = new HashMap<>();
        runningThreads = new HashMap<>();
        failedTasks = new ArrayList<>();
        pausedTasks = new ArrayList<>();
        runningTasks = new ArrayList<>();

        totalThreads = sharedPreferences.getTotalThreads();
    }

    public void startDownload(DownloadTaskData taskData) {
//        downloadTaskNewList.put(taskData.getId(), new DownloadTaskNew(context, listener, taskData));
//        downloadTaskNewList.get(taskData.getId()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if (!runningTasks.contains(taskData)) runningTasks.add(taskData);
        if (!pausedTasks.contains(taskData)) pausedTasks.remove(taskData);
        if (!failedTasks.contains(taskData.getId())) failedTasks.remove((Object) taskData.getId());

        int startedThreads = 0;
        for (int threadNo = 0; threadNo < totalThreads; threadNo++) {
            if (!downloadTaskThreads.containsKey(taskData.getId()))
                downloadTaskThreads.put(taskData.getId(), new ArrayList<>());
            if (!runningThreads.containsKey(taskData.getId()))
                runningThreads.put(taskData.getId(), 0);

            ThreadData threadData = getThreadData(threadNo, taskData);
            downloadTaskThreads.get(taskData.getId()).add(threadNo, threadData);
            if (threadData.getThread() != null) {
                //Objects.requireNonNull(downloadTaskThreads.get(taskData.getId())).set(threadNo, threadData);
                startedThreads++;
                downloadTaskThreads.get(taskData.getId()).get(threadNo).getThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                runningThreads.put(taskData.getId(), runningThreads.get(taskData.getId()) + 1);
            }

    }
        if (startedThreads > 0) {
            listener.onStart(taskData);
        } else {
            if (!isPrimaryTaskCompleted(taskData)) {
                sharedPreferences.deleteStartingPoints(taskData.getId());
                taskData.setPrimaryStatus(DownloadStates.STATE_COMPLETE);
                downloadTaskThreads.remove(taskData.getId());
                runningThreads.remove(taskData.getId());
                listener.onComplete(taskData, false);
                startDownload(taskData);
            } else if (hasSecondaryTask(taskData) && !isSecondaryTaskCompleted(taskData)){
                sharedPreferences.deleteStartingPoints(taskData.getId());
                taskData.setPrimaryStatus(DownloadStates.STATE_COMPLETE);
                downloadTaskThreads.remove(taskData.getId());
                runningThreads.remove(taskData.getId());
                listener.onComplete(taskData, true);
            }
        }
    }

    private ThreadData getThreadData(int threadNo, DownloadTaskData taskData) {
        long taskSize = (isPrimaryTaskCompleted(taskData) && hasSecondaryTask(taskData)) ? taskData.getSecondaryFileSize() : taskData.getPrimaryFileSize();
        long threadSize = Math.round(Math.floor(taskSize / (totalThreads * 1.0f)));
        Log.d("DownloadTask", threadNo+ ". " + threadSize);
        long start = sharedPreferences.getThreadStartingPoint(taskData.getId(), threadNo, getDefaultStartingPoint(threadNo, threadSize));
        long downloaded = getDownloadedSize(threadNo, threadSize, start);
        long end = getEndPoint(threadNo, threadSize, taskSize);

        if (start < end)
            return new ThreadData(new DownloadThread(this, threadNo, taskData, start, end, downloaded), downloaded, Algorithms.getProgressPercentage(downloaded, taskData.getTotalSize()));
        else
            return new ThreadData(null, downloaded, Algorithms.getProgressPercentage(downloaded, taskData.getTotalSize()));
    }

    public void pauseDownload(DownloadTaskData taskData) {
        if (downloadTaskThreads.size() <= 0 && stopAllTask) {
            listener.onZeroTaskRemain(true);
            return;
        }

        if (pausedTasks.contains(taskData)) pausedTasks.add(taskData);
        runningTasks.remove(taskData);

        if (downloadTaskThreads.containsKey(taskData.getId())) {

            boolean isSecondaryTask = isPrimaryTaskCompleted(taskData) && hasSecondaryTask(taskData);
            int threadNo = 0;
            long totalDownloaded = 0;
            for (ThreadData threadData : downloadTaskThreads.get(taskData.getId())) {
                if (threadData.getThread() != null) {
                    threadData.getThread().isCancelled = true;
                    stopThread(threadNo, taskData, threadData.getThread().getDownloaded(), isSecondaryTask);
                } else {
                    stopThread(threadNo, taskData, threadData.getDownloaded(), isSecondaryTask);
                }
                totalDownloaded += threadData.getDownloaded();
                threadNo++;
            }
            if (isSecondaryTask)
                taskData.setSecondaryDownloadedSize(totalDownloaded);
            else
                taskData.setPrimaryDownloadedSize(totalDownloaded);

            downloadTaskThreads.remove(taskData.getId());

            listener.onPause(taskData, isSecondaryTask);

            if (downloadTaskThreads.size() <= 0 && stopAllTask) {
                listener.onZeroTaskRemain(true);
            }
        }
    }

    public void pauseAllDownloads() {
        stopAllTask = true;
        if (runningTasks != null && runningTasks.size() > 0) {
            for (DownloadTaskData taskData : runningTasks) {
                pauseDownload(taskData);
            }
        } else {
            listener.onZeroTaskRemain(true);
        }
    }

    private long getDownloadedSize(int threadNo, long threadSize, long start) {
        return start - getDefaultStartingPoint(threadNo, threadSize);
    }

    private long getDefaultStartingPoint(int threadNo, long threadSize) {
        return threadNo * threadSize;
    }

    private long getEndPoint(int threadNo, long threadSize, long size) {
        if (threadNo == totalThreads - 1)
            return size;
        else
            return getDefaultStartingPoint(threadNo, threadSize) + threadSize;
    }

    private int stopThread(int threadNo, DownloadTaskData taskData, long downloaded, boolean isSecondaryTask) {
        long taskSize = isSecondaryTask ? taskData.getSecondaryFileSize() : taskData.getPrimaryFileSize();
        long defaultStartingPoint = getDefaultStartingPoint(threadNo, Math.round(Math.floor(taskSize / (totalThreads * 1.0f))));
        sharedPreferences.setThreadStartingPoint(taskData.getId(), threadNo, downloaded + defaultStartingPoint);

        ArrayList<ThreadData> threads = downloadTaskThreads.get(taskData.getId());
        if (threads != null && threads.size() > threadNo) {
            threads.get(threadNo).setDownloaded(downloaded);
            runningThreads.put(taskData.getId(), runningThreads.get(taskData.getId()) - 1);
        }

        return runningThreads.get(taskData.getId());
    }

    @Override
    public void onProgress(DownloadTaskData taskData, int threadNo, int progress, long downloaded, boolean isSecondaryTask) {
            long totalDownloaded = 0;
            int totalProgress = isSecondaryTask ? Algorithms.getProgressPercentage(taskData.getPrimaryDownloadedSize(), taskData.getTotalSize()) : 0;

            if (downloadTaskThreads.containsKey(taskData.getId())) {
                Objects.requireNonNull(downloadTaskThreads.get(taskData.getId())).get(threadNo).setProgress(progress);
                Objects.requireNonNull(downloadTaskThreads.get(taskData.getId())).get(threadNo).setDownloaded(downloaded);
                for (ThreadData threadData : Objects.requireNonNull(downloadTaskThreads.get(taskData.getId()))) {
                    if (threadData != null) {
                        totalDownloaded += threadData.getDownloaded();
                        totalProgress += threadData.getProgress();
                    }
                }

            }

            if(isSecondaryTask)
                taskData.setSecondaryDownloadedSize(totalDownloaded);
            else
                taskData.setPrimaryDownloadedSize(totalDownloaded);

            //Log.d("DownloadTask", "onProgress " + threadNo + Algorithms.getSize(downloaded));
            //if (System.currentTimeMillis() - lastProgressUpdateTime >= 1000) {
                //Log.d("DownloadTask", "calling service onProgress " + threadNo + Algorithms.getSize(downloaded));
                listener.onProgress(taskData, Algorithms.getProgressPercentage(totalDownloaded, isSecondaryTask ? taskData.getSecondaryFileSize() : taskData.getPrimaryFileSize()));
                lastProgressUpdateTime = System.currentTimeMillis();
            //}
    }

    @Override
    public void onPause(DownloadTaskData taskData, int threadNo, long downloaded, boolean isSecondaryTask) {

    }

    @Override
    public void onComplete(DownloadTaskData taskData, int threadNo, long downloaded, boolean isSecondaryTask) {

        long totalDownloaded = 0;
        if (downloadTaskThreads.containsKey(taskData.getId())) {
            Objects.requireNonNull(downloadTaskThreads.get(taskData.getId())).get(threadNo).setDownloaded(downloaded);
            for (ThreadData threadData : Objects.requireNonNull(downloadTaskThreads.get(taskData.getId()))) {
                totalDownloaded += threadData.getDownloaded();
            }
        }

        if (isSecondaryTask)
            taskData.setSecondaryDownloadedSize(totalDownloaded);
        else
            taskData.setPrimaryDownloadedSize(totalDownloaded);

        if (runningTasks.contains(taskData) && runningThreads.containsKey(taskData.getId()) && stopThread(threadNo, taskData, downloaded, isSecondaryTask) <= 0) {
            downloadTaskThreads.remove(taskData.getId());

            if (!isSecondaryTask) {
                if (taskData.getPrimaryDownloadedSize() >= taskData.getPrimaryFileSize()) {
                    sharedPreferences.deleteStartingPoints(taskData.getId());
                    taskData.setPrimaryStatus(DownloadStates.STATE_COMPLETE);
                    listener.onComplete(taskData, false);

                    if (hasSecondaryTask(taskData)) {
                        startDownload(taskData);
                    } else {
                        runningTasks.remove(taskData);
                        if (pausedTasks.isEmpty() && runningTasks.isEmpty())
                            listener.onZeroTaskRemain(false);
                    }
                } else {
                    taskData.setPrimaryStatus(DownloadStates.STATE_PAUSE);
                    listener.onPause(taskData, false);
                }
            } else {
                if (taskData.getSecondaryDownloadedSize() >= taskData.getSecondaryFileSize()) {
                    sharedPreferences.deleteStartingPoints(taskData.getId());
                    taskData.setSecondaryStatus(DownloadStates.STATE_COMPLETE);
                    listener.onComplete(taskData, true);
                    runningTasks.remove(taskData);

                } else {
                    taskData.setSecondaryStatus(DownloadStates.STATE_PAUSE);
                    listener.onPause(taskData, true);
                }
            }
        }

    }

    @Override
    public void onError(DownloadTaskData taskData, int threadNo, String error, long downloaded, boolean isSecondaryTask) {
        if (runningThreads.containsKey(taskData.getId()) && downloadTaskThreads.containsKey(taskData.getId())) {

            if (error.startsWith(DownloadThread.ERROR_FILE_NOT_FOUND)) {
                for (ThreadData threadData : downloadTaskThreads.get(taskData.getId())) {
                    if (threadData.getThread() != null)
                        threadData.getThread().isCancelled = true;
                }
                if (!failedTasks.contains(taskData.getId())) {
                    failedTasks.add(taskData.getId());
                    listener.onError(taskData, error, isSecondaryTask);
                    downloadTaskThreads.remove(taskData.getId());
                }
            } else if (error.startsWith(DownloadThread.ERROR_UNABLE_RESOLVE_HOST)){
                if (stopThread(threadNo, taskData, downloaded, isSecondaryTask) <= 0) {
                    pausedTasks.add(taskData);
                    listener.onPause(taskData, isSecondaryTask);
                    downloadTaskThreads.remove(taskData.getId());
                }
            } else {
                stopThread(threadNo, taskData, downloaded, isSecondaryTask);
                if (!pausedTasks.contains(taskData)) {
                    ThreadData threadData = getThreadData(threadNo, taskData);
                    if (threadData.getThread() != null) {
                        downloadTaskThreads.get(taskData.getId()).set(threadNo, threadData);
                        downloadTaskThreads.get(taskData.getId()).get(threadNo).getThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        if (!runningThreads.containsKey(taskData.getId()))
                            runningThreads.put(taskData.getId(), 0);
                        runningThreads.put(taskData.getId(), runningThreads.get(taskData.getId()) + 1);
                    }
                }
            }
        }

    }

    public static int getStatus(DownloadTaskData taskData) {
        return isPrimaryTaskCompleted(taskData) && hasSecondaryTask(taskData) ? taskData.getSecondaryStatus() : taskData.getPrimaryStatus();
    }

    public static boolean isPrimaryTaskCompleted(DownloadTaskData taskData) {
        return taskData.getPrimaryStatus() == DownloadStates.STATE_COMPLETE;
    }

    public static boolean hasSecondaryTask(DownloadTaskData taskData) {
        return taskData.getSecondaryStatus() != DownloadStates.STATE_NOT_AVAILABLE;
    }


    public static boolean isSecondaryTaskCompleted(DownloadTaskData taskData) {
        return taskData.getSecondaryStatus() == DownloadStates.STATE_COMPLETE;
    }

    public static boolean isCompleted(DownloadTaskData taskData) {
        return isPrimaryTaskCompleted(taskData) && (!hasSecondaryTask(taskData) || isSecondaryTaskCompleted(taskData));
    }

}
