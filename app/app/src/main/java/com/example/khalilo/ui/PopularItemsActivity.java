package com.example.khalilo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PopularItemsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SelectedItemsAdapter adapter;
    private String groupName;
    ImageButton buttonBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_items);

        groupName = getIntent().getStringExtra("groupName");

        recyclerView = findViewById(R.id.popularRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadPopularItems();
        ImageButton buttonBack = findViewById(R.id.buttonBackPopular);
        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(PopularItemsActivity.this, GroupDetailsActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            intent.putExtra("groupNumber", getIntent().getStringExtra("groupNumber"));
            startActivity(intent);
            finish();
        });

    }

    private void loadPopularItems() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<List<History>> call = apiService.getPopularItemsByGroup(groupName);

        call.enqueue(new Callback<List<History>>() {
            @Override
            public void onResponse(Call<List<History>> call, Response<List<History>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HashMap<Item, Integer> popularMap = new HashMap<>();
                    for (History history : response.body()) {
                        String imageUrl = history.getImageUrl();  // שדה חדש שמגיע מהשרת
                        long barcode = history.getBarcode(); // ודא שזה קיים באובייקט History
                        Item item = new Item(history.getItemName(), imageUrl, history.getCategory(), barcode);
                        popularMap.put(item, history.getQuantity());
                    }

                    adapter = new SelectedItemsAdapter(PopularItemsActivity.this, popularMap);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(PopularItemsActivity.this, "No popular items found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<History>> call, Throwable t) {
                Toast.makeText(PopularItemsActivity.this, "Failed to load popular items: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private int getImageByName(String name) {
        int resId = getResources().getIdentifier(name, "drawable", getPackageName());
        return resId == 0 ? android.R.drawable.ic_menu_report_image : resId;
    }
}
