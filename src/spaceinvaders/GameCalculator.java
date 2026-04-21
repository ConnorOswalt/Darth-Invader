package spaceinvaders;

import spaceinvaders.UI.SpaceInvadersUI;
import spaceinvaders.characters.Bullet;
import spaceinvaders.characters.DeathEffect;
import spaceinvaders.characters.Explosion;
import spaceinvaders.characters.Invader;
import spaceinvaders.characters.Boss;
import spaceinvaders.characters.PowerUp;

import java.awt.*;
import java.util.Iterator;

/**
 * GameCalculator runs on a separate thread and handles:
 * - Position updates for the shooter, invaders, and bullets
 * - Spawning of new invaders
 * - Collision detection between bullets and invaders
 * 
 * The UI rendering and event handling remain on the AWT-EventQueue-0 thread.
 */
public class GameCalculator extends Thread {
    private final SpaceInvadersUI game;
    private volatile boolean running = true;
    private static final long UPDATE_INTERVAL_MS = 20; // Same as original timer interval
    private static final long FIRE_INTERVAL_MS = 150;
    private static final long BOSS_FIRE_INTERVAL_MS = 650;
    private static final long EXPLOSION_DURATION_MS = 300;
        private static final long MODIFIER_DURATION_MS = 8000;
    private static final long POWER_UP_DURATION_MS = 8000;
    private static final long LASER_BEAM_FLASH_MS = 220;
        private static final int LASER_BEAM_HALF_WIDTH_PX = 26;
    private long lastFireTimeMs = 0;
    private long lastBossFireTimeMs = 0;
    private long nextModifierRollMs = System.currentTimeMillis() + 15000;
    private long nextAchievementMs = System.currentTimeMillis() + 12000;
    private long nextPowerUpSpawnMs = System.currentTimeMillis() + 18000;
    private int spawnCounter = 0;

        private static final String[] FAKE_ACHIEVEMENTS = {
            "Achievement Unlocked: Professional Button Presser",
            "Achievement Unlocked: Certified Space Janitor",
            "Achievement Unlocked: Panic Strategist",
            "Achievement Unlocked: Physics? Optional"
        };

    public GameCalculator(SpaceInvadersUI game) {
        this.game = game;
        setDaemon(true);
        setName("GameCalculator-Thread");
    }

    @Override
    public void run() {
        long lastUpdateTime = System.currentTimeMillis();
        
        while (running) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - lastUpdateTime;
            
            if (elapsed >= UPDATE_INTERVAL_MS) {
                updateGameState();
                lastUpdateTime = currentTime;
            } else {
                // Sleep to avoid busy waiting
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    GameExceptions.handleInterrupted("GameCalculator loop", e);
                    break;
                }
            }
        }
    }

    private void updateGameState() {
        game.updateTemporaryRickThemeRestore();
        game.updateScreenShake();

        if (!game.hasGameStarted()) {
            return;
        }

        // Skip game updates if paused
        if (game.isPaused()) {
            return;
        }
        updateSillyModeState();

        updateShooterPosition();
        handleShooting();
        updateInvaderPositions();
        updateBossPositions();
        updateBossAttacks();
        updateBulletPositions();
        updateBossProjectilePositions();
        checkCollisions();
        checkBossCollisions();
        updateExplosions();
        updateDeathEffects();
        spawnNewInvaders();
        spawnPowerUp();
        updatePowerUpPositions();
        checkPowerUpCollection();
    }

    private void updateSillyModeState() {
        long now = System.currentTimeMillis();

        if (!game.isSillinessModeEnabled()) {
            return;
        }

        if (game.isModifierExpired()) {
            game.clearActiveSillyModifier();
            game.setAnnouncerMessage("Modifier ended. Back to normal chaos.", 1500);
        }

        if (now >= nextModifierRollMs && game.getActiveSillyModifier() == SpaceInvadersUI.SillyModifier.NONE) {
            activateRandomModifier();
            nextModifierRollMs = now + 20000 + game.random.nextInt(20000);
        }

        if (now >= nextAchievementMs) {
            String achievement = FAKE_ACHIEVEMENTS[game.random.nextInt(FAKE_ACHIEVEMENTS.length)];
            game.setFakeAchievementMessage(achievement, 2400);
            nextAchievementMs = now + 16000 + game.random.nextInt(9000);
        }
    }

    private void activateRandomModifier() {
        SpaceInvadersUI.SillyModifier[] modifiers = {
                SpaceInvadersUI.SillyModifier.MOON_GRAVITY,
                SpaceInvadersUI.SillyModifier.ZOOMIES,
                SpaceInvadersUI.SillyModifier.TINY_PANIC,
                SpaceInvadersUI.SillyModifier.MIRROR,
                SpaceInvadersUI.SillyModifier.DISCO,
                SpaceInvadersUI.SillyModifier.PACIFIST
        };

        SpaceInvadersUI.SillyModifier selected = modifiers[game.random.nextInt(modifiers.length)];
        game.activateSillyModifier(selected, MODIFIER_DURATION_MS, getModifierBanner(selected));
    }

    private String getModifierBanner(SpaceInvadersUI.SillyModifier modifier) {
        switch (modifier) {
        case MOON_GRAVITY:
            return "Moon Gravity Mode!";
        case ZOOMIES:
            return "Zoomies Mode!";
        case TINY_PANIC:
            return "Tiny Panic Mode!";
        case MIRROR:
            return "Mirror Controls Mode!";
        case DISCO:
            return "Disco Mode!";
        case PACIFIST:
            return "Weapon Jam Mode!";
        default:
            return "Silly Mode!";
        }
    }

    private void updateExplosions() {
        synchronized (game) {
            Iterator<Explosion> explosionIterator = game.explosions.iterator();
            while (explosionIterator.hasNext()) {
                if (explosionIterator.next().isExpired()) {
                    explosionIterator.remove();
                }
            }
        }
    }

    private void updateDeathEffects() {
        synchronized (game) {
            Iterator<DeathEffect> it = game.deathEffects.iterator();
            while (it.hasNext()) {
                if (it.next().isExpired()) {
                    it.remove();
                }
            }
        }
    }

    private void handleShooting() {
        synchronized (game) {
            if (game.isGameOver() || !game.fireHeld) {
                return;
            }

            long now = System.currentTimeMillis();
            SpaceInvadersUI.PowerUpType powerUp = game.getActivePowerUp();
            long interval = powerUp == SpaceInvadersUI.PowerUpType.RAPID_FIRE ? FIRE_INTERVAL_MS / 3 : FIRE_INTERVAL_MS;
            if (now - lastFireTimeMs < interval) {
                return;
            }

            if (game.isPacifistModeActive() && game.random.nextInt(100) < 45) {
                int centerX = game.getShooter_X_Coordinate() + game.getShooterWidth() / 2;
                int centerY = game.getHeight() - game.getShooterHeight();
                game.explosions.add(new Explosion(centerX, centerY, 10, 140));
                game.setAnnouncerMessage("JAMMED!", 500);
                lastFireTimeMs = now;
                return;
            }

            int shooterX = game.getShooter_X_Coordinate();
            int shooterWidth = game.getShooterWidth();
            int shooterHeight = game.getShooterHeight();
            int baseX = shooterX + shooterWidth / 2;
            int baseY = game.getHeight() - shooterHeight;

            switch (powerUp) {
                case TRIPLE_SHOT -> {
                    game.bullets.add(new Bullet(baseX, baseY, 0));
                    game.bullets.add(new Bullet(baseX - 18, baseY, -1));
                    game.bullets.add(new Bullet(baseX + 18, baseY, 1));
                }
                case SHOTGUN -> {
                    for (int i = -2; i <= 2; i++) {
                        game.bullets.add(new Bullet(baseX + i * 14, baseY, i * 2));
                    }
                }
                case PIERCING -> {
                    Bullet pb = new Bullet(baseX, baseY, 0);
                    pb.setPiercing(true);
                    game.bullets.add(pb);
                }
                case LASER_BEAM -> fireLaserBeam(shooterX, shooterWidth, baseX, now);
                default -> game.bullets.add(new Bullet(baseX, baseY, 0));
            }
            lastFireTimeMs = now;
        }
    }

    private void fireLaserBeam(int shooterX, int shooterWidth, int beamX, long now) {
        game.laserBeamX = beamX;
        game.laserBeamUntilMs = now + LASER_BEAM_FLASH_MS;
        Rectangle beamRect = new Rectangle(
                beamX - LASER_BEAM_HALF_WIDTH_PX,
                0,
                LASER_BEAM_HALF_WIDTH_PX * 2,
                game.getHeight());
        Iterator<Invader> it = game.invaders.iterator();
        while (it.hasNext()) {
            Invader inv = it.next();
            Rectangle invaderRect = new Rectangle(inv.getX(), inv.getY(), inv.getSize(), inv.getSize());
            if (beamRect.intersects(invaderRect)) {
                addExplosionForInvader(inv);
                it.remove();
                game.recordInvaderDefeatCombo();
                game.recordInvaderKill();
                int points = game.getActiveSillyModifier() == SpaceInvadersUI.SillyModifier.TINY_PANIC ? 20 : 10;
                game.addPoints(points);
            }
        }

        // Laser beam also hits bosses
        Iterator<Boss> bossIt = game.bosses.iterator();
        while (bossIt.hasNext()) {
            Boss boss = bossIt.next();
            if (beamRect.intersects(new Rectangle(boss.getX(), boss.getY(), boss.getSize(), boss.getSize()))) {
                boss.takeDamage(boss.getMaxHealth()); // Laser one-shots the boss
                addExplosionForBoss(boss);
                bossIt.remove();
                game.handleBossDefeated(boss);
            }
        }
    }

    private void updateShooterPosition() {
        synchronized (game) {
            int shooter_X_Coordinate = game.getShooter_X_Coordinate();
            int shooter_Width = game.getShooterWidth();
            int movementStep = game.getActiveSillyModifier() == SpaceInvadersUI.SillyModifier.ZOOMIES ? 9 : 5;
            
            // Move shooter left or right
            if (game.moveLeft && shooter_X_Coordinate > 0) {
                game.setShooter_X_Coordinate(shooter_X_Coordinate - movementStep);
            }
            if (game.moveRight && shooter_X_Coordinate < game.getWidth() - shooter_Width) {
                game.setShooter_X_Coordinate(shooter_X_Coordinate + movementStep);
            }
        }
    }

    private void spawnNewInvaders() {
        synchronized (game) {
            if (game.isFinalBossLevelActive()) {
                return;
            }

            // Difficulty multiplier increases spawn chance over time
            double spawnChance = 5 * game.getDifficultyMultiplier();
            if (game.random.nextInt(100) < spawnChance) {
                // Use a margin so invaders always spawn within the visible playfield
                int spawnMargin = 50;
                int spawnWidth = Math.max(1, game.getWidth() - spawnMargin * 2);
                int x = spawnMargin + game.random.nextInt(spawnWidth);
                spawnCounter++;

                int baseSize = 40;
                if (game.getActiveSillyModifier() == SpaceInvadersUI.SillyModifier.TINY_PANIC) {
                    baseSize = 20;
                }

                // Big-head gag: every 7th invader is oversized.
                if (game.isSillinessModeEnabled() && spawnCounter % 7 == 0) {
                    baseSize = Math.max(baseSize, 72);
                }

                // Calculate speed based on difficulty (scales from 2 to 5 pixels/update)
                int baseSpeed = 2;
                int difficultySpeed = (int) (baseSpeed + (game.getDifficultyMultiplier() - 1.0) * 3);
                Invader invader = new Invader(x, 0, baseSize, false, difficultySpeed);
                game.invaders.add(invader);
            }
        }
    }

    private void updateInvaderPositions() {
        synchronized (game) {
            Iterator<Invader> invaderIterator = game.invaders.iterator();
            while (invaderIterator.hasNext()) {
                Invader invader = invaderIterator.next();
                int y = invader.getY();
                int step = invader.getSpeed();
                if (game.getActiveSillyModifier() == SpaceInvadersUI.SillyModifier.MOON_GRAVITY) {
                    step = Math.max(1, step / 2);
                } else if (game.getActiveSillyModifier() == SpaceInvadersUI.SillyModifier.ZOOMIES) {
                    step = step * 2;
                }

                invader.setY(y + step);
                if (invader.getY() > game.getHeight()) {
                    invaderIterator.remove();
                }
            }
        }
    }

    private void updateBulletPositions() {
        synchronized (game) {
            Iterator<Bullet> bulletIterator = game.bullets.iterator();
            while (bulletIterator.hasNext()) {
                Bullet bullet = bulletIterator.next();
                int y = bullet.getY();
                int yStep = 5;
                if (game.getActiveSillyModifier() == SpaceInvadersUI.SillyModifier.MOON_GRAVITY) {
                    yStep = 3;
                    int drift = (int) Math.signum(Math.sin((bullet.getY() + bullet.getX()) * 0.07));
                    bullet.setX(Math.max(0, Math.min(game.getWidth(), bullet.getX() + drift)));
                } else if (game.getActiveSillyModifier() == SpaceInvadersUI.SillyModifier.ZOOMIES) {
                    yStep = 8;
                }

                // Apply horizontal velocity (spread shots)
                if (bullet.getVx() != 0) {
                    bullet.setX(bullet.getX() + bullet.getVx());
                }

                bullet.setY(y - yStep);
                if (bullet.getY() < 0) {
                    bulletIterator.remove();
                }
            }
        }
    }

    private void checkCollisions() {
        synchronized (game) {
            // Check bullet-invader collisions
            Iterator<Bullet> bulletIterator = game.bullets.iterator();
            while (bulletIterator.hasNext()) {
                Bullet bullet = bulletIterator.next();
                Iterator<Invader> invaderIterator = game.invaders.iterator();
                while (invaderIterator.hasNext()) {
                    Invader invader = invaderIterator.next();
                    if (new Rectangle(bullet.getX() - 5, bullet.getY(), 10, 10).intersects(
                            new Rectangle(invader.getX(), invader.getY(), invader.getSize(),
                                    invader.getSize()))) {
                        addExplosionForInvader(invader);
                        if (!bullet.isPiercing()) {
                            bulletIterator.remove();
                        }
                        invaderIterator.remove();
                        game.recordInvaderDefeatCombo();
                        game.recordInvaderKill(); // New: triggers difficulty scaling, boss spawning, screen shake
                        int points = game.getActiveSillyModifier() == SpaceInvadersUI.SillyModifier.TINY_PANIC ? 20 : 10;
                        game.addPoints(points);
                        if (!bullet.isPiercing()) break;
                    }
                }
            }

            // Check laser beam kills
            // (laser collision handled in fireLaserBeam, but we should also track kills there)

            // Check shooter-invader collisions
            if (!game.isGameOver()) {
                int shooterX = game.getShooter_X_Coordinate();
                int shooterY = game.getHeight() - game.getShooterHeight();
                int shooterWidth = game.getShooterWidth();
                int shooterHeight = game.getShooterHeight();

                Rectangle shooterRect = new Rectangle(shooterX, shooterY, shooterWidth, shooterHeight);

                Iterator<Invader> invaderIterator = game.invaders.iterator();
                while (invaderIterator.hasNext()) {
                    Invader invader = invaderIterator.next();
                    Rectangle invaderRect = new Rectangle(invader.getX(), invader.getY(),
                            invader.getSize(), invader.getSize());
                    if (shooterRect.intersects(invaderRect)) {
                        addExplosionForInvader(invader);
                        invaderIterator.remove();
                        game.damagePlayer();
                        game.triggerScreenShake(100, 6); // Screen shake on hit
                        break; // Only one collision per frame
                    }
                }
            }
        }
    }

    private void addExplosionForInvader(Invader invader) {
        int centerX = invader.getX() + invader.getSize() / 2;
        int centerY = invader.getY() + invader.getSize() / 2;

        String deathExplosionSound = game.getDeathExplosionSoundEffectPath();
        if (deathExplosionSound != null && game.getMusicHandler() != null) {
            game.getMusicHandler().playOneShotEffect(deathExplosionSound);
        }

        // If a death skin is set, show the flash/fade effect instead of the normal explosion
        java.awt.Image deathSkin = game.imageSelection.getDeathSkinImage();
        if (deathSkin != null) {
            synchronized (game) {
                game.deathEffects.add(new DeathEffect(invader.getX(), invader.getY(), invader.getSize(), deathSkin,
                        game.imageSelection.isDeathSkinFadeOutEnabled()));
            }
            return;
        }

        if (!game.isExplosionsEnabled()) {
            return;
        }

        int maxRadius = Math.max(12, invader.getSize());
        synchronized (game) {
            game.explosions.add(new Explosion(centerX, centerY, maxRadius, EXPLOSION_DURATION_MS));
        }
    }

    private void updateBossPositions() {
        synchronized (game) {
            Iterator<Boss> bossIterator = game.bosses.iterator();
            while (bossIterator.hasNext()) {
                Boss boss = bossIterator.next();
                if (boss.isFinalBoss()) {
                    long elapsedMs = System.currentTimeMillis() - boss.getSpawnTimeMs();
                    float t = elapsedMs / 1000.0f;

                    int maxTravelX = Math.max(20, game.getWidth() - boss.getSize());
                    int centerX = maxTravelX / 2;
                    int swingX = Math.max(24, (int) (maxTravelX * 0.38));
                    int patternX = (int) (centerX + Math.sin(t * 1.45 + boss.getPatternPhase()) * swingX);

                    int hoverBaseY = Math.max(35, boss.getSpawnY());
                    int hoverY = hoverBaseY
                            + (int) (Math.sin(t * 2.1 + boss.getPatternPhase() * 0.7f) * 28)
                            + (int) (Math.cos(t * 0.9f + boss.getPatternPhase()) * 12);

                    boss.setX(Math.max(0, Math.min(maxTravelX, patternX)));
                    boss.setY(Math.max(20, Math.min(game.getHeight() / 2, hoverY)));
                    continue;
                }

                int y = boss.getY();
                int step = 2; // Regular bosses drift downward
                boss.setY(y + step);
                if (boss.getY() > game.getHeight()) {
                    bossIterator.remove();
                }
            }
        }
    }

    private void updateBossAttacks() {
        long now = System.currentTimeMillis();
        if (now - lastBossFireTimeMs < BOSS_FIRE_INTERVAL_MS) {
            return;
        }

        synchronized (game) {
            if (game.bosses.isEmpty() || game.isGameOver()) {
                return;
            }

            for (Boss boss : game.bosses) {
                if (!boss.isFinalBoss()) {
                    continue;
                }

                int bossCenterX = boss.getX() + boss.getSize() / 2;
                int bossBottomY = boss.getY() + boss.getSize() - 8;
                int shooterCenterX = game.getShooter_X_Coordinate() + game.getShooterWidth() / 2;

                int dx = shooterCenterX - bossCenterX;
                int vy = 6;
                int vx = Math.max(-5, Math.min(5, Math.round(dx / 55.0f)));

                game.bossProjectiles.add(new SpaceInvadersUI.BossProjectile(bossCenterX, bossBottomY, vx, vy, 12));
                game.bossProjectiles.add(new SpaceInvadersUI.BossProjectile(bossCenterX - 12, bossBottomY, vx - 1, vy, 10));
                game.bossProjectiles.add(new SpaceInvadersUI.BossProjectile(bossCenterX + 12, bossBottomY, vx + 1, vy, 10));
            }
        }

        lastBossFireTimeMs = now;
    }

    private void updateBossProjectilePositions() {
        synchronized (game) {
            Iterator<SpaceInvadersUI.BossProjectile> projectileIterator = game.bossProjectiles.iterator();

            int shooterX = game.getShooter_X_Coordinate();
            int shooterY = game.getHeight() - game.getShooterHeight();
            Rectangle shooterRect = new Rectangle(shooterX, shooterY, game.getShooterWidth(), game.getShooterHeight());

            while (projectileIterator.hasNext()) {
                SpaceInvadersUI.BossProjectile projectile = projectileIterator.next();
                projectile.tick();

                Rectangle projectileRect = new Rectangle(
                        projectile.getX() - projectile.getSize() / 2,
                        projectile.getY() - projectile.getSize() / 2,
                        projectile.getSize(),
                        projectile.getSize());

                if (!game.isGameOver() && shooterRect.intersects(projectileRect)) {
                    projectileIterator.remove();
                    game.damagePlayer();
                    game.triggerScreenShake(120, 5);
                    int centerX = projectile.getX();
                    int centerY = projectile.getY();
                    game.explosions.add(new Explosion(centerX, centerY, 16, EXPLOSION_DURATION_MS));
                    continue;
                }

                if (projectile.isOffScreen(game.getWidth(), game.getHeight())) {
                    projectileIterator.remove();
                }
            }
        }
    }

    private void checkBossCollisions() {
        synchronized (game) {
            // Check bullet-boss collisions
            Iterator<Bullet> bulletIterator = game.bullets.iterator();
            while (bulletIterator.hasNext()) {
                Bullet bullet = bulletIterator.next();
                Iterator<Boss> bossIterator = game.bosses.iterator();
                while (bossIterator.hasNext()) {
                    Boss boss = bossIterator.next();
                    if (new Rectangle(bullet.getX() - 5, bullet.getY(), 10, 10).intersects(
                            new Rectangle(boss.getX(), boss.getY(), boss.getSize(), boss.getSize()))) {
                        boss.takeDamage(1);
                        if (!bullet.isPiercing()) {
                            bulletIterator.remove();
                        }
                        if (boss.isDead()) {
                            // Boss destroyed - massive rewards!
                            addExplosionForBoss(boss);
                            bossIterator.remove();
                            game.handleBossDefeated(boss);
                        } else {
                            // Boss hit but survives - create small explosion
                            int centerX = boss.getX() + boss.getSize() / 2;
                            int centerY = boss.getY() + boss.getSize() / 2;
                            synchronized (game) {
                                game.explosions.add(new Explosion(centerX, centerY, boss.getSize() / 3, EXPLOSION_DURATION_MS));
                            }
                        }
                        if (!bullet.isPiercing()) break;
                    }
                }
            }

            // Check shooter-boss collisions
            if (!game.isGameOver()) {
                int shooterX = game.getShooter_X_Coordinate();
                int shooterY = game.getHeight() - game.getShooterHeight();
                int shooterWidth = game.getShooterWidth();
                int shooterHeight = game.getShooterHeight();

                Rectangle shooterRect = new Rectangle(shooterX, shooterY, shooterWidth, shooterHeight);

                Iterator<Boss> bossIterator = game.bosses.iterator();
                while (bossIterator.hasNext()) {
                    Boss boss = bossIterator.next();
                    Rectangle bossRect = new Rectangle(boss.getX(), boss.getY(), boss.getSize(), boss.getSize());
                    if (shooterRect.intersects(bossRect)) {
                        // Boss collides with shooter
                        int centerX = boss.getX() + boss.getSize() / 2;
                        int centerY = boss.getY() + boss.getSize() / 2;
                        synchronized (game) {
                            game.explosions.add(new Explosion(centerX, centerY, boss.getSize() / 2, EXPLOSION_DURATION_MS));
                        }
                        bossIterator.remove();
                        game.damagePlayer();
                        game.triggerScreenShake(150, 8); // Big shake on boss hit
                        break;
                    }
                }
            }
        }
    }

    private void addExplosionForBoss(Boss boss) {
        // Create a larger explosion for boss death
        int centerX = boss.getX() + boss.getSize() / 2;
        int centerY = boss.getY() + boss.getSize() / 2;

        if (!game.isExplosionsEnabled()) {
            return;
        }

        int maxRadius = boss.getSize();
        synchronized (game) {
            // Create multiple explosions for dramatic effect
            game.explosions.add(new Explosion(centerX, centerY, maxRadius, EXPLOSION_DURATION_MS));
            game.explosions.add(new Explosion(centerX - 30, centerY - 20, maxRadius / 2, EXPLOSION_DURATION_MS));
            game.explosions.add(new Explosion(centerX + 30, centerY + 20, maxRadius / 2, EXPLOSION_DURATION_MS));
        }
    }

    public void stopThread() {
        running = false;
    }

    // ----------------------------- Power-up lifecycle ---------------------------

    private void spawnPowerUp() {
        long now = System.currentTimeMillis();
        if (now < nextPowerUpSpawnMs) return;
        SpaceInvadersUI.PowerUpType[] types = {
            SpaceInvadersUI.PowerUpType.RAPID_FIRE,
            SpaceInvadersUI.PowerUpType.TRIPLE_SHOT,
            SpaceInvadersUI.PowerUpType.PIERCING,
            SpaceInvadersUI.PowerUpType.SHOTGUN,
            SpaceInvadersUI.PowerUpType.LASER_BEAM
        };
        SpaceInvadersUI.PowerUpType type = types[game.random.nextInt(types.length)];
        int x = game.random.nextInt(Math.max(1, game.getWidth() - PowerUp.SIZE));
        synchronized (game) {
            game.powerUps.add(new PowerUp(x, type));
        }
        nextPowerUpSpawnMs = now + 15000 + game.random.nextInt(10000);
    }

    private void updatePowerUpPositions() {
        synchronized (game) {
            Iterator<PowerUp> it = game.powerUps.iterator();
            while (it.hasNext()) {
                PowerUp p = it.next();
                p.tick();
                if (p.isOffScreen(game.getHeight())) it.remove();
            }
        }
    }

    private void checkPowerUpCollection() {
        int sx = game.getShooter_X_Coordinate();
        int sy = game.getHeight() - game.getShooterHeight();
        Rectangle shooter = new Rectangle(sx, sy, game.getShooterWidth(), game.getShooterHeight());
        synchronized (game) {
            Iterator<PowerUp> it = game.powerUps.iterator();
            while (it.hasNext()) {
                PowerUp p = it.next();
                if (shooter.intersects(new Rectangle(p.getX(), p.getY(), PowerUp.SIZE, PowerUp.SIZE))) {
                    game.activatePowerUp(p.getType(), POWER_UP_DURATION_MS);
                    it.remove();
                }
            }
        }
    }
}
