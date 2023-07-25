package com.ocean.smdownloader.InstagramMediaHandler;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ocean.smdownloader.Download.DownloadServiceHelper;
import com.ocean.smdownloader.Download.DownloadServiceListener;
import com.ocean.smdownloader.Download.DownloadTaskData;
import com.ocean.smdownloader.Download.DownloadTasks;
import com.ocean.smdownloader.Download.DownloadTasksDatabase;
import com.ocean.smdownloader.Download.MediaFileHandler;
import com.ocean.smdownloader.Download.MediaFileHandlerListener;
import com.ocean.smdownloader.MediaQualityViewerActivity;
import com.ocean.smdownloader.MySharedPreferences;
import com.ocean.smdownloader.R;
import com.ocean.smdownloader.RequestManger;
import com.ocean.smdownloader.RuntimeData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class InstaQualityViewer implements InstaMediaRecyclerviewActionListener {
    private final String LOGIN_PAGE_LINK = "https://www.instagram.com";
    private final String GET_TEXT_OF_WEBPAGE = "(function() { return (document.getElementsByTagName('html')[0].innerText); })();";
    private final String INSTA_LINK_SUFFIX = "&__a=1&__d=dis";
    private final int TYPE_IMAGE = 1;
    private final int TYPE_VIDEO = 2;
    private final int TYPE_SIDECAR = 8;

    private final Context context;
    private final MediaQualityViewerActivity activity;
    private String link;

    private LinearLayout bottomSheetLayout;
    private TextView loginRequestTextView;
    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout mediaDetailLayout;
    private Button downloadAllHighestQualityButton, downloadAllMediumQualityButton, downloadAllLowestQualityButton;
    private RecyclerView mediaDetailsRecyclerView;
    private FloatingActionButton multiDownloadFab;
    private ArrayList<InstaMediaDetailData> list;
    InstaMediaDetailAdapter adapter;

    private MySharedPreferences sharedPreferences;
    public RequestManger requestManger;
    private BottomSheetBehavior bottomSheetBehavior;

    private ArrayList<InstaDownloadTaskData> multiDownloadDataList;
    private InstaDownloadTaskData pendingTask;

    public InstaQualityViewer(Context context, MediaQualityViewerActivity activity, View instaQualityView, String link) {
        this.context = context;
        this.activity = activity;
        this.link = link + INSTA_LINK_SUFFIX;

        list = new ArrayList<>();
        multiDownloadDataList = new ArrayList<>();

        bottomSheetLayout = instaQualityView.findViewById(R.id.insta_media_details_bottom_sheet);
        loginRequestTextView = instaQualityView.findViewById(R.id.insta_login_req_text_view);
        webView = instaQualityView.findViewById(R.id.insta_media_details_web_view);
        progressBar = instaQualityView.findViewById(R.id.insta_media_details_progress_bar);
        mediaDetailLayout = instaQualityView.findViewById(R.id.insta_media_details_layout);
        downloadAllHighestQualityButton = instaQualityView.findViewById(R.id.insta_media_download_all_highest_quality_button);
        downloadAllMediumQualityButton = instaQualityView.findViewById(R.id.insta_media_download_all_medium_quality_button);
        downloadAllLowestQualityButton = instaQualityView.findViewById(R.id.insta_media_download_all_lowest_quality_button);
        mediaDetailsRecyclerView = instaQualityView.findViewById(R.id.insta_media_details_recycler_view);
        multiDownloadFab = instaQualityView.findViewById(R.id.insta_media_multi_download_button);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        requestManger = new RequestManger(activity);
        sharedPreferences = new MySharedPreferences(context);

        setupViews(false);
        setupListeners();

        if (link != null) {
            if (sharedPreferences.getUserLoggedInToInsta()) {
                webView.loadUrl(this.link);
            } else {
                loginRequestTextView.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    webView.setForceDarkAllowed(true);
                    int nightModeFlag = context.getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
                    if (nightModeFlag == Configuration.UI_MODE_NIGHT_YES) {
                        webView.getSettings().setForceDark(WebSettings.FORCE_DARK_ON);
                    }
                }
                webView.loadUrl(LOGIN_PAGE_LINK);
            }
        } else {
            showMessage("Link not provided");
            activity.finish();
        }
    }

    private void setupViews(boolean isQualitiesLoaded) {
        if (sharedPreferences.getUserLoggedInToInsta()) {
            ViewGroup.LayoutParams layoutParams = webView.getLayoutParams();
            layoutParams.height = 0;
            layoutParams.width = 0;
            webView.setLayoutParams(layoutParams);
        }
        progressBar.setVisibility(isQualitiesLoaded || !sharedPreferences.getUserLoggedInToInsta() ? View.GONE : View.VISIBLE);
        mediaDetailLayout.setVisibility(isQualitiesLoaded ? View.VISIBLE : View.GONE);

        if (RuntimeData.isInstaQualityInMultiSelectMode)
            multiDownloadFab.show();
        else
            multiDownloadFab.hide();
    }

    private void setupListeners() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (url.startsWith("https://www.instagram.com/accounts/onetap/?next")) {
                    loginRequestTextView.setVisibility(View.GONE);
                    sharedPreferences.setUserLoggedInToInsta(true);
                    setupViews(false);
                    webView.loadUrl(link);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("InstaQualityViewer", url);
                if (url.endsWith(INSTA_LINK_SUFFIX)) {
                    webView.evaluateJavascript(GET_TEXT_OF_WEBPAGE, s -> {
                        String jsonString = s.replace("\\\"", "\"").replace("\\\\\"", "\\\"");
                        jsonString = jsonString.substring(1, jsonString.length() - 1);

                        loadMediaJson(jsonString);
                    });
                }
            }
        });

        downloadAllHighestQualityButton.setOnClickListener(view -> {
            ArrayList<InstaDownloadTaskData> multiDownloadData = new ArrayList<>();
            for (InstaMediaDetailData data : list) {
                multiDownloadData.add(new InstaDownloadTaskData(data, 0));
            }
            startDownload(multiDownloadData, null);
        });

        downloadAllMediumQualityButton.setOnClickListener(view -> {
            ArrayList<InstaDownloadTaskData> multiDownloadData = new ArrayList<>();
            for (InstaMediaDetailData data : list) {
                multiDownloadData.add(new InstaDownloadTaskData(data, (int) Math.round(Math.ceil(data.getQualities().size() / 2.0f))));
            }
            startDownload(multiDownloadData, null);
        });

        downloadAllLowestQualityButton.setOnClickListener(view -> {
            ArrayList<InstaDownloadTaskData> multiDownloadData = new ArrayList<>();
            for (InstaMediaDetailData data : list) {
                multiDownloadData.add(new InstaDownloadTaskData(data, data.getQualities().size() - 1));
            }
            startDownload(multiDownloadData, null);
        });

        multiDownloadFab.setOnClickListener(view -> {
            startDownload(multiDownloadDataList, null);
            RuntimeData.isInstaQualityInMultiSelectMode = false;
        });
    }

    private void showMediaDetails() {
        adapter = new InstaMediaDetailAdapter(context, this, list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mediaDetailsRecyclerView.setLayoutManager(layoutManager);
        mediaDetailsRecyclerView.setAdapter(adapter);
        setupViews(true);
    }

    private void startDownload(ArrayList<InstaDownloadTaskData> taskList, InstaDownloadTaskData taskData) {
        adapter.cancelAllTasks();

        if (permissionsGranted()) {
            Intent multiDownloadHandlerService = new Intent(context, InstaDownloadHelperService.class);
            if (!RuntimeData.isInstaDownloadHelperServiceRunning)
                context.startService(multiDownloadHandlerService);

            context.bindService(multiDownloadHandlerService, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    if (taskList != null)
                        ((InstaDownloadHelperService.InstaMultiDownloadBinder) iBinder).getService().addMultipleDownload(taskList);

                    if (taskData != null)
                        ((InstaDownloadHelperService.InstaMultiDownloadBinder) iBinder).getService().addDownload(taskData);

                    context.unbindService(this);
                    activity.finish();
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            }, Context.BIND_NOT_FOREGROUND);
        } else {
            pendingTask = taskData;
            multiDownloadDataList = taskList;
        }
    }

    public void startPendingDownload() {
        startDownload(multiDownloadDataList, pendingTask);
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

    @Override
    public void onQualityClick(int qualityPosition, InstaMediaDetailData data) {

        startDownload(null, new InstaDownloadTaskData(data, qualityPosition));

    }

    @Override
    public void onQualitySelected(InstaDownloadTaskData multiDownloadData) {
        multiDownloadDataList.add(multiDownloadData);
        if (multiDownloadDataList.size() == 1) {
            RuntimeData.isInstaQualityInMultiSelectMode = true;
            multiDownloadFab.show();
        }
    }

    @Override
    public void onQualityDeselect(InstaDownloadTaskData multiDownloadData) {
        multiDownloadDataList.remove(multiDownloadData);
        if (multiDownloadDataList.size() == 0) {
            multiDownloadFab.hide();
            RuntimeData.isInstaQualityInMultiSelectMode = false;
        }
    }

    private void loadImageQualities(JSONObject imageDetailsJsonObj, String userName) throws JSONException {
        if (userName == null)
            userName = imageDetailsJsonObj.getJSONObject("user").getString("full_name");
        String name = generateMediaName(userName);
        JSONArray imageQualitiesArray = imageDetailsJsonObj.getJSONObject("image_versions2").getJSONArray("candidates");

        ArrayList<Integer> imageWidths = new ArrayList<>();
        String thumbnailLink = "";
        int curThumbnailWidth = 0;
        ArrayList<InstaMediaQualityData> qualities = new ArrayList<>();
        for (int i = 0; i < imageQualitiesArray.length();  i++) {
            JSONObject qualityObject = imageQualitiesArray.getJSONObject(i);
            String link = qualityObject.getString("url");
            int width = qualityObject.getInt("width");

            if (!imageWidths.contains(width)) {
                if (curThumbnailWidth == 0 || curThumbnailWidth > width) {
                    curThumbnailWidth = width;
                    thumbnailLink = link;
                }
                imageWidths.add(width);

                InstaMediaQualityData instaMediaQualityData = new InstaMediaQualityData(qualities.size(), width + "p", 0, link);
                qualities.add(instaMediaQualityData);
            }
        }

        list.add(new InstaMediaDetailData(list.size(), name, MediaFileHandler.TYPE_IMAGE, thumbnailLink, qualities));
    }

    private void  loadVideoQualities(JSONObject videoDetailsJsonObj, String userName) throws JSONException{
        if (userName == null)
            userName = videoDetailsJsonObj.getJSONObject("user").getString("full_name");
        String name = generateMediaName(userName);
        JSONArray thumbnailQualities = videoDetailsJsonObj.getJSONObject("image_versions2").getJSONArray("candidates");

        String thumbnailLink = "";
        int curThumbnailWidth = 0;
        for (int i = 0; i < thumbnailQualities.length(); i++) {
            JSONObject thumbnailQuality = thumbnailQualities.getJSONObject(i);
            int width = thumbnailQuality.getInt("width");
            if (curThumbnailWidth == 0 || curThumbnailWidth > width) {
                curThumbnailWidth = width;
                thumbnailLink = thumbnailQuality.getString("url");
            }
        }

        JSONArray videoQualityArray = videoDetailsJsonObj.getJSONArray("video_versions");
        ArrayList<InstaMediaQualityData> qualities = new ArrayList<>();

        for (int i = 0; i < videoQualityArray.length(); i++) {
            JSONObject videoQuality = videoQualityArray.getJSONObject(i);
            String videoLink = videoQuality.getString("url");
            InstaMediaQualityData instaMediaQualityData = new InstaMediaQualityData(qualities.size(), videoQuality.getInt("width") + "x" + videoQuality.getInt("height"), 0, videoLink);
            qualities.add(instaMediaQualityData);
        }
        list.add(new InstaMediaDetailData(list.size(), name, MediaFileHandler.TYPE_VIDEO, thumbnailLink, qualities));
    }

    private void loadMediaQualities(JSONArray mediaDetails, String userName) throws JSONException{
            for (int i = 0; i < mediaDetails.length(); i++) {
                JSONObject mediaDetail = mediaDetails.getJSONObject(i);
                switch (mediaDetail.getInt("media_type")) {
                    case TYPE_IMAGE:
                        loadImageQualities(mediaDetail, userName);
                        break;
                    case TYPE_VIDEO:
                        loadVideoQualities(mediaDetail, userName);
                        break;
                    case TYPE_SIDECAR:
                        JSONArray sideCarMediaArray = mediaDetail.getJSONArray("carousel_media");
                        if (userName == null)
                            userName = mediaDetail.getJSONObject("user").getString("full_name");
                        loadMediaQualities(sideCarMediaArray, userName);
                        break;
                    default:
                        showMessage("TypeName not supported " + mediaDetail.getInt("media_type"));
                        break;
                }
            }
    }

    private void loadMediaJson(String mediaJson) {
        try {
            JSONObject jsonObject = new JSONObject(mediaJson);
            loadMediaQualities(jsonObject.getJSONArray("items"), null);
            showMediaDetails();
        } catch (JSONException e) {
            showMessage(e.getMessage());
            Log.e("InstaQualityViewer", e.toString());
            e.printStackTrace();
            if (("No value for items").equals(e.getMessage())) {
                sharedPreferences.setUserLoggedInToInsta(false);
            }
            activity.finish();
        }
    }

    private String generateMediaName(String userName) {
        DecimalFormat decimalFormat = new DecimalFormat("00");
        Calendar cal = Calendar.getInstance();
        String mediaNo = list.size() > 0 ? "_" + decimalFormat.format(list.size()) : "";

        return userName + "_" + cal.get(Calendar.YEAR) + decimalFormat.format(cal.get(Calendar.MONTH)) + decimalFormat.format(cal.get(Calendar.DATE)) + "_" + decimalFormat.format(cal.get(Calendar.HOUR)) + decimalFormat.format(cal.get(Calendar.MINUTE)) + decimalFormat.format(cal.get(Calendar.SECOND)) + mediaNo;
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public void cancelAllTask() {
        RuntimeData.isInstaQualityInMultiSelectMode = false;
        if (adapter != null)
            adapter.cancelAllTasks();
    }
}
