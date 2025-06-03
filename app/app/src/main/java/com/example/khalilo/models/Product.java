package com.example.khalilo.models;

import com.google.gson.annotations.SerializedName;

public class Product {
    @SerializedName("שם המוצר")
    private String productName;

    @SerializedName("מחיר")
    private String price;

    @SerializedName("מבצע")
    private String sale;

    public String getProductName() {
        return productName;
    }

    public String getPrice() {
        return price;
    }

    public String getSale() {
        return sale;
    }
}
