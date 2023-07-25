package com.ocean.smdownloader;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.ocean.smdownloader.Download.DownloadService;
import com.ocean.smdownloader.Download.DownloadStates;
import com.ocean.smdownloader.Download.DownloadTask;
import com.ocean.smdownloader.Download.DownloadTaskAdapter;
import com.ocean.smdownloader.Download.DownloadTaskData;
import com.ocean.smdownloader.Download.DownloadTasks;
import com.ocean.smdownloader.Download.DownloadTaskListener;
import com.ocean.smdownloader.Download.DownloadTaskUpdatableViews;
import com.ocean.smdownloader.Download.DownloadTasksDatabase;
import com.ocean.smdownloader.Download.MediaFileHandler;
import com.ocean.smdownloader.Download.MediaMuxerTaskListener;
import com.ocean.smdownloader.Download.RecyclerViewItemActionListener;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements RecyclerViewItemActionListener, DownloadTaskListener, MediaMuxerTaskListener {
    private final String[] THEME_STATES_STRING = new String[]{"System", "Light", "Dark"};
    public static final int[] THEME_STATES = new int[]{AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES};

    private static PendingIntent pendingIntent;
    private int firstVisibleTask, lastVisibleTask;
    private int currentTasks;
    DownloadTaskAdapter downloadTaskAdapter;

    MySharedPreferences mySharedPreferences;
    DownloadTasksDatabase downloadTasksDatabase;

    BroadcastReceiver serviceStartReceiver;
    DownloadService downloadService;
    ServiceConnection downloadServiceConnection;
    Intent boundedIntent;

    DrawerArrowDrawable drawerArrowDrawable;
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle actionBarDrawerToggle;
    Toolbar toolbar;
    ActionMenuView actionMenuView;
    NavigationView navigationView;
    RecyclerView downloadTaskRecyclerView;
    FloatingActionButton addDownloadFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RuntimeData.isMainActivityRunning = true;
        mySharedPreferences = new MySharedPreferences(this);
        downloadTasksDatabase = new DownloadTasksDatabase(this);
        AppCompatDelegate.setDefaultNightMode(THEME_STATES[mySharedPreferences.getThemeState()]);

        drawerArrowDrawable = new DrawerArrowDrawable(this);
        drawerLayout = findViewById(R.id.main_drawer_layout);
        toolbar = findViewById(R.id.mainToolBar);
        actionMenuView = findViewById(R.id.action_bar_menu_view);
        navigationView = findViewById(R.id.main_navigation_view);
        downloadTaskRecyclerView = findViewById(R.id.downloadTasksRecyclerView);
        addDownloadFab = findViewById(R.id.mainDownloadButton);

        setupActionBar();
        setupListeners();
        loadDownloadTasks();
        initReceiver();

        if (RuntimeData.isDownloadServiceRunning) {
            bindDownloadService(new Intent(this, DownloadService.class), null, false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            downloadTaskAdapter.deselectAllItems();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        drawerArrowDrawable.setColor(getColor(R.color.default_text_color));
        drawerArrowDrawable.setSpinEnabled(true);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                drawerArrowDrawable.setProgress(slideOffset);
            }
        };
        actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
        actionBarDrawerToggle.setHomeAsUpIndicator(drawerArrowDrawable);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    private void setupListeners() {
        addDownloadFab.setOnClickListener(view -> showDownloadDialog());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        downloadTaskRecyclerView.setLayoutManager(layoutManager);

        downloadTaskRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                firstVisibleTask = layoutManager.findFirstVisibleItemPosition();
                lastVisibleTask = layoutManager.findLastVisibleItemPosition();
            }
        });

        actionMenuView.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_menu_delete) {
                showDeleteAlertDialog();
                return true;
            }
            return false;
        });

        AppCompatSpinner themeSpinner = (AppCompatSpinner) navigationView.getMenu().findItem(R.id.menu_option_app_theme).getActionView();
        themeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, THEME_STATES_STRING));
        themeSpinner.setSelection(mySharedPreferences.getThemeState());

        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                AppCompatDelegate.setDefaultNightMode(THEME_STATES[i]);
                mySharedPreferences.setThemeState(i);
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_option_app_theme) {
                themeSpinner.performClick();
                return true;
            }
            return false;
        });

        actionBarDrawerToggle.setToolbarNavigationClickListener(view -> {
            if (downloadTaskAdapter.isInSelectingMode()) {
                downloadTaskAdapter.deselectAllItems();
            } else {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });
    }

    private void showDeleteAlertDialog() {
        View view = getSystemService(LayoutInflater.class).inflate(R.layout.download_tasks_delete_alert, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete " + downloadTaskAdapter.getSelectedTasks().size() + " items")
                .setView(view)
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", (dialogInterface, i) -> {})
                .create();

        dialog.show();

        Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        CheckBox deleteFromStorage = view.findViewById(R.id.download_tasks_delete_from_storage_checkbox);

        okButton.setOnClickListener(view1 -> {
            deleteSelectedItems(deleteFromStorage.isChecked());
            dialog.dismiss();
        });
    }

    private void deleteSelectedItems(boolean fromStorageAlso) {
        for (DownloadTaskData taskData : downloadTaskAdapter.getSelectedTasks()) {
            if (taskData.getPrimaryStatus() == DownloadStates.STATE_RUNNING || taskData.getSecondaryStatus() == DownloadStates.STATE_RUNNING) {
                if (downloadService != null)
                    downloadService.pauseTask(taskData);
            }
            DownloadTasks.list.remove(taskData);
            downloadTasksDatabase.deleteData(taskData.getId());
            new File(getExternalCacheDir() + "/.thumb/" + taskData.getName().substring(0, taskData.getName().lastIndexOf(".")) + MediaFileHandler.getExtension(MediaFileHandler.TYPE_IMAGE)).delete();

            if (fromStorageAlso) {
                if (MediaFileHandler.TYPE_VIDEO.equals(taskData.getType()) && !DownloadTask.isCompleted(taskData)) {
                    File folder = new File(taskData.getPrimaryFilePath().substring(0, taskData.getPrimaryFilePath().lastIndexOf("/")));
                    if (folder.exists()) {
                        for (File fileIn : folder.listFiles()) {
                            fileIn.delete();
                        }
                        folder.delete();
                    }
                } else {
                    new File(taskData.getPrimaryFilePath()).delete();
                }
            }
        }
        downloadTaskAdapter.notifyDataSetChanged();
        downloadTaskRecyclerView.startLayoutAnimation();

        downloadTaskAdapter.getSelectedTasks().clear();
        downloadTaskAdapter.setInSelectingMode(false);
        onItemSelectionEnd();
    }

    private void initReceiver() {

        serviceStartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (downloadServiceConnection == null) {
                    bindDownloadService(new Intent(MainActivity.this, DownloadService.class), null, false);
                }
            }
        };

        registerReceiver(serviceStartReceiver, new IntentFilter(DownloadService.ACTION_SERVICE_START));
    }

    private void loadDownloadTasks() {
        Cursor cursor = downloadTasksDatabase.getAllData();

        if (cursor.getCount() > DownloadTasks.list.size()) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                if (!isTaskInList(id)) {
                    int index;
                    if (cursor.getInt(13) == DownloadStates.STATE_COMPLETE && (cursor.getInt(14) == DownloadStates.STATE_NOT_AVAILABLE || cursor.getInt(14) == DownloadStates.STATE_COMPLETE)) {
                        index = DownloadTasks.downloadedStartIndex;
                    } else {
                        index = DownloadTasks.newAddedTasks;
                        DownloadTasks.downloadedStartIndex++;
                    }
                    DownloadTasks.list.add(index, new DownloadTaskData(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getString(5),
                            cursor.getString(6),
                            cursor.getString(7),
                            cursor.getString(8),
                            cursor.getLong(9),
                            cursor.getLong(10),
                            cursor.getLong(11),
                            cursor.getLong(12),
                            cursor.getInt(13),
                            cursor.getInt(14)));

                }
            }
        }
        cursor.close();


        downloadTaskAdapter = new DownloadTaskAdapter(this, this, DownloadTasks.list);
        downloadTaskRecyclerView.setAdapter(downloadTaskAdapter);
        currentTasks = DownloadTasks.list.size();
    }

    private boolean isTaskInList(int id) {
        for (DownloadTaskData taskData : DownloadTasks.list) {
            if (id == taskData.getId()) {
                return true;
            }
        }

        return false;
    }

    private void showDownloadDialog() {
        View addDownloadView = getSystemService(LayoutInflater.class).inflate(R.layout.dialog_edit_text_layout, null, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Download")
                .setView(addDownloadView)
                .setPositiveButton("Download", null)
                .setNegativeButton("Cancel", (dialogInterface, i) -> {})
                .create();

        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();

        AppCompatEditText linkEditText = addDownloadView.findViewById(R.id.downloadLinkEditText);
        Button downloadButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        linkEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        linkEditText.requestFocus();

        linkEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String link = Objects.requireNonNull(linkEditText.getText()).toString();

                if (link.startsWith("http")) {
                    Intent intent = new Intent(this, MediaQualityViewerActivity.class);
                    intent.putExtra(Intent.EXTRA_TEXT, link);
                    startActivity(intent);

                    dialog.dismiss();
                } else {
                    linkEditText.setError("Invalid link");
                }
                return true;
            }
            return false;
        });
        downloadButton.setOnClickListener(view -> {
            String link = Objects.requireNonNull(linkEditText.getText()).toString();

            if (link.startsWith("http")) {
                Intent intent = new Intent(this, MediaQualityViewerActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT, link);
                startActivity(intent);

                dialog.dismiss();
            } else {
                linkEditText.setError("Invalid link");
            }
        });
    }

    private void startDownload(DownloadTaskData downloadTaskData, boolean isNewTask) {
        if (RuntimeData.isDownloadServiceRunning && downloadService != null) {
            if (isNewTask)
                downloadService.addDownload(downloadTaskData);
            else
                downloadService.resumeTask(downloadTaskData);
        } else {
            Intent intent = new Intent(this, DownloadService.class);
            if (!RuntimeData.isDownloadServiceRunning)
                startService(intent);
            bindDownloadService(intent, downloadTaskData, isNewTask);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void bindDownloadService(Intent intent, DownloadTaskData downloadTaskData, boolean isNewTask) {
        downloadServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                downloadService = ((DownloadService.DownloadServiceBinder) iBinder).getService();
                downloadService.setListeners(MainActivity.this, MainActivity.this);
                boundedIntent = intent;

                if (currentTasks != DownloadTasks.list.size()) {
                    downloadTaskAdapter.notifyDataSetChanged();
                    downloadTaskRecyclerView.startLayoutAnimation();
                }
                if (downloadTaskData != null) {
                    if (isNewTask) {
                        downloadService.addDownload(downloadTaskData);
                    } else {
                        downloadService.resumeTask(downloadTaskData);
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                if (downloadService != null) {
                    downloadService.setListeners(null, null);
                    downloadService = null;
                }
            }
        };
        bindService(intent, downloadServiceConnection, BIND_NOT_FOREGROUND);
    }


    @Override
    public void onPlayPauseButtonClick(DownloadTaskData downloadTaskData) {
        int status = downloadTaskData.getPrimaryStatus() == DownloadStates.STATE_COMPLETE ? downloadTaskData.getSecondaryStatus() : downloadTaskData.getPrimaryStatus();
        if (status == DownloadStates.STATE_RUNNING) {
            if (downloadService != null) {
                downloadService.pauseTask(downloadTaskData);
            }
        } else if (status == DownloadStates.STATE_PAUSE) {
            startDownload(downloadTaskData, false);
        }
    }

    @Override
    public void onItemClick(DownloadTaskData taskData) {
        String path = taskData.getPrimaryFilePath();

        if (new File(path).exists()) {
            try {
                Intent viewMediaIntent = new Intent(Intent.ACTION_VIEW);
                Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(path));
                viewMediaIntent.setDataAndType(fileUri, taskData.getType());
                viewMediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(viewMediaIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No app found on your device for open this file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "File not found! " + path, Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", path);
        }
    }

    @Override
    public void onItemSelectionStart() {
        animateDrawerButtonToArrow();
        actionMenuView.getMenu().clear();
        getMenuInflater().inflate(R.menu.selection_mode_action_bar_menu, actionMenuView.getMenu());
        drawerLayout.removeDrawerListener(actionBarDrawerToggle);
        drawerArrowDrawable.setProgress(1);
    }

    @Override
    public void onItemSelectionChange(int selected) {
        Objects.requireNonNull(getSupportActionBar()).setTitle(String.valueOf(selected));
    }

    @Override
    public void onItemSelectionEnd() {
        animateDrawerButtonToHamburger();
        actionMenuView.getMenu().clear();
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.app_name));
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        drawerArrowDrawable.setProgress(0);
    }

    float progress = 0;
    private void animateDrawerButtonToArrow() {
        progress = drawerArrowDrawable.getProgress();
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                progress += 0.1f;

                drawerArrowDrawable.setProgress(progress);

                if (progress < 1) {
                    handler.postDelayed(this, 10);
                }
            }
        });

    }

    private void animateDrawerButtonToHamburger() {
        progress = drawerArrowDrawable.getProgress();
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                progress -= 0.1f;
                if (progress >= 0)
                    drawerArrowDrawable.setProgress(progress);
                if (progress > 0) {
                    handler.postDelayed(this, 10);
                }
            }
        });

    }
    /********************************* DOWNLOAD TASK STATUS LISTENERS *****************************/

    @SuppressLint("SetTextI18n")
    @Override
    public void onProgress(DownloadTaskData taskData, int progress) {

        if (downloadTaskAdapter != null) {
            DownloadTaskUpdatableViews views = downloadTaskAdapter.getUpdatableViews(taskData.getId(), firstVisibleTask, lastVisibleTask);
            if (views != null) {
                views.getProgressBar().setProgress(progress);
                views.getProgressTextView().setText(progress + "%");
                views.getSizeTextView().setText(Algorithms.getSize(taskData.getPrimaryDownloadedSize() + taskData.getSecondaryDownloadedSize()) + "/" + Algorithms.getSize(taskData.getTotalSize()));
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onStart(DownloadTaskData taskData) {
        downloadTaskAdapter.notifyDataSetChanged();
        downloadTaskRecyclerView.startLayoutAnimation();
    }

    @Override
    public void onResume(DownloadTaskData taskData) {

        if (downloadTaskAdapter != null) {

            DownloadTaskUpdatableViews views = downloadTaskAdapter.getUpdatableViews(taskData.getId(), firstVisibleTask, lastVisibleTask);
            if (views != null)
                views.getImageButton().setImageResource(R.drawable.ic_pause);
        }
    }

    @Override
    public void onPause(DownloadTaskData taskData, boolean isSecondaryTask) {

        if (downloadTaskAdapter != null) {

            DownloadTaskUpdatableViews views = downloadTaskAdapter.getUpdatableViews(taskData.getId(), firstVisibleTask, lastVisibleTask);
            if (views != null) {
                views.getImageButton().setImageResource(R.drawable.ic_play);
                views.getSizeTextView().setText(Algorithms.getOutOfSizeText(taskData.getTotalDownloadedSize(), taskData.getTotalSize()));
            }
        }
    }

    @Override
    public void onZeroTaskRemain(boolean removeNotification) {

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onComplete(DownloadTaskData taskData, boolean isSecondaryTask) {
        if (downloadTaskAdapter != null) {

            DownloadTaskUpdatableViews views = downloadTaskAdapter.getUpdatableViews(taskData.getId(), firstVisibleTask, lastVisibleTask);
            if (views != null) {
                views.getImageButton().setVisibility(View.GONE);
                views.getProgressBar().setVisibility(View.GONE);
                views.getProgressTextView().setText("Download Complete");
                views.getSizeTextView().setText(Algorithms.getSize(taskData.getTotalDownloadedSize()));
            }
            downloadTaskAdapter.notifyDataSetChanged();
            downloadTaskRecyclerView.startLayoutAnimation();

        }
    }

    @Override
    public void onError(DownloadTaskData taskData, String error, boolean isSecondaryTask) {
        if (downloadTaskAdapter != null) {

            DownloadTaskUpdatableViews views = downloadTaskAdapter.getUpdatableViews(taskData.getId(), firstVisibleTask, lastVisibleTask);
            if (views != null) {
                views.getImageButton().setVisibility(View.GONE);
                views.getSizeTextView().setText(error);
            }
        }
    }

    @Override
    public void restart(DownloadTaskData taskData) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Restart Download")
                .setMessage("File not found. May renamed, moved or deleted.")
                .setPositiveButton("Restart", (dialogInterface, i) -> {
                    mySharedPreferences.deleteStartingPoints(taskData.getId());
                    downloadTasksDatabase.deleteData(taskData.getId());
                    DownloadTasks.list.remove(taskData);
                    startDownload(taskData, true);
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {

                })
                .create();

        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        currentTasks = DownloadTasks.list.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();

        if (currentTasks != DownloadTasks.list.size()) {
            downloadTaskAdapter.notifyDataSetChanged();
            downloadTaskRecyclerView.startLayoutAnimation();
        }
    }

    /**********************************************************************************************/

    /********************************* MEDIA MUXER LISTENERS **************************************/


    @Override
    public void onMuxStart(DownloadTaskData taskData) {
        if (downloadTaskAdapter != null) {

            DownloadTaskUpdatableViews views = downloadTaskAdapter.getUpdatableViews(taskData.getId(), firstVisibleTask, lastVisibleTask);
            if (views != null) {
                views.getSizeTextView().setText("Merging...");
                views.getProgressTextView().setText("0%");
                views.getProgressBar().setProgress(0);
                views.getImageButton().setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onMuxProgress(DownloadTaskData taskData, int progress) {
        if (downloadTaskAdapter != null) {

            DownloadTaskUpdatableViews views = downloadTaskAdapter.getUpdatableViews(taskData.getId(), firstVisibleTask, lastVisibleTask);
            if (views != null) {
                views.getProgressTextView().setText(progress + "%");
                views.getProgressBar().setProgress(progress);
            }
        }
    }

    @Override
    public void onMuxComplete(DownloadTaskData taskData) {
        if (downloadTaskAdapter != null) {

            DownloadTaskUpdatableViews views = downloadTaskAdapter.getUpdatableViews(taskData.getId(), firstVisibleTask, lastVisibleTask);
            if (views != null) {
                views.getSizeTextView().setText(Algorithms.getSize(taskData.getTotalSize()));
                views.getProgressTextView().setText("Completed");
                views.getProgressBar().setVisibility(View.GONE);
            }
            downloadTaskAdapter.notifyDataSetChanged();
            downloadTaskRecyclerView.startLayoutAnimation();

        }
    }

    @Override
    public void onMuxFailed(DownloadTaskData taskData, String error) {
        if (downloadTaskAdapter != null) {

            DownloadTaskUpdatableViews views = downloadTaskAdapter.getUpdatableViews(taskData.getId(), firstVisibleTask, lastVisibleTask);
            if (views != null) {
                views.getSizeTextView().setText(error);
                views.getProgressBar().setVisibility(View.GONE);
            }
        }
    }

    /**********************************************************************************************/

    public static PendingIntent getPendingIntent(Context context) {
        if (pendingIntent == null) {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);
        }
        return pendingIntent;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        RuntimeData.isMainActivityRunning = false;
        unregisterReceiver(serviceStartReceiver);

////        Log.d("MainActivity", "onDestroy " +RuntimeData.isDownloadServiceRunning + " " + downloadServiceConnection);
        if (RuntimeData.isDownloadServiceRunning && downloadServiceConnection != null)
            unbindService(downloadServiceConnection);

        if (!RuntimeData.isDownloadServiceRunning)
            downloadTasksDatabase.close();

        if (downloadService != null) {
            downloadService.setListeners(null, null);
            downloadService = null;
        }
    }
}