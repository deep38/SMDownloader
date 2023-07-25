package com.ocean.smdownloader.Download;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.AnimatedVectorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ocean.smdownloader.Algorithms;
import com.ocean.smdownloader.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class DownloadTaskAdapter extends RecyclerView.Adapter<DownloadTaskAdapter.DownloadTaskViewHolder> {

    private final Context context;
    private final RecyclerViewItemActionListener listener;
    private final ArrayList<DownloadTaskData> tasksData;
    private final HashMap<Integer, Integer> taskPositionHashMap;
    private final HashMap<Integer, DownloadTaskUpdatableViews> viewsHashMap;

    private boolean inSelectingMode;
    private final ArrayList<DownloadTaskData> selectedTasks;

    public DownloadTaskAdapter(Context context, RecyclerViewItemActionListener listener, ArrayList<DownloadTaskData> tasksData) {
        this.context = context;
        this.listener = listener;
        this.tasksData = tasksData;

        taskPositionHashMap = new HashMap<>();
        viewsHashMap = new HashMap<>();
        selectedTasks = new ArrayList<>();
    }

    @NonNull
    @Override
    public DownloadTaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new DownloadTaskViewHolder(context.getSystemService(LayoutInflater.class).inflate(R.layout.download_task_view, viewGroup, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull DownloadTaskViewHolder downloadTaskViewHolder, int position) {
        DownloadTaskData taskData = tasksData.get(downloadTaskViewHolder.getAdapterPosition());

        downloadTaskViewHolder.cardView.setOnLongClickListener(view -> {
            changeSelection(taskData, view);
            return true;
        });

        downloadTaskViewHolder.cardView.setOnClickListener(view -> {
            if (inSelectingMode) {
                changeSelection(taskData, view);
            } else {
                if (DownloadTask.isCompleted(taskData))
                    listener.onItemClick(taskData);
            }
        });

        downloadTaskViewHolder.cardView.setSelected(taskData.isSelected());

        if (DownloadTask.isCompleted(taskData)) {
            downloadTaskViewHolder.playPauseButton.setVisibility(View.GONE);
            downloadTaskViewHolder.nameTextView.setText(taskData.getName());
            downloadTaskViewHolder.sizeTextView.setText(Algorithms.getSize(taskData.getTotalSize()));
            downloadTaskViewHolder.progressTextView.setText("Completed");
            downloadTaskViewHolder.progressBar.setVisibility(View.GONE);
        } else {
            downloadTaskViewHolder.playPauseButton.setVisibility(View.VISIBLE);
            downloadTaskViewHolder.progressBar.setVisibility(View.VISIBLE);
            downloadTaskViewHolder.playPauseButton.setImageResource(DownloadTask.getStatus(taskData) == DownloadStates.STATE_RUNNING ? R.drawable.ic_pause : R.drawable.ic_play);
            downloadTaskViewHolder.nameTextView.setText(taskData.getName());
            downloadTaskViewHolder.sizeTextView.setText(Algorithms.getSize(taskData.getTotalDownloadedSize()) + File.separator + Algorithms.getSize(taskData.getTotalSize()));
            downloadTaskViewHolder.progressTextView.setText(Algorithms.getProgressPercentage(taskData.getTotalDownloadedSize(), taskData.getTotalSize()) + "%");
            downloadTaskViewHolder.progressBar.setProgress(Algorithms.getProgressPercentage(taskData.getTotalDownloadedSize(), taskData.getTotalSize()));

            downloadTaskViewHolder.playPauseButton.setOnClickListener(view -> {
                listener.onPlayPauseButtonClick(taskData);
            });
            taskPositionHashMap.put(taskData.getId(), downloadTaskViewHolder.getAdapterPosition());
            viewsHashMap.put(taskData.getId(), new DownloadTaskUpdatableViews(downloadTaskViewHolder.playPauseButton, downloadTaskViewHolder.sizeTextView, downloadTaskViewHolder.progressTextView, downloadTaskViewHolder.progressBar));
        }

        switch (taskData.getType()) {
            case MediaFileHandler.TYPE_IMAGE:
                downloadTaskViewHolder.mediaIndicatorView.setImageResource(R.drawable.ic_baseline_image);
                break;
            case MediaFileHandler.TYPE_AUDIO:
                downloadTaskViewHolder.mediaIndicatorView.setImageResource(R.drawable.ic_music_note);
                break;
            case MediaFileHandler.TYPE_VIDEO:
                downloadTaskViewHolder.mediaIndicatorView.setImageResource(R.drawable.ic_baseline_videocam);
                break;
        }

        if (taskData.getThumbnailPath() != null) {
            downloadTaskViewHolder.thumbnailImageView.setImageBitmap(BitmapFactory.decodeFile(taskData.getThumbnailPath()));
        } else {
            downloadTaskViewHolder.thumbnailImageView.setImageBitmap(null);
        }

    }

    private void changeSelection(DownloadTaskData taskData, View view) {
        taskData.setSelected(!taskData.isSelected());
        view.setSelected(taskData.isSelected());

        if (taskData.isSelected()) {
            selectTask(taskData);
        } else {
            deselectTask(taskData);
        }
    }

    private void selectTask(DownloadTaskData taskData) {
        selectedTasks.add(taskData);
        listener.onItemSelectionChange(selectedTasks.size());

        if (selectedTasks.size() == 1) {
            inSelectingMode = true;
            listener.onItemSelectionStart();
        }
    }

    private void deselectTask(DownloadTaskData taskData) {
        selectedTasks.remove(taskData);
        listener.onItemSelectionChange(selectedTasks.size());

        if (selectedTasks.size() == 0) {
            inSelectingMode = false;
            listener.onItemSelectionEnd();
        }
    }

    public void deselectAllItems() {
        for (DownloadTaskData taskData : selectedTasks) {
            taskData.setSelected(false);
        }

        notifyDataSetChanged();
        selectedTasks.clear();
        listener.onItemSelectionEnd();
        inSelectingMode = false;
    }

    public ArrayList<DownloadTaskData> getSelectedTasks() {
        return selectedTasks;
    }

    public void setInSelectingMode(boolean inSelectingMode) {
        this.inSelectingMode = inSelectingMode;
    }

    public boolean isInSelectingMode() {
        return inSelectingMode;
    }

    @Override
    public int getItemCount() {
        return tasksData.size();
    }

    public int getPositionByTaskId(int id) {
        if (taskPositionHashMap.containsKey(id))
            return taskPositionHashMap.get(id);
        else
            return 0;
    }

    public DownloadTaskUpdatableViews getUpdatableViews(int id, int firstVisiblePosition, int lastVisiblePosition) {
        int position = getPositionByTaskId(id);
        if (position >= firstVisiblePosition && position <= lastVisiblePosition) {
            return viewsHashMap.get(id);
        } else {
            return null;
        }
    }

    static class DownloadTaskViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        ImageView thumbnailImageView;
        ImageView mediaIndicatorView;
        ImageButton playPauseButton;
        TextView nameTextView;
        TextView sizeTextView;
        TextView progressTextView;
        ProgressBar progressBar;

        public DownloadTaskViewHolder(View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.download_task_card_view);
            thumbnailImageView = itemView.findViewById(R.id.download_task_thumbnail_view);
            mediaIndicatorView = itemView.findViewById(R.id.download_task_media_indicator);
            playPauseButton = itemView.findViewById(R.id.download_task_controller_button);
            nameTextView = itemView.findViewById(R.id.download_task_file_name_textview);
            sizeTextView = itemView.findViewById(R.id.download_task_size_textview);
            progressTextView = itemView.findViewById(R.id.download_task_progress_textview);
            progressBar = itemView.findViewById(R.id.download_task_progressbar);
        }
    }
}
