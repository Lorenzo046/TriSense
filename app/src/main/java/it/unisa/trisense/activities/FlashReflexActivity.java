package it.unisa.trisense.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import it.unisa.trisense.R;
import it.unisa.trisense.managers.LeaderboardManager;
import it.unisa.trisense.managers.LocalGameManager;
import it.unisa.trisense.views.FlashReflexView;

public class FlashReflexActivity extends AppCompatActivity {

    private FlashReflexView gameView;

    private View layoutPause;
    private ImageButton btnPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_reflex);

        gameView = findViewById(R.id.gameView);

        View layoutGameOver = findViewById(R.id.layoutGameOver);
        View btnRetry = findViewById(R.id.btnRetry);
        View btnExit = findViewById(R.id.btnExit);
        TextView tvScore = findViewById(R.id.tvScore);

        // Gestione pausa
        btnPause = findViewById(R.id.btnPause);
        layoutPause = findViewById(R.id.layoutPause);
        View btnResume = findViewById(R.id.btnResume);
        View btnPauseExit = findViewById(R.id.btnPauseExit);

        btnPause.setOnClickListener(v -> {
            if (gameView.isGameActive()) {
                gameView.pause();
                layoutPause.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
            }
        });

        btnResume.setOnClickListener(v -> {
            layoutPause.setVisibility(View.GONE);
            btnPause.setVisibility(View.VISIBLE);
            gameView.resumeGame();
        });

        btnPauseExit.setOnClickListener(v -> finish());

        btnExit.setOnClickListener(v -> finish());

        btnRetry.setOnClickListener(v -> {
            layoutGameOver.setVisibility(View.GONE);
            btnPause.setVisibility(View.VISIBLE);
            gameView.reset();
            gameView.start();
        });

        gameView.setOnGameEventListener(new FlashReflexView.OnGameEventListener() {
            @Override
            public void onGameOver(int score) {
                runOnUiThread(() -> {
                    tvScore.setText("Score: " + score);
                    layoutGameOver.setVisibility(View.VISIBLE);
                    btnPause.setVisibility(View.GONE);

                    // Salva lo score su Firebase Firestore (lo salva solamente se è un nuovo top score)
                    LeaderboardManager.getInstance().saveScore("game2", score, success -> {
                        // Salva lo score su Firebase leaderboard
                    });

                    // Salva lo score localmente (aggiorna il top score e la media)
                    LocalGameManager localGameManager = new LocalGameManager(FlashReflexActivity.this);
                    localGameManager.saveScore("game2", score);
                });
            }

            @Override
            public void onScoreUpdate(int score) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.stop();
    }
}
