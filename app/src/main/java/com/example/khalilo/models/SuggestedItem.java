package com.example.khalilo.models;

public class SuggestedItem {
    private String itemName;
    private String category;
    private String lastPurchased;
    private int groupsBought;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
// Getters and setters

    public int getGroupsBought() {
        return groupsBought;
    }

    public void setGroupsBought(int groupsBought) {
        this.groupsBought = groupsBought;
    }

    public String getLastPurchased() {
        return lastPurchased;
    }

    public void setLastPurchased(String lastPurchased) {
        this.lastPurchased = lastPurchased;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}