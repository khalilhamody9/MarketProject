package com.example.khalilo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.models.ShopWithProducts;

import java.util.List;

public class ShopWithProductsAdapter extends RecyclerView.Adapter<ShopWithProductsAdapter.ViewHolder> {
    private List<ShopWithProducts> shopList;
    private Context context;

    public ShopWithProductsAdapter(Context context, List<ShopWithProducts> shopList) {
        this.context = context;
        this.shopList = shopList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shop, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ShopWithProducts shop = shopList.get(position);
        holder.shopName.setText(shop.getShopKey());
        holder.shopTotalPrice.setText("מחיר כולל: ₪" + String.format("%.2f", shop.getTotal_price()));

        holder.productContainer.removeAllViews();
        for (ShopWithProducts.Product p : shop.getProducts()) {
            TextView productView = new TextView(context);
            productView.setText("- " + p.getName() + " | ₪" + p.getPrice() + (p.getSale() != null && !p.getSale().isEmpty() ? " | מבצע: " + p.getSale() : ""));
            productView.setTextSize(14);
            holder.productContainer.addView(productView);
        }

        // Toggle products on click
        holder.itemView.setOnClickListener(v -> {
            if (holder.productContainer.getVisibility() == View.GONE) {
                holder.productContainer.setVisibility(View.VISIBLE);
            } else {
                holder.productContainer.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return shopList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView shopName, shopTotalPrice;
        LinearLayout productContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            shopName = itemView.findViewById(R.id.shopName);
            shopTotalPrice = itemView.findViewById(R.id.shopTotalPrice);
            productContainer = itemView.findViewById(R.id.productContainer);
        }
    }
}
