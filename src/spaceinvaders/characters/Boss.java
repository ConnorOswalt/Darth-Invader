package spaceinvaders.characters;

/**
 * Boss represents a mini-boss invader with health.
 * Spawns every ~30-50 kills as a challenge encounter.
 */
public class Boss {
    private int x, y;
    private int size;
    private int health;
    private final int maxHealth;
    private final String themePath;
    private final String shooterSkinPath;
    private final String themeName;
    private final boolean finalBoss;
    private final boolean miniBoss;
    private final int spawnX;
    private final int spawnY;
    private final long spawnTimeMs;
    private final float patternPhase;
    private static final int BASE_SIZE = 100;
    private static final int BASE_HEALTH = 5; // Takes 5 hits to kill
    private static final int FINAL_BOSS_HEALTH = 20; // Final boss takes 20 hits to kill
    private static final int MINI_BOSS_SIZE = 70;
    private static final int MINI_BOSS_HEALTH = 3;

    public Boss(int x, int y) {
        this(x, y, null, null, null, false, false);
    }

    public Boss(int x, int y, String themePath, String shooterSkinPath, String themeName) {
        this(x, y, themePath, shooterSkinPath, themeName, false, false);
    }

    public Boss(int x, int y, String themePath, String shooterSkinPath, String themeName, boolean finalBoss) {
        this(x, y, themePath, shooterSkinPath, themeName, finalBoss, false);
    }

    public Boss(int x, int y, String themePath, String shooterSkinPath, String themeName, boolean finalBoss, boolean miniBoss) {
        this.x = x;
        this.y = y;
        this.miniBoss = miniBoss;
        this.size = miniBoss ? MINI_BOSS_SIZE : BASE_SIZE;
        this.maxHealth = finalBoss ? FINAL_BOSS_HEALTH : (miniBoss ? MINI_BOSS_HEALTH : BASE_HEALTH);
        this.health = maxHealth;
        this.themePath = themePath;
        this.shooterSkinPath = shooterSkinPath;
        this.themeName = themeName;
        this.finalBoss = finalBoss;
        this.spawnX = x;
        this.spawnY = y;
        this.spawnTimeMs = System.currentTimeMillis();
        this.patternPhase = (float) ((x * 0.037) % (Math.PI * 2));
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

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void takeDamage(int damage) {
        this.health = Math.max(0, health - damage);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public float getHealthRatio() {
        return health / (float) maxHealth;
    }

    public String getThemePath() {
        return themePath;
    }

    public String getShooterSkinPath() {
        return shooterSkinPath;
    }

    public String getThemeName() {
        return themeName;
    }

    public boolean isFinalBoss() {
        return finalBoss;
    }

    public boolean isMiniBoss() {
        return miniBoss;
    }

    public int getSpawnX() {
        return spawnX;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public long getSpawnTimeMs() {
        return spawnTimeMs;
    }

    public float getPatternPhase() {
        return patternPhase;
    }
}
