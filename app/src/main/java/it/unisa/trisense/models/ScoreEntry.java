package it.unisa.trisense.models;

public class ScoreEntry {
    private String userId;
    private String username;
    private double score; // Using double to support both int and float scores (e.g. time)

    public ScoreEntry() {
    }

    public ScoreEntry(String userId, String username, double score) {
        this.userId = userId;
        this.username = username;
        this.score = score;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
