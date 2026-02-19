package it.unisa.trisense.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalGameManager {
    private static final String PREF_NAME = "TriSenseLocalStats";
    private final SharedPreferences prefs;

    public LocalGameManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveScore(String gameId, int score) {
        // Aggiorna il punteggio massimo
        int currentTop = getTopScore(gameId);
        if (score > currentTop) {
            prefs.edit().putInt(gameId + "_top", score).apply();
        }

        // Aggiorna la media
        float currentAvg = getAvgScore(gameId);
        int playCount = prefs.getInt(gameId + "_count", 0);

        float newTotal = (currentAvg * playCount) + score;
        int newCount = playCount + 1;
        float newAvg = newTotal / newCount;

        prefs.edit()
                .putFloat(gameId + "_avg", newAvg)
                .putInt(gameId + "_count", newCount)
                .apply();
    }

    public int getTopScore(String gameId) {
        return prefs.getInt(gameId + "_top", 0);
    }

    public float getAvgScore(String gameId) {
        return prefs.getFloat(gameId + "_avg", 0.0f);
    }
}
