package com.quizleaderboard.service;

import com.quizleaderboard.model.QuizData;
import com.quizleaderboard.model.TeamScore;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Java port of read_quiz_data() from live_leaderboard_server.py.
 *
 * Same contract as the Python version:
 *  - Opens the workbook fresh on every call (no caching), so editing and
 *    saving the Excel file is picked up on the next poll - that's what
 *    makes the dashboard "live".
 *  - Sheet "Score Entry": header row is Excel row 4 (columns B-F = Round 1..5),
 *    team rows are Excel rows 5-14 (column A = team name, G = total, H = rank).
 *  - Total/rank are recalculated here rather than trusted from the Excel
 *    formulas, exactly like the Python version does, so the dashboard
 *    doesn't depend on Excel's cached formula results.
 */
@Service
public class QuizDataService {

    @Value("${quiz.excel.path:./Premium_Quiz_Master_Dashboard.xlsx}")
    private String excelPath;

    @Value("${quiz.excel.sheet:Score Entry}")
    private String sheetName;

    // Excel row/col numbers (1-based, matching the spreadsheet you actually see)
    private static final int HEADER_ROW = 4;
    private static final int FIRST_ROUND_COL = 2; // B
    private static final int LAST_ROUND_COL = 6;  // F
    private static final int FIRST_TEAM_ROW = 5;
    private static final int LAST_TEAM_ROW = 14;
    private static final int TEAM_NAME_COL = 1;   // A
    private static final int TOTAL_COL = 7;        // G
    private static final int RANK_COL = 8;         // H

    public QuizData readQuizData() throws IOException {
        Path path = Paths.get(excelPath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Excel file not found: " + path.toAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(path.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalStateException("Sheet not found: " + sheetName);
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            List<String> rounds = readRounds(sheet, evaluator);
            List<TeamScore> teams = readTeams(sheet, evaluator);

            applyRanking(teams);

            QuizData data = new QuizData();
            data.setRounds(rounds);
            data.setTeams(teams);
            data.setSourceFile(path.getFileName().toString());
            data.setSheet(sheetName);
            data.setLastModified(Files.getLastModifiedTime(path).toMillis() / 1000.0);
            return data;
        }
    }

    private List<String> readRounds(Sheet sheet, FormulaEvaluator evaluator) {
        List<String> rounds = new ArrayList<>();
        Row headerRow = sheet.getRow(HEADER_ROW - 1);
        if (headerRow == null) {
            return rounds;
        }
        for (int col = FIRST_ROUND_COL; col <= LAST_ROUND_COL; col++) {
            String header = getStringValue(headerRow.getCell(col - 1), evaluator);
            if (header != null && !header.isBlank()) {
                rounds.add(header);
            }
        }
        return rounds;
    }

    private List<TeamScore> readTeams(Sheet sheet, FormulaEvaluator evaluator) {
        List<TeamScore> teams = new ArrayList<>();

        for (int r = FIRST_TEAM_ROW; r <= LAST_TEAM_ROW; r++) {
            Row row = sheet.getRow(r - 1);
            if (row == null) {
                continue;
            }
            String teamName = getStringValue(row.getCell(TEAM_NAME_COL - 1), evaluator);
            if (teamName == null || teamName.isBlank()) {
                continue;
            }

            List<Double> scores = new ArrayList<>();
            for (int col = FIRST_ROUND_COL; col <= LAST_ROUND_COL; col++) {
                scores.add(getNumericValue(row.getCell(col - 1), evaluator));
            }

            double total = getNumericValue(row.getCell(TOTAL_COL - 1), evaluator);
            if (total == 0) {
                total = scores.stream().mapToDouble(Double::doubleValue).sum();
            }

            Cell rankCell = row.getCell(RANK_COL - 1);
            Double excelRank = rankCell != null ? getNumericValue(rankCell, evaluator) : null;

            TeamScore team = new TeamScore();
            team.setTeam(teamName);
            team.setScores(scores);
            team.setTotal(total);
            team.setExcelRank(excelRank);
            teams.add(team);
        }
        return teams;
    }

    /**
     * Sort by total desc, team name asc (tie-break), then assign competition
     * ranks (ties share a rank, e.g. 1, 2, 2, 4) - same logic as the Python
     * server's sorted_teams loop.
     */
    private void applyRanking(List<TeamScore> teams) {
        teams.sort((a, b) -> {
            int cmp = Double.compare(b.getTotal(), a.getTotal());
            if (cmp != 0) {
                return cmp;
            }
            return a.getTeam().compareTo(b.getTeam());
        });

        Double previousScore = null;
        int previousRank = 0;
        for (int i = 0; i < teams.size(); i++) {
            TeamScore team = teams.get(i);
            int index = i + 1;
            if (previousScore != null && team.getTotal() == previousScore) {
                team.setRank(previousRank);
            } else {
                team.setRank(index);
                previousRank = index;
            }
            previousScore = team.getTotal();
        }
    }

    private String getStringValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.FORMULA) {
                CellValue value = evaluator.evaluate(cell);
                return value.getStringValue();
            }
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private double getNumericValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return 0;
        }
        try {
            if (cell.getCellType() == CellType.FORMULA) {
                CellValue value = evaluator.evaluate(cell);
                return value.getNumberValue();
            }
            return switch (cell.getCellType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> {
                    String raw = cell.getStringCellValue();
                    yield (raw == null || raw.isBlank()) ? 0 : Double.parseDouble(raw.trim());
                }
                default -> 0;
            };
        } catch (Exception e) {
            return 0;
        }
    }
}
