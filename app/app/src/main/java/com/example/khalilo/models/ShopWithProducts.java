package com.example.khalilo.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ShopWithProducts {
    private String shopKey;
    private double total_price;
    private List<Product> products;

    public static class Product {
        @SerializedName("שם המוצר")
        private String name;

        @SerializedName("מחיר")
        private String price;

        @SerializedName("מבצע")
        private String sale;

        // ✅ קונסטרקטור עם פרמטרים
        public Product(String name, String price, String sale) {
            this.name = name;
            this.price = price;
            this.sale = sale;
        }

        // ✅ קונסטרקטור ריק – חובה לספריית Gson
        public Product() {}

        // Getters
        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getSale() { return sale; }
    }

    public String getShopKey() { return shopKey; }
    public double getTotal_price() { return total_price; }
    public List<Product> getProducts() { return products; }

    public void setShopKey(String key) { this.shopKey = key; }
    public void setTotalPrice(double price) { this.total_price = price; }
    public void setProducts(List<Product> products) { this.products = products; }
}
