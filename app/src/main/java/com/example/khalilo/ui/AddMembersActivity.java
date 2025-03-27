package com.example.khalilo.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.khalilo.R;
import com.example.khalilo.models.GroupResponse;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMembersActivity extends AppCompatActivity {

    EditText inputMemberName;
    Button btnAddMember;
    ImageButton btnBack;
    String groupName;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_members);

        // Get data from intent
        groupName = getIntent().getStringExtra("groupName");
        username = getIntent().getStringExtra("username");

        // Find views
        inputMemberName = findViewById(R.id.inputMemberName);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnBack = findViewById(R.id.buttonBackCheckGroup);

        // Add member logic
        btnAddMember.setOnClickListener(v -> {
            String memberName = inputMemberName.getText().toString().trim();
            if (!memberName.isEmpty()) {
                addMemberToGroup(memberName);
            } else {
                Toast.makeText(AddMembersActivity.this, "Please enter a member name", Toast.LENGTH_SHORT).show();
            }
        });

        // Back button logic
        btnBack.setOnClickListener(v -> {
            finish(); // go back to previous activity
        });
    }

    private void addMemberToGroup(String memberName) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Map<String, String> body = new HashMap<>();
        body.put("username", memberName);
        body.put("groupName", groupName);
        body.put("adminName", username);

        Call<GroupResponse> call = apiService.addUserToGroup(body);
        call.enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getMessage();
                    Toast.makeText(AddMembersActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after successful addition
                } else {
                    Toast.makeText(AddMembersActivity.this, "Failed to add member", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                Toast.makeText(AddMembersActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
