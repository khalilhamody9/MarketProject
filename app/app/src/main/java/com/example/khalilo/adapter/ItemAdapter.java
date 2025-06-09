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
import android.widget.Toast;

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
    private OnItemChangeListener onItemChangeListener;
    private HashMap<Item, Integer> selectedItems = new HashMap<>();

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

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        if (item == null || item.getName() == null) return;

        String key = item.getName();
        holder.itemName.setText(key);

        // תמונה
        if (item.getImg() != null && item.getImg().startsWith("data:image")) {
            try {
                String base64Image = item.getImg().split(",")[1];
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.itemImage.setImageBitmap(decodedByte);
            } catch (Exception e) {
                holder.itemImage.setImageResource(R.drawable.apple);
            }
        } else if (item.getImg() != null) {
            Glide.with(context)
                    .load(item.getImg())
                    .placeholder(R.drawable.apple)
                    .into(holder.itemImage);
        } else {
            holder.itemImage.setImageResource(R.drawable.apple);
        }

        int quantity = quantityMap.getOrDefault(key, 0);
        holder.itemQuantity.setText(String.valueOf(quantity));

        // לחצן הגדלה
        holder.btnIncrease.setOnClickListener(v -> {
            int currentQuantity = (quantityMap.containsKey(key) ? quantityMap.get(key) : 0) + 1;
            quantityMap.put(key, currentQuantity);
            holder.itemQuantity.setText(String.valueOf(currentQuantity));
            selectedItems.put(item, currentQuantity);

            if (currentQuantity > 0) {
                addToHistory(item, currentQuantity, "Increased");
            }

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
                selectedItems.put(item, currentQuantity);

                addToHistory(item, currentQuantity, "Decreased");

                if (onItemChangeListener != null) {
                    onItemChangeListener.onItemChanged();
                }
            }
        });

    }

    private void addToHistory(Item item, Integer quantity, String action) {
        if (item == null || item.getName() == null || quantity <= 0) {
            return; // אל תשלח לשרת אם הכמות לא תקפה
        }

        Map<String, Object> body = new HashMap<>();
        body.put("itemName", item.getName());
        body.put("imageUrl", item.getImg());
        body.put("action", action);
        body.put("quantity", Integer.valueOf(quantity));        body.put("category", item.getCategory());
        body.put("groupName", groupName);
        body.put("username", username);
        String category = item.getCategory() != null ? item.getCategory() : "Unknown";
        body.put("category", category);

        quantity = (quantity == null ? 0 : quantity);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<Void> call = apiService.addHistory(body);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(context, "Failed to sync history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public HashMap<Item, Integer> getSelectedItems() {
        return selectedItems;
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateList(List<Item> newList) {
        itemList = newList;
        itemMap.clear();
        quantityMap.clear();

        for (Item item : newList) {
            if (item != null && item.getName() != null) {
                itemMap.put(item.getName(), item);
                quantityMap.put(item.getName(), 0);
            }
        }

        notifyDataSetChanged();
    }

    public void restoreSelectedItems(HashMap<Item, Integer> restoredItems) {
        selectedItems.clear();
        quantityMap.clear();

        for (Map.Entry<Item, Integer> entry : restoredItems.entrySet()) {
            Item item = entry.getKey();
            if (item != null && item.getName() != null) {
                selectedItems.put(item, entry.getValue());
                quantityMap.put(item.getName(), entry.getValue());
                itemMap.put(item.getName(), item);
            }
        }

        notifyDataSetChanged();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemQuantity;
        ImageButton btnIncrease, btnDecrease;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemQuantity = itemView.findViewById(R.id.itemQuantity);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
        }
    }
    public void increaseItem(Item item) {
        if (item == null || item.getName() == null) return;

        String key = item.getName();
        int currentQuantity = (quantityMap.containsKey(key) ? quantityMap.get(key) : 0);
        quantityMap.put(key, currentQuantity);
        selectedItems.put(item, currentQuantity);

        if (currentQuantity > 0) {
            addToHistory(item, currentQuantity, "Increased");
        }

        if (onItemChangeListener != null) {
            onItemChangeListener.onItemChanged();
        }

        notifyDataSetChanged();  // Refresh UI
    }
    public interface OnItemChangeListener {
        void onItemChanged();
    }
}
