package com.example.khalilo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.entities.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SelectedItemsAdapter extends RecyclerView.Adapter<SelectedItemsAdapter.SelectedItemViewHolder> {

    private Context context;
    private HashMap<Item, Integer> selectedItems;
    private List<Item> itemList;

    public SelectedItemsAdapter(Context context, HashMap<Item, Integer> selectedItems) {
        this.context = context;
        this.selectedItems = selectedItems;
        this.itemList = new ArrayList<>(selectedItems.keySet());
    }

    @NonNull
    @Override
    public SelectedItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected, parent, false);
        return new SelectedItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        holder.itemImage.setImageResource(item.getPic());
        holder.itemName.setText(item.getName());
        holder.itemQuantity.setText("Quantity: " + selectedItems.get(item));

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class SelectedItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemQuantity;
        ImageView itemImage;

        public SelectedItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.selectedItemName);
            itemQuantity = itemView.findViewById(R.id.selectedItemQuantity);
            itemImage = itemView.findViewById(R.id.selectedItemImage);

        }
    }
}
