package com.ocean.smdownloader.YTMediaHandler;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.ocean.smdownloader.Algorithms;
import com.ocean.smdownloader.Download.DownloadService;
import com.ocean.smdownloader.Download.DownloadStates;
import com.ocean.smdownloader.Download.DownloadTaskData;
import com.ocean.smdownloader.Download.DownloadTasks;
import com.ocean.smdownloader.Download.DownloadTasksDatabase;
import com.ocean.smdownloader.Download.MediaFileHandler;
import com.ocean.smdownloader.Download.MediaFileHandlerListener;
import com.ocean.smdownloader.Internet.BitmapLoadListener;
import com.ocean.smdownloader.Internet.ContentLoadListener;
import com.ocean.smdownloader.Internet.LoadBitmapFromLink;
import com.ocean.smdownloader.Internet.LoadContentFromLink;
import com.ocean.smdownloader.Internet.LoadSize;
import com.ocean.smdownloader.Internet.SizeLoadListener;
import com.ocean.smdownloader.MediaQualityViewerActivity;
import com.ocean.smdownloader.MySharedPreferences;
import com.ocean.smdownloader.R;
import com.ocean.smdownloader.RequestManger;
import com.ocean.smdownloader.RuntimeData;
import com.ocean.smdownloader.Sources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class YTMediaQualityViewer implements ContentLoadListener, BitmapLoadListener, SizeLoadListener, MediaFileHandlerListener, YTMediaQualityRecyclerviewActionListener {

    private final Context context;
    private final MediaQualityViewerActivity activity;
    private final String link;

    private LinearLayout qualityViewerSheet;
    private BottomSheetBehavior bottomSheetBehavior;
    private ImageView thumbnailImageView;
    private EditText mediaTitleEditText;
    private WebView webView;
    private ProgressBar progressBar;
    private View qualityView;
    private RecyclerView audioQualityRecyclerView, videoQualityRecyclerView;

    private String name;
    private String baseJsLink;
    private String primaryOutFilePath, secondaryOutFilePath, thumbnailPath;
    private Bitmap thumbnail;
    private ArrayList<YTMediaQualityData> audioQualityData, videoQualityData;
    private YTMediaQualityData pendingDownload;

    public RequestManger requestManger;
    MediaFileHandler mediaFileHandler;
    MySharedPreferences mySharedPreferences;
    private DownloadTasksDatabase downloadTasksDatabase;
    private LoadContentFromLink loadContentTask;
    private LoadBitmapFromLink loadThumbnailTask;
    private int zeroSizes;
    int webmAudioIndex;

    boolean isMediaLoadingStarted = false;

    @SuppressLint("SetJavaScriptEnabled")
    public YTMediaQualityViewer(Context context, MediaQualityViewerActivity activity, String link, View ytMediaQualityView) {
        this.context = context;
        this.activity = activity;
        this.link = link;

        qualityViewerSheet = ytMediaQualityView.findViewById(R.id.ytMediaQualityViewerSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(qualityViewerSheet);
        thumbnailImageView = ytMediaQualityView.findViewById(R.id.ytMediaThumbnailView);
        mediaTitleEditText = ytMediaQualityView.findViewById(R.id.ytMediaTitleView);
        webView = ytMediaQualityView.findViewById(R.id.ytWebView);
        progressBar = ytMediaQualityView.findViewById(R.id.ytMediaQualityProgressBar);
        qualityView = ytMediaQualityView.findViewById(R.id.ytQualityView);
        audioQualityRecyclerView = qualityView.findViewById(R.id.ytMediaAudioQualityRecyclerView);
        videoQualityRecyclerView = qualityView.findViewById(R.id.ytMediaVideoQualityRecyclerView);

        audioQualityData = new ArrayList<>();
        videoQualityData = new ArrayList<>();

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        webView.getSettings().setJavaScriptEnabled(true);

        requestManger = new RequestManger(activity);
        mediaFileHandler = new MediaFileHandler(context, this);
        mySharedPreferences = new MySharedPreferences(context);
        downloadTasksDatabase = new DownloadTasksDatabase(context);

        setupListeners();

        setupViews(false);

        loadContentTask = new LoadContentFromLink(this, null);
        loadThumbnailTask = new LoadBitmapFromLink(this, null);

        loadContentTask.execute(link);
        loadThumbnailTask.execute(YTMediaRetrieverAlgorithms.getThumbnailLink(link));
    }

    private void setupListeners() {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("LoadQuality", url);
                if (!isMediaLoadingStarted /*&& url.startsWith("https://youtube.com")*/) {
                    isMediaLoadingStarted = true;
                    webView.stopLoading();
                    loadMediaData();
                }
            }
        });
    }

    @Override
    public void onItemClick(YTMediaQualityData data) {
        if (permissionsGranted())
            startDownloadService(data);
        else
            pendingDownload = data;
    }

    private boolean permissionsGranted() {
        if (!requestManger.checkStoragePermission()) {
            requestManger.requestStoragePermission();

            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return requestManger.requestManageExternalStorage();
        }

        return true;
    }

    private void startDownloadService(YTMediaQualityData data) {
        //Log.d("DownloadStart", data.getQuality() +", " +data.getType()+", "+data.getSecondaryUrl());
        name = MediaFileHandler.getSupportedName(mediaTitleEditText.getText().toString());

        if (thumbnail != null)
            thumbnailPath = saveThumbnail();

        if (primaryOutFilePath == null)
            primaryOutFilePath = mediaFileHandler.getPathForFile(name, data.getType(), null, data.getSecondaryUrl() != null, data.getPrimarySize(), data.getSecondarySize());

        if (primaryOutFilePath == null) {
            pendingDownload = data;
            return;
        }

        if (data.getSecondaryUrl() != null) {
            name = primaryOutFilePath.substring(primaryOutFilePath.lastIndexOf("/") + 1, primaryOutFilePath.lastIndexOf("."));
            if (secondaryOutFilePath == null)
                secondaryOutFilePath = mediaFileHandler.getPathForFile(name, MediaFileHandler.TYPE_AUDIO, MediaFileHandler.getSecondaryDir(primaryOutFilePath), true, data.getSecondarySize());

            if (secondaryOutFilePath == null) {
                return;
            }
        }

        name = primaryOutFilePath.substring(primaryOutFilePath.lastIndexOf("/") + 1);

        DownloadTaskData taskData = new DownloadTaskData(mySharedPreferences.incrementDownloadCounts(),
                name,
                data.getType(),
                Sources.YOUTUBE,
                thumbnailPath,
                data.getPrimaryUrl(),
                data.getSecondaryUrl(),
                primaryOutFilePath,
                secondaryOutFilePath,
                0,
                0,
                data.getPrimarySize(),
                data.getSecondarySize(),
                DownloadStates.STATE_PAUSE,
                secondaryOutFilePath != null ? DownloadStates.STATE_PAUSE : DownloadStates.STATE_NOT_AVAILABLE);

        if (data.getPrimaryUrl().startsWith("http")) {
            Intent downloadService = new Intent(context, DownloadService.class);
            if (!RuntimeData.isDownloadServiceRunning)
                context.startService(downloadService);

            context.bindService(downloadService, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    ((DownloadService.DownloadServiceBinder) iBinder).getService().addDownload(taskData);
                    context.unbindService(this);

                    activity.finish();
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            }, Context.BIND_NOT_FOREGROUND);
        } else {
            Intent ytMediaDownloadHandlerService = new Intent(context, YTMediaDownloadHandlerService.class);
            context.startService(ytMediaDownloadHandlerService);

            context.bindService(ytMediaDownloadHandlerService, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    YTMediaDownloadHandlerService service = ((YTMediaDownloadHandlerService.ServiceBinder) iBinder).getService();
                    service.setWebView(webView);
                    service.loadLinkAndDownload(baseJsLink, taskData);
                    context.unbindService(this);
                    activity.finish();
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            }, Context.BIND_NOT_FOREGROUND);

        }
    }

    /****************************** FILE HANDLER **************************************************/
    @Override
    public void onFileCreated(String primaryFilePath, String secondaryFilePath, boolean isOverwrite) {
        if (isOverwrite) {
            DownloadTasks.removeByName(primaryFilePath.substring(primaryFilePath.lastIndexOf("/") + 1));
            downloadTasksDatabase.deleteData(primaryFilePath.substring(primaryFilePath.lastIndexOf("/") + 1));
        }

        this.primaryOutFilePath = primaryFilePath;
        this.secondaryOutFilePath = secondaryFilePath;

        startPendingDownload();
    }

    @Override
    public void onCanceled() {
        activity.finish();
    }

    @Override
    public void onError(String fileName, String error) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
        activity.finish();
    }

    /**********************************************************************************************/

    public void startPendingDownload() {
        if (pendingDownload != null)
            startDownloadService(pendingDownload);
    }

    private void loadMediaData() {
        webView.evaluateJavascript(YTMediaRetrieverAlgorithms.getTitle, title -> {
            name = title.substring(1, title.length() - 1);
            name = MediaFileHandler.getSupportedName(name);
            mediaTitleEditText.setText(name);
        });
        webView.evaluateJavascript(YTMediaRetrieverAlgorithms.getYtInitialResponse, this::loadMediaQuality);
        webView.evaluateJavascript(YTMediaRetrieverAlgorithms.getBaseJsUrl, result ->
                baseJsLink = "https://youtube.com" + result.replace("\"", ""));

    }

    private void loadMediaQuality(String jsonString) {
        Log.d("LoadQuality", "Loading.. : " + jsonString);
        jsonString = (jsonString.substring(1, jsonString.length() - 1)).replace("\\\"", "\"").replace("\\\\\"", "\\\"");
        ArrayList<String> audioQualities = new ArrayList<>();
        ArrayList<String> videoQualities = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(jsonString);
            JSONArray adaptiveFormats = jsonObj.getJSONArray("adaptiveFormats");
            for (int i = 0; i < adaptiveFormats.length(); i++) {
                JSONObject obj = adaptiveFormats.getJSONObject(i);

                String mimeType = obj.getString("mimeType");
                if (!mimeType.contains("mp4a") && !mimeType.contains("avc1"))
                    continue;

                long size = 0;
                if (obj.has("contentLength"))
                    size = Long.parseLong(obj.getString("contentLength"));

                String quality;
                String url;

                if (obj.has("url")) {
                    url = URLDecoder.decode(obj.getString("url"), StandardCharsets.UTF_8.name());
                } else {
                    url = URLDecoder.decode(obj.getString("signatureCipher"), StandardCharsets.UTF_8.name());
                }

                if (obj.has("qualityLabel")) {
                    quality = obj.getString("qualityLabel");
                    if (!videoQualities.contains(quality)) {
                        videoQualityData.add(new YTMediaQualityData(MediaFileHandler.TYPE_VIDEO, mimeType, quality, url, size));
                        videoQualities.add(quality);
                        if (size == 0) {
                            zeroSizes++;
                            new LoadSize(this, videoQualityData.get(videoQualityData.size() - 1)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, videoQualityData.get(videoQualityData.size() - 1).getPrimaryUrl());
                        }
                    }
                } else {
                    quality = obj.getString("bitrate");
                    if (!audioQualities.contains(quality)) {
                        audioQualityData.add(new YTMediaQualityData(MediaFileHandler.TYPE_AUDIO, mimeType, Algorithms.getAudioQuality(Long.parseLong(quality)), url, size));
                        webmAudioIndex++;
                        if (size == 0) {
                            zeroSizes++;
                            new LoadSize(this, audioQualityData.get(audioQualityData.size() - 1)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, audioQualityData.get(audioQualityData.size() - 1).getPrimaryUrl());
                        }
                        audioQualities.add(quality);
                    }
                }
            }

            if (zeroSizes <= 0) {
                addQualities();
                setupViews(true);
            }

        } catch (JSONException | UnsupportedEncodingException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            cancelAllTask();
            activity.finish();
        }
    }

    private void addQualities() {
        for (YTMediaQualityData data : videoQualityData) {
            data.setSecondaryUrl(audioQualityData.get(0).getPrimaryUrl());
            data.setSecondarySize(audioQualityData.get(0).getPrimarySize());
        }

        YTMediaQualityAdapter audioQualityAdapter = new YTMediaQualityAdapter(context, this, audioQualityData);
        audioQualityRecyclerView.setAdapter(audioQualityAdapter);

        YTMediaQualityAdapter videoQualityAdapter = new YTMediaQualityAdapter(context, this, videoQualityData);
        videoQualityRecyclerView.setAdapter(videoQualityAdapter);
    }

    private String saveThumbnail() {
        File thumbnailFolder = new File(context.getExternalCacheDir() + "/.thumb");
        String thumbnailFilePath;

        if (thumbnailFolder.exists() || thumbnailFolder.mkdirs()) {
            thumbnailFilePath = thumbnailFolder + File.separator + name + MediaFileHandler.getExtension(MediaFileHandler.TYPE_IMAGE);

            try {
                FileOutputStream outputStream = new FileOutputStream(thumbnailFilePath);
                thumbnail.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                return thumbnailFilePath;

            } catch (FileNotFoundException e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
        } else {
            Toast.makeText(context, "Failed to save thumbnail", Toast.LENGTH_SHORT).show();
        }

        return null;
    }


    private void setupViews(boolean isQualitiesLoaded) {
        mediaTitleEditText.setVisibility(isQualitiesLoaded ? View.VISIBLE : View.GONE);
        thumbnailImageView.setVisibility(isQualitiesLoaded ? View.VISIBLE : View.GONE);
        qualityView.setVisibility(isQualitiesLoaded ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(isQualitiesLoaded ? View.GONE : View.VISIBLE);
        if (isQualitiesLoaded)
            mediaTitleEditText.requestFocus();
    }

    @Override
    public void onComplete(String result, DownloadTaskData taskData) {
        webView.loadDataWithBaseURL(link, result, "text/html", "UTF-8", link);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Object object) {
        thumbnail = bitmap;
        thumbnailImageView.setImageBitmap(bitmap);
    }

    @Override
    public void onFailed(String error, DownloadTaskData taskData) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
        activity.finish();
    }

    @Override
    public void onLoad(long size, Object object) {
        Log.d("LoadQuality", "Loaded " + size);
        ((YTMediaQualityData)object).setPrimarySize(size);
        zeroSizes--;
        if (zeroSizes <= 0) {
            addQualities();
            setupViews(true);
        }
    }

    @Override
    public void onFailed(String error, Object object) {

    }

    public void cancelAllTask() {
        if (loadContentTask != null && loadContentTask.getStatus() == AsyncTask.Status.RUNNING)
            loadContentTask.cancel(true);

        if (loadThumbnailTask != null && loadThumbnailTask.getStatus() == AsyncTask.Status.RUNNING)
            loadThumbnailTask.cancel(true);
    }

}