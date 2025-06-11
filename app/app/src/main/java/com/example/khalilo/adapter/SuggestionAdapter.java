package com.example.khalilo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.khalilo.R;
import com.example.khalilo.entities.Item;
import com.example.khalilo.models.History;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {

    private List<History> suggestionList;
    private Context context;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;
    private ApiService apiService;

    public SuggestionAdapter(List<History> suggestionList, Context context, ItemAdapter itemAdapter, List<Item> itemList) {
        this.suggestionList = suggestionList;
        this.context = context;
        this.itemAdapter = itemAdapter;
        this.itemList = itemList;
        this.apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        History history = suggestionList.get(position);
        holder.itemName.setText(history.getItemName());
        holder.itemInfo.setText("Last bought: " + history.getDate());

        // Try to display image if available
        if (history.getImageUrl() != null && history.getImageUrl().startsWith("data:image")) {
            try {
                String base64Image = history.getImageUrl().split(",")[1];
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.itemImage.setImageBitmap(decodedByte);
            } catch (Exception e) {
                holder.itemImage.setImageResource(R.drawable.no_img);
            }
        } else if (history.getImageUrl() != null && !history.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(history.getImageUrl())
                    .placeholder(R.drawable.no_img)
                    .error(R.drawable.no_img)
                    .into(holder.itemImage);
        } else {
            fetchImageForItem(history, holder);
        }

        holder.btnApprove.setOnClickListener(v -> {
            for (Item i : itemList) {
                if (i.getName().equalsIgnoreCase(history.getItemName())) {
                    itemAdapter.increaseItem(i);
                    Toast.makeText(context, i.getName() + " added", Toast.LENGTH_SHORT).show();
                    holder.itemView.setVisibility(View.GONE);
                    return;
                }
            }

            // If item not found locally, fetch from server and mark as selected
            apiService.searchItems(history.getItemName()).enqueue(new Callback<List<Item>>() {
                @Override
                public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        Item fetched = response.body().get(0);

                        fetched.setBought(true); // ✅ Mark as selected
                        itemList.add(fetched);
                        itemAdapter.increaseItem(fetched); // ✅ Increase count
                        itemAdapter.notifyDataSetChanged();

                        Toast.makeText(context, fetched.getName() + " added", Toast.LENGTH_SHORT).show();
                        holder.itemView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(Call<List<Item>> call, Throwable t) {
                    Toast.makeText(context, "Failed to add item", Toast.LENGTH_SHORT).show();
                }
            });
        });

        holder.btnDeny.setOnClickListener(v -> {
            Toast.makeText(context, history.getItemName() + " denied", Toast.LENGTH_SHORT).show();
            holder.itemView.setVisibility(View.GONE);
        });
    }

    private void fetchImageForItem(History history, SuggestionViewHolder holder) {
        apiService.searchItems(history.getItemName()).enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Item item = response.body().get(0);
                    String imageUrl = item.getImg();
                    history.setImageUrl(imageUrl); // update the local object

                    if (imageUrl != null && imageUrl.startsWith("data:image")) {
                        try {
                            String base64Image = imageUrl.split(",")[1];
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            holder.itemImage.setImageBitmap(decodedByte);
                        } catch (Exception e) {
                            holder.itemImage.setImageResource(R.drawable.no_img);
                        }
                    } else if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(context)
                                .load(imageUrl)
                                .placeholder(R.drawable.no_img)
                                .error(R.drawable.no_img)
                                .into(holder.itemImage);
                    } else {
                        holder.itemImage.setImageResource(R.drawable.no_img);
                    }
                } else {
                    holder.itemImage.setImageResource(R.drawable.no_img);
                }
            }

            @Override
            public void onFailure(Call<List<Item>> call, Throwable t) {
                Log.e("SuggestionAdapter", "Failed to fetch image: " + t.getMessage());
                holder.itemImage.setImageResource(R.drawable.no_img);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestionList.size();
    }

    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemInfo;
        Button btnApprove, btnDeny;
        ImageView itemImage;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.suggestedItemName);
            itemInfo = itemView.findViewById(R.id.suggestedItemInfo);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDeny = itemView.findViewById(R.id.btnDeny);
            itemImage = itemView.findViewById(R.id.suggestedItemImage);
        }
    }
}
