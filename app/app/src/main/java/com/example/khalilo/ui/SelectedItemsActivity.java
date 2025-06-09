package com.example.khalilo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.adapter.SelectedItemsAdapter;
import com.example.khalilo.entities.Item;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectedItemsActivity extends AppCompatActivity {

    RecyclerView recyclerViewSelectedItems;
    SelectedItemsAdapter selectedItemsAdapter;
    HashMap<Item, Integer> selectedItems;
    ImageButton buttonBackToStore, buttonFinish;
    private String username, groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_items);

        recyclerViewSelectedItems = findViewById(R.id.recyclerViewSelectedItems);
        recyclerViewSelectedItems.setLayoutManager(new LinearLayoutManager(this));

        selectedItems = (HashMap<Item, Integer>) getIntent().getSerializableExtra("selectedItems");
        selectedItemsAdapter = new SelectedItemsAdapter(this, selectedItems);
        recyclerViewSelectedItems.setAdapter(selectedItemsAdapter);

        username = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");

        buttonBackToStore = findViewById(R.id.buttonBackToStore);
        buttonBackToStore.setOnClickListener(v -> {
            Intent intent = new Intent(SelectedItemsActivity.this, Store.class);
            intent.putExtra("selectedItems", selectedItems);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        buttonFinish = findViewById(R.id.buttonFinish);
        buttonFinish.setOnClickListener(v -> showFinishConfirmation());
    }

    private void showFinishConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Finish Confirmation")
                .setMessage("Are you sure you want to finish?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    checkIfAdmin(username, groupName, isAdmin -> {
                        if (isAdmin) {
                            saveGroupChanges(new HashMap<>(selectedItems), true);
                            runScraper(groupName, selectedItems);
                            Toast.makeText(this, "All items cleared by admin", Toast.LENGTH_SHORT).show();
                        } else {
                            saveGroupChanges(selectedItems, false);
                            Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();
                        }

                        navigateToComparePrices();  // ✅ moved before clear
                    });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void navigateToComparePrices() {
        Intent intent = new Intent(this, ComparePricesActivity.class);
        intent.putExtra("selectedItems", selectedItems);  // 🟩 Still intact here
        intent.putExtra("username", username);
        intent.putExtra("groupName", groupName);
        startActivity(intent);
        finish();
    }

    private void runScraper(String groupName, HashMap<Item, Integer> selectedItems) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Map<String, Object> body = new HashMap<>();
        body.put("city", "תל אביב");

        List<String> products = new ArrayList<>();
        for (Item item : selectedItems.keySet()) {
            if (selectedItems.get(item) > 0) {
                products.add(item.getName().trim());
            }
        }
        body.put("products", products);

        apiService.runScraping(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string(); // התשובה כטקסט
                        Log.d("SCRAPE_RESULT", "📦 JSON: " + json);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("SCRAPE", "❌ " + t.getMessage());
            }
        });

    }

    private void saveGroupChanges(HashMap<Item, Integer> selectedItems, boolean clearList) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        if (clearList) {
            for (Map.Entry<Item, Integer> entry : selectedItems.entrySet()) {
                Item item = entry.getKey();
                int quantity = entry.getValue();
                if (quantity > 0) {
                    Map<String, Object> historyEntry = new HashMap<>();
                    historyEntry.put("itemName", item.getName());
                    historyEntry.put("quantity", quantity);
                    historyEntry.put("category", item.getCategory());
                    historyEntry.put("username", username);
                    historyEntry.put("groupName", groupName);
                    historyEntry.put("imageUrl", item.getImg());
                    historyEntry.put("action", "bought");

                    apiService.addHistory(historyEntry).enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                        @Override public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(SelectedItemsActivity.this, "Failed to log bought item", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("groupName", groupName);
        body.put("username", username);
        body.put("clearList", clearList);

        Map<String, Integer> itemsMap = new HashMap<>();
        Map<String, String> imageUrls = new HashMap<>();
        Map<String, String> categories = new HashMap<>();

        for (Map.Entry<Item, Integer> entry : selectedItems.entrySet()) {
            Item item = entry.getKey();
            itemsMap.put(item.getName(), entry.getValue());
            imageUrls.put(item.getName(), item.getImg());
            categories.put(item.getName(), item.getCategory());
        }

        body.put("selectedItems", itemsMap);
        body.put("imageUrls", imageUrls);
        body.put("categories", categories);

        apiService.saveSelectedItems(body).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SelectedItemsActivity.this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SelectedItemsActivity.this, "Failed to save changes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(SelectedItemsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfAdmin(String username, String groupName, AdminCheckCallback callback) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.checkIfAdmin(username, groupName).enqueue(new Callback<Map<String, Boolean>>() {
            @Override public void onResponse(Call<Map<String, Boolean>> call, Response<Map<String, Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(response.body().get("isAdmin"));
                } else {
                    callback.onResult(false);
                }
            }

            @Override public void onFailure(Call<Map<String, Boolean>> call, Throwable t) {
                callback.onResult(false);
            }
        });
    }
}
