# Quiz Leaderboard — Spring Boot port

A Spring Boot replica of the Python `live_leaderboard_server.py` project.
Same idea: keep editing `Premium_Quiz_Master_Dashboard.xlsx`, save it, and the
browser dashboard updates on its own — no restart needed.

## Run it

Requires JDK 17+ and Maven.

```bash
cd quiz-leaderboard-springboot
mvn spring-boot:run
```

Then open:

```
http://localhost:8000
```

To let others on your network view it, use your machine's LAN IP instead of
`localhost`, e.g. `http://10.21.7.245:8000` — Spring Boot's embedded Tomcat
binds to all interfaces by default, so no extra config is needed for that
part (unlike raw Python `http.server`/Flask, where you have to explicitly
pass `host='0.0.0.0'`).

To build a runnable jar instead:

```bash
mvn clean package
java -jar target/quiz-leaderboard.jar
```

Run it from the project root (or copy the jar next to the `.xlsx` file) so
the default relative Excel path resolves correctly.

## How score updates work

1. Open `Premium_Quiz_Master_Dashboard.xlsx`, edit scores on the "Score Entry"
   sheet, save.
2. The browser polls `GET /data` every 10 seconds (`REFRESH_MS` in
   `static/index.html`).
3. Each poll re-opens the Excel file fresh from disk — nothing is cached in
   memory — so your saved changes show up on the next poll.

## Project structure vs. the Python original

| Python file                         | Spring Boot equivalent                                              |
|--------------------------------------|-----------------------------------------------------------------------|
| `live_leaderboard_server.py` (`read_quiz_data()`) | `service/QuizDataService.java` |
| `live_leaderboard_server.py` (`Handler.do_GET` `/data` branch) | `controller/LeaderboardController.java` |
| `live_leaderboard_server.py` (`Handler.do_GET` static file branch) | Nothing to write — Spring Boot auto-serves `src/main/resources/static/**` |
| `openpyxl`                            | Apache POI (`poi-ooxml`) |
| JSON dict returned by `read_quiz_data()` | `model/QuizData.java` + `model/TeamScore.java` |
| `leaderboard.html`                    | `static/index.html` (same UI, same `/data` contract) |
| `PORT = 8000`, `HOST = "0.0.0.0"`     | `application.properties` → `server.port=8000` |

## Configuration

In `src/main/resources/application.properties`:

```properties
server.port=8000
quiz.excel.path=./Premium_Quiz_Master_Dashboard.xlsx
quiz.excel.sheet=Score Entry
```

Override at runtime without rebuilding, e.g.:

```bash
java -jar target/quiz-leaderboard.jar --quiz.excel.path=/path/to/another.xlsx
```

## Excel layout this expects (Score Entry sheet)

- Row 4: headers — `Team | Round 1 | Round 2 | Round 3 | Round 4 | Round 5 | Total | Rank`
- Rows 5–14: one team per row, column A = team name, B–F = round scores

Total and rank are always recalculated server-side from the round scores
(same as the Python version), so it doesn't matter if the Excel formulas in
columns G/H are stale — they're read only as a fallback.

## One fix vs. the original

The original `leaderboard.html` referenced a `#statusText` element in its
error handler that didn't actually exist in the page, so a failed `/data`
request would throw silently in the console instead of showing an error.
`static/index.html` here adds that element so connection errors are visible
in the UI.
