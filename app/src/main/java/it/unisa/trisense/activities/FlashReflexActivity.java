package it.unisa.trisense.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import it.unisa.trisense.R;
import it.unisa.trisense.managers.LeaderboardManager;
import it.unisa.trisense.managers.LocalGameManager;
import it.unisa.trisense.views.FlashReflexView;

public class FlashReflexActivity extends AppCompatActivity {

    private FlashReflexView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_reflex);

        gameView = findViewById(R.id.gameView);

        View layoutGameOver = findViewById(R.id.layoutGameOver);
        View btnRetry = findViewById(R.id.btnRetry);
        View btnExit = findViewById(R.id.btnExit);
        TextView tvScore = findViewById(R.id.tvScore);

        btnExit.setOnClickListener(v -> finish());

        btnRetry.setOnClickListener(v -> {
            layoutGameOver.setVisibility(View.GONE);
            gameView.reset();
            gameView.start();
        });

        gameView.setOnGameEventListener(new FlashReflexView.OnGameEventListener() {
            @Override
            public void onGameOver(int score) {
                runOnUiThread(() -> {
                    tvScore.setText("Score: " + score);
                    layoutGameOver.setVisibility(View.VISIBLE);

                    // Save score to Firebase Firestore
                    LeaderboardManager.getInstance().saveScore("game2", score, success -> {
                        // Score saved to Firebase leaderboard
                    });

                    // Save score locally
                    LocalGameManager localGameManager = new LocalGameManager(FlashReflexActivity.this);
                    localGameManager.saveScore("game2", score);
                });
            }

            @Override
            public void onScoreUpdate(int score) {
                // Could update a live score view here if needed
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
