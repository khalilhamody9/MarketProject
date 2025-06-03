package com.example.khalilo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.khalilo.R;
import com.example.khalilo.models.History;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private Context context;
    private List<History> historyList;
    private String groupName;
    private String username;

    public HistoryAdapter(Context context, List<History> historyList, String groupName, String username) {
        this.context = context;
        this.historyList = historyList;
        this.groupName = groupName;
        this.username = username;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        History history = historyList.get(position);
        holder.itemName.setText(history.getItemName());
      //  holder.itemPrice.setText("Price: $" + history.getPrice());
        holder.itemCategory.setText("Category: " + history.getCategory());
        holder.action.setText("Action: " + history.getAction());
        holder.timestamp.setText("Date: " + history.getDate());
        holder.username.setText("By: " + history.getUsername());

        try {
            int imageResId = Integer.parseInt(history.getImageUrl());
            Glide.with(context)
                    .load(imageResId)
                    .into(holder.itemImage);
        } catch (NumberFormatException e) {
            // If it's not an integer, treat it as a URL or file path
            Glide.with(context)
                    .load(history.getImageUrl())
                    .into(holder.itemImage);
        }

    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemPrice, itemCategory, action, timestamp, username;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemCategory = itemView.findViewById(R.id.itemCategory);
            action = itemView.findViewById(R.id.itemAction);
            timestamp = itemView.findViewById(R.id.itemTimestamp);
            username = itemView.findViewById(R.id.itemUser);
        }
    }
}
