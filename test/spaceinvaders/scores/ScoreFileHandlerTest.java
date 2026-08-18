package spaceinvaders.scores;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScoreFileHandlerTest {
    private ScoreFileHandlerTest() {
    }

    public static void main(String[] args) throws IOException {
        Path testDirectory = Files.createTempDirectory("darth-invader-scores-");
        try {
            testRoundTrip(testDirectory);
            testMalformedRowsAreSkipped(testDirectory);
            testRankingKeepsTopTen();
            System.out.println("ScoreFileHandlerTest passed");
        } finally {
            deleteRecursively(testDirectory);
        }
    }

    private static void testRoundTrip(Path testDirectory) {
        ScoreFileHandler handler = new ScoreFileHandler(testDirectory.resolve("scores.txt"));
        List<ScoreEntry> expectedScores = List.of(
                new ScoreEntry("Ada", 200),
                new ScoreEntry("Ben", 75));

        handler.saveScores(expectedScores);
        List<ScoreEntry> actualScores = handler.loadScores();

        require(actualScores.size() == 2, "Round-trip should preserve the score count");
        require("Ada".equals(actualScores.get(0).getName()) && actualScores.get(0).getScore() == 200,
                "Round-trip should preserve the first score");
        require("Ben".equals(actualScores.get(1).getName()) && actualScores.get(1).getScore() == 75,
                "Round-trip should preserve the second score");
    }

    private static void testMalformedRowsAreSkipped(Path testDirectory) throws IOException {
        Path scoreFile = testDirectory.resolve("malformed-scores.txt");
        Files.writeString(scoreFile, "Ada,100\nnot-a-score\nBen,not-a-number\n", StandardCharsets.UTF_8);

        List<ScoreEntry> scores = new ScoreFileHandler(scoreFile).loadScores();

        require(scores.size() == 1, "Malformed score rows should be skipped");
        require("Ada".equals(scores.get(0).getName()) && scores.get(0).getScore() == 100,
                "A valid row should survive malformed rows");
    }

    private static void testRankingKeepsTopTen() {
        List<ScoreEntry> existingScores = new ArrayList<>();
        for (int score = 1; score <= 10; score++) {
            existingScores.add(new ScoreEntry("Player" + score, score * 10));
        }

        List<ScoreEntry> rankedScores = ScoreManager.rankScores(existingScores, new ScoreEntry("Winner", 999));

        require(rankedScores.size() == 10, "Leaderboard should retain only ten scores");
        require("Winner".equals(rankedScores.get(0).getName()) && rankedScores.get(0).getScore() == 999,
                "Leaderboard should sort scores descending");
        require(rankedScores.get(9).getScore() == 20, "Leaderboard should remove the lowest score");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not delete test file: " + path, e);
                }
            });
        }
    }
}