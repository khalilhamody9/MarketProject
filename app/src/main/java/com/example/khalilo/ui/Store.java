package com.example.khalilo.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.adapter.ItemAdapter;
import com.example.khalilo.adapter.SuggestionAdapter;
import com.example.khalilo.database.AppDatabase;
import com.example.khalilo.entities.Item;
import com.example.khalilo.models.History;
import com.example.khalilo.models.RecommendationResponse;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Store extends AppCompatActivity {

    // UI Components
    RecyclerView recyclerView;
    ItemAdapter adapter;
    TextView userInfo;
    ImageButton  buttonFinish,BackButton;
    Spinner groupSelector;
    ArrayAdapter<String> groupAdapter;
    private boolean hasUnsavedChanges = false;

    // Data
    List<Item> itemList;
    List<String> groupList = new ArrayList<>();
    AppDatabase db;
    String username, groupName;
    boolean isAdmin = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        // Initialize UI Components
        BackButton = findViewById(R.id.buttonBack2);
        userInfo = findViewById(R.id.userInfo);
        recyclerView = findViewById(R.id.recyclerView);
        buttonFinish = findViewById(R.id.buttonFinish);
        // Get username and group name from Intent
        username = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");
// Check if username or groupName is null
        if (username == null || groupName == null) {
            Log.e("Store", "username or groupName is null");
        } else {
            loadSavedItems();  // Load saved items after groupName is initialized
        }
        buttonFinish.setOnClickListener(v -> {
            HashMap<Item, Integer> selectedItems = adapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(Store.this, "No items selected", Toast.LENGTH_SHORT).show();
            } else {
                // Navigate to SelectedItemsActivity
                Intent intent = new Intent(Store.this, SelectedItemsActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("groupName", groupName);
                intent.putExtra("selectedItems", selectedItems);
                startActivity(intent);
            }
        });

        // Set up Back Button with Confirmation Dialog
        BackButton.setOnClickListener(v -> {
            if (hasUnsavedChanges) {
                new AlertDialog.Builder(Store.this)
                        .setTitle("Unsaved Changes")
                        .setMessage("You have unsaved changes. Do you want to exit?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Exit and go to GroupDetailsActivity
                            Intent i = new Intent(Store.this, GroupDetailsActivity.class);
                            i.putExtra("username", username);
                            i.putExtra("groupName", groupName);
                            startActivity(i);
                            finish();
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            // Stay on the current page
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();
            } else {
                // No changes made, exit directly
                Intent i = new Intent(Store.this, GroupDetailsActivity.class);
                i.putExtra("username", username);
                i.putExtra("groupName", groupName);
                startActivity(i);
                finish();
            }
        });


        if (username == null || groupName == null) {
            Log.e("Store", "username or groupName is null");
        }

        userInfo.setText(username + ", Group: " + groupName);

        // Initialize Database
        db = AppDatabase.getInstance(this);
        db.itemDao().clearItems();

        // Insert Items into Database
        insertItems();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemList = db.itemDao().getAllItems();
        adapter = new ItemAdapter(this, itemList, groupName, username);
        recyclerView.setAdapter(adapter);


        // Check if returning from SelectedItemsActivity
        if (getIntent().hasExtra("selectedItems")) {
            HashMap<Item, Integer> restoredItems =
                    (HashMap<Item, Integer>) getIntent().getSerializableExtra("selectedItems");

            if (restoredItems != null) {
                adapter.restoreSelectedItems(restoredItems);
            }
        }
        adapter.setOnItemChangeListener(() -> {
            hasUnsavedChanges = true;
        });

        // Set up Category Buttons
        setupCategoryButtons();
        fetchRecommendations();

    }

    private void insertItems() {
        // Groceries
        db.itemDao().insert(new Item(10, "Cola", R.drawable.cola, "Groceries"));
        db.itemDao().insert(new Item(5, "Apple", R.drawable.apple, "Groceries"));
        db.itemDao().insert(new Item(3, "Banana", R.drawable.banana, "Groceries"));
        db.itemDao().insert(new Item(8, "Orange Juice", R.drawable.orange_juice, "Groceries"));
        db.itemDao().insert(new Item(6, "Bread", R.drawable.bread, "Groceries"));
        db.itemDao().insert(new Item(4, "Milk", R.drawable.milk, "Groceries"));
        db.itemDao().insert(new Item(12, "Eggs", R.drawable.eggs, "Groceries"));
        db.itemDao().insert(new Item(20, "Cheese", R.drawable.cheese, "Groceries"));
        db.itemDao().insert(new Item(15, "Cereal", R.drawable.cereal, "Groceries"));
        db.itemDao().insert(new Item(30, "Coffee", R.drawable.coffee, "Groceries"));

        // Kitchen
        db.itemDao().insert(new Item(25, "Frying Pan", R.drawable.frying_pan, "Kitchen"));
        db.itemDao().insert(new Item(15, "Dish Soap", R.drawable.dish_soap, "Kitchen"));
    }

    private void setupCategoryButtons() {
        findViewById(R.id.btnAll).setOnClickListener(v -> {
            itemList = db.itemDao().getAllItems();
            adapter.updateList(itemList);
        });

        findViewById(R.id.btnKitchen).setOnClickListener(v -> {
            itemList = db.itemDao().getItemsByCategory("Kitchen");
            adapter.updateList(itemList);
        });

        findViewById(R.id.btnBathroom).setOnClickListener(v -> {
            itemList = db.itemDao().getItemsByCategory("Bathroom");
            adapter.updateList(itemList);
        });

        findViewById(R.id.btnGroceries).setOnClickListener(v -> {
            itemList = db.itemDao().getItemsByCategory("Groceries");
            adapter.updateList(itemList);
        });
    }
    private void fetchRecommendations() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<RecommendationResponse> call = apiService.getRecommendations(groupName);

        call.enqueue(new Callback<RecommendationResponse>() {
            @Override
            public void onResponse(Call<RecommendationResponse> call, Response<RecommendationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<History> suggestions = response.body().getRecommendations();

                    if (suggestions != null && !suggestions.isEmpty()) {
                        LayoutInflater inflater = LayoutInflater.from(Store.this);
                        View dialogView = inflater.inflate(R.layout.dialog_suggestions, null);
                        RecyclerView recyclerView = dialogView.findViewById(R.id.suggestionsRecyclerView);

                        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(
                                suggestions, Store.this, adapter, itemList);

                        recyclerView.setLayoutManager(new LinearLayoutManager(Store.this));
                        recyclerView.setAdapter(suggestionAdapter);

                        new AlertDialog.Builder(Store.this)
                                .setTitle("Suggested Items")
                                .setView(dialogView)
                                .setPositiveButton("Close", null)
                                .show();
                    }
                    else {
                        Toast.makeText(Store.this, "No suggestions available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Store.this, "No suggestions available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RecommendationResponse> call, Throwable t) {
                Toast.makeText(Store.this, "Failed to fetch suggestions", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void loadSavedItems() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Call<Map<String, Integer>> call = apiService.getSelectedItems(groupName);
        call.enqueue(new Callback<Map<String, Integer>>() {
            @Override
            public void onResponse(Call<Map<String, Integer>> call, Response<Map<String, Integer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> savedItems = response.body();

                    // Convert Map<String, Integer> to HashMap<Item, Integer>
                    HashMap<Item, Integer> restoredItems = new HashMap<>();
                    for (Map.Entry<String, Integer> entry : savedItems.entrySet()) {
                        String itemName = entry.getKey();
                        Integer quantity = entry.getValue();

                        // Find Item by name
                        for (Item item : itemList) {
                            if (item.getName().equals(itemName)) {
                                restoredItems.put(item, quantity);
                                break;
                            }
                        }
                    }

                    // Restore the selected items
                    adapter.restoreSelectedItems(restoredItems);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Integer>> call, Throwable t) {
                Toast.makeText(Store.this, "Failed to load saved items", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showSuggestionDialog(History item) {
        new AlertDialog.Builder(Store.this)
                .setTitle("Suggested Item")
                .setMessage(item.getItemName() + "\nCategory: " + item.getCategory() + "\nLast bought: " + item.getDate())
                .setPositiveButton("Approve", (dialog, which) -> {
                    for (Item i : itemList) {
                        if (i.getName().equalsIgnoreCase(item.getItemName())) {
                            adapter.increaseItem(i);
                            Toast.makeText(Store.this, i.getName() + " added to list", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                })
                .setNegativeButton("Deny", (dialog, which) -> {
                    Toast.makeText(Store.this, "Item denied", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }





}
