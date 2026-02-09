package it.unisa.trisense.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.unisa.trisense.R;
import it.unisa.trisense.models.ScoreEntry;

public class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder> {

    private List<ScoreEntry> scores;

    public ScoreAdapter(List<ScoreEntry> scores) {
        this.scores = scores;
    }

    public void updateScores(List<ScoreEntry> newScores) {
        this.scores = newScores;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_score, parent, false);
        return new ScoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreViewHolder holder, int position) {
        ScoreEntry entry = scores.get(position);
        holder.tvRank.setText((position + 1) + ".");
        holder.tvUsername.setText(entry.getUsername());

        // Format score based on value (integer if no decimal, or 1 decimal place)
        if (entry.getScore() % 1 == 0) {
            holder.tvScore.setText(String.valueOf((int) entry.getScore()));
        } else {
            holder.tvScore.setText(String.format(java.util.Locale.getDefault(), "%.1f", entry.getScore()));
        }
    }

    @Override
    public int getItemCount() {
        return scores.size();
    }

    static class ScoreViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvUsername, tvScore;

        public ScoreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvScore = itemView.findViewById(R.id.tvScore);
        }
    }
}
