package com.quizleaderboard.model;

import java.util.List;

/**
 * Full payload for GET /data. Mirrors the dict returned by
 * read_quiz_data() in the original Python server.
 */
public class QuizData {

    private List<String> rounds;
    private List<TeamScore> teams;
    private String sourceFile;
    private String sheet;
    private double lastModified; // epoch seconds, like Python's Path.stat().st_mtime

    public List<String> getRounds() {
        return rounds;
    }

    public void setRounds(List<String> rounds) {
        this.rounds = rounds;
    }

    public List<TeamScore> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamScore> teams) {
        this.teams = teams;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public double getLastModified() {
        return lastModified;
    }

    public void setLastModified(double lastModified) {
        this.lastModified = lastModified;
    }
}
