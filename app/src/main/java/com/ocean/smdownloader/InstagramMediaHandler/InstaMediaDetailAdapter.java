package com.ocean.smdownloader.InstagramMediaHandler;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ocean.smdownloader.Internet.BitmapLoadListener;
import com.ocean.smdownloader.Internet.LoadBitmapFromLink;
import com.ocean.smdownloader.R;

import java.util.ArrayList;

public class InstaMediaDetailAdapter extends RecyclerView.Adapter<InstaMediaDetailAdapter.InstaMediaDetailViewHolder> implements BitmapLoadListener {

    private final Context context;
    private final InstaMediaRecyclerviewActionListener listener;
    private final ArrayList<InstaMediaDetailData> list;

    private final ArrayList<LoadBitmapFromLink> loadThumbnailTasks;
    private final ArrayList<InstaMediaQualityAdapter> qualityAdapters;

    public InstaMediaDetailAdapter(Context context, InstaMediaRecyclerviewActionListener listener, ArrayList<InstaMediaDetailData> list) {
        this.context = context;
        this.listener = listener;
        this.list = list;

        loadThumbnailTasks = new ArrayList<>();
        qualityAdapters = new ArrayList<>();
    }

    @NonNull
    @Override
    public InstaMediaDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = context.getSystemService(LayoutInflater.class).inflate(R.layout.insta_media_quality, parent, false);
        return new InstaMediaDetailViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull InstaMediaDetailViewHolder holder, int position) {
        if (position >= loadThumbnailTasks.size() && list.get(holder.getAdapterPosition()).getThumbnail() == null) {
            loadThumbnailTasks.add(new LoadBitmapFromLink(this, list.get(holder.getAdapterPosition())));
            loadThumbnailTasks.get(loadThumbnailTasks.size() - 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, list.get(holder.getAdapterPosition()).getThumbnailLink());
        } else {
            holder.thumbnailImageView.setImageBitmap(list.get(holder.getAdapterPosition()).getThumbnail());
        }
        if (position >= qualityAdapters.size()) {
            InstaMediaQualityAdapter adapter = new InstaMediaQualityAdapter(context, listener, list.get(holder.getAdapterPosition()));
            qualityAdapters.add(adapter);
            holder.qualitiesRecyclerView.setAdapter(adapter);
        } else {
            holder.qualitiesRecyclerView.setAdapter(qualityAdapters.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Object object) {
        if (object instanceof InstaMediaDetailData) {
            int position = ((InstaMediaDetailData) object).getPosition();

            if (bitmap != null) {
                ((InstaMediaDetailData) object).setThumbnail(bitmap);
                notifyItemChanged(position);
            } else {
                loadThumbnailTasks.set(position, new LoadBitmapFromLink(this, object));
                loadThumbnailTasks.get(position).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ((InstaMediaDetailData) object).getThumbnailLink());
            }
        }
    }

    public void cancelAllTasks() {
        for (LoadBitmapFromLink thumbnailTask : loadThumbnailTasks) {
            if (thumbnailTask != null && thumbnailTask.getStatus() != AsyncTask.Status.FINISHED)
                thumbnailTask.cancel(true);
        }

        for (InstaMediaQualityAdapter adapter : qualityAdapters) {
            if (adapter != null) {
                adapter.cancelAllTask();
            }
        }
    }

    public void cancelAllTaskExcept(int detailPosition, int qualityPosition) {
        int curDetailPosition = -1;
        for (LoadBitmapFromLink thumbnailTask : loadThumbnailTasks) {
            if (detailPosition == (++curDetailPosition))
                continue;

            if (thumbnailTask != null && thumbnailTask.getStatus() != AsyncTask.Status.FINISHED)
                thumbnailTask.cancel(true);
        }

        curDetailPosition = -1;
        for (InstaMediaQualityAdapter adapter : qualityAdapters) {
            if (detailPosition == (++curDetailPosition)) {
                adapter.cancelAllTaskExcept(qualityPosition);
                continue;
            }
            if (adapter != null) {
                adapter.cancelAllTask();
            }
        }
    }

    static class InstaMediaDetailViewHolder extends RecyclerView.ViewHolder {

        ImageView thumbnailImageView;
        RecyclerView qualitiesRecyclerView;

        public InstaMediaDetailViewHolder(@NonNull View itemView) {
            super(itemView);

            thumbnailImageView = itemView.findViewById(R.id.insta_media_details_thumbnail_view);
            qualitiesRecyclerView = itemView.findViewById(R.id.insta_media_details_quality_recycler_view);
        }
    }
}
