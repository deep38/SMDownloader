package com.ocean.smdownloader.YTMediaHandler;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ocean.smdownloader.Algorithms;
import com.ocean.smdownloader.R;

import java.util.ArrayList;

public class YTMediaQualityAdapter extends RecyclerView.Adapter<YTMediaQualityAdapter.YTMediaQualityViewHolder> {

    private final Context context;
    private final YTMediaQualityRecyclerviewActionListener listener;
    private final ArrayList<YTMediaQualityData> dataList;

    public YTMediaQualityAdapter(Context context, YTMediaQualityRecyclerviewActionListener listener, ArrayList<YTMediaQualityData> dataList) {
        this.context = context;
        this.listener = listener;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public YTMediaQualityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = context.getSystemService(LayoutInflater.class).inflate(R.layout.quality_list_item_view, parent, false);
        return new YTMediaQualityViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull YTMediaQualityViewHolder holder, int position) {
        YTMediaQualityData data = dataList.get(holder.getAdapterPosition());

        holder.qualityTextView.setText(data.getQuality());
        holder.sizeTextView.setText(Algorithms.getSize(data.getPrimarySize() + data.getSecondarySize()));

        holder.itemView.setOnClickListener(view -> {
            listener.onItemClick(data);
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class YTMediaQualityViewHolder extends RecyclerView.ViewHolder {
        private TextView qualityTextView;
        private TextView sizeTextView;

        public YTMediaQualityViewHolder(View itemView) {
            super(itemView);

            qualityTextView = itemView.findViewById(R.id.qualityListItemQualityTextView);
            sizeTextView = itemView.findViewById(R.id.qualityListItemSizeTextView);
        }
    }
}
