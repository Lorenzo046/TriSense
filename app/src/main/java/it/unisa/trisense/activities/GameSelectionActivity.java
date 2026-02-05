package it.unisa.trisense.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import it.unisa.trisense.R;
import it.unisa.trisense.models.Game;



public class GameSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gameselection);

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

        // click listener
        cardGame1.setOnClickListener(v -> openGame(game1));
        cardGame2.setOnClickListener(v -> openGame(game2));
        cardGame3.setOnClickListener(v -> openGame(game3));
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
