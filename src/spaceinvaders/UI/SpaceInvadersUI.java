package spaceinvaders.UI;

import spaceinvaders.DataHandlers.MusicHandler;
import spaceinvaders.DataHandlers.MenuImplementations.ThemeImplementation;
import spaceinvaders.GameCalculator;
import spaceinvaders.ListenerActions;
import spaceinvaders.scores.LeaderboardPanel;
import spaceinvaders.scores.ScoreManager;
import spaceinvaders.characters.Bullet;
import spaceinvaders.characters.Explosion;
import spaceinvaders.characters.Invader;
import spaceinvaders.characters.DeathEffect;
import spaceinvaders.UI.JMenus.ShooterMenu;
import spaceinvaders.UI.JMenus.InvaderMenu;
import spaceinvaders.UI.JMenus.BulletMenu;
import spaceinvaders.UI.JMenus.MusicMenu;
import spaceinvaders.UI.JMenus.BackgroundMenu;
import spaceinvaders.UI.JMenus.EffectsMenu;
import spaceinvaders.UI.JMenus.ThemesMenu;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class SpaceInvadersUI extends JPanel implements KeyListener {
    public static class BossProjectile {
        private int x;
        private int y;
        private final int vx;
        private final int vy;
        private final int size;

        public BossProjectile(int x, int y, int vx, int vy, int size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getSize() {
            return size;
        }

        public void tick() {
            x += vx;
            y += vy;
        }

        public boolean isOffScreen(int width, int height) {
            return x < -size || x > width + size || y < -size || y > height + size;
        }
    }

    public enum SillyModifier {
        NONE,
        MOON_GRAVITY,
        ZOOMIES,
        TINY_PANIC,
        MIRROR,
        DISCO,
        PACIFIST
    }

    public enum PowerUpType {
        NONE,
        RAPID_FIRE,
        TRIPLE_SHOT,
        PIERCING,
        SHOTGUN,
        LASER_BEAM
    }

    public enum DifficultyPreset { EASY, NORMAL, HARD }
    private DifficultyPreset difficultyPreset = DifficultyPreset.NORMAL;

    private static SpaceInvadersUI activeInstance;
    private static final String DEATH_SOUND_EFFECT_PATH = "/resources/SoundEffects/player_death.wav";
    private static final String TITLE_SCREEN_MUSIC_PATH = "/resources/Music/Space_Invaders_Title.wav";
    private static final String FINAL_BOSS_THEME_PATH = "/resources/Themes/FinalBoss.json";
        private static final String DEFAULT_THEME_PATH = "/resources/Themes/Default.json";
    private static final String DEFAULT_SHOOTER_IMAGE_PATH = "/resources/Shooter/ShooterImage.png";
        private static final String[] THEME_PROGRESSION_PATHS = {
            "/resources/Themes/Doom.json",
            "/resources/Themes/Family_Guy.json",
            "/resources/Themes/Retro.json",
            "/resources/Themes/Sabaton.json"
        };

    private final Timer repaintTimer;
    public ArrayList<Invader> invaders;
    public ArrayList<Bullet> bullets;
    public ArrayList<Explosion> explosions;
    public ArrayList<DeathEffect> deathEffects;
    public List<spaceinvaders.characters.PowerUp> powerUps;
    public List<BossProjectile> bossProjectiles;
    public Random random;
    public boolean moveLeft, moveRight;
    public boolean fireHeld;
    private boolean explosionsEnabled = true;
    private final ListenerActions listenerActions;
    public final ImageSelection imageSelection;
    private final PaintingActions paintingActions;
    private int shooter_width = 50;
    private int shooter_height = 60;
    private int shooter_X_Coordinate = 200;
    private GameCalculator gameCalculator;
    private MusicHandler musicHandler;
    public static int breakpointcounter = 0;
    private ScoreManager scoreManager;
    private String playerNameForLeaderboard = "Anonymous";
    private static final int MAX_PLAYER_HEALTH = 100;
    private static final int PLAYER_HIT_DAMAGE = 34;
    private static final int INVADER_KILL_HEAL = 6;
    private int playerHealth = MAX_PLAYER_HEALTH;
    private boolean playerFlashing = false;
    private long playerFlashStartTime = 0;
    private static final long PLAYER_FLASH_DURATION = 300;
    private boolean gameOver = false;
    private boolean deathSoundPlayed = false;
    private boolean deathSoundEnabled = true;
    private boolean deathSoundLooping = false;
    private String deathSoundEffectPath = DEATH_SOUND_EFFECT_PATH;
    private String deathExplosionSoundEffectPath;
    private long gameOverFlashStartTime = 0;
    private static final long GAME_OVER_FLASH_DURATION = 500; // Flash for 500ms after game over
    private boolean paused = false;
    private boolean gameStarted = false;
    private boolean gameWon = false;
    private boolean sillinessModeEnabled = true;
    private SillyModifier activeSillyModifier = SillyModifier.NONE;
    private long activeModifierUntilMs = 0;
    private PowerUpType activePowerUp = PowerUpType.NONE;
    private long activePowerUpUntilMs = 0;
    public int laserBeamX = -1;
    public long laserBeamUntilMs = 0;
    private String announcerMessage = "";
    private long announcerMessageUntilMs = 0;
    private String fakeAchievementMessage = "";
    private long fakeAchievementUntilMs = 0;
    private String comboMessage = "";
    private long comboMessageUntilMs = 0;
    private int comboCount = 0;
    private long comboWindowUntilMs = 0;
    private String currentThemePath;

    // Theme integrity checker
    private String currentThemeExpectedMusicPath;
    private long nextThemeIntegrityCheckMs = 0;

    // Screen shake and difficulty tracking
    private int screenShakeOffsetX = 0;
    private int screenShakeOffsetY = 0;
    private long screenShakeEndTimeMs = 0;
    private int totalInvaderKills = 0;
    public ArrayList<spaceinvaders.characters.Boss> bosses;
    private static final int BOSS_SPAWN_MILESTONE = 30; // Spawn mini-boss every 30 kills
    private double difficultyMultiplier = 1.0; // Scales spawn rate and speed
    private final Deque<ThemeCycleEntry> themeProgressionQueue = new ArrayDeque<>();
    private boolean finalBossLevelActive = false;
    private boolean finalBossSpawned = false;
    private int miniBossKillCount = 0; // Tracks kills toward the next theme boss

    private static class ThemeCycleEntry {
        private final String themePath;
        private final String shooterSkinPath;
        private final String themeName;

        private ThemeCycleEntry(String themePath, String shooterSkinPath, String themeName) {
            this.themePath = themePath;
            this.shooterSkinPath = shooterSkinPath;
            this.themeName = themeName;
        }
    }

    // Constructor
    public SpaceInvadersUI() {
        activeInstance = this;

        // Timer is used only for repainting (UI thread)
        repaintTimer = new Timer(20, e -> repaint()); // 20ms delay for smoother animations
        // GameCalculator will be started after UI is fully initialized
        invaders = new ArrayList<>(); // Need to describe what ArrayList<> is.
        bullets = new ArrayList<>();
        explosions = new ArrayList<>();
        deathEffects = new ArrayList<>();
        bosses = new ArrayList<>();
        powerUps = new ArrayList<>();
        bossProjectiles = new ArrayList<>();
        random = new Random();
        moveLeft = false;
        moveRight = false;
        fireHeld = false;
        listenerActions = new ListenerActions();
        imageSelection = new ImageSelection();
        paintingActions = new PaintingActions();
        scoreManager = new ScoreManager();
        musicHandler = new MusicHandler();
        musicHandler.start();
        // For debugging

        // Set images
        imageSelection.setGameImages();

        setFocusable(true);
        addKeyListener(this);
        
        // Add ComponentListener to detect when the panel is properly sized
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (getWidth() > 0 && getHeight() > 0) {
                    // Apply Default theme first — sets stars background, no music
                    initializeThemeProgressionQueue();
                    ThemeImplementation.requestThemeChange(SpaceInvadersUI.this, DEFAULT_THEME_PATH);
                    startTitleScreenMusic();
                    repaint();
                    requestFocusInWindow();
                    // Now that UI is initialized, start the game calculator thread
                    gameCalculator = new GameCalculator(SpaceInvadersUI.this);
                    gameCalculator.start();
                    scoreManager.start(); // Start the score manager thread
                    removeComponentListener(this); // Remove listener after starting
                }
            }
        });
        
        // Start the repaint timer (UI thread)
        repaintTimer.start();
    }

    public static SpaceInvadersUI getActiveInstance() {
        return activeInstance;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        listenerActions.keyPressed(e, this);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        listenerActions.keyReleased(e, this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used. Not providing an implementation Violates Interface Segregation
        // Principle
        // Could be used for character keys.
    }

    @Override
    // Let's move these methods into a separate PaintUI class
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform originalTransform = g2d.getTransform();

        // Apply screen shake transform
        int shakeX = getScreenShakeOffsetX();
        int shakeY = getScreenShakeOffsetY();
        if (shakeX != 0 || shakeY != 0) {
            g2d.translate(shakeX, shakeY);
        }

        paintingActions.drawBackground(g, this);

        if (gameOver) {
            drawGameOver(g);
            g2d.setTransform(originalTransform);
            return;
        }

        if (!gameStarted) {
            drawStarterScreen(g);
            g2d.setTransform(originalTransform);
            return;
        }

        // Draw shooter
        paintingActions.drawShooter(g, this);

        // Draw falling invaders (as images)
        paintingActions.drawInvaders(g, this, imageSelection.getInvaderImage());

        // Draw bosses
        paintingActions.drawBosses(g, this, imageSelection.getBossImage());

        // Draw invader explosion effects
        paintingActions.drawExplosions(g, this);

        // Draw invader death-skin flash/fade effects
        paintingActions.drawDeathEffects(g, this);

        // Draw power-up collectibles
        paintingActions.drawPowerUps(g, this);

        // Draw boss projectiles
        paintingActions.drawBossProjectiles(g, this);

        // Draw bullets
        paintingActions.drawBullets(g, this);

        // Laser beam flash
        paintingActions.drawLaserBeam(g, this);

        // Draw player health hearts
        paintingActions.drawPlayerHealth(g, this);

        // Draw current score
        paintingActions.drawCurrentScore(g, this);

        // Active power-up HUD bar
        paintingActions.drawActivePowerUpHud(g, this);

        // Restore transform before drawing overlay
        g2d.setTransform(originalTransform);

        if (paused) {
            drawPauseScreen(g);
            return;
        }

        drawSillyOverlay(g);
    }

    private void drawSillyOverlay(Graphics g) {
        long now = System.currentTimeMillis();
        Graphics2D g2d = (Graphics2D) g;
        Font originalFont = g2d.getFont();
        AffineTransform originalTransform = g2d.getTransform();

        if (sillinessModeEnabled && activeSillyModifier == SillyModifier.DISCO && now < activeModifierUntilMs) {
            float phase = (now % 1200L) / 1200.0f;
            Color disco = Color.getHSBColor(phase, 0.7f, 1.0f);
            g2d.setColor(new Color(disco.getRed(), disco.getGreen(), disco.getBlue(), 45));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();

        if (!announcerMessage.isEmpty() && now < announcerMessageUntilMs) {
            int w = fm.stringWidth(announcerMessage);
            int x = (getWidth() - w) / 2;
            int y = Math.max(84, getHeight() / 3);
            g2d.setColor(new Color(0, 0, 0, 170));
            g2d.fillRoundRect(x - 18, y - 28, w + 36, 40, 16, 16);
            g2d.setColor(new Color(255, 255, 255, 35));
            g2d.drawRoundRect(x - 18, y - 28, w + 36, 40, 16, 16);
            g2d.setColor(Color.WHITE);
            g2d.drawString(announcerMessage, x, y);
        }

        if (sillinessModeEnabled && activeSillyModifier != SillyModifier.NONE && now < activeModifierUntilMs) {
            String effectName = getActiveEffectDisplayName();
            long remainingMs = Math.max(0, activeModifierUntilMs - now);
            float remainingRatio = Math.min(1.0f, remainingMs / 8000.0f);
            int panelWidth = 220;
            int panelHeight = 58;
            int panelX = getWidth() - panelWidth - 18;
            int panelY = 34;

            g2d.setColor(new Color(0, 0, 0, 170));
            g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);

            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(new Color(180, 220, 255));
            g2d.drawString("CURRENT EFFECT", panelX + 12, panelY + 18);

            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.setColor(Color.WHITE);
            g2d.drawString(effectName, panelX + 12, panelY + 38);

            g2d.setColor(new Color(45, 45, 45, 210));
            g2d.fillRoundRect(panelX + 12, panelY + 43, panelWidth - 24, 8, 8, 8);
            g2d.setColor(new Color(255, 120, 60));
            g2d.fillRoundRect(panelX + 12, panelY + 43, (int) ((panelWidth - 24) * remainingRatio), 8, 8, 8);
        }

        if (!fakeAchievementMessage.isEmpty() && now < fakeAchievementUntilMs) {
            g2d.setFont(new Font("Arial", Font.BOLD, 22));
            fm = g2d.getFontMetrics();
            int w = fm.stringWidth(fakeAchievementMessage);
            int x = (getWidth() - w) / 2;
            int y = getHeight() - 28;
            g2d.setColor(new Color(10, 10, 10, 190));
            g2d.fillRoundRect(x - 12, y - 22, w + 24, 30, 14, 14);
            g2d.setColor(new Color(90, 255, 150));
            g2d.drawString(fakeAchievementMessage, x, y);
        }

        if (!comboMessage.isEmpty() && now < comboMessageUntilMs) {
            float pulse = 1.0f + 0.18f * (float) Math.sin(now * 0.018);
            Font comboFont = new Font("Arial", Font.BOLD, 24);
            g2d.setFont(comboFont);
            FontMetrics comboFm = g2d.getFontMetrics();
            int w = comboFm.stringWidth(comboMessage);
            int x = getWidth() - w - 18;
            int y = (sillinessModeEnabled && activeSillyModifier != SillyModifier.NONE && now < activeModifierUntilMs)
                    ? 136
                    : 96;
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRoundRect(x - 16, y - 30, w + 32, 42, 14, 14);
            g2d.translate(x + w / 2.0, y - 10.0);
            g2d.scale(pulse, pulse);
            g2d.setColor(new Color(255, 220, 90));
            g2d.drawString(comboMessage, -comboFm.stringWidth(comboMessage) / 2, comboFm.getAscent() / 2);
            g2d.setTransform(originalTransform);
        }

        g2d.setFont(originalFont);
    }

    private String getActiveEffectDisplayName() {
        return switch (activeSillyModifier) {
            case MOON_GRAVITY -> "MOON GRAVITY";
            case ZOOMIES -> "ZOOMIES";
            case TINY_PANIC -> "TINY PANIC";
            case MIRROR -> "MIRROR";
            case DISCO -> "DISCO";
            case PACIFIST -> "WEAPON JAM";
            default -> "NONE";
        };
    }

    public int getShooterWidth() {
        return (shooter_width);
    }

    public int getShooterHeight() {
        return (shooter_height);
    }

    public int getShooter_X_Coordinate() {
        return (shooter_X_Coordinate);
    }

    public void setShooter_X_Coordinate(int shooter_X) {
        shooter_X_Coordinate = shooter_X;
    }

    /**
     * Creates and returns the menu bar for the game.
     * @return the JMenuBar with game menus
     */
    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new ShooterMenu());
        menuBar.add(new InvaderMenu());
        menuBar.add(new BulletMenu());
        menuBar.add(new MusicMenu());
        menuBar.add(new BackgroundMenu());
        menuBar.add(new EffectsMenu());
        menuBar.add(new ThemesMenu());
        return menuBar;
    }

    /**
     * Adds points to the current score.
     * 
     * @param points the number of points to add
     */
    public void addPoints(int points) {
        scoreManager.addPoints(points);
    }

    /**
     * Gets the ScoreManager instance.
     * 
     * @return the ScoreManager
     */
    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public String getPlayerNameForLeaderboard() {
        if (playerNameForLeaderboard == null || playerNameForLeaderboard.isBlank()) {
            return "Anonymous";
        }
        return playerNameForLeaderboard;
    }

    private void promptForPlayerNameIfNeeded() {
        if (!LeaderboardPanel.isLeaderboardEnabled()) {
            return;
        }

        String initialValue = getPlayerNameForLeaderboard();
        String playerName = JOptionPane.showInputDialog(this,
                "Enter your name for the leaderboard:",
                initialValue);

        if (playerName == null || playerName.trim().isEmpty()) {
            playerNameForLeaderboard = "Anonymous";
            return;
        }

        playerNameForLeaderboard = playerName.trim();
    }

    public MusicHandler getMusicHandler() {
        return musicHandler;
    }

    public void setCurrentThemePath(String currentThemePath) {
        this.currentThemePath = currentThemePath;
    }

    /**
     * Called by ThemeImplementation after music is resolved.
     * Stores the expected music path so the integrity checker can verify it later.
     */
    public void setCurrentThemeExpectedMusicPath(String path) {
        this.currentThemeExpectedMusicPath = path;
    }

    /**
     * Periodically verifies that the active theme's music is still playing.
     * If it has drifted, re-applies the theme.
     */
    public void checkThemeIntegrity() {
        long now = System.currentTimeMillis();
        if (now < nextThemeIntegrityCheckMs) return;
        nextThemeIntegrityCheckMs = now + 3000;

        if (currentThemePath == null || currentThemePath.isBlank()) return;

        // Only check if this theme specifies music (music_enabled: false themes have no expected track)
        if (currentThemeExpectedMusicPath == null) return;

        // If the expected track is no longer playing, re-apply the theme to restore it
        if (musicHandler != null && !musicHandler.isTrackActive(currentThemeExpectedMusicPath)) {
            ThemeImplementation.requestThemeChange(this, currentThemePath);
        }
    }

    public boolean isSillinessModeEnabled() {
        return sillinessModeEnabled;
    }

    public void setSillinessModeEnabled(boolean sillinessModeEnabled) {
        this.sillinessModeEnabled = sillinessModeEnabled;
        if (!sillinessModeEnabled) {
            clearActiveSillyModifier();
        }
    }

    public SillyModifier getActiveSillyModifier() {
        return activeSillyModifier;
    }

    public boolean isMirrorControlsActive() {
        return sillinessModeEnabled && activeSillyModifier == SillyModifier.MIRROR
                && System.currentTimeMillis() < activeModifierUntilMs;
    }

    public boolean isPacifistModeActive() {
        return sillinessModeEnabled && activeSillyModifier == SillyModifier.PACIFIST
                && System.currentTimeMillis() < activeModifierUntilMs;
    }

    public void activateSillyModifier(SillyModifier modifier, long durationMs, String bannerText) {
        this.activeSillyModifier = modifier;
        this.activeModifierUntilMs = System.currentTimeMillis() + durationMs;
        setAnnouncerMessage(bannerText, 2600);
    }

    public void clearActiveSillyModifier() {
        this.activeSillyModifier = SillyModifier.NONE;
        this.activeModifierUntilMs = 0;
    }

    public PowerUpType getActivePowerUp() {
        if (activePowerUp != PowerUpType.NONE && System.currentTimeMillis() >= activePowerUpUntilMs) {
            activePowerUp = PowerUpType.NONE;
        }
        return activePowerUp;
    }

    public long getActivePowerUpUntilMs() {
        return activePowerUpUntilMs;
    }

    public void activatePowerUp(PowerUpType type, long durationMs) {
        this.activePowerUp = type;
        this.activePowerUpUntilMs = System.currentTimeMillis() + durationMs;
        String name = switch (type) {
            case RAPID_FIRE  -> "RAPID FIRE!";
            case TRIPLE_SHOT -> "TRIPLE SHOT!";
            case PIERCING    -> "PIERCING ROUNDS!";
            case SHOTGUN     -> "SHOTGUN BLAST!";
            case LASER_BEAM  -> "LASER BEAM!";
            default          -> "";
        };
        if (!name.isEmpty()) setAnnouncerMessage(name, 2000);
    }

    public void clearActivePowerUp() {
        this.activePowerUp = PowerUpType.NONE;
        this.activePowerUpUntilMs = 0;
    }

    public boolean isModifierExpired() {
        return activeSillyModifier != SillyModifier.NONE && System.currentTimeMillis() >= activeModifierUntilMs;
    }

    public void setAnnouncerMessage(String message, long durationMs) {
        this.announcerMessage = message == null ? "" : message;
        this.announcerMessageUntilMs = System.currentTimeMillis() + durationMs;
    }

    public void setFakeAchievementMessage(String message, long durationMs) {
        this.fakeAchievementMessage = message == null ? "" : message;
        this.fakeAchievementUntilMs = System.currentTimeMillis() + durationMs;
    }

    public void recordInvaderDefeatCombo() {
        long now = System.currentTimeMillis();
        if (now > comboWindowUntilMs) {
            comboCount = 0;
        }
        comboCount++;
        comboWindowUntilMs = now + 1800;

        if (comboCount >= 2) {
            comboMessage = getComboTitle(comboCount) + " x" + comboCount;
            comboMessageUntilMs = now + 1400;
        }
    }

    private String getComboTitle(int comboCount) {
        if (comboCount >= 8) {
            return "Cosmic Bonk";
        }
        if (comboCount >= 5) {
            return "Mega Bonk";
        }
        return "Double Bonk";
    }

    public int getComboMultiplier() {
        if (comboCount >= 5) return 3;
        if (comboCount >= 2) return 2;
        return 1;
    }

    public void setDeathSoundEffectPath(String deathSoundEffectPath) {
        if (deathSoundEffectPath == null || deathSoundEffectPath.isBlank()) {
            this.deathSoundEffectPath = DEATH_SOUND_EFFECT_PATH;
            return;
        }
        this.deathSoundEffectPath = deathSoundEffectPath;
    }

    public void setDeathSoundEnabled(boolean deathSoundEnabled) {
        this.deathSoundEnabled = deathSoundEnabled;
    }

    public void setDeathSoundLooping(boolean deathSoundLooping) {
        this.deathSoundLooping = deathSoundLooping;
    }

    public String getDefaultDeathSoundEffectPath() {
        return DEATH_SOUND_EFFECT_PATH;
    }

    public void setDeathExplosionSoundEffectPath(String deathExplosionSoundEffectPath) {
        this.deathExplosionSoundEffectPath = deathExplosionSoundEffectPath;
    }

    public void clearDeathExplosionSoundEffectPath() {
        this.deathExplosionSoundEffectPath = null;
    }

    public String getDeathExplosionSoundEffectPath() {
        return deathExplosionSoundEffectPath;
    }

    public boolean isExplosionsEnabled() {
        return explosionsEnabled;
    }

    public void setExplosionsEnabled(boolean explosionsEnabled) {
        this.explosionsEnabled = explosionsEnabled;
        if (!explosionsEnabled) {
            synchronized (this) {
                explosions.clear();
            }
        }
    }

    public int getPlayerHealth() {
        return playerHealth;
    }

    public int getMaxPlayerHealth() {
        return MAX_PLAYER_HEALTH;
    }

    public void healPlayerFromInvaderKill() {
        if (gameOver || playerHealth <= 0) {
            return;
        }

        int healAmount = Math.round(INVADER_KILL_HEAL * getDifficultyHealMultiplier());
        playerHealth = Math.min(MAX_PLAYER_HEALTH, playerHealth + healAmount);
    }

    public void damagePlayer() {
        if (playerHealth <= 0 || gameOver) {
            return;
        }

        int damage = Math.round(PLAYER_HIT_DAMAGE * getDifficultyDamageMultiplier());
        playerHealth = Math.max(0, playerHealth - damage);
        playerFlashing = true;
        playerFlashStartTime = System.currentTimeMillis();
        repaint();

        if (playerHealth <= 0) {
            setGameOver(true);
        }
    }

    public boolean isPlayerFlashing() {
        if (playerFlashing && System.currentTimeMillis() - playerFlashStartTime > PLAYER_FLASH_DURATION) {
            playerFlashing = false;
        }
        return playerFlashing;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        if (gameOver && this.gameOver) {
            return;
        }

        this.gameOver = gameOver;
        if (gameOver) {
            fireHeld = false;
            gameOverFlashStartTime = System.currentTimeMillis(); // Record when game ended
            if (gameCalculator != null) {
                gameCalculator.stopThread();
            }
            if (!gameWon && musicHandler != null) {
                musicHandler.stopCurrentTrack();
                if (!deathSoundPlayed && deathSoundEnabled) {
                    if (deathSoundLooping) {
                        long gifDurationMs = imageSelection.getDeathScreenGifDurationMs();
                        if (gifDurationMs > 0) {
                            musicHandler.playLoopingEffectSyncedTo(deathSoundEffectPath, gifDurationMs);
                        } else {
                            musicHandler.playLoopingEffect(deathSoundEffectPath);
                        }
                    } else {
                        musicHandler.playOneShotEffect(deathSoundEffectPath);
                    }
                    deathSoundPlayed = true;
                }
            }
            // DON'T stop the repaintTimer - we need it to display the game over screen
            repaint();
        }
    }

    private void drawPauseScreen(Graphics g) {
        // Draw semi-transparent overlay to darken background
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw pause text in bright green (smaller font)
        g.setColor(new Color(0, 255, 0)); // Bright green
        g.setFont(new Font("Arial", Font.BOLD, 32));
        FontMetrics fm = g.getFontMetrics();
        String pauseText = "Paused Press P to resume";
        int textWidth = fm.stringWidth(pauseText);
        int textHeight = fm.getAscent();
        g.drawString(pauseText, (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2);
    }

    private void drawStarterScreen(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(new Color(0, 0, 0, 145));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        int panelWidth = Math.min(520, getWidth() - 40);
        int panelHeight = Math.min(340, getHeight() - 80);
        int panelX = (getWidth() - panelWidth) / 2;
        int panelY = (getHeight() - panelHeight) / 2;

        g2d.setColor(new Color(15, 18, 28, 225));
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 24, 24);
        g2d.setColor(new Color(120, 210, 255, 120));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 24, 24);

        long now = System.currentTimeMillis();
        float glowPulse = 0.72f + 0.38f * (float) Math.sin(now * 0.006);

        g2d.setFont(new Font("Arial", Font.BOLD, 44));
        String title = "DARTH INVADERS";
        FontMetrics titleMetrics = g2d.getFontMetrics();
        int titleX = panelX + (panelWidth - titleMetrics.stringWidth(title)) / 2;
        int titleY = panelY + 82;

        // Deep 3D extrusion layers (drawn per character for cleaner depth edges)
        int titleDepth = 14;
        for (int depth = titleDepth; depth >= 1; depth--) {
            int shade = Math.max(14, 120 - depth * 6);
            g2d.setColor(new Color(8, shade, 170, 195));
            int xOffset = titleX;
            for (int i = 0; i < title.length(); i++) {
                char c = title.charAt(i);
                g2d.drawString(String.valueOf(c), xOffset + depth, titleY + depth);
                xOffset += titleMetrics.charWidth(c);
            }
        }

        // Stronger outer glow passes
        int glowAlpha = (int) (95 + 105 * glowPulse);
        for (int radius = 10; radius >= 2; radius--) {
            int alphaScale = Math.max(26, glowAlpha - radius * 12);
            g2d.setColor(new Color(40, 180, 255, alphaScale));
            g2d.drawString(title, titleX - radius, titleY);
            g2d.drawString(title, titleX + radius, titleY);
            g2d.drawString(title, titleX, titleY - radius);
            g2d.drawString(title, titleX, titleY + radius);

            g2d.setColor(new Color(90, 245, 255, Math.max(20, alphaScale - 10)));
            g2d.drawString(title, titleX - radius, titleY - radius / 2);
            g2d.drawString(title, titleX + radius, titleY + radius / 2);
        }

        // Character shadows and face pass
        int xOffset = titleX;
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            String letter = String.valueOf(c);

            // Per-character layered shadows
            g2d.setColor(new Color(0, 0, 0, 185));
            g2d.drawString(letter, xOffset + 4, titleY + 5);
            g2d.setColor(new Color(0, 0, 0, 105));
            g2d.drawString(letter, xOffset + 7, titleY + 8);

            // Main title face
            g2d.setColor(new Color(110, 240, 255));
            g2d.drawString(letter, xOffset, titleY);

            // Crisp highlight bevel
            g2d.setColor(new Color(235, 255, 255, 205));
            g2d.drawString(letter, xOffset - 1, titleY - 1);

            xOffset += titleMetrics.charWidth(c);
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.setColor(new Color(240, 240, 240));
        String subtitle = "Defend the planet. Survive the chaos.";
        FontMetrics subtitleMetrics = g2d.getFontMetrics();
        int subtitleX = panelX + (panelWidth - subtitleMetrics.stringWidth(subtitle)) / 2;
        g2d.drawString(subtitle, subtitleX, panelY + 125);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String startText = "Press ENTER or SPACE to start";
        FontMetrics startMetrics = g2d.getFontMetrics();
        int startX = panelX + (panelWidth - startMetrics.stringWidth(startText)) / 2;
        int startY = panelY + panelHeight - 92;
        g2d.setColor(new Color(255, 220, 90));
        g2d.drawString(startText, startX, startY);

        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        String controls = "Move: LEFT/RIGHT    Fire: SPACE    Pause: P";
        FontMetrics controlsMetrics = g2d.getFontMetrics();
        int controlsX = panelX + (panelWidth - controlsMetrics.stringWidth(controls)) / 2;
        g2d.setColor(new Color(190, 215, 255));
        g2d.drawString(controls, controlsX, panelY + panelHeight - 52);

        // Difficulty selector
        g2d.setFont(new Font("Arial", Font.BOLD, 15));
        Color diffColor = switch (difficultyPreset) {
            case EASY  -> new Color(80, 220, 110);
            case HARD  -> new Color(255, 110, 110);
            default    -> new Color(255, 220, 90);
        };
        String diffText = "[D] Difficulty: " + difficultyPreset.name();
        FontMetrics diffFm = g2d.getFontMetrics();
        int diffX = panelX + (panelWidth - diffFm.stringWidth(diffText)) / 2;
        g2d.setColor(diffColor);
        g2d.drawString(diffText, diffX, panelY + panelHeight - 26);
    }

    private void drawGameOver(Graphics g) {
        Image deathScreenImage = imageSelection.getDeathScreenImage();
        if (deathScreenImage != null) {
            g.drawImage(deathScreenImage, 0, 0, getWidth(), getHeight(), this);
        }

        // Draw red flash effect for a short duration after death game over
        if (!gameWon && System.currentTimeMillis() - gameOverFlashStartTime < GAME_OVER_FLASH_DURATION) {
            g.setColor(new Color(255, 0, 0, 180)); // Semi-transparent red
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, 
                             java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        String gameOverText = gameWon ? "YOU WIN" : "GAME OVER";
        int textHeight = fm.getAscent();
        int textWidth = fm.stringWidth(gameOverText);
        int baseX = (getWidth() - textWidth) / 2;
        int baseY = (getHeight() + textHeight) / 2;
        
        // Calculate wave animation
        long timeSinceGameOver = System.currentTimeMillis() - gameOverFlashStartTime;
        double wavePhase = (timeSinceGameOver % 2000) / 2000.0; // 2 second cycle
        
        // Draw each letter with wave effect
        int xOffset = baseX;
        for (int i = 0; i < gameOverText.length(); i++) {
            char c = gameOverText.charAt(i);
            
            // Calculate vertical offset for this letter using sine wave
            double letterPhase = wavePhase * Math.PI * 2 + (i * 0.4);
            double yOffset = Math.sin(letterPhase) * 8; // Wave amplitude of 8 pixels
            
            int charWidth = fm.charWidth(c);
            
            // Draw shadow
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.drawString(String.valueOf(c), xOffset + 3, (int)(baseY + yOffset + 3));
            
            // Draw letter
            g2d.setColor(gameWon ? new Color(80, 240, 130) : Color.RED);
            g2d.drawString(String.valueOf(c), xOffset, (int)(baseY + yOffset));
            
            xOffset += charWidth;
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        fm = g2d.getFontMetrics();
        String restartText = gameWon ? "Press R to play again" : "Press R to restart";
        textWidth = fm.stringWidth(restartText);
        int restartX = (getWidth() - textWidth) / 2;
        int restartY = (getHeight() + textHeight) / 2 + 115;

        // Draw final score
        int finalScore = scoreManager.getCurrentScore();
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics scoreFm = g2d.getFontMetrics();
        String finalScoreText = "Score: " + finalScore;
        int scoreTextX = (getWidth() - scoreFm.stringWidth(finalScoreText)) / 2;
        int scoreTextY = (getHeight() + textHeight) / 2 + 52;
        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.drawString(finalScoreText, scoreTextX + 2, scoreTextY + 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(finalScoreText, scoreTextX, scoreTextY);

        // New high score indicator
        java.util.List<spaceinvaders.scores.ScoreEntry> lb = scoreManager.getLeaderboard();
        int topScore = lb.isEmpty() ? 0 : lb.get(0).getScore();
        if (finalScore > 0 && finalScore >= topScore) {
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics hsFm = g2d.getFontMetrics();
            String hsText = "NEW HIGH SCORE!";
            int hsX = (getWidth() - hsFm.stringWidth(hsText)) / 2;
            int hsY = scoreTextY + 34;
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.drawString(hsText, hsX + 2, hsY + 2);
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(hsText, hsX, hsY);
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        fm = g2d.getFontMetrics();
        
        // Draw shadow for restart text
        g2d.setColor(new Color(0, 0, 0, 150)); // Dark shadow
        g2d.drawString(restartText, restartX + 2, restartY + 2);
        
        // Draw restart text
        g2d.setColor(Color.YELLOW);
        g2d.drawString(restartText, restartX, restartY);
    }

    public void restartGame() {
        synchronized (this) {
            invaders.clear();
            bullets.clear();
            explosions.clear();
            deathEffects.clear();
            powerUps.clear();
            bossProjectiles.clear();
            bosses.clear();
            clearActivePowerUp();
            laserBeamX = -1;
            laserBeamUntilMs = 0;
            gameOver = false;
            gameWon = false;
            gameStarted = true;
            deathSoundPlayed = false;
            gameOverFlashStartTime = 0; // Reset game over flash timer
            if (musicHandler != null) {
                musicHandler.stopLoopingEffect();
            }
            playerHealth = MAX_PLAYER_HEALTH;
            playerFlashing = false;
            shooter_X_Coordinate = 200;
            moveLeft = false;
            moveRight = false;
            fireHeld = false;
            clearActiveSillyModifier();
            announcerMessage = "";
            fakeAchievementMessage = "";
            comboMessage = "";
            comboCount = 0;
            comboWindowUntilMs = 0;
            clearTemporaryRickRestore();
            totalInvaderKills = 0;
            difficultyMultiplier = 1.0;
            screenShakeOffsetX = 0;
            screenShakeOffsetY = 0;
            screenShakeEndTimeMs = 0;
            currentThemeExpectedMusicPath = null;
            nextThemeIntegrityCheckMs = 0;
            themeProgressionQueue.clear();
            finalBossLevelActive = false;
            finalBossSpawned = false;
        }

        // Reset the current score for the new game
        scoreManager.resetScore();

        gameCalculator = new GameCalculator(this);
        gameCalculator.start();
        // Don't restart scoreManager - it's a daemon thread that keeps running
        repaintTimer.start();

        initializeThemeProgressionQueue();
        if (musicHandler != null) {
            musicHandler.clearInterruptedTrack();
            musicHandler.stopCurrentTrack();
        }
        ThemeImplementation.requestThemeChange(this, DEFAULT_THEME_PATH);
    }

    // === Screen Shake & Difficulty Tracking Methods ===

    /**
     * Triggers screen shake effect for the specified duration.
     * @param durationMs how long the shake lasts
     * @param intensity how far to offset (pixels)
     */
    public void triggerScreenShake(long durationMs, int intensity) {
        screenShakeEndTimeMs = System.currentTimeMillis() + durationMs;
        // Initial shake offset
        int offsetX = random.nextInt(intensity * 2 + 1) - intensity;
        int offsetY = random.nextInt(intensity * 2 + 1) - intensity;
        screenShakeOffsetX = offsetX;
        screenShakeOffsetY = offsetY;
    }

    /**
     * Updates screen shake offset (called during render).
     * @return true if still shaking, false if ended
     */
    public boolean updateScreenShake() {
        long now = System.currentTimeMillis();
        if (now >= screenShakeEndTimeMs) {
            screenShakeOffsetX = 0;
            screenShakeOffsetY = 0;
            return false;
        }

        // Randomize shake offset each frame for jittery effect
        int intensity = 5;
        screenShakeOffsetX = random.nextInt(intensity * 2 + 1) - intensity;
        screenShakeOffsetY = random.nextInt(intensity * 2 + 1) - intensity;
        return true;
    }

    public int getScreenShakeOffsetX() {
        return screenShakeOffsetX;
    }

    public int getScreenShakeOffsetY() {
        return screenShakeOffsetY;
    }

    private void initializeThemeProgressionQueue() {
        List<ThemeCycleEntry> entries = new ArrayList<>();
        for (String themePath : THEME_PROGRESSION_PATHS) {
            String shooterPath = ThemeImplementation.readOptionalThemeResourcePath(themePath, "shooter");
            String themeName = ThemeImplementation.readOptionalThemeString(themePath, "name");
            if (themeName == null || themeName.isBlank()) {
                themeName = themePath.substring(themePath.lastIndexOf('/') + 1).replace(".json", "");
            }
            entries.add(new ThemeCycleEntry(themePath, shooterPath, themeName));
        }

        Collections.shuffle(entries, random);
        synchronized (this) {
            themeProgressionQueue.clear();
            themeProgressionQueue.addAll(entries);
            finalBossLevelActive = false;
            finalBossSpawned = false;
        }
    }

    public void handleBossDefeated(spaceinvaders.characters.Boss defeatedBoss) {
        if (defeatedBoss == null) {
            return;
        }

        if (defeatedBoss.isFinalBoss()) {
            addPoints(1500);
            totalInvaderKills++;
            difficultyMultiplier = 1.0 + (totalInvaderKills / 20) * 0.1;
            triggerScreenShake(650, 14);
            setAnnouncerMessage("FINAL BOSS DESTROYED!", 2600);
            synchronized (this) {
                finalBossLevelActive = false;
            }
            gameWon = true;
            setGameOver(true);
            return;
        }

        if (defeatedBoss.isMiniBoss()) {
            addPoints(200);
            totalInvaderKills++;
            difficultyMultiplier = 1.0 + (totalInvaderKills / 20) * 0.1;
            triggerScreenShake(300, 7);
            synchronized (this) {
                miniBossKillCount++;
                int remaining = 5 - (miniBossKillCount % 5);
                if (miniBossKillCount % 5 == 0) {
                    setAnnouncerMessage("5 MINI-BOSSES DOWN! BOSS INCOMING!", 2200);
                    spawnThemeBoss();
                    miniBossKillCount = 0;
                } else {
                    setAnnouncerMessage("MINI-BOSS DOWN! " + remaining + " MORE TO BOSS!", 2000);
                }
            }
            return;
        }

        addPoints(500);
        totalInvaderKills++;
        difficultyMultiplier = 1.0 + (totalInvaderKills / 20) * 0.1;
        triggerScreenShake(400, 10);

        ThemeCycleEntry nextEntry;
        synchronized (this) {
            nextEntry = themeProgressionQueue.peekFirst();
            if (nextEntry != null
                    && ((nextEntry.themePath == null && defeatedBoss.getThemePath() == null)
                    || (nextEntry.themePath != null && nextEntry.themePath.equals(defeatedBoss.getThemePath())))) {
                themeProgressionQueue.removeFirst();
            }
        }

        String defeatedThemePath = defeatedBoss.getThemePath();
        String defeatedThemeName = defeatedBoss.getThemeName();
        if (defeatedThemeName == null || defeatedThemeName.isBlank()) {
            defeatedThemeName = "THEME";
        }

        if (defeatedThemePath != null && !defeatedThemePath.isBlank()) {
            setAnnouncerMessage(defeatedThemeName.toUpperCase() + " UNLOCKED!", 2000);
            ThemeImplementation.requestThemeChange(this, defeatedThemePath);
        } else {
            setAnnouncerMessage("BOSS DESTROYED!", 2000);
        }

        synchronized (this) {
            if (themeProgressionQueue.isEmpty()) {
                startFinalBossLevel();
            }
        }
    }

    /**
     * Records an invader kill and updates difficulty scaling.
     * Triggers boss spawn at milestones.
     */
    public void recordInvaderKill() {
        if (finalBossLevelActive) {
            return;
        }

        totalInvaderKills++;

        // Update difficulty multiplier: every 20 kills, increase by 10%
        difficultyMultiplier = 1.0 + (totalInvaderKills / 20) * 0.1;

        // Spawn mini-boss at milestones (every 30 kills)
        if (totalInvaderKills % BOSS_SPAWN_MILESTONE == 0 && totalInvaderKills > 0) {
            spawnMiniBoss();
        }

        triggerScreenShake(200, 4); // Shake camera on invader kill
        healPlayerFromInvaderKill();
    }

    public int getTotalInvaderKills() {
        return totalInvaderKills;
    }

    public double getDifficultyMultiplier() {
        return difficultyMultiplier;
    }

    public DifficultyPreset getDifficultyPreset() {
        return difficultyPreset;
    }

    public void cycleDifficultyPreset() {
        difficultyPreset = switch (difficultyPreset) {
            case EASY -> DifficultyPreset.NORMAL;
            case NORMAL -> DifficultyPreset.HARD;
            case HARD -> DifficultyPreset.EASY;
        };
    }

    public float getDifficultySpawnMultiplier() {
        return switch (difficultyPreset) {
            case EASY -> 0.65f;
            case HARD -> 1.45f;
            default -> 1.0f;
        };
    }

    public float getDifficultySpeedMultiplier() {
        return switch (difficultyPreset) {
            case EASY -> 0.70f;
            case HARD -> 1.30f;
            default -> 1.0f;
        };
    }

    public float getDifficultyDamageMultiplier() {
        return switch (difficultyPreset) {
            case EASY -> 0.50f;
            case HARD -> 1.50f;
            default -> 1.0f;
        };
    }

    public float getDifficultyHealMultiplier() {
        return switch (difficultyPreset) {
            case EASY -> 2.0f;
            case HARD -> 0.50f;
            default -> 1.0f;
        };
    }

    public int getRemainingThemeCount() {
        synchronized (this) {
            if (finalBossLevelActive) {
                return gameWon ? 0 : 1;
            }
            return themeProgressionQueue.size();
        }
    }

    public String getNextThemeBossName() {
        synchronized (this) {
            if (finalBossLevelActive) {
                return gameWon ? "NONE" : "FINAL BOSS";
            }
            ThemeCycleEntry next = themeProgressionQueue.peekFirst();
            if (next == null || next.themeName == null || next.themeName.isBlank()) {
                return "NONE";
            }
            return next.themeName;
        }
    }

    private void spawnMiniBoss() {
        synchronized (this) {
            if (finalBossLevelActive || themeProgressionQueue.isEmpty()) {
                return;
            }
            // Only one mini-boss active at a time
            for (spaceinvaders.characters.Boss b : bosses) {
                if (b.isMiniBoss()) return;
            }
        }

        int bossX = random.nextInt(Math.max(1, getWidth() - 70));
        spaceinvaders.characters.Boss miniBoss = new spaceinvaders.characters.Boss(
                bossX, -70, null, null, null, false, true);
        synchronized (this) {
            bosses.add(miniBoss);
        }
        setAnnouncerMessage("MINI-BOSS INCOMING!", 2000);
        triggerScreenShake(200, 6);
    }

    private void spawnThemeBoss() {
        ThemeCycleEntry nextTheme;
        synchronized (this) {
            if (themeProgressionQueue.isEmpty() || finalBossLevelActive || finalBossSpawned) {
                return;
            }
            // Don't spawn a second theme boss if one is already active
            for (spaceinvaders.characters.Boss b : bosses) {
                if (!b.isMiniBoss() && !b.isFinalBoss()) return;
            }
            nextTheme = themeProgressionQueue.peekFirst();
        }

        // Spawn theme boss at random x position at top
        int bossX = random.nextInt(Math.max(1, getWidth() - 100));
        spaceinvaders.characters.Boss boss = new spaceinvaders.characters.Boss(
                bossX,
                0,
                nextTheme == null ? null : nextTheme.themePath,
                nextTheme == null ? null : nextTheme.shooterSkinPath,
                nextTheme == null ? null : nextTheme.themeName);
        synchronized (this) {
            bosses.add(boss);
        }
        if (nextTheme != null && nextTheme.themeName != null && !nextTheme.themeName.isBlank()) {
            setAnnouncerMessage("BOSS INCOMING: " + nextTheme.themeName.toUpperCase(), 2200);
        } else {
            setAnnouncerMessage("BOSS INCOMING!", 2000);
        }
        triggerScreenShake(300, 8);
    }

    private void startFinalBossLevel() {
        if (finalBossSpawned || gameWon || gameOver) {
            return;
        }

        String finalBossShooterPath = ThemeImplementation.readOptionalThemeResourcePath(FINAL_BOSS_THEME_PATH, "shooter");
        String finalBossMusicPath = ThemeImplementation.readOptionalThemeResourcePath(FINAL_BOSS_THEME_PATH, "music");
        String finalBossDeathSoundPath = ThemeImplementation.readOptionalThemeResourcePath(FINAL_BOSS_THEME_PATH, "deathsound");
        String finalBossName = ThemeImplementation.readOptionalThemeString(FINAL_BOSS_THEME_PATH, "name");
        if (finalBossName == null || finalBossName.isBlank()) {
            finalBossName = "Final Boss";
        }

        finalBossLevelActive = true;
        finalBossSpawned = true;
        invaders.clear();
        powerUps.clear();
        bossProjectiles.clear();

        if (finalBossDeathSoundPath != null) {
            setDeathSoundEffectPath(finalBossDeathSoundPath);
        }

        if (finalBossMusicPath != null && musicHandler != null) {
            setCurrentThemeExpectedMusicPath(finalBossMusicPath);
            if (hasGameStarted()) {
                musicHandler.selectTrack(finalBossMusicPath);
            }
        }

        int bossX = Math.max(0, (getWidth() - 100) / 2);
        spaceinvaders.characters.Boss finalBoss = new spaceinvaders.characters.Boss(
                bossX,
                40,
                FINAL_BOSS_THEME_PATH,
                finalBossShooterPath != null ? finalBossShooterPath : DEFAULT_SHOOTER_IMAGE_PATH,
                finalBossName,
                true);
        bosses.add(finalBoss);
        setAnnouncerMessage((finalBossName + " LEVEL!").toUpperCase(), 2400);
        triggerScreenShake(500, 12);
    }

    public boolean isFinalBossLevelActive() {
        return finalBossLevelActive;
    }

    /**
     * Gracefully shuts down the game calculator thread and stops the repaint timer.
     * Call this before closing the application.
     */
    public void shutdown() {
        if (gameCalculator != null && gameCalculator.isAlive()) {
            gameCalculator.stopThread();
        }
        if (repaintTimer != null) {
            repaintTimer.stop();
        }
        if (scoreManager != null && scoreManager.isAlive()) {
            scoreManager.stopThread();
        }
        if (musicHandler != null && musicHandler.isAlive()) {
            musicHandler.stopThread();
        }
    }

    public boolean hasGameStarted() {
        return gameStarted;
    }

    public void startGameFromStarterScreen() {
        promptForPlayerNameIfNeeded();
        gameStarted = true;
        paused = false;
        if (musicHandler != null) {
            if (currentThemeExpectedMusicPath != null && !currentThemeExpectedMusicPath.isBlank()) {
                musicHandler.selectTrack(currentThemeExpectedMusicPath);
            } else {
                musicHandler.stopCurrentTrack();
            }
        }
        requestFocusInWindow();
        repaint();
    }

    private void startTitleScreenMusic() {
        if (musicHandler == null) {
            return;
        }

        if (SpaceInvadersUI.class.getResource(TITLE_SCREEN_MUSIC_PATH) == null) {
            return;
        }

        musicHandler.selectTrack(TITLE_SCREEN_MUSIC_PATH);
    }

}
