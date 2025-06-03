package com.example.khalilo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.entities.Item;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    private Context context;
    private List<Item> itemList;
    private String groupName;
    private String username;
    private HashMap<Integer, Integer> quantityMap = new HashMap<>();
    private OnItemChangeListener onItemChangeListener;
    private HashMap<Item, Integer> selectedItems = new HashMap<>();

    public ItemAdapter(Context context, List<Item> itemList, String groupName, String username) {
        this.context = context;
        this.itemList = itemList;
        this.groupName = groupName;
        this.username = username;
    }

    public void setOnItemChangeListener(OnItemChangeListener listener) {
        this.onItemChangeListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);

        holder.itemName.setText(item.getName());
        holder.itemImage.setImageResource(item.getPic());

        // Initialize quantity if not already in map
        if (!quantityMap.containsKey(item.getId())) {
            quantityMap.put(item.getId(), 0);  // Start from 0
        }
        holder.itemQuantity.setText(String.valueOf(quantityMap.get(item.getId())));

        // Increase Quantity
        holder.btnIncrease.setOnClickListener(v -> {
            int currentQuantity = quantityMap.get(item.getId());
            currentQuantity++;
            quantityMap.put(item.getId(), currentQuantity);
            holder.itemQuantity.setText(String.valueOf(currentQuantity));

            // Add to selected items (even if quantity is 0)
            selectedItems.put(item, currentQuantity);

            // Add to history only if quantity > 0
            if (currentQuantity > 0) {
                addToHistory(item, currentQuantity, "Increased");
            }

            if (onItemChangeListener != null) {
                onItemChangeListener.onItemChanged();
            }
        });

        // Decrease Quantity
        holder.btnDecrease.setOnClickListener(v -> {
            int currentQuantity = quantityMap.get(item.getId());

            // Allow decrement to 0
            if (currentQuantity > 0) {
                currentQuantity--;
                quantityMap.put(item.getId(), currentQuantity);
                holder.itemQuantity.setText(String.valueOf(currentQuantity));

                // Update selected items
                selectedItems.put(item, currentQuantity);

                // Add to history only if quantity > 0
                if (currentQuantity > 0) {
                    addToHistory(item, currentQuantity, "Decreased");
                }

                if (onItemChangeListener != null) {
                    onItemChangeListener.onItemChanged();
                }
            }
        });
    }


    public void increaseItem(Item item) {
        int currentQuantity = quantityMap.containsKey(item.getId()) ? quantityMap.get(item.getId()) : 0;
        currentQuantity++;
        quantityMap.put(item.getId(), currentQuantity);
        selectedItems.put(item, currentQuantity);

        if (currentQuantity > 0) {
            addToHistory(item, currentQuantity, "Increased");
        }

        if (onItemChangeListener != null) {
            onItemChangeListener.onItemChanged();
        }

        notifyDataSetChanged();  // Refresh UI
    }

    private void addToHistory(Item item, int quantity, String action) {
        Map<String, Object> body = new HashMap<>();
        body.put("itemName", item.getName());
        //body.put("price", item.getPrice());
        body.put("imageUrl", item.getPic());
        body.put("action", action);
        body.put("quantity", quantity);
        body.put("category", item.getCategory());
        body.put("groupName", groupName);
        body.put("username", username);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<Void> call = apiService.addHistory(body);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, action + " " + item.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to sync history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Get Selected Items
    public HashMap<Item, Integer> getSelectedItems() {
        return selectedItems;
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateList(List<Item> newList) {
        itemList = newList;
        notifyDataSetChanged();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemPrice, itemQuantity;
        ImageButton btnIncrease, btnDecrease;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);

            //  itemPrice = itemView.findViewById(R.id.itemPrice);
            itemQuantity = itemView.findViewById(R.id.itemQuantity);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
        }
    }

    public interface OnItemChangeListener {
        void onItemChanged();
    }
    // Restore Selected Items
    public void restoreSelectedItems(HashMap<Item, Integer> restoredItems) {
        selectedItems.clear();
        selectedItems.putAll(restoredItems);

        for (Map.Entry<Item, Integer> entry : restoredItems.entrySet()) {
            Item item = entry.getKey();
            Integer quantity = entry.getValue();
            quantityMap.put(item.getId(), quantity);
        }

        notifyDataSetChanged();
    }
}
