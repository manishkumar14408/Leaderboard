package com.quizleaderboard.controller;

import com.quizleaderboard.model.QuizData;
import com.quizleaderboard.service.QuizDataService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Java port of the /data route in the Python Handler.do_GET().
 * Static files (index.html, logo.png) don't need a controller at all -
 * Spring Boot serves everything under src/main/resources/static
 * automatically, which replaces the manual file-serving branch
 * in the Python do_GET() method.
 */
@RestController
@CrossOrigin(origins = "*") // matches Access-Control-Allow-Origin: * in the Python version
public class LeaderboardController {

    private final QuizDataService quizDataService;

    public LeaderboardController(QuizDataService quizDataService) {
        this.quizDataService = quizDataService;
    }

    @GetMapping("/data")
    public ResponseEntity<Object> getData() {
        try {
            QuizData data = quizDataService.readQuizData();
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore()) // no-store, no-cache, must-revalidate
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }
}
