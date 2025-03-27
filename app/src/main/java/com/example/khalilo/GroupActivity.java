package com.example.khalilo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.khalilo.models.GroupResponse;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupActivity extends AppCompatActivity {

    ImageButton buttonBack;
    EditText inputGroupName, inputMaxUsers, inputAdminName;
    Button btnCreateGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        buttonBack = findViewById(R.id.buttonBack);
        inputGroupName = findViewById(R.id.inputGroupName);
        inputMaxUsers = findViewById(R.id.inputMaxUsers);
        inputAdminName = findViewById(R.id.inputAdminName);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);

        // Get the username from the intent and set it as the admin name
        String adminName = getIntent().getStringExtra("username");
        inputAdminName.setText(adminName);
        inputAdminName.setEnabled(false);  // Make it non-editable

        buttonBack.setOnClickListener(v -> {
            Intent i = new Intent(this, CheckGroup.class);
            i.putExtra("username", adminName); // Pass the username back
            startActivity(i);
            finish();
        });

        btnCreateGroup.setOnClickListener(v -> {
            String groupName = inputGroupName.getText().toString().trim();
            String maxUsers = inputMaxUsers.getText().toString().trim();

            if (groupName.isEmpty() || maxUsers.isEmpty()) {
                Toast.makeText(GroupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                createGroup(groupName, maxUsers, adminName);
            }
        });
    }


    // Method to Create Group
    private void createGroup(String groupName, String maxUsers, String adminName) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Map<String, String> body = new HashMap<>();
        body.put("groupName", groupName);
        body.put("maxUsers", maxUsers);
        body.put("adminName", adminName);

        Call<GroupResponse> call = apiService.createGroup(body);
        call.enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getMessage();
                    Toast.makeText(GroupActivity.this, message, Toast.LENGTH_SHORT).show();
                    if (message.equals("Group created successfully")) {
                        // Add group to admin's details and return to Store page
                        addGroupToAdminDetails(groupName, adminName);
                        Intent intent = new Intent(GroupActivity.this, CheckGroup.class);
                        intent.putExtra("username", adminName);
                        intent.putExtra("groupName", groupName);
                        startActivity(intent);
                        finish();
                    } else if (message.equals("Group name already exists")) {
                        Toast.makeText(GroupActivity.this, "Group name already exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(GroupActivity.this, "Server error or invalid response", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Server error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Add Group to Admin Details and Reload Store Page
    private void addGroupToAdminDetails(String groupName, String adminName) {
        // Create Request Body
        Map<String, String> body = new HashMap<>();
        body.put("username", adminName);
        body.put("groupName", groupName);

        // Create API Service
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Call the API to add the group to admin's details
        Call<GroupResponse> call = apiService.addUserToGroup(body);
        call.enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getMessage();
                    Toast.makeText(GroupActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GroupActivity.this, "Failed to add group to user details.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}
