package it.unisa.trisense.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import it.unisa.trisense.R;
import it.unisa.trisense.views.DecibelJumpView;

public class DecibelJumpActivity extends AppCompatActivity {

    private DecibelJumpView gameView;
    private TextView tvGameOver;
    private static final int PERMISSION_REQ_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decibel_jump);

        gameView = findViewById(R.id.gameView);
        tvGameOver = findViewById(R.id.tvGameOver);

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

        if (checkPermissions()) {
            // Game will start in onResume
        } else {
            requestPermissions();
        }

        gameView.setOnGameEventListener(new DecibelJumpView.OnGameEventListener() {
            @Override
            public void onGameOver(int score) {
                runOnUiThread(() -> {
                    tvScore.setText("Score: " + score);
                    layoutGameOver.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onScoreUpdate(int score) {
                // Could update a live score view here if we added one
            }
        });
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, PERMISSION_REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGame();
            } else {
                Toast.makeText(this, "Permission Denied. Game cannot start.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startGame() {
        gameView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            gameView.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.stop();
    }
}
