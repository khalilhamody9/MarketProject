package com.example.khalilo.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

public class Store extends AppCompatActivity {

    RecyclerView recyclerView;
    ItemAdapter adapter;
    TextView userInfo;
    ImageButton buttonFinish, BackButton;
    Spinner groupSelector;
    boolean hasUnsavedChanges = false;

    List<Item> itemList;
    List<String> groupList = new ArrayList<>();
    String username, groupName;

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
        EditText searchBar = findViewById(R.id.searchBar);

        username = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");

        if (username == null || groupName == null) {
            Log.e("Store", "username or groupName is null");
        }

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<List<Item>> call = apiService.getItemsFromFile();

        call.enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    itemList = response.body();
                    // תקן מוצרים בלי קטגוריה
                    for (Item item : itemList) {
                        if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                            item.setCategory("Unknown"); // או כל קטגוריה שתבחר
                            Log.w("MISSING_CATEGORY", "✅ Set default category for item: " + item.getName());
                        }
                    }
                    Log.d("Store", "Items loaded: " + itemList.size());

                    adapter = new ItemAdapter(Store.this, itemList, groupName, username);
                    recyclerView.setAdapter(adapter);

                    if (getIntent().hasExtra("selectedItems")) {
                        HashMap<Item, Integer> restoredItems =
                                (HashMap<Item, Integer>) getIntent().getSerializableExtra("selectedItems");
                        if (restoredItems != null) {
                            adapter.restoreSelectedItems(restoredItems);
                        }
                    }

                    adapter.setOnItemChangeListener(() -> hasUnsavedChanges = true);
                    setupCategoryButtons(); // ✅ חיבור קטגוריות
                } else {
                    Toast.makeText(Store.this, "שגיאה בטעינת נתונים מהשרת", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Item>> call, Throwable t) {
                Toast.makeText(Store.this, "שגיאה בחיבור לשרת", Toast.LENGTH_SHORT).show();
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

        fetchRecommendations();
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
        findViewById(R.id.btnAll).setOnClickListener(v -> adapter.updateList(itemList));
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

    private void filterByCategory(String category) {
        List<Item> filteredList = new ArrayList<>();
        for (Item item : itemList) {
            if (item.getCategory().equalsIgnoreCase(category)) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
    }

    private void fetchRecommendations() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
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
        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(suggestions, Store.this, adapter, itemList);

        recyclerView.setLayoutManager(new LinearLayoutManager(Store.this));
        recyclerView.setAdapter(suggestionAdapter);

        new AlertDialog.Builder(Store.this)
                .setTitle("Suggested Items")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }
}
