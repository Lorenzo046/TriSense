package it.unisa.trisense.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import it.unisa.trisense.R;
import it.unisa.trisense.models.ScoreEntry;
import it.unisa.trisense.models.User;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUsername, tvEmail, tvScoreGame1, tvScoreGame2, tvScoreGame3;
    private Button btnLogout;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvScoreGame1 = findViewById(R.id.tvScoreGame1);
        tvScoreGame2 = findViewById(R.id.tvScoreGame2);
        tvScoreGame3 = findViewById(R.id.tvScoreGame3);
        btnLogout = findViewById(R.id.btnLogout);

        loadUserData();
        loadScores();

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            tvUsername.setText(user.getUsername());
                            tvEmail.setText(user.getEmail());
                        }
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(this, "Errore nel caricamento del profilo", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadScores() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            fetchScore("1", userId, tvScoreGame1);
            fetchScore("2", userId, tvScoreGame2);
            fetchScore("3", userId, tvScoreGame3);
        }
    }

    private void fetchScore(String gameId, String userId, TextView textView) {
        db.collection("scores").document(gameId)
                .collection("user_scores").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    ScoreEntry entry = documentSnapshot.toObject(ScoreEntry.class);
                    if (entry != null) {
                        textView.setText(String.valueOf(entry.getScore()));
                    } else {
                        textView.setText("-");
                    }
                });
    }
}
