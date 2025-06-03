package com.example.khalilo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.khalilo.models.GroupResponse;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;
import com.example.khalilo.ui.GroupDetailsActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckGroup extends AppCompatActivity {

    RadioGroup groupChoice;
    RadioButton radioYes, radioNo;
    EditText inputGroupName;
    Button btnContinue, buttonRequestJoin;
    private String username;
    ImageButton buttonBackCheckGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_group);

        groupChoice = findViewById(R.id.groupChoice);
        radioYes = findViewById(R.id.radioYes);
        radioNo = findViewById(R.id.radioNo);
        inputGroupName = findViewById(R.id.inputGroupName);
        btnContinue = findViewById(R.id.btnContinue);
        buttonRequestJoin = findViewById(R.id.buttonRequestJoin);

        // Get the username from Intent
        username = getIntent().getStringExtra("username");

        if (username == null) {
            // Handle missing username
            Toast.makeText(this, "Username is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Hide group name input by default
        inputGroupName.setVisibility(View.GONE);

        // Navigate to GroupActivity if "No" is selected
        groupChoice.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioNo) {
                Intent intent = new Intent(CheckGroup.this, GroupActivity.class);
                intent.putExtra("username", username); // Pass username
                startActivity(intent);
                finish();
            } else if (checkedId == R.id.radioYes) {
                inputGroupName.setVisibility(View.VISIBLE);
            }
        });
        ImageButton buttonBackCheckGroup = findViewById(R.id.buttonBackCheckGroup);
        buttonBackCheckGroup.setOnClickListener(v -> {
            Intent intent = new Intent(CheckGroup.this, Login.class); // or whatever your login screen is
            startActivity(intent);
            finish();
        });

        // Continue Button Click
        btnContinue.setOnClickListener(v -> {
            if (radioYes.isChecked()) {
                String groupName = inputGroupName.getText().toString().trim();
                if (groupName.isEmpty()) {
                    Toast.makeText(CheckGroup.this, "Please enter a group name", Toast.LENGTH_SHORT).show();
                } else {
                    checkUserInGroup(username, groupName);
                }
            } else {
                Toast.makeText(CheckGroup.this, "Please select an option", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserInGroup(String username, String groupName) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Call<GroupResponse> call = apiService.checkUserInGroup(username, groupName);
        call.enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getMessage();
                    if (message.equals("User in group")) {
                        Intent i = new Intent(CheckGroup.this, GroupDetailsActivity.class);
                        i.putExtra("username", username); // Pass username to GroupDetailsActivity
                        i.putExtra("groupName", groupName);
                        startActivity(i);
                        finish();
                    } else {
                        Toast.makeText(CheckGroup.this, "You are not in this group", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CheckGroup.this, "Invalid group name or user", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                Toast.makeText(CheckGroup.this, "Server error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
