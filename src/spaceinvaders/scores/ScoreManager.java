package spaceinvaders.scores;

import spaceinvaders.GameExceptions;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ScoreManager extends Thread {
    private volatile int currentScore = 0;
    private List<ScoreEntry> leaderboard;
    private ScoreFileHandler fileHandler;
    private volatile boolean running = true;
    
    // Inner class to hold score save data
    private static class ScoreSaveTask {
        String playerName;
        int score;
        
        ScoreSaveTask(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
        }
    }
    
    private final LinkedBlockingQueue<ScoreSaveTask> saveQueue = new LinkedBlockingQueue<>();

    public ScoreManager() {
        fileHandler = new ScoreFileHandler();
        leaderboard = fileHandler.loadScores();
        
        // Set as daemon so it doesn't block app shutdown
        setDaemon(true);
        setName("ScoreManager");
    }

    @Override
    public void run() {
        // Thread processes save operations from the queue
        while (running) {
            try {
                // Wait for a save operation (blocks until one is available or timeout)
                ScoreSaveTask task = saveQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                
                if (task != null) {
                    // Perform the save operation on this thread
                    performSaveScore(task.playerName, task.score);
                }
            } catch (InterruptedException e) {
                GameExceptions.handleInterrupted("ScoreManager queue poll", e);
                break;
            }
        }
    }

    /**
     * Adds points to the current score.
     * 
     * @param points the number of points to add
     */
    public synchronized void addPoints(int points) {
        currentScore += points;
    }

    /**
     * Gets the current score.
     * 
     * @return the current score
     */
    public int getCurrentScore() {
        return currentScore;
    }

    /**
     * Resets the current score to 0.
     */
    public synchronized void resetScore() {
        currentScore = 0;
    }

    /**
     * Queues a score save operation to be processed by the ScoreManager thread.
     * The actual file I/O happens on the ScoreManager thread, not the caller's thread.
     * The current score is captured at the time this method is called.
     * 
     * @param playerName the name of the player
     */
    public void saveScore(String playerName) {
        // Capture the current score value at the time of the save
        int scoreToSave = currentScore;
        
        // Queue the save operation with the captured score
        try {
            saveQueue.put(new ScoreSaveTask(playerName, scoreToSave));
        } catch (InterruptedException e) {
            GameExceptions.handleInterrupted("ScoreManager save queue", e);
        }
    }

    /**
     * Performs the actual score saving operation (runs on ScoreManager thread).
     * This handles file I/O and leaderboard updates.
     * 
     * @param playerName the name of the player
     * @param score the score to save
     */
    private synchronized void performSaveScore(String playerName, int score) {
        leaderboard = rankScores(leaderboard, new ScoreEntry(playerName, score));
        
        fileHandler.saveScores(leaderboard);
    }

    static List<ScoreEntry> rankScores(List<ScoreEntry> existingScores, ScoreEntry newScore) {
        List<ScoreEntry> rankedScores = new ArrayList<>(existingScores);
        rankedScores.add(newScore);
        rankedScores.sort((first, second) -> Integer.compare(second.getScore(), first.getScore()));
        return rankedScores.size() <= 10
                ? rankedScores
                : new ArrayList<>(rankedScores.subList(0, 10));
    }

    /**
     * Gets a copy of the current leaderboard.
     * 
     * @return a copy of the leaderboard list
     */
    public synchronized List<ScoreEntry> getLeaderboard() {
        return new ArrayList<>(leaderboard);
    }

    /**
     * Stops the score manager thread.
     */
    public void stopThread() {
        running = false;
    }
}
