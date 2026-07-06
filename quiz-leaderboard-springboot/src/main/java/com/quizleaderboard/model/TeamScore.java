package com.quizleaderboard.model;

import java.util.List;

/**
 * One team's row in the leaderboard.
 * Field names match the JSON the original Python /data endpoint returned,
 * so the existing frontend JS (leaderboard.html) works unchanged.
 */
public class TeamScore {

    private String team;
    private List<Double> scores;
    private double total;
    private Double excelRank; // rank as typed into Excel, if present (nullable)
    private int rank;          // rank recalculated by the server (source of truth)

    public TeamScore() {
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public List<Double> getScores() {
        return scores;
    }

    public void setScores(List<Double> scores) {
        this.scores = scores;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Double getExcelRank() {
        return excelRank;
    }

    public void setExcelRank(Double excelRank) {
        this.excelRank = excelRank;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
