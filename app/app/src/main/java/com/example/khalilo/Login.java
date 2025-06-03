package com.example.khalilo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.khalilo.models.LoginResponse;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Login extends AppCompatActivity {
    EditText inputUsername, inputPassword;
    Button btnLogin, btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputUsername = findViewById(R.id.editTextText);
        inputPassword = findViewById(R.id.editTextTextPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        // Sign Up Button Logic
        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Signup.class);
            startActivity(intent);
        });

        // Login Button Logic
        btnLogin.setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(Login.this, "Please enter both fields", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(username, password);
            }
        });
    }

    private void loginUser(String username, String password) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Create request body
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        // Send request
        Call<LoginResponse> call = apiService.loginUser(body);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getMessage();
                    Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();

                    // Check if login was successful
                    if (message.equals("Login successful")) {
                        Intent intent = new Intent(Login.this, CheckGroup.class);
                        intent.putExtra("username", username);  // Pass the username to CheckGroup
                        startActivity(intent);
                        finish();
                    }

                } else {
                    Toast.makeText(Login.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(Login.this, "Server error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
