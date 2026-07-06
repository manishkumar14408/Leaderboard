package com.quizleaderboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Equivalent of running `python live_leaderboard_server.py`.
 *
 * On startup this serves:
 *  - GET /          -> static/index.html (the leaderboard UI)
 *  - GET /logo.png  -> static/logo.png
 *  - GET /data      -> latest scores read live from the Excel workbook, as JSON
 */
@SpringBootApplication
public class QuizLeaderboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizLeaderboardApplication.class, args);
    }
}
