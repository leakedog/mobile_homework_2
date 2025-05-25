package com.example.homework2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MonitoredUrlAdapter extends RecyclerView.Adapter<MonitoredUrlAdapter.ViewHolder> {

    private List<MonitoredUrl> monitoredUrls;
    private final OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int id);
    }

    public MonitoredUrlAdapter(List<MonitoredUrl> monitoredUrls, OnDeleteClickListener onDeleteClickListener) {
        this.monitoredUrls = monitoredUrls;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monitored_url, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonitoredUrl currentUrl = monitoredUrls.get(position);
        holder.textViewUrl.setText(currentUrl.url);
        holder.buttonDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(currentUrl.id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return monitoredUrls.size();
    }

    public void setUrls(List<MonitoredUrl> urls) {
        this.monitoredUrls = urls;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUrl;
        Button buttonDelete;

        ViewHolder(View itemView) {
            super(itemView);
            textViewUrl = itemView.findViewById(R.id.textViewUrl);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteUrl);
        }
    }
} 