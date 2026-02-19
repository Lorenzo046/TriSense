package it.unisa.trisense.managers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import it.unisa.trisense.models.ScoreEntry;
import it.unisa.trisense.models.User;

public class LeaderboardManager {
    private static LeaderboardManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private LeaderboardManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized LeaderboardManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardManager();
        }
        return instance;
    }

    public void saveScore(String gameId, double score, Consumer<Boolean> callback) {
        if (auth.getCurrentUser() == null) {
            callback.accept(false);
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // 1. Ottieni il nome utente dell'utente corrente
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            User user = documentSnapshot.toObject(User.class);
            if (user != null) {
                // 2. Controlla il punteggio esistente
                db.collection("scores").document(gameId)
                        .collection("user_scores").document(userId)
                        .get()
                        .addOnSuccessListener(scoreSnapshot -> {
                            ScoreEntry currentEntry = scoreSnapshot.toObject(ScoreEntry.class);

                            // Controlla se il nuovo punteggio è migliore (più alto è meglio)
                            // NOTA: Giochi diversi potrebbero avere logiche di punteggio differenti.
                            // Per la classifica generica, si assume che il punteggio PIÙ ALTO sia migliore.

                            if (currentEntry == null || score > currentEntry.getScore()) {
                                ScoreEntry newEntry = new ScoreEntry(userId, user.getUsername(), score);
                                db.collection("scores").document(gameId)
                                        .collection("user_scores").document(userId)
                                        .set(newEntry)
                                        .addOnSuccessListener(aVoid -> callback.accept(true))
                                        .addOnFailureListener(e -> callback.accept(false));
                            } else {
                                callback.accept(true); // Punteggio non sufficientemente alto, ma operazione "riuscita"
                            }
                        })
                        .addOnFailureListener(e -> callback.accept(false));
            } else {
                callback.accept(false);
            }
        }).addOnFailureListener(e -> callback.accept(false));
    }

    public void getTopScores(String gameId, Consumer<List<ScoreEntry>> callback) {
        db.collection("scores").document(gameId)
                .collection("user_scores")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ScoreEntry> scores = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        scores.add(document.toObject(ScoreEntry.class));
                    }
                    callback.accept(scores);
                })
                .addOnFailureListener(e -> callback.accept(new ArrayList<>()));
    }

    public void getUserScore(String gameId, Consumer<Double> callback) {
        if (auth.getCurrentUser() == null) {
            callback.accept(-1.0);
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        db.collection("scores").document(gameId)
                .collection("user_scores").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    ScoreEntry entry = documentSnapshot.toObject(ScoreEntry.class);
                    if (entry != null) {
                        callback.accept(entry.getScore());
                    } else {
                        callback.accept(-1.0); // Nessun punteggio trovato
                    }
                })
                .addOnFailureListener(e -> callback.accept(-1.0));
    }
}
