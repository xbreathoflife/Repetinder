package ru.hse.java.repetinder.match;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import ru.hse.java.repetinder.R;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesViewHolder> {
    private List<Match> matchList;
    private Context context;

    public MatchesAdapter(List<Match> matchList, Context context) {
        this.matchList = matchList;
        this.context = context;
    }

    @NonNull
    @Override
    public MatchesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.matches_item, null, false);
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(layoutParams);
        return new MatchesViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchesViewHolder holder, int position) {
        holder.matchFullname.setText(matchList.get(position).getFullname());
        holder.matchEmail.setText(matchList.get(position).getEmail());
        holder.matchId = matchList.get(position).getUserId();
        holder.matchUserRole = matchList.get(position).getUserRole();
        if (!matchList.get(position).getProfileImageUrl().equals("default")) {
            Glide.with(context).load(matchList.get(position).getProfileImageUrl()).into(holder.matchImage);
        }
    }

    @Override
    public int getItemCount() {
        return matchList.size();
    }
}
