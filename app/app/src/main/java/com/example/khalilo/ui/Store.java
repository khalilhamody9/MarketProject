package com.example.khalilo.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class Store extends AppCompatActivity {


    RecyclerView recyclerView;
    ItemAdapter adapter;
    TextView userInfo;
    ImageButton buttonFinish, BackButton;
    Spinner groupSelector;
    ArrayAdapter<String> groupAdapter;
    private boolean hasUnsavedChanges = false;

    List<Item> itemList;
    List<String> groupList = new ArrayList<>();
    AppDatabase db;
    String username, groupName;
    boolean isAdmin = false;
    private boolean isFirstTimeLoading() {
        return getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("csv_loaded", false) == false;
    }

    private void markCsvAsLoaded() {
        getSharedPreferences("prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("csv_loaded", true)
                .apply();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        BackButton = findViewById(R.id.buttonBack2);
        userInfo = findViewById(R.id.userInfo);
        recyclerView = findViewById(R.id.recyclerView);
        buttonFinish = findViewById(R.id.buttonFinish);

        username = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");

        if (username == null || groupName == null) {
            Log.e("Store", "username or groupName is null");
        } else {
            loadSavedItems();
        }

        buttonFinish.setOnClickListener(v -> {
            HashMap<Item, Integer> selectedItems = adapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(Store.this, "No items selected", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Store.this, SelectedItemsActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("groupName", groupName);
                intent.putExtra("selectedItems", selectedItems);
                startActivity(intent);
            }
        });

        BackButton.setOnClickListener(v -> {
            if (hasUnsavedChanges) {
                new AlertDialog.Builder(Store.this)
                        .setTitle("Unsaved Changes")
                        .setMessage("You have unsaved changes. Do you want to exit?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            Intent i = new Intent(Store.this, GroupDetailsActivity.class);
                            i.putExtra("username", username);
                            i.putExtra("groupName", groupName);
                            startActivity(i);
                            finish();
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .setCancelable(false)
                        .show();
            } else {
                Intent i = new Intent(Store.this, GroupDetailsActivity.class);
                i.putExtra("username", username);
                i.putExtra("groupName", groupName);
                startActivity(i);
                finish();
            }
        });

        userInfo.setText(username + ", Group: " + groupName);

        db = AppDatabase.getInstance(this);

        if (isFirstTimeLoading()) {
            new Thread(() -> {
                insertItems();
                markCsvAsLoaded();  // שמור דגל שנטען
                runOnUiThread(() -> {
                    itemList = db.itemDao().getAllItems();
                    adapter = new ItemAdapter(this, itemList, groupName, username);
                    recyclerView.setAdapter(adapter);
                });
            }).start();
        } else {
            itemList = db.itemDao().getAllItems();
            adapter = new ItemAdapter(this, itemList, groupName, username);
            recyclerView.setAdapter(adapter);
        }


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemList = db.itemDao().getAllItems();
        adapter = new ItemAdapter(this, itemList, groupName, username);
        recyclerView.setAdapter(adapter);

        if (getIntent().hasExtra("selectedItems")) {
            HashMap<Item, Integer> restoredItems =
                    (HashMap<Item, Integer>) getIntent().getSerializableExtra("selectedItems");

            if (restoredItems != null) {
                adapter.restoreSelectedItems(restoredItems);
            }
        }
        adapter.setOnItemChangeListener(() -> hasUnsavedChanges = true);
        EditText searchBar = findViewById(R.id.searchBar);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupCategoryButtons();
        fetchRecommendations();
    }

    private void insertItems() {
        try {
            InputStream inputStream = getAssets().open("products.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length >= 5) {
                    String barcode = tokens[0].trim();
                    String category = tokens[6].trim();
                    String name = tokens[3].trim();

                    int imageResId = R.drawable.apple;
                    Item item = new Item(name, imageResId, category, barcode);
                    db.itemDao().insert(item);
                }
            }

            reader.close();
            inputStream.close();
            Toast.makeText(this, "Items loaded from CSV", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Items loaded from CSV", Toast.LENGTH_SHORT).show());
        }
    }
    private void filterItems(String query) {
        List<Item> filteredList = new ArrayList<>();
        for (Item item : itemList) {
            if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
    }




    private void setupCategoryButtons() {
        findViewById(R.id.btnAll).setOnClickListener(v -> adapter.updateList(db.itemDao().getAllItems()));
        findViewById(R.id.btnKitchen).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Kitchen")));
        findViewById(R.id.btnBathroom).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Bathroom")));
        findViewById(R.id.btnGroceries).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Groceries")));
        findViewById(R.id.btnCleaning).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Cleaning")));
        findViewById(R.id.btnBakery).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Bakery")));
        findViewById(R.id.btnBeverages).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Beverages")));
        findViewById(R.id.btnFruits).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Fruits")));
        findViewById(R.id.btnGrains).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Grains")));
        findViewById(R.id.btnMeat).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Meat")));
        findViewById(R.id.btnPantry).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Pantry")));
        findViewById(R.id.btnVegetables).setOnClickListener(v -> adapter.updateList(db.itemDao().getItemsByCategory("Vegetables")));
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

                        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(suggestions, Store.this, adapter, itemList);

                        recyclerView.setLayoutManager(new LinearLayoutManager(Store.this));
                        recyclerView.setAdapter(suggestionAdapter);

                        new AlertDialog.Builder(Store.this)
                                .setTitle("Suggested Items")
                                .setView(dialogView)
                                .setPositiveButton("Close", null)
                                .show();
                    } else {
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
                    HashMap<Item, Integer> restoredItems = new HashMap<>();

                    for (Map.Entry<String, Integer> entry : savedItems.entrySet()) {
                        for (Item item : itemList) {
                            if (item.getName().equals(entry.getKey())) {
                                restoredItems.put(item, entry.getValue());
                                break;
                            }
                        }
                    }

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
                .setNegativeButton("Deny", (dialog, which) -> Toast.makeText(Store.this, "Item denied", Toast.LENGTH_SHORT).show())
                .setCancelable(false)
                .show();
    }
}
