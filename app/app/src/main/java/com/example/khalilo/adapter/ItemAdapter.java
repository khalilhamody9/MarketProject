package com.example.khalilo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
    private HashMap<String, Integer> quantityMap = new HashMap<>();
    private HashMap<String, Item> itemMap = new HashMap<>();
    private HashMap<Item, Integer> selectedItems = new HashMap<>();

    // Listeners for activity communication
    private OnItemChangeListener onItemChangeListener;
    private OnItemDeleteListener deleteListener;

    // Interface to notify about quantity changes
    public interface OnItemChangeListener {
        void onItemChanged();
    }

    // Interface to notify about delete clicks
    public interface OnItemDeleteListener {
        void onItemDelete(Item item);
    }

    public ItemAdapter(Context context, List<Item> itemList, String groupName, String username) {
        this.context = context;
        this.itemList = itemList;
        this.groupName = groupName;
        this.username = username;

        for (Item item : itemList) {
            if (item != null && item.getName() != null) {
                itemMap.put(item.getName(), item);
                quantityMap.put(item.getName(), 0);
            }
        }
    }

    public void setOnItemChangeListener(OnItemChangeListener listener) {
        this.onItemChangeListener = listener;
    }

    // Method to set the delete listener from the Activity
    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure you are using the correct layout file that includes the delete button
        View view = LayoutInflater.from(context).inflate(R.layout.item_home, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        if (item == null || item.getName() == null) return;

        String key = item.getName();
        holder.itemName.setText(key);

        if (item.getImg() != null && item.getImg().startsWith("data:image")) {
            try {
                String base64Image = item.getImg().split(",")[1];
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.itemImage.setImageBitmap(decodedByte);
            } catch (Exception e) {
                holder.itemImage.setImageResource(R.drawable.no_img);
            }
        } else if (item.getImg() != null && !item.getImg().isEmpty()) {
            Glide.with(context)
                    .load(item.getImg())
                    .placeholder(R.drawable.no_img)
                    .into(holder.itemImage);
        } else {
            // No image available, show default and fetch from server
            holder.itemImage.setImageResource(R.drawable.no_img);
            ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
            apiService.searchItems(item.getName()).enqueue(new Callback<List<Item>>() {
                @Override
                public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        Item fetched = response.body().get(0);
                        item.setImg(fetched.getImg());
                        notifyItemChanged(holder.getAdapterPosition());
                    }
                }

                @Override
                public void onFailure(Call<List<Item>> call, Throwable t) {
                    // keep default image
                }
            });
        }

        int quantity = quantityMap.getOrDefault(key, 0);
        holder.itemQuantity.setText(String.valueOf(quantity));

        holder.btnIncrease.setOnClickListener(v -> {
            int currentQuantity = quantityMap.getOrDefault(key, 0) + 1;
            quantityMap.put(key, currentQuantity);
            holder.itemQuantity.setText(String.valueOf(currentQuantity));
            selectedItems.put(item, currentQuantity);
            addToHistory(item, currentQuantity, "Increased");
            if (onItemChangeListener != null) {
                onItemChangeListener.onItemChanged();
            }
        });

        holder.btnDecrease.setOnClickListener(v -> {
            int currentQuantity = quantityMap.getOrDefault(key, 0);
            if (currentQuantity > 0) {
                currentQuantity--;
                quantityMap.put(key, currentQuantity);
                holder.itemQuantity.setText(String.valueOf(currentQuantity));
                if (currentQuantity == 0) {
                    selectedItems.remove(item);
                } else {
                    selectedItems.put(item, currentQuantity);
                }
                addToHistory(item, currentQuantity, "Decreased");
                if (onItemChangeListener != null) {
                    onItemChangeListener.onItemChanged();
                }
            }
        });

        holder.btnDeleteItem.setOnClickListener(v -> {
            if (deleteListener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    deleteListener.onItemDelete(itemList.get(adapterPosition));
                }
            }
        });
    }


    private void addToHistory(Item item, Integer quantity, String action) {
        if (item == null || item.getName() == null) {
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("itemName", item.getName());
        body.put("imageUrl", item.getImg());
        body.put("action", action);
        body.put("quantity", quantity);
        body.put("category", item.getCategory() != null ? item.getCategory() : "Unknown");
        body.put("groupName", groupName);
        body.put("username", username);
        body.put("barcode", item.getBarcode()); // ✅ הוסף את זה

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<Void> call = apiService.addHistory(body);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Silently fail or log for debugging
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Silently fail
            }
        });
    }

    public HashMap<Item, Integer> getSelectedItems() {
        HashMap<Item, Integer> currentSelectedItems = new HashMap<>();
        for (Map.Entry<String, Integer> entry : quantityMap.entrySet()) {
            if (entry.getValue() > 0 && itemMap.containsKey(entry.getKey())) {
                currentSelectedItems.put(itemMap.get(entry.getKey()), entry.getValue());
            }
        }
        return currentSelectedItems;
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateList(List<Item> newList) {
        this.itemList = newList;

        // השאר את quantityMap הקיים ולא תחליף אותו
        for (Item item : newList) {
            String name = item.getName();
            if (!quantityMap.containsKey(name)) {
                quantityMap.put(name, 0);  // הוסף חדש אם לא קיים
            }
            if (!itemMap.containsKey(name)) {
                itemMap.put(name, item);
            }
        }

        notifyDataSetChanged();
    }


    /**
     * Increases the quantity of a specific item, typically called from outside the adapter (e.g., from suggestions).
     * @param item The item to increase the quantity for.
     */
    public void increaseItem(Item item) {
        if (item == null || item.getName() == null) return;

        String key = item.getName();

        // הוסף ל־itemMap אם עדיין לא קיים
        if (!itemMap.containsKey(key)) {
            itemMap.put(key, item);
        }

        // הוסף ל־quantityMap אם לא קיים
        int currentQuantity = quantityMap.getOrDefault(key, 0) + 1;
        quantityMap.put(key, currentQuantity);

        // הוסף ל־selectedItems
        selectedItems.put(item, currentQuantity);

        // אם לא קיים ב־itemList (רשימת התצוגה), הוסף אותו
        if (!itemList.contains(item)) {
            itemList.add(item);
        }

        addToHistory(item, currentQuantity, "Increased");

        if (onItemChangeListener != null) {
            onItemChangeListener.onItemChanged();
        }

        notifyDataSetChanged();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemQuantity;
        ImageButton btnIncrease, btnDecrease, btnDeleteItem; // Delete button is included here

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemQuantity = itemView.findViewById(R.id.itemQuantity);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
            btnDeleteItem = itemView.findViewById(R.id.btnDeleteItem); // Initialize from layout
        }
    }
}
