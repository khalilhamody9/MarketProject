package com.example.khalilo.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.khalilo.R;
import com.example.khalilo.adapter.ItemAdapter;
import com.example.khalilo.database.AppDatabase;
import com.example.khalilo.entities.Item;
import java.util.List;

public class CategoryItemActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    ItemAdapter adapter;
    List<Item> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String categoryName = getIntent().getStringExtra("categoryName");
        String groupName = getIntent().getStringExtra("groupName"); // Get groupName
        String username = getIntent().getStringExtra("username");   // Get username

        itemList = AppDatabase.getInstance(this).itemDao().getItemsByCategory(categoryName);

        adapter = new ItemAdapter(this, itemList, groupName, username);  // Pass groupName and username

        recyclerView.setAdapter(adapter);
    }
}
