package com.example.khalilo.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.adapter.HistoryAdapter;
import com.example.khalilo.models.History;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    HistoryAdapter historyAdapter;
    List<History> historyList;
    private String groupName;
    private String username;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("HistoryPrefs", MODE_PRIVATE);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        groupName = getIntent().getStringExtra("groupName");
        username = getIntent().getStringExtra("username");

        ImageButton buttonBackHistory = findViewById(R.id.buttonBackHistory);
        buttonBackHistory.setOnClickListener(v -> {
            Intent i = new Intent(this, GroupDetailsActivity.class);
            i.putExtra("username", username);
            i.putExtra("groupName", groupName);
            startActivity(i);
            finish();
        });

        // Load History Data
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<List<History>> call = apiService.getHistoryByGroup(groupName);

        call.enqueue(new Callback<List<History>>() {
            @Override
            public void onResponse(Call<List<History>> call, Response<List<History>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historyList = response.body();

                    // Initialize the adapter and set it to RecyclerView
                    historyAdapter = new HistoryAdapter(HistoryActivity.this, historyList, groupName, username);
                    recyclerView.setAdapter(historyAdapter);

                    // Apply saved sorting order
                    String savedSortOrder = sharedPreferences.getString("sortOrder", "Ascending");
                    sortHistoryList(savedSortOrder);
                } else {
                    Toast.makeText(HistoryActivity.this, "Failed to retrieve history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<History>> call, Throwable t) {
                Toast.makeText(HistoryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize Spinner for Sorting
        Spinner sortSpinner = findViewById(R.id.sortSpinner);
        String[] sortOptions = {"Ascending", "Descending", "Date (Newest First)", "Date (Oldest First)"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_hidden_selected, // hides selected item
                sortOptions
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        // Load saved sorting preference (Default is "Ascending")
        String savedSortOrder = sharedPreferences.getString("sortOrder", "Ascending");
        int defaultPosition = savedSortOrder.equals("Ascending") ? 0 : 1;
        sortSpinner.setSelection(defaultPosition);

        // Spinner selection listener
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sortOrder = parent.getItemAtPosition(position).toString();

                // Save sorting preference
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("sortOrder", sortOrder);
                editor.apply();

                // Sort and update the list
                sortHistoryList(sortOrder);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // Sorting Method
    private void sortHistoryList(String sortOrder) {
        if (historyList == null) return; // 🚨 Prevent crash if historyList not ready yet

        if (sortOrder.equals("Ascending")) {
            Collections.sort(historyList, (o1, o2) -> o1.getItemName().compareTo(o2.getItemName()));
        } else if (sortOrder.equals("Descending")) {
            Collections.sort(historyList, (o1, o2) -> o2.getItemName().compareTo(o1.getItemName()));
        } else if (sortOrder.equals("Date (Newest First)")) {
            Collections.sort(historyList, (o1, o2) -> o2.getDate().compareTo(o1.getDate()));
        } else if (sortOrder.equals("Date (Oldest First)")) {
            Collections.sort(historyList, (o1, o2) -> o1.getDate().compareTo(o2.getDate()));
        }

        if (historyAdapter != null) {
            historyAdapter.notifyDataSetChanged();
        }
    }
}
