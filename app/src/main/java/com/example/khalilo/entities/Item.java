package com.example.khalilo.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "items")
public class Item implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private int price;
    private int pic;
    private String category;
    private boolean isBought; // New Field
    private String actionUser; // New field for the user who performed the action
    private static final long serialVersionUID = 1L;

    public Item(int price, String name, int pic, String category) {
        this.price = price;
        this.name = name;
        this.pic = pic;
        this.category = category;
        this.isBought = false; // Default to not bought
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getPic() {
        return pic;
    }

    public String getCategory() {
        return category;
    }

    public boolean isBought() { // Getter for isBought
        return isBought;
    }

    public void setBought(boolean bought) { // Setter for isBought
        isBought = bought;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPic(int pic) {
        this.pic = pic;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setId(int id) {
        this.id = id;
    }
    public String getActionUser() {
        return actionUser;
    }

    public void setActionUser(String actionUser) {
        this.actionUser = actionUser;
    }
}
