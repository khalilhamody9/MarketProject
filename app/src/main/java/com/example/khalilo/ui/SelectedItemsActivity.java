package com.example.khalilo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.adapter.SelectedItemsAdapter;
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

public class SelectedItemsActivity extends AppCompatActivity {

    RecyclerView recyclerViewSelectedItems;
    SelectedItemsAdapter selectedItemsAdapter;
    HashMap<Item, Integer> selectedItems;
    ImageButton buttonBackToStore,buttonFinish;
    private String username,groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_items);

        recyclerViewSelectedItems = findViewById(R.id.recyclerViewSelectedItems);
        recyclerViewSelectedItems.setLayoutManager(new LinearLayoutManager(this));

        // Get selected items from Intent
        selectedItems = (HashMap<Item, Integer>) getIntent().getSerializableExtra("selectedItems");
        // Initialize and Set Adapter
        selectedItemsAdapter = new SelectedItemsAdapter(this, selectedItems);
        recyclerViewSelectedItems.setAdapter(selectedItemsAdapter);
        // Initialize and Set Adapter
        // Get username and group name from Intent
        username = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");


        // Find and Set Click Listener for Back Button
        buttonBackToStore = findViewById(R.id.buttonBackToStore);
        buttonBackToStore.setOnClickListener(v -> {
            Intent intent = new Intent(SelectedItemsActivity.this, Store.class);
            intent.putExtra("selectedItems", selectedItems);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        buttonFinish = findViewById(R.id.buttonFinish);
        buttonFinish.setOnClickListener(v -> {
            // Show Confirmation Dialog
            showFinishConfirmation();
        });
    }
// Show Confirmation Dialog
    private void showFinishConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Finish Confirmation")
                .setMessage("Are you sure you want to finish?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    checkIfAdmin(username, groupName, isAdmin -> {
                        if (isAdmin) {
                            // Admin: Clear the list
                            saveGroupChanges(new HashMap<>(selectedItems), true);

                            selectedItems.clear();
                            Toast.makeText(SelectedItemsActivity.this, "All items cleared by admin", Toast.LENGTH_SHORT).show();

                        } else {
                            // Regular User: Save changes
                            saveGroupChanges(selectedItems, false);  // Pass false to save changes
                            Toast.makeText(SelectedItemsActivity.this, "Changes saved", Toast.LENGTH_SHORT).show();
                        }
                        // Navigate to GroupDetailsActivity after saving/clearing
                        fetchPopularFinalizedItems();  // new function below


                        navigateToGroupDetails();
                    });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }




    // Navigate to GroupDetailsActivity
// Navigate to GroupDetailsActivity
    private void navigateToGroupDetails() {
        Intent intent = new Intent(SelectedItemsActivity.this, GroupDetailsActivity.class);
        intent.putExtra("groupName", groupName);
        intent.putExtra("username", username);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    // Navigate to StoreActivity with selected items intact
    private void navigateToStore() {
        Intent intent = new Intent(SelectedItemsActivity.this, Store.class);
        intent.putExtra("selectedItems", selectedItems);
        intent.putExtra("groupName", groupName);
        intent.putExtra("username", username);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    private void saveGroupChanges(HashMap<Item, Integer> selectedItems, boolean clearList) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // If admin is finishing, log each bought item to history
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
                    historyEntry.put("imageUrl", String.valueOf(item.getPic()));
                    historyEntry.put("action", "bought");

                    apiService.addHistory(historyEntry).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {}
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
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

        // These are needed even if clearing the list
        Map<String, Integer> itemsMap = new HashMap<>();
        Map<String, String> imageUrls = new HashMap<>();
        Map<String, String> categories = new HashMap<>();

        for (Map.Entry<Item, Integer> entry : selectedItems.entrySet()) {
            Item item = entry.getKey();
            itemsMap.put(item.getName(), entry.getValue());
            imageUrls.put(item.getName(), String.valueOf(item.getPic()));
            categories.put(item.getName(), item.getCategory());
        }

        body.put("selectedItems", itemsMap);
        body.put("imageUrls", imageUrls);
        body.put("categories", categories);

        Call<Void> call = apiService.saveSelectedItems(body);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SelectedItemsActivity.this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SelectedItemsActivity.this, "Failed to save changes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(SelectedItemsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Check if User is Admin
// Check if User is Admin
    private void fetchPopularFinalizedItems() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<List<History>> call = apiService.getPopularItemsByGroup(groupName); // already points to finalized

        call.enqueue(new Callback<List<History>>() {
            @Override
            public void onResponse(Call<List<History>> call, Response<List<History>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(SelectedItemsActivity.this, "Popular items updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SelectedItemsActivity.this, "Failed to load finalized popular items", Toast.LENGTH_SHORT).show();
                }

                navigateToGroupDetails();
            }

            @Override
            public void onFailure(Call<List<History>> call, Throwable t) {
                Toast.makeText(SelectedItemsActivity.this, "Error fetching finalized popular items", Toast.LENGTH_SHORT).show();
                navigateToGroupDetails(); // still navigate
            }
        });
    }

    private void checkIfAdmin(String username, String groupName, AdminCheckCallback callback) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Call<Map<String, Boolean>> call = apiService.checkIfAdmin(username, groupName);

        call.enqueue(new Callback<Map<String, Boolean>>() {
            @Override
            public void onResponse(Call<Map<String, Boolean>> call, Response<Map<String, Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isAdmin = response.body().get("isAdmin");
                    callback.onResult(isAdmin);
                } else {
                    callback.onResult(false);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Boolean>> call, Throwable t) {
                callback.onResult(false);
            }
        });
    }




}
