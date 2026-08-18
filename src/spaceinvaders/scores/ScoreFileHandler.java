package spaceinvaders.scores;

import spaceinvaders.GameExceptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ScoreFileHandler {
    private static final String APPLICATION_DIRECTORY = "Darth-Invader";
    private static final String SCORES_FILE_NAME = "scores.txt";
    private final Path scoresFile;

    public ScoreFileHandler() {
        this(resolveDefaultScoresFile());
    }

    ScoreFileHandler(Path scoresFile) {
        this.scoresFile = scoresFile;
    }

    private static Path resolveDefaultScoresFile() {
        String appDataDirectory = System.getenv("APPDATA");
        Path dataDirectory = appDataDirectory == null || appDataDirectory.isBlank()
                ? Path.of(System.getProperty("user.home"), ".darth-invader")
                : Path.of(appDataDirectory, APPLICATION_DIRECTORY);
        return dataDirectory.resolve(SCORES_FILE_NAME);
    }

    Path getScoresFile() {
        return scoresFile;
    }

    /**
     * Loads scores from the scores.txt file.
     * Each line should be formatted as "name,score".
     * Malformed lines are skipped with a warning.
     * 
     * @return a List of ScoreEntry objects, or an empty list if the file doesn't exist
     */
    public List<ScoreEntry> loadScores() {
        List<ScoreEntry> scores = new ArrayList<>();
        Path sourceFile = getExistingScoresFile();
        if (sourceFile == null) {
            return scores;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // Parse the line as "name,score"
                    String[] parts = line.split(",", 2);
                    if (parts.length != 2) {
                        GameExceptions.logWarning("Malformed score line skipped: " + line);
                        continue;
                    }
                    
                    String name = parts[0].trim();
                    int score = Integer.parseInt(parts[1].trim());
                    scores.add(new ScoreEntry(name, score));
                } catch (NumberFormatException e) {
                    GameExceptions.logWarning("Could not parse score in line: " + line);
                }
            }
        } catch (IOException e) {
            GameExceptions.handleWithDialog("Error reading scores file", e);
        }
        
        return scores;
    }

    /**
     * Saves a list of scores to the scores.txt file.
     * Each entry is written as "name,score" on a new line.
     * 
     * @param scores the list of ScoreEntry objects to write
     */
    public void saveScores(List<ScoreEntry> scores) {
        Path targetFile = scoresFile.toAbsolutePath();
        Path dataDirectory = targetFile.getParent();

        try {
            Files.createDirectories(dataDirectory);
            Path temporaryFile = Files.createTempFile(dataDirectory, "scores-", ".tmp");
            writeScores(temporaryFile, scores);
            moveIntoPlace(temporaryFile, targetFile);
        } catch (IOException e) {
            GameExceptions.handleWithDialog("Error writing scores file", e);
        }
    }

    private Path getExistingScoresFile() {
        if (Files.exists(scoresFile)) {
            return scoresFile;
        }

        Path legacyScoresFile = Path.of(SCORES_FILE_NAME);
        if (Files.exists(legacyScoresFile)) {
            return legacyScoresFile;
        }
        return null;
    }

    private void writeScores(Path file, List<ScoreEntry> scores) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (ScoreEntry entry : scores) {
                writer.write(entry.getName() + "," + entry.getScore());
                writer.newLine();
            }
        }
    }

    private void moveIntoPlace(Path temporaryFile, Path targetFile) throws IOException {
        try {
            Files.move(temporaryFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
