package com.ocean.smdownloader.InstagramMediaHandler;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ocean.smdownloader.Algorithms;
import com.ocean.smdownloader.Internet.LoadSize;
import com.ocean.smdownloader.Internet.SizeLoadListener;
import com.ocean.smdownloader.R;
import com.ocean.smdownloader.RuntimeData;

import java.util.ArrayList;

public class InstaMediaQualityAdapter extends RecyclerView.Adapter<InstaMediaQualityAdapter.InstaMediaQualityViewHolder> implements SizeLoadListener {

    private final Context context;
    private final InstaMediaRecyclerviewActionListener listener;
    private final InstaMediaDetailData data;
    private final ArrayList<InstaMediaQualityData> qualities;

    private final ArrayList<LoadSize> loadSizeTasks;

    private int selectedQualityPosition = -1;
    private InstaDownloadTaskData selectedData;

    public InstaMediaQualityAdapter(Context context, InstaMediaRecyclerviewActionListener listener, InstaMediaDetailData data) {
        this.context = context;
        this.listener = listener;
        this.data = data;
        this.qualities = data.getQualities();

        loadSizeTasks = new ArrayList<>();
    }

    @NonNull
    @Override
    public InstaMediaQualityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = context.getSystemService(LayoutInflater.class).inflate(R.layout.quality_list_item_view, parent, false);
        return new InstaMediaQualityViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull InstaMediaQualityViewHolder holder, int position) {
        InstaMediaQualityData qualityData = qualities.get(holder.getAdapterPosition());

        holder.layout.setSelected(qualityData.isSelected());
        holder.qualityTextView.setText(qualityData.getQuality());
        if (position >= loadSizeTasks.size() && qualities.get(holder.getAdapterPosition()).getSize() == 0) {
            holder.sizeTextView.setText("Loading...");
            loadSizeTasks.add(new LoadSize(this, qualityData));
            loadSizeTasks.get(loadSizeTasks.size() - 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, qualityData.getLink());
        } else {
            holder.sizeTextView.setText(qualityData.getSize() <= 0 ? "Loading..." : Algorithms.getSize(qualityData.getSize()));
        }

        holder.layout.setOnClickListener(view -> {
            if (!RuntimeData.isInstaQualityInMultiSelectMode) {
                selectedQualityPosition = holder.getAdapterPosition();
                listener.onQualityClick(holder.getAdapterPosition(), data);
            } else {
                if (qualityData.isSelected()) {
                    view.setSelected(false);
                    deselectQuality();
                } else {
                    view.setSelected(true);
                    selectQuality(holder.getAdapterPosition());
                }
            }
        });

        holder.layout.setOnLongClickListener(view -> {
            if (qualityData.isSelected()) {
                view.setSelected(false);
                deselectQuality();
            } else {
                view.setSelected(true);
                selectQuality(holder.getAdapterPosition());
            }
            return true;
        });

        Log.d("InstaQualityAdapter", "Size: " + loadSizeTasks.size());
    }

    private void selectQuality(int position) {
        if (selectedData != null) {
            deselectQuality();
        }
        selectedQualityPosition = position;
        qualities.get(selectedQualityPosition).setSelected(true);
        selectedData = new InstaDownloadTaskData(data, position);
        listener.onQualitySelected(selectedData);
    }

    private void deselectQuality() {
        listener.onQualityDeselect(selectedData);
        qualities.get(selectedQualityPosition).setSelected(false);
        notifyItemChanged(selectedQualityPosition);

        selectedData = null;
        selectedQualityPosition = -1;
    }

    @Override
    public int getItemCount() {
        return qualities.size();
    }

    @Override
    public void onLoad(long size, Object object) {
        if (object instanceof InstaMediaQualityData) {
            int position = ((InstaMediaQualityData) object).getPosition();

            if (size > 0) {
                ((InstaMediaQualityData) object).setSize(size);
                notifyItemChanged(position);

            } else {
                loadSizeTasks.set(position, new LoadSize(this, object));
                loadSizeTasks.get(position).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ((InstaMediaQualityData) object).getLink());
            }
        } else {
            showMessage("Invalid type of object");
        }
    }

    @Override
    public void onFailed(String error, Object object) {
        showMessage(error);
    }

    public void cancelAllTask() {
        for (LoadSize loadSize : loadSizeTasks) {
            if (loadSize != null && loadSize.getStatus() != AsyncTask.Status.FINISHED)
                loadSize.cancel(true);
        }
    }

    public void cancelAllTaskExcept(int qualityPosition) {
        int curQualityPosition = -1;
        for (LoadSize loadSize : loadSizeTasks) {
            if (++curQualityPosition == qualityPosition)
                continue;

            if (loadSize != null && loadSize.getStatus() != AsyncTask.Status.FINISHED)
                loadSize.cancel(true);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    static class InstaMediaQualityViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout layout;
        TextView qualityTextView, sizeTextView;

        public InstaMediaQualityViewHolder(View itemView) {
            super(itemView);

            layout = itemView.findViewById(R.id.qualityListItemLayout);
            qualityTextView = itemView.findViewById(R.id.qualityListItemQualityTextView);
            sizeTextView = itemView.findViewById(R.id.qualityListItemSizeTextView);
        }
    }
}
