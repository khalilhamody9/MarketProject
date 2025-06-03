package com.example.khalilo.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.adapter.MembersAdapter;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupMembersActivity extends AppCompatActivity {

    RecyclerView recyclerViewMembers;
    MembersAdapter membersAdapter;
    List<String> membersList;
    private String groupName;
    private String username;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_members);
        username = getIntent().getStringExtra("username");

        // Get groupName from Intent
        groupName = getIntent().getStringExtra("groupName");

        // Initialize RecyclerView
        recyclerViewMembers = findViewById(R.id.recyclerViewMembers);
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(this));

        // Back Button
        ImageButton buttonBackMembers = findViewById(R.id.buttonBackMembers);
        buttonBackMembers.setOnClickListener(v -> finish());

        // Load Members Data
        loadMembers();
    }

    private void loadMembers() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<List<String>> call = apiService.getGroupMembers(groupName);

        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    membersList = response.body();
                    membersAdapter = new MembersAdapter(GroupMembersActivity.this, membersList, groupName, username);
                    recyclerViewMembers.setAdapter(membersAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                // Handle failure
            }
        });
    }
}
