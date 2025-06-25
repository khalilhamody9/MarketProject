package com.example.khalilo.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.adapter.FavoriteStoresAdapter;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteStoresActivity extends AppCompatActivity {

    private String groupName, username;
    private RecyclerView recyclerViewFavorites;
    private Button btnAddStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_stores);

        groupName = getIntent().getStringExtra("groupName");
        username = getIntent().getStringExtra("username");

        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this)); // ✅ חובה להוספת LayoutManager

        btnAddStore = findViewById(R.id.btnAddStore);

        loadFavoriteStores();

        btnAddStore.setOnClickListener(v -> showStoreSelectionDialog());

        ImageButton btnBack = findViewById(R.id.buttonBack3);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteStoresActivity.this, GroupDetailsActivity.class);
            intent.putExtra("groupName", groupName);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });
    }

    private void loadFavoriteStores() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getFavoriteStores(groupName).enqueue(new Callback<Map<String, List<String>>>() {
            @Override
            public void onResponse(Call<Map<String, List<String>>> call, Response<Map<String, List<String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> favorites = response.body().get("favoriteStores");
                    if (favorites != null) {
                        showFavoritesInRecycler(favorites);
                    }
                } else {
                    Toast.makeText(FavoriteStoresActivity.this, "שגיאה בטעינת חנויות אהובות", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<String>>> call, Throwable t) {
                Toast.makeText(FavoriteStoresActivity.this, "שגיאת רשת: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFavoritesInRecycler(List<String> stores) {
        FavoriteStoresAdapter adapter = new FavoriteStoresAdapter(this, stores);
        recyclerViewFavorites.setAdapter(adapter);
    }

    private void showStoreSelectionDialog() {
        String[] allStores = {
                "שופרסל דיל", "רמי לוי", "ויקטורי", "יוחננוף", "חצי חינם", "מחסני השוק", "אושר עד"
        };

        new AlertDialog.Builder(this)
                .setTitle("בחר חנות להוספה למועדפים")
                .setItems(allStores, (dialog, which) -> {
                    String selectedStore = allStores[which];
                    addStoreToFavorites(selectedStore);
                })
                .show();
    }

    private void addStoreToFavorites(String shopName) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("shopName", shopName);

        apiService.addFavoriteStore(groupName, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FavoriteStoresActivity.this, "✅ החנות נוספה למועדפים", Toast.LENGTH_SHORT).show();
                    loadFavoriteStores();
                } else {
                    Toast.makeText(FavoriteStoresActivity.this, "❌ שגיאה בהוספה", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(FavoriteStoresActivity.this, "שגיאת רשת: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
