package it.unisa.trisense.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import it.unisa.trisense.R;
import it.unisa.trisense.adapters.ScoreAdapter;
import it.unisa.trisense.managers.LeaderboardManager;

public class Game3Activity extends AppCompatActivity {

    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private TextView tvNoScores;
    private ScoreAdapter adapter;
    private MaterialButton btnStartGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game3);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        progressBar = findViewById(R.id.progressBar);
        tvNoScores = findViewById(R.id.tvNoScores);
        btnStartGame = findViewById(R.id.btnStartGame);

        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScoreAdapter(new ArrayList<>());
        rvLeaderboard.setAdapter(adapter);

        // Load Global Leaderboard
        loadLeaderboard();

        // Load Local Stats
        TextView tvTopScore = findViewById(R.id.tvTopScore);
        TextView tvAvgScore = findViewById(R.id.tvAvgScore);

        it.unisa.trisense.managers.LocalGameManager localGameManager = new it.unisa.trisense.managers.LocalGameManager(
                this);
        tvTopScore.setText(String.valueOf(localGameManager.getTopScore("game3")));
        tvAvgScore.setText(String.format(java.util.Locale.getDefault(), "%.1f", localGameManager.getAvgScore("game3")));

        btnStartGame.setOnClickListener(v -> {
            Intent intent = new Intent(Game3Activity.this, GyroMazeActivity.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload leaderboard and local stats when returning from the game
        loadLeaderboard();

        TextView tvTopScore = findViewById(R.id.tvTopScore);
        TextView tvAvgScore = findViewById(R.id.tvAvgScore);

        // Load Top Score from Firebase
        LeaderboardManager.getInstance().getUserScore("game3", score -> {
            if (score != -1.0) {
                if (score % 1 == 0) {
                    tvTopScore.setText(String.valueOf(score.intValue()));
                } else {
                    tvTopScore.setText(String.format(java.util.Locale.getDefault(), "%.1f", score));
                }
            } else {
                tvTopScore.setText("0");
            }
        });

        // Load Average from Local Storage
        it.unisa.trisense.managers.LocalGameManager localGameManager = new it.unisa.trisense.managers.LocalGameManager(
                this);
        tvAvgScore.setText(String.format(java.util.Locale.getDefault(), "%.1f", localGameManager.getAvgScore("game3")));
    }

    private void loadLeaderboard() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoScores.setVisibility(View.GONE);

        LeaderboardManager.getInstance().getTopScores("game3", scores -> {
            progressBar.setVisibility(View.GONE);
            if (scores.isEmpty()) {
                tvNoScores.setVisibility(View.VISIBLE);
            } else {
                adapter.updateScores(scores);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}