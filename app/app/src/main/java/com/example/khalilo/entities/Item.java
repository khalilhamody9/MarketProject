package com.example.khalilo.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "items")
public class Item implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String img;
    private String category;
    private boolean isBought;
    private String actionUser;
    private long barcode;


    private static final long serialVersionUID = 1L;

    public Item(String name, String img, String category, long barcode) {
        this.name = name;
        this.img = img;
        this.category = category;
        this.barcode = barcode;
        this.isBought = false;
    }

    public int getId() { return id; }

    public String getName() { return name; }

    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }
    public String getCategory() { return category; }

    public boolean isBought() { return isBought; }

    public void setBought(boolean bought) { isBought = bought; }

    public void setName(String name) { this.name = name; }

    public void setCategory(String category) { this.category = category; }


    public void setId(int id) { this.id = id; }

    public String getActionUser() { return actionUser; }

    public void setActionUser(String actionUser) { this.actionUser = actionUser; }

    public long getBarcode() {
        return barcode;
    }

    public void setBarcode(long barcode) {
        this.barcode = barcode;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Item item = (Item) o;

        // השוואה לפי שם בלבד
        return name != null ? name.equals(item.name) : item.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
