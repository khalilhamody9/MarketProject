package com.example.khalilo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.entities.Item;
import com.example.khalilo.models.History;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {

    private List<History> suggestionList;
    private Context context;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;

    public SuggestionAdapter(List<History> suggestionList, Context context, ItemAdapter itemAdapter, List<Item> itemList) {
        this.suggestionList = suggestionList;
        this.context = context;
        this.itemAdapter = itemAdapter;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        History history = suggestionList.get(position);
        holder.itemName.setText(history.getItemName());
        holder.itemInfo.setText("Last bought: " + history.getDate() );
        int imageResId = getImageByName(history.getItemName().toLowerCase().replace(" ", "_"));
        holder.itemImage.setImageResource(imageResId);
        holder.btnApprove.setOnClickListener(v -> {
            for (Item i : itemList) {
                if (i.getName().equalsIgnoreCase(history.getItemName())) {
                    itemAdapter.increaseItem(i);
                    Toast.makeText(context, i.getName() + " added", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            holder.itemView.setVisibility(View.GONE);
        });

        holder.btnDeny.setOnClickListener(v -> {
            Toast.makeText(context, history.getItemName() + " denied", Toast.LENGTH_SHORT).show();
            holder.itemView.setVisibility(View.GONE);
        });

        holder.itemImage.setImageResource(imageResId);

    }

    @Override
    public int getItemCount() {
        return suggestionList.size();
    }

    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemInfo;
        Button btnApprove, btnDeny;
        ImageView itemImage;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.suggestedItemName);
            itemInfo = itemView.findViewById(R.id.suggestedItemInfo);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDeny = itemView.findViewById(R.id.btnDeny);
            itemImage = itemView.findViewById(R.id.suggestedItemImage);

        }
    }
    private int getImageByName(String name) {
        int resId = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        return resId == 0 ? android.R.drawable.ic_menu_report_image : resId;
    }
}
