package com.example.khalilo.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
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
import com.example.khalilo.entities.Item;
import com.example.khalilo.models.History;
import com.example.khalilo.models.RecommendationResponse;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Implement the delete listener interface
public class Store extends AppCompatActivity implements ItemAdapter.OnItemDeleteListener {

    RecyclerView recyclerView;
    ItemAdapter adapter;
    TextView userInfo;
    ImageButton buttonFinish, BackButton;
    Spinner groupSelector;
    boolean hasUnsavedChanges = false;

    // This is the master list of all items from the server
    List<Item> masterItemList = new ArrayList<>();
    List<String> groupList = new ArrayList<>();
    String username, groupName;
    EditText searchBar;

    private int offset = 0;
    private final int limit = 50;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private ApiService apiService;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        BackButton = findViewById(R.id.buttonBack2);
        userInfo = findViewById(R.id.userInfo);
        buttonFinish = findViewById(R.id.buttonFinish);
        searchBar = findViewById(R.id.searchBar);

        username = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Initialize the adapter with the master list
        adapter = new ItemAdapter(Store.this, masterItemList, groupName, username);
        recyclerView.setAdapter(adapter);

        // Set the listeners
        adapter.setOnItemChangeListener(() -> hasUnsavedChanges = true);
        adapter.setOnItemDeleteListener(this); // Set the delete listener to this activity

        loadItemsPaginated(offset, limit);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastVisibleItemPosition() >= masterItemList.size() - 10 && !isLoading && !isLastPage) {
                    loadItemsPaginated(offset, limit);
                }
            }
        });

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

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

//        setupCategoryButtons();
        fetchRecommendations();
    }

    // This is the implementation of the delete listener method
    @Override
    public void onItemDelete(Item item) {
        // Remove the item from the master list to ensure it doesn't reappear
        masterItemList.remove(item);

        // After removing from the master list, re-apply the current filter
        // to update the UI correctly.
        String currentQuery = searchBar.getText().toString();
        filterItems(currentQuery); // This will call adapter.updateList with the correct filtered list

        Toast.makeText(this, item.getName() + " removed", Toast.LENGTH_SHORT).show();
        hasUnsavedChanges = true; // Mark that changes have been made
    }

    private void loadItemsPaginated(int offset, int limit) {
        if (isLoading || isLastPage) return;
        isLoading = true;

        apiService.getPaginatedItems(offset, limit).enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<Item> newItems = response.body();

                    for (Item item : newItems) {
                        if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                            item.setCategory("Unknown");
                        }
                    }

                    masterItemList.addAll(newItems);
                    // Update the adapter with the full, unfiltered list
                    adapter.updateList(masterItemList);

                    if (newItems.size() < limit) {
                        isLastPage = true;
                    } else {
                        Store.this.offset += limit;
                    }
                } else {
                    Toast.makeText(Store.this, "שגיאה בטעינת מוצרים", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Item>> call, Throwable t) {
                isLoading = false;
                Toast.makeText(Store.this, "שגיאה בחיבור לשרת", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterItems(String query) {
        List<Item> filteredList = new ArrayList<>();
        for (Item item : masterItemList) {
            if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }

        if (filteredList.isEmpty() && !query.trim().isEmpty()) {
            // קריאה לשרת רק אם אין תוצאות מקומיות
            searchItemFromServer(query);
        } else {
            adapter.updateList(filteredList);
        }
    }

    private void searchItemFromServer(String query) {
        apiService.searchItems(query).enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Item> serverResults = response.body();
                    // אופציונלי: הוסף לתוך masterItemList כדי שלא תבצע שוב את אותה בקשה
                    for (Item item : serverResults) {
                        if (!masterItemList.contains(item)) {
                            masterItemList.add(item);
                        }
                    }
                    adapter.updateList(serverResults);
                } else {
                    Toast.makeText(Store.this, "לא נמצאו תוצאות מהשרת", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Item>> call, Throwable t) {
                Toast.makeText(Store.this, "שגיאה בטעינת תוצאות מהשרת", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void filterByCategory(String category) {
        List<Item> filteredList = new ArrayList<>();
        // Always filter from the master list
        for (Item item : masterItemList) {
            if (item.getCategory() != null && item.getCategory().equalsIgnoreCase(category)) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
        searchBar.setText(""); // Clear search when a category is selected
    }

    private void setupCategoryButtons() {
        // When "All" is clicked, clear filters and show the master list
        findViewById(R.id.btnAll).setOnClickListener(v -> {
            adapter.updateList(masterItemList);
            searchBar.setText("");
        });
        findViewById(R.id.btnKitchen).setOnClickListener(v -> filterByCategory("Kitchen"));
        findViewById(R.id.btnBathroom).setOnClickListener(v -> filterByCategory("Bathroom"));
        findViewById(R.id.btnGroceries).setOnClickListener(v -> filterByCategory("Groceries"));
        findViewById(R.id.btnCleaning).setOnClickListener(v -> filterByCategory("Cleaning"));
        findViewById(R.id.btnBakery).setOnClickListener(v -> filterByCategory("Bakery"));
        findViewById(R.id.btnBeverages).setOnClickListener(v -> filterByCategory("Beverages"));
        findViewById(R.id.btnFruits).setOnClickListener(v -> filterByCategory("Fruits"));
        findViewById(R.id.btnGrains).setOnClickListener(v -> filterByCategory("Grains"));
        findViewById(R.id.btnMeat).setOnClickListener(v -> filterByCategory("Meat"));
        findViewById(R.id.btnPantry).setOnClickListener(v -> filterByCategory("Pantry"));
        findViewById(R.id.btnVegetables).setOnClickListener(v -> filterByCategory("Vegetables"));
    }

    private void fetchRecommendations() {
        Call<RecommendationResponse> call = apiService.getSmartRecommendations(username);
        call.enqueue(new Callback<RecommendationResponse>() {
            @Override
            public void onResponse(Call<RecommendationResponse> call, Response<RecommendationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<History> suggestions = response.body().getRecommendations();
                    if (suggestions != null && !suggestions.isEmpty()) {
                        showSuggestionsDialog(suggestions);
                    }
                }
            }

            @Override
            public void onFailure(Call<RecommendationResponse> call, Throwable t) {
                Toast.makeText(Store.this, "Failed to fetch suggestions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuggestionsDialog(List<History> suggestions) {
        View dialogView = LayoutInflater.from(Store.this).inflate(R.layout.dialog_suggestions, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.suggestionsRecyclerView);
        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(suggestions, Store.this, adapter, masterItemList);

        recyclerView.setLayoutManager(new LinearLayoutManager(Store.this));
        recyclerView.setAdapter(suggestionAdapter);

        new AlertDialog.Builder(Store.this)
                .setTitle("Suggested Items")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }
}
