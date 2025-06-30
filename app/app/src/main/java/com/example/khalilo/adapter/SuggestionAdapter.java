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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {

    private List<History> suggestionList;
    private Context context;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;
    private ApiService apiService;
    private Map<History, Boolean> handledMap = new HashMap<>();

    public SuggestionAdapter(List<History> suggestionList, Context context, ItemAdapter itemAdapter, List<Item> itemList) {
        this.suggestionList = suggestionList;
        this.context = context;
        this.itemAdapter = itemAdapter;
        this.itemList = itemList;
        this.apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        for (History h : suggestionList) {
            handledMap.put(h, false);
        }
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
        holder.itemScore.setText("Score: " + String.format("%.3f", history.getScore()));

        loadImage(holder.itemImage, history);

        holder.btnApprove.setOnClickListener(v -> {
            handledMap.put(history, true);
            for (Item i : itemList) {
                if (i.getName().equalsIgnoreCase(history.getItemName())) {
                    itemAdapter.increaseItem(i);
                    Toast.makeText(context, i.getName() + " added", Toast.LENGTH_SHORT).show();
                    holder.itemView.setVisibility(View.GONE);
                    increaseScore(history.getItemName());
                    return;
                }
            }

            // אם לא קיים - נחפש מהשרת ונוסיף
            apiService.searchItems(history.getItemName()).enqueue(new Callback<List<Item>>() {
                @Override
                public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        Item fetched = response.body().get(0);
                        fetched.setBought(true);
                        itemList.add(fetched);
                        itemAdapter.increaseItem(fetched);
                        itemAdapter.notifyDataSetChanged();

                        Toast.makeText(context, fetched.getName() + " added", Toast.LENGTH_SHORT).show();
                        holder.itemView.setVisibility(View.GONE);
                        increaseScore(history.getItemName());
                    }
                }

                @Override
                public void onFailure(Call<List<Item>> call, Throwable t) {
                    Toast.makeText(context, "Failed to add item", Toast.LENGTH_SHORT).show();
                }
            });
        });

        holder.btnDeny.setOnClickListener(v -> {
            handledMap.put(history, true);
            Toast.makeText(context, history.getItemName() + " denied", Toast.LENGTH_SHORT).show();
            holder.itemView.setVisibility(View.GONE);
            decreaseScore(history.getItemName());
        });
    }

    private void loadImage(ImageView imageView, History history) {
        String imageUrl = history.getImageUrl();

        if (imageUrl != null && imageUrl.startsWith("data:image")) {
            try {
                String base64Image = imageUrl.split(",")[1];
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageView.setImageBitmap(decodedByte);
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.no_img);
            }
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.no_img)
                    .error(R.drawable.no_img)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.no_img);
        }
    }

    private void increaseScore(String itemName) {
        Log.d("ScoreUpdate", "📤 Sending increase score for: " + itemName);
        Map<String, String> body = new HashMap<>();
        body.put("itemName", itemName);
        apiService.increaseRecommendationScore(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("ScoreUpdate", "✅ Response for increase: " + response.code());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("ScoreUpdate", "❌ Failed to increase score", t);
            }
        });
    }

    private void decreaseScore(String itemName) {
        Map<String, String> body = new HashMap<>();
        body.put("itemName", itemName);
        apiService.decreaseRecommendationScore(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("ScoreUpdate", "Decreased score for: " + itemName);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("ScoreUpdate", "Failed to decrease score for: " + itemName, t);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestionList.size();
    }

    public Map<History, Boolean> getHandledMap() {
        return handledMap;
    }

    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemInfo, itemScore;
        Button btnApprove, btnDeny;
        ImageView itemImage;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.suggestedItemName);
            itemInfo = itemView.findViewById(R.id.suggestedItemInfo);
            itemScore = itemView.findViewById(R.id.suggestedItemScore);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDeny = itemView.findViewById(R.id.btnDeny);
            itemImage = itemView.findViewById(R.id.suggestedItemImage);
        }
    }
}
