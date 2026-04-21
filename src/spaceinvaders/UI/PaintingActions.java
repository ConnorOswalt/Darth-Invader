package spaceinvaders.UI;

import spaceinvaders.characters.Bullet;
import spaceinvaders.characters.DeathEffect;
import spaceinvaders.characters.Explosion;
import spaceinvaders.characters.Invader;
import spaceinvaders.characters.Boss;
import spaceinvaders.characters.PowerUp;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * PaintingActions handles all rendering of game objects.
 * This is called on the AWT-EventQueue-0 thread during repainting.
 * Reads shared game state safely using synchronized access.
 */
public class PaintingActions {

    public PaintingActions() {

    }

    public void drawBackground(Graphics g, SpaceInvadersUI game) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, game.getWidth(), game.getHeight());

        // Zoom background by 10% to cover screen shake offset and prevent white edges from showing
        double zoomFactor = 1.10;
        int zoomedWidth = (int)(game.getWidth() * zoomFactor);
        int zoomedHeight = (int)(game.getHeight() * zoomFactor);
        int offsetX = (int)((zoomedWidth - game.getWidth()) / 2.0);
        int offsetY = (int)((zoomedHeight - game.getHeight()) / 2.0);

        if (game.imageSelection.isStarsBackgroundEnabled()) {
            g.setColor(Color.WHITE);
            for (Point star : game.imageSelection.getStarsSnapshot()) {
                g.fillOval(star.x, star.y, 2, 2);
            }
            return;
        }

        Image backgroundImage = game.imageSelection.getBackgroundImage();
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, -offsetX, -offsetY, zoomedWidth, zoomedHeight, game);
        }
    }

    public void drawShooter(Graphics g, SpaceInvadersUI game) {
        Image shooter_image = game.imageSelection.getShooterImage();
        int shooter_height = game.getShooterHeight();
        int shooter_width = game.getShooterWidth();
        int shooter_X_Coordinate = game.getShooter_X_Coordinate();
        int shooter_Y_Coordinate = game.getHeight() - shooter_height;

        g.drawImage(shooter_image, shooter_X_Coordinate, shooter_Y_Coordinate, shooter_width, shooter_height, game);

    }

    public void drawInvaders(Graphics g, SpaceInvadersUI game, Image invaderImage) {
        // Create a safe copy of the invaders list to avoid concurrent modification issues
        List<Invader> invadersCopy;
        synchronized (game) {
            invadersCopy = new ArrayList<>(game.invaders);
        }

        Image rickInvaderImage = game.imageSelection.getRickInvaderImage();
        
        for (Invader invader : invadersCopy) {
            Image imageToDraw = invader.isRickRollTarget() && rickInvaderImage != null
                    ? rickInvaderImage
                    : invaderImage;
            g.drawImage(imageToDraw, invader.getX(), invader.getY(), invader.getSize(),
                    invader.getSize(), game);
        }
    }

    public void drawExplosions(Graphics g, SpaceInvadersUI game) {
        List<Explosion> explosionsCopy;
        synchronized (game) {
            explosionsCopy = new ArrayList<>(game.explosions);
        }

        for (Explosion explosion : explosionsCopy) {
            double progress = explosion.getProgress();
            int radius = (int) Math.max(2, explosion.getMaxRadius() * progress);
            int alpha = (int) Math.max(0, 255 * (1.0 - progress));

            g.setColor(new Color(255, 170, 0, alpha));
            g.fillOval(explosion.getCenterX() - radius, explosion.getCenterY() - radius,
                    radius * 2, radius * 2);

            int innerRadius = Math.max(2, radius / 2);
            g.setColor(new Color(255, 240, 120, alpha));
            g.fillOval(explosion.getCenterX() - innerRadius, explosion.getCenterY() - innerRadius,
                    innerRadius * 2, innerRadius * 2);
        }
    }

    public void drawBullets(Graphics g, SpaceInvadersUI game) {
        // Create a safe copy of the bullets list to avoid concurrent modification issues
        List<Bullet> bulletsCopy;
        synchronized (game) {
            bulletsCopy = new ArrayList<>(game.bullets);
        }

        for (Bullet bullet : bulletsCopy) {
            Image bulletImage = game.imageSelection.getBulletImage(bullet);
            if (bulletImage != null) {
                // Render image/gif centred on bullet x, tip at bullet y
                g.drawImage(bulletImage, bullet.getX() - 12, bullet.getY(), 24, 40, game);
            } else {
                // Fallback: yellow triangle
                g.setColor(Color.YELLOW);
                int[] xPoints = { bullet.getX(), bullet.getX() - 12, bullet.getX() + 12 };
                int[] yPoints = { bullet.getY(), bullet.getY() + 20, bullet.getY() + 20 };
                g.fillPolygon(xPoints, yPoints, 3);
            }
        }
    }
    public void drawPlayerHealth(Graphics g, SpaceInvadersUI game) {
        int playerHealth = game.getPlayerHealth();
        int maxHealth = game.getMaxPlayerHealth();
        int barX = 10;
        int barY = 12;
        int barWidth = 180;
        int barHeight = 16;
        float healthRatio = Math.max(0.0f, Math.min(1.0f, playerHealth / (float) maxHealth));

        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Health", barX, barY - 2);

        g2d.setColor(new Color(35, 35, 35, 220));
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 10, 10);

        Color fillColor;
        if (healthRatio > 0.6f) {
            fillColor = new Color(80, 220, 110);
        } else if (healthRatio > 0.3f) {
            fillColor = new Color(255, 190, 70);
        } else {
            fillColor = new Color(255, 85, 85);
        }

        g2d.setColor(fillColor);
        g2d.fillRoundRect(barX, barY, Math.max(0, (int) (barWidth * healthRatio)), barHeight, 10, 10);
        g2d.setColor(new Color(255, 255, 255, 45));
        g2d.drawRoundRect(barX, barY, barWidth, barHeight, 10, 10);

        String healthText = playerHealth + "/" + maxHealth;
        FontMetrics fm = g2d.getFontMetrics();
        int textX = barX + (barWidth - fm.stringWidth(healthText)) / 2;
        int textY = barY + barHeight - 3;
        g2d.setColor(Color.WHITE);
        g2d.drawString(healthText, textX, textY);

        // Draw flashing effect when player is hit
        if (game.isPlayerFlashing()) {
            g.setColor(new Color(255, 0, 0, 128)); // Semi-transparent red
            g.fillRect(0, 0, game.getWidth(), game.getHeight());
        }
    }

    public void drawCurrentScore(Graphics g, SpaceInvadersUI game) {
        Graphics2D g2d = (Graphics2D) g;
        int score = game.getScoreManager().getCurrentScore();
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String scoreText = String.format("Score: %03d", score);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(scoreText);
        int scoreX = game.getWidth() - textWidth - 10;
        g2d.drawString(scoreText, scoreX, 20);

        int themesLeft = game.getRemainingThemeCount();
        String nextBossTheme = game.getNextThemeBossName();
        String nextText = "Next Boss Theme: " + nextBossTheme;
        String leftText = "Themes Left: " + themesLeft;

        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics hudFm = g2d.getFontMetrics();
        int hudWidth = Math.max(hudFm.stringWidth(nextText), hudFm.stringWidth(leftText)) + 18;
        int hudHeight = 36;
        int hudX = game.getWidth() - hudWidth - 10;
        int hudY = 30;

        g2d.setColor(new Color(0, 0, 0, 165));
        g2d.fillRoundRect(hudX, hudY, hudWidth, hudHeight, 10, 10);
        g2d.setColor(new Color(255, 255, 255, 45));
        g2d.drawRoundRect(hudX, hudY, hudWidth, hudHeight, 10, 10);

        g2d.setColor(new Color(180, 225, 255));
        g2d.drawString(nextText, hudX + 9, hudY + 14);
        g2d.setColor(new Color(255, 235, 120));
        g2d.drawString(leftText, hudX + 9, hudY + 29);
    }

    public void drawDeathEffects(Graphics g, SpaceInvadersUI game) {
        List<DeathEffect> copy;
        synchronized (game) {
            copy = new ArrayList<>(game.deathEffects);
        }

        Graphics2D g2d = (Graphics2D) g;
        Composite originalComposite = g2d.getComposite();

        for (DeathEffect effect : copy) {
            int alpha = effect.getAlpha();
            if (alpha <= 0) {
                continue;
            }

            BufferedImage frame = effect.getCachedFrame(game);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255.0f));
            g2d.drawImage(frame, effect.getX(), effect.getY(), game);
        }

        g2d.setComposite(originalComposite);
    }

    public void drawPowerUps(Graphics g, SpaceInvadersUI game) {
        List<PowerUp> copy;
        synchronized (game) {
            copy = new ArrayList<>(game.powerUps);
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (PowerUp p : copy) {
            g2d.setColor(p.getColor());
            g2d.fillRoundRect(p.getX(), p.getY(), PowerUp.SIZE, PowerUp.SIZE, 8, 8);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            FontMetrics fm = g2d.getFontMetrics();
            String label = p.getLabel();
            int lx = p.getX() + (PowerUp.SIZE - fm.stringWidth(label)) / 2;
            int ly = p.getY() + (PowerUp.SIZE + fm.getAscent()) / 2 - 2;
            g2d.drawString(label, lx, ly);
        }
    }

    public void drawBossProjectiles(Graphics g, SpaceInvadersUI game) {
        List<SpaceInvadersUI.BossProjectile> copy;
        synchronized (game) {
            copy = new ArrayList<>(game.bossProjectiles);
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (SpaceInvadersUI.BossProjectile projectile : copy) {
            int size = projectile.getSize();
            int x = projectile.getX() - size / 2;
            int y = projectile.getY() - size / 2;

            g2d.setColor(new Color(255, 90, 90, 230));
            g2d.fillOval(x, y, size, size);
            g2d.setColor(new Color(255, 235, 180, 220));
            int inner = Math.max(2, size / 2);
            g2d.fillOval(projectile.getX() - inner / 2, projectile.getY() - inner / 2, inner, inner);
        }
    }

    public void drawLaserBeam(Graphics g, SpaceInvadersUI game) {
        if (game.laserBeamX < 0 || System.currentTimeMillis() > game.laserBeamUntilMs) return;
        Graphics2D g2d = (Graphics2D) g;
        Composite orig = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.78f));
        g2d.setColor(new Color(255, 120, 255));
        g2d.fillRect(game.laserBeamX - 12, 0, 24, game.getHeight());

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        Image laserBeamImage = game.imageSelection.getLaserBeamImage();
        if (laserBeamImage != null) {
            g2d.drawImage(laserBeamImage, game.laserBeamX - 26, 0, 52, game.getHeight(), game);
        } else {
            g2d.setColor(new Color(255, 80, 255));
            g2d.fillRect(game.laserBeamX - 10, 0, 20, game.getHeight());
        }
        g2d.setComposite(orig);
    }

    public void drawActivePowerUpHud(Graphics g, SpaceInvadersUI game) {
        SpaceInvadersUI.PowerUpType active = game.getActivePowerUp();
        if (active == SpaceInvadersUI.PowerUpType.NONE) return;
        long remaining = game.getActivePowerUpUntilMs() - System.currentTimeMillis();
        if (remaining <= 0) return;

        Graphics2D g2d = (Graphics2D) g;
        int barW = 150;
        int barH = 12;
        int bx = 10;
        int by = game.getHeight() - 55;

        String name = switch (active) {
            case RAPID_FIRE  -> "RAPID FIRE";
            case TRIPLE_SHOT -> "TRIPLE SHOT";
            case PIERCING    -> "PIERCING";
            case SHOTGUN     -> "SHOTGUN";
            case LASER_BEAM  -> "LASER BEAM";
            default          -> "";
        };

        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(Color.WHITE);
        g2d.drawString(name, bx, by - 4);

        // Background bar
        g2d.setColor(new Color(60, 60, 60, 200));
        g2d.fillRoundRect(bx, by, barW, barH, 4, 4);

        // Fill bar proportional to time remaining
        float frac = Math.min(1f, remaining / 8000f);
        g2d.setColor(new Color(80, 200, 255));
        g2d.fillRoundRect(bx, by, (int)(barW * frac), barH, 4, 4);
    }

    public void drawBosses(Graphics g, SpaceInvadersUI game, Image bossImage) {
        // Create a safe copy of the bosses list
        List<Boss> bossesCopy;
        synchronized (game) {
            bossesCopy = new ArrayList<>(game.bosses);
        }

        Graphics2D g2d = (Graphics2D) g;
        for (Boss boss : bossesCopy) {
            Image themedBossImage = game.imageSelection.getImageFromResourcePath(boss.getShooterSkinPath());
            Image imageToDraw = themedBossImage != null ? themedBossImage : bossImage;

            if (imageToDraw != null) {
                g2d.drawImage(imageToDraw, boss.getX(), boss.getY(), boss.getSize(), boss.getSize(), game);
            } else {
                // Fallback: Draw a red rectangle with health bar
                g2d.setColor(new Color(255, 100, 100));
                g2d.fillRect(boss.getX(), boss.getY(), boss.getSize(), boss.getSize());
                g2d.setColor(new Color(255, 200, 200));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(boss.getX(), boss.getY(), boss.getSize(), boss.getSize());
            }

            // Draw health bar above boss
            int healthBarWidth = boss.getSize();
            int healthBarHeight = 6;
            int healthBarX = boss.getX();
            int healthBarY = boss.getY() - 12;

            g2d.setColor(new Color(50, 50, 50, 200));
            g2d.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

            float healthRatio = boss.getHealthRatio();
            Color healthColor = healthRatio > 0.5f ? new Color(80, 220, 110) : 
                               healthRatio > 0.25f ? new Color(255, 190, 70) : 
                               new Color(255, 85, 85);
            g2d.setColor(healthColor);
            g2d.fillRect(healthBarX, healthBarY, (int)(healthBarWidth * healthRatio), healthBarHeight);

            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
        }
    }
}
