package com.example.khalilo.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.khalilo.entities.Item;
import java.util.List;

@Dao
public interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Item item);

    @Query("SELECT * FROM items")
    List<Item> getAllItems();

    @Query("SELECT * FROM items WHERE category = :categoryName")
    List<Item> getItemsByCategory(String categoryName);

    @Query("DELETE FROM items")
    void clearItems();
    @Query("UPDATE items SET isBought = :isBought WHERE name = :name")
    void updateItemStatus(String name, boolean isBought);

}
