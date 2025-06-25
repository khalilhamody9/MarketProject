package com.example.khalilo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FavoriteStoresAdapter extends RecyclerView.Adapter<FavoriteStoresAdapter.ViewHolder> {
    private List<String> favoriteStores;
    private Context context;

    public FavoriteStoresAdapter(Context context, List<String> favoriteStores) {
        this.context = context;
        this.favoriteStores = favoriteStores;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(favoriteStores.get(position));
    }

    @Override
    public int getItemCount() {
        return favoriteStores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
