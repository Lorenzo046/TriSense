package it.unisa.trisense.activities;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import it.unisa.trisense.R;
import it.unisa.trisense.managers.LeaderboardManager;
import it.unisa.trisense.managers.LocalGameManager;
import it.unisa.trisense.views.GyroMazeView;

public class GyroMazeActivity extends AppCompatActivity implements SensorEventListener {

    private GyroMazeView gameView;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private View layoutPause;
    private View layoutGameOver;
    private ImageButton btnPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyro_maze);

        gameView = findViewById(R.id.gameView);
        btnPause = findViewById(R.id.btnPause);
        layoutPause = findViewById(R.id.layoutPause);
        layoutGameOver = findViewById(R.id.layoutGameOver);

        View btnResume = findViewById(R.id.btnResume);
        View btnPauseExit = findViewById(R.id.btnPauseExit);
        View btnRetry = findViewById(R.id.btnRetry);
        View btnExit = findViewById(R.id.btnExit);
        TextView tvScore = findViewById(R.id.tvScore);

        // Sensor setup
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Pause button
        btnPause.setOnClickListener(v -> {
            if (gameView.isGameActive()) {
                gameView.pause();
                layoutPause.setVisibility(View.VISIBLE);
                btnPause.setVisibility(View.GONE);
            }
        });

        // Resume button
        btnResume.setOnClickListener(v -> {
            layoutPause.setVisibility(View.GONE);
            btnPause.setVisibility(View.VISIBLE);
            gameView.resumeGame();
        });

        // Exit from pause
        btnPauseExit.setOnClickListener(v -> finish());

        // Exit from game over
        btnExit.setOnClickListener(v -> finish());

        // Retry
        btnRetry.setOnClickListener(v -> {
            layoutGameOver.setVisibility(View.GONE);
            btnPause.setVisibility(View.VISIBLE);
            gameView.reset();
            gameView.start();
        });

        // Game events
        gameView.setOnGameEventListener(new GyroMazeView.OnGameEventListener() {
            @Override
            public void onGameOver(int score) {
                runOnUiThread(() -> {
                    tvScore.setText("Monete: " + score);
                    layoutGameOver.setVisibility(View.VISIBLE);
                    btnPause.setVisibility(View.GONE);

                    // Save score to Firebase Firestore
                    LeaderboardManager.getInstance().saveScore("game3", score, success -> {
                        // Score saved to Firebase leaderboard
                    });

                    // Save score locally
                    LocalGameManager localGameManager = new LocalGameManager(GyroMazeActivity.this);
                    localGameManager.saveScore("game3", score);
                });
            }

            @Override
            public void onScoreUpdate(int score) {
                // Could update a live score view here if needed
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            gameView.updateSensor(x, y);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.start();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        gameView.stop();
    }
}
