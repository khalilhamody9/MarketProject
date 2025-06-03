package com.example.khalilo.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.khalilo.R;
import com.example.khalilo.ui.GroupDetailsActivity;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {
    private List<String> members;
    private String groupName;
    private String username;
    private Context context;

    public MembersAdapter(Context context, List<String> members, String groupName, String username) {
        this.context = context;
        this.members = members;
        this.groupName = groupName;
        this.username = username;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        String memberName = members.get(position);
        holder.memberName.setText(memberName);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GroupDetailsActivity.class);
            intent.putExtra("groupName", groupName);
            intent.putExtra("username", username);
            intent.putExtra("memberName", memberName);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView memberName;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            memberName = itemView.findViewById(R.id.memberName);
        }
    }
}
