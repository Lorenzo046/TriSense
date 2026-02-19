package it.unisa.trisense.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import it.unisa.trisense.R;
import it.unisa.trisense.models.Game;

public class GameSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gameselection);

        TextView txtWelcome = findViewById(R.id.txtWelcome);

        // Otteniamo il nome dell'utente loggato
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (currentUser != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            txtWelcome.setText("Bentornato " + username);
                        }
                    });
        }

        // Creiamo i giochi usando il model
        Game game1 = new Game(1, getString(R.string.game_1));
        Game game2 = new Game(2, getString(R.string.game_2));
        Game game3 = new Game(3, getString(R.string.game_3));

        // Collego i pulsanti
        MaterialCardView cardGame1 = findViewById(R.id.cardGame1);
        cardGame1.setOnClickListener(v -> openGame(game1));
        MaterialCardView cardGame2 = findViewById(R.id.cardGame2);
        cardGame2.setOnClickListener(v -> openGame(game2));
        MaterialCardView cardGame3 = findViewById(R.id.cardGame3);
        cardGame3.setOnClickListener(v -> openGame(game3));

        // Profile button
        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            Intent intent = new Intent(GameSelectionActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void openGame(Game game) {
        Intent intent;

        // decidi quale Activity aprire
        switch (game.getId()) {
            case 1:
                intent = new Intent(this, Game1Activity.class);
                break;
            case 2:
                intent = new Intent(this, Game2Activity.class);
                break;
            case 3:
                intent = new Intent(this, Game3Activity.class);
                break;
            default:
                return;
        }

        // passo l'ID del gioco alla prossima Activity
        intent.putExtra("game_id", game.getId());
        startActivity(intent);
    }

}
