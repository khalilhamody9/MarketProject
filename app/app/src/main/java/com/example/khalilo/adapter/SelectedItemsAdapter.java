package com.example.khalilo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.khalilo.R;
import com.example.khalilo.entities.Item;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        // טען שם וכמות
        holder.itemName.setText(item.getName());
        holder.itemQuantity.setText("Quantity: " + selectedItems.get(item));

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
            // ✅ הצג מיד no_img כברירת מחדל
            holder.itemImage.setImageResource(R.drawable.no_img);

            // ⏳ במקביל נסה למשוך תמונה מהשרת
            ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
            apiService.searchItems(item.getName()).enqueue(new Callback<List<Item>>() {
                @Override
                public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        Item fetchedItem = response.body().get(0);
                        item.setImg(fetchedItem.getImg());
                        notifyItemChanged(holder.getAdapterPosition());
                    }
                }

                @Override
                public void onFailure(Call<List<Item>> call, Throwable t) {
                    // כבר מוצגת תמונת no_img, אין צורך בשום פעולה
                }
            });
        }



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
