package com.example.khalilo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.khalilo.models.SignUpResponse;
import com.example.khalilo.network.ApiService;
import com.example.khalilo.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Signup extends AppCompatActivity {
    EditText inputEmail, inputUsername, inputPassword, inputConfirmPassword;
    Button btnSignup;
    TextView btnLogin;
    ImageButton ButtonBack;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        inputEmail = findViewById(R.id.inputEmail);
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnSignup = findViewById(R.id.buttonSignup);
        btnLogin = findViewById(R.id.btnLogin);
        ButtonBack=findViewById(R.id.buttonBack);
        ButtonBack.setOnClickListener(v->{
            Intent i = new Intent(this,Login.class);
            startActivity(i);
        });

        // Navigate to Login Activity
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(Signup.this, Login.class);
            startActivity(intent);
            finish();
        });



        // Sign Up Logic
        btnSignup.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String confirmPassword = inputConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(Signup.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(Signup.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(email, username, password);
            }
        });
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void registerUser(String email, String username, String password) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("username", username);
        body.put("password", password);

        Call<SignUpResponse> call = apiService.registerUser(body);
        call.enqueue(new Callback<SignUpResponse>() {
            @Override
            public void onResponse(Call<SignUpResponse> call, Response<SignUpResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getMessage();
                    Toast.makeText(Signup.this, message, Toast.LENGTH_SHORT).show();
                    if (message.equals("khalil")) {
                        Intent intent = new Intent(Signup.this, Login.class);
                        startActivity(intent);
                        finish();
                    }
                    if (message.equals("User registered successfully")) {
                        Intent intent = new Intent(Signup.this, Login.class);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(Signup.this, "Username already exists", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SignUpResponse> call, Throwable t) {
                Toast.makeText(Signup.this, "Server error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
