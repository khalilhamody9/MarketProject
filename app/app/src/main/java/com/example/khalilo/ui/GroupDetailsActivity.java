package com.example.khalilo.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.khalilo.CheckGroup;
import com.example.khalilo.R;

public class GroupDetailsActivity extends AppCompatActivity {

    private String username,adminName;
    private String groupName;
    ImageButton btnBack;
    CardView btnAddMembers;
    CardView btnDeleteMembers;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //loadSelectedItems();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        // Get username and groupName from Intent
        username = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");
        //adminName =  getIntent().getStringExtra("adminName");
        // Check if username or groupName is null
        if (username == null || groupName == null) {
            // Handle the case when either is null
            finish(); // Close the activity if required information is missing
            return;
        }

        TextView welcomeText = findViewById(R.id.welcomeText);
        welcomeText.setText("Welcome, " + username);

        // Find the Add Members Card
        btnAddMembers = findViewById(R.id.btnAddMembers);

        // Find the Delete Members Card
        btnDeleteMembers = findViewById(R.id.btnDeleteMembers);

        // Set Click Listener for Adding Members
        btnAddMembers.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailsActivity.this, AddMembersActivity.class);
            intent.putExtra("groupName", groupName);
            intent.putExtra("username", username);
            startActivity(intent);
        });
        CardView btnFavoriteStores = findViewById(R.id.btnFavoriteStores);
        btnFavoriteStores.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailsActivity.this, FavoriteStoresActivity.class);
            intent.putExtra("groupName", groupName);
            intent.putExtra("username", username);
            startActivity(intent);
        });
        // Set Click Listener for Deleting Members
        btnDeleteMembers.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailsActivity.this, DeleteMemberActivity.class);
            intent.putExtra("groupName", groupName);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // Back Button
        btnBack = findViewById(R.id.buttonBack3);
        btnBack.setOnClickListener(v -> {
            Intent i = new Intent(this, CheckGroup.class);
            i.putExtra("username", username); // Pass username back to CheckGroup
            i.putExtra("groupName", groupName);
            startActivity(i);
            finish();
        });

        // Find cards
        CardView btnHistory = findViewById(R.id.btnHistory);
        CardView btnStore = findViewById(R.id.btnStore);
        CardView btnPopularItems = findViewById(R.id.btnPopularItems);
        CardView btnMembers = findViewById(R.id.btnMembers);

        // Set up button actions
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailsActivity.this, HistoryActivity.class);
            intent.putExtra("groupName", groupName);
            intent.putExtra("username", username); // Pass username to HistoryActivity
            startActivity(intent);
        });

        btnStore.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailsActivity.this, Store.class);
            intent.putExtra("username", username); // Pass username to Store
            intent.putExtra("groupName", groupName);
            startActivity(intent);
        });

        btnPopularItems.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailsActivity.this, PopularItemsActivity.class);
            intent.putExtra("groupName", groupName);
            intent.putExtra("username", username); // Pass username to PopularItemsActivity
            startActivity(intent);
        });

        // Members Card
        btnMembers.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailsActivity.this, GroupMembersActivity.class);
            intent.putExtra("groupName", groupName);
            startActivity(intent);
        });
    }
    // Get and Display Selected Items for Group
//    private void loadSelectedItems() {
//        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
//
//        Call<Map<String, HashMap<Item, Integer>>> call = apiService.getSelectedItems(groupName);
//
//        call.enqueue(new Callback<Map<String, HashMap<Item, Integer>>>() {
//            @Override
//            public void onResponse(Call<Map<String, HashMap<Item, Integer>>> call, Response<Map<String, HashMap<Item, Integer>>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    HashMap<Item, Integer> selectedItems = response.body().get("selectedItems");
//                    // Display selected items in the UI
//                    displaySelectedItems(selectedItems);
//                }
//            }
//
//            @Override
//            public void onFailure(Call<Map<String, HashMap<Item, Integer>>> call, Throwable t) {
//                Toast.makeText(GroupDetailsActivity.this, "Failed to load selected items", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

}
