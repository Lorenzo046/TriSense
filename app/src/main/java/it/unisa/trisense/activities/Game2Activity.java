package it.unisa.trisense.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class Game2Activity extends AppCompatActivity {

    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private TextView tvNoScores;
    private ScoreAdapter adapter;
    private MaterialButton btnStartGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game2);

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

        // Caricamento classifica globale
        loadLeaderboard();

        // Caricamento statistiche locali
        TextView tvTopScore = findViewById(R.id.tvTopScore);
        TextView tvAvgScore = findViewById(R.id.tvAvgScore);

        it.unisa.trisense.managers.LocalGameManager localGameManager = new it.unisa.trisense.managers.LocalGameManager(
                this);
        tvTopScore.setText(String.valueOf(localGameManager.getTopScore("game2")));
        tvAvgScore.setText(String.format(java.util.Locale.getDefault(), "%.1f", localGameManager.getAvgScore("game2")));

        btnStartGame.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(Game2Activity.this, FlashReflexActivity.class);
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
        // Ricarica la classifica globale e le statistich elocali quando ritorniamo dal gioco
        loadLeaderboard();

        TextView tvTopScore = findViewById(R.id.tvTopScore);
        TextView tvAvgScore = findViewById(R.id.tvAvgScore);

        // Carica i top score da FireBase
        LeaderboardManager.getInstance().getUserScore("game2", score -> {
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

        // Carica la media dallo storage locale
        it.unisa.trisense.managers.LocalGameManager localGameManager = new it.unisa.trisense.managers.LocalGameManager(
                this);
        tvAvgScore.setText(String.format(java.util.Locale.getDefault(), "%.1f", localGameManager.getAvgScore("game2")));
    }

    private void loadLeaderboard() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoScores.setVisibility(View.GONE);

        LeaderboardManager.getInstance().getTopScores("game2", scores -> {
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