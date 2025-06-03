package com.example.khalilo.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "items")
public class Item implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private int pic;
    private String category;
    private boolean isBought;
    private String actionUser;
    private String barcode; // 🔹 שדה חדש לברקוד

    private static final long serialVersionUID = 1L;

    public Item(String name, int pic, String category, String barcode) {
        this.name = name;
        this.pic = pic;
        this.category = category;
        this.barcode = barcode;
        this.isBought = false;
    }

    public int getId() { return id; }

    public String getName() { return name; }

    public int getPic() { return pic; }

    public String getCategory() { return category; }

    public boolean isBought() { return isBought; }

    public void setBought(boolean bought) { isBought = bought; }

    public void setName(String name) { this.name = name; }

    public void setCategory(String category) { this.category = category; }

    public void setPic(int pic) { this.pic = pic; }

    public void setId(int id) { this.id = id; }

    public String getActionUser() { return actionUser; }

    public void setActionUser(String actionUser) { this.actionUser = actionUser; }

    public String getBarcode() { return barcode; }

    public void setBarcode(String barcode) { this.barcode = barcode; }
}
