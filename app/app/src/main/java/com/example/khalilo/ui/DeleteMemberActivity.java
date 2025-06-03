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

public class DeleteMemberActivity extends AppCompatActivity {

    EditText inputMemberName;
    Button btnDeleteMember;
    ImageButton buttonBackDeleteMember;
    String groupName;
    String adminName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_member);

        groupName = getIntent().getStringExtra("groupName");
        adminName = getIntent().getStringExtra("username");

        inputMemberName = findViewById(R.id.inputMemberName);
        btnDeleteMember = findViewById(R.id.btnDeleteMember);
        buttonBackDeleteMember = findViewById(R.id.buttonBackDeleteMember);

        buttonBackDeleteMember.setOnClickListener(v -> finish());

        btnDeleteMember.setOnClickListener(v -> {
            String memberName = inputMemberName.getText().toString().trim();
            if (!memberName.isEmpty()) {
                deleteMemberFromGroup(memberName);
            } else {
                Toast.makeText(this, "Please enter a member name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMemberFromGroup(String memberName) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Map<String, String> body = new HashMap<>();
        body.put("username", memberName);
        body.put("groupName", groupName);
        body.put("adminName", adminName);

        Call<GroupResponse> call = apiService.deleteUserFromGroup(body);
        call.enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(DeleteMemberActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(DeleteMemberActivity.this, "Failed to delete member", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                Toast.makeText(DeleteMemberActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
