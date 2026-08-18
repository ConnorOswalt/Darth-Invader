package spaceinvaders.DataHandlers;

import spaceinvaders.UI.SpaceInvadersUI;

import java.util.Properties;

/**
 * Persists and restores the player's cosmetic customization choices
 * (shooter/invader/bullet skins, background, explosion effects) across sessions.
 * Theme and music selections are intentionally excluded since themes are
 * unlocked/progressed within a single playthrough rather than being a fixed preference.
 */
public final class PreferencesManager {
    private static final String KEY_SHOOTER = "shooter";
    private static final String KEY_INVADER = "invader";
    private static final String KEY_BULLET = "bullet";
    private static final String KEY_BACKGROUND = "background";
    private static final String KEY_STARS_BACKGROUND = "starsBackground";
    private static final String KEY_EXPLOSIONS_ENABLED = "explosionsEnabled";

    private static final PreferencesHandler HANDLER = new PreferencesHandler();

    private PreferencesManager() {
    }

    public static void saveCurrentSelections(SpaceInvadersUI game) {
        Properties properties = new Properties();
        putIfPresent(properties, KEY_SHOOTER, game.imageSelection.getShooterImagePath());
        putIfPresent(properties, KEY_INVADER, game.imageSelection.getInvaderImagePath());
        putIfPresent(properties, KEY_BULLET, game.imageSelection.getBulletImagePath());
        if (game.imageSelection.isStarsBackgroundEnabled()) {
            properties.setProperty(KEY_STARS_BACKGROUND, "true");
        } else {
            putIfPresent(properties, KEY_BACKGROUND, game.imageSelection.getBackgroundImagePath());
        }
        properties.setProperty(KEY_EXPLOSIONS_ENABLED, Boolean.toString(game.isExplosionsEnabled()));
        HANDLER.save(properties);
    }

    public static void applySavedCosmeticPreferences(SpaceInvadersUI game) {
        Properties properties = HANDLER.load();
        if (properties.isEmpty()) {
            return;
        }

        applyImageIfValid(properties.getProperty(KEY_SHOOTER), game.imageSelection::setShooterImageFromResourcePath);
        applyImageIfValid(properties.getProperty(KEY_INVADER), game.imageSelection::setInvaderImageFromResourcePath);
        applyImageIfValid(properties.getProperty(KEY_BULLET), game.imageSelection::setBulletImageFromResourcePath);

        if ("true".equals(properties.getProperty(KEY_STARS_BACKGROUND))) {
            game.imageSelection.enableStarsBackground(game);
        } else {
            applyImageIfValid(properties.getProperty(KEY_BACKGROUND), game.imageSelection::setBackgroundImageFromResourcePath);
        }

        String explosionsEnabled = properties.getProperty(KEY_EXPLOSIONS_ENABLED);
        if (explosionsEnabled != null) {
            game.setExplosionsEnabled(Boolean.parseBoolean(explosionsEnabled));
        }
    }

    private static void putIfPresent(Properties properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.setProperty(key, value);
        }
    }

    private static void applyImageIfValid(String resourcePath, java.util.function.Consumer<String> applier) {
        if (resourcePath != null && !resourcePath.isBlank()
                && SpaceInvadersUI.class.getResource(resourcePath) != null) {
            applier.accept(resourcePath);
        }
    }
}
