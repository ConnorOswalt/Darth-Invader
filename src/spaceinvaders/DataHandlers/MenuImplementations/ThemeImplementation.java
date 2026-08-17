package spaceinvaders.DataHandlers.MenuImplementations;

import spaceinvaders.GameExceptions;
import spaceinvaders.UI.SpaceInvadersUI;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ThemeImplementation {
    private static final Pattern STRING_FIELD_PATTERN_TEMPLATE =
            Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern BOOLEAN_FIELD_PATTERN_TEMPLATE =
        Pattern.compile("\"%s\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Object THEME_LOCK = new Object();
    private static SpaceInvadersUI pendingGame;
    private static String pendingThemePath;
    private static boolean hasPendingTheme;

    static {
        Thread themeThread = new Thread(ThemeImplementation::processThemeChanges, "ThemeHandler-Thread");
        themeThread.setDaemon(true);
        themeThread.start();
    }

    public ThemeImplementation() {
    }

    public static void requestThemeChange(SpaceInvadersUI game, String themePath) {
        if (game == null || themePath == null || themePath.isBlank()) {
            return;
        }

        String normalizedPath = normalizeResourcePath(themePath);
        if (ThemeImplementation.class.getResource(normalizedPath) == null) {
            GameExceptions.showErrorDialog("Theme file not found: " + normalizedPath);
            return;
        }

        synchronized (THEME_LOCK) {
            pendingGame = game;
            pendingThemePath = normalizedPath;
            hasPendingTheme = true;
            THEME_LOCK.notifyAll();
        }
    }

    public void handleThemeSelection(ActionEvent e) {
        if (!(e.getSource() instanceof JMenuItem)) {
            return;
        }

        JMenuItem selectedItem = (JMenuItem) e.getSource();
        String selectedPath = selectedItem.getName();

        SpaceInvadersUI game = SpaceInvadersUI.getActiveInstance();
        if (game == null) {
            return;
        }

        if (selectedPath == null || selectedPath.isBlank()) {
            String customPath = JOptionPane.showInputDialog(
                    null,
                    "Enter theme JSON path (example: /resources/Themes/Retro.json):");

            if (customPath == null || customPath.isBlank()) {
                return;
            }

            selectedPath = normalizeResourcePath(customPath);
        }

        if (ThemeImplementation.class.getResource(selectedPath) == null) {
            GameExceptions.showErrorDialog("Theme file not found: " + selectedPath);
            return;
        }

        if (game.getMusicHandler() != null) {
            game.getMusicHandler().clearInterruptedTrack();
        }
        queueThemeChange(game, selectedPath);
    }

    private void queueThemeChange(SpaceInvadersUI game, String selectedPath) {
        requestThemeChange(game, selectedPath);
    }

    private static void processThemeChanges() {
        while (true) {
            SpaceInvadersUI game;
            String themePath;

            synchronized (THEME_LOCK) {
                while (!hasPendingTheme) {
                    try {
                        THEME_LOCK.wait();
                    } catch (InterruptedException e) {
                        GameExceptions.handleInterrupted("Theme handler", e);
                        return;
                    }
                }

                game = pendingGame;
                themePath = pendingThemePath;
                hasPendingTheme = false;
            }

            try {
                applyThemeFromJson(game, themePath);
            } catch (RuntimeException e) {
                GameExceptions.handleWithDialog("Failed to apply theme", e);
            }
        }
    }

    private static void applyThemeFromJson(SpaceInvadersUI game, String jsonResourcePath) {
        String jsonContent = readResourceFile(jsonResourcePath);
        if (jsonContent == null) {
            GameExceptions.showErrorDialog("Unable to read theme JSON: " + jsonResourcePath);
            return;
        }

        game.setCurrentThemePath(jsonResourcePath);

        Boolean restoreDefaults = extractBoolean(jsonContent, "restore_defaults");
        if (Boolean.TRUE.equals(restoreDefaults)) {
            game.imageSelection.restoreDefaultThemeState(game);
            game.setDeathSoundEffectPath(game.getDefaultDeathSoundEffectPath());
            game.clearDeathExplosionSoundEffectPath();
            game.setDeathSoundEnabled(true);
            game.setDeathSoundLooping(false);
        }

        applyImagePath(game, jsonContent, "shooter", game.imageSelection::setShooterImageFromResourcePath);
        applyImagePath(game, jsonContent, "invader", game.imageSelection::setInvaderImageFromResourcePath);
        applyImagePath(game, jsonContent, "bullet", game.imageSelection::setBulletImageFromResourcePath);

        Boolean starsBackground = extractBoolean(jsonContent, "stars_background");
        if (Boolean.TRUE.equals(starsBackground)) {
            game.imageSelection.enableStarsBackground(game);
        } else {
            applyImagePath(game, jsonContent, "background", game.imageSelection::setBackgroundImageFromResourcePath);
        }

        String backgroundPath = extractPath(jsonContent, "background");
        String deathScreenPath = extractPath(jsonContent, "deathscreen");
        if (deathScreenPath != null) {
            game.imageSelection.setDeathScreenImageFromResourcePath(deathScreenPath);
        } else if (backgroundPath != null) {
            game.imageSelection.setDeathScreenImageFromResourcePath(backgroundPath);
        } else {
            game.imageSelection.clearDeathScreenImage();
        }

        // death_skin is optional — clear it if not present in this theme
        String deathSkinPath = extractPath(jsonContent, "death_skin");
        if (deathSkinPath != null) {
            game.imageSelection.setDeathSkinImageFromResourcePath(deathSkinPath);
            Boolean deathSkinFadeOut = extractBoolean(jsonContent, "death_skin_fade_out");
            game.imageSelection.setDeathSkinFadeOut(deathSkinFadeOut == null || deathSkinFadeOut);
        } else {
            game.imageSelection.clearDeathSkinImage();
        }

        String deathSoundPath = extractPath(jsonContent, "deathsound");
        if (deathSoundPath != null) {
            game.setDeathSoundEffectPath(deathSoundPath);
        } else {
            game.setDeathSoundEffectPath(game.getDefaultDeathSoundEffectPath());
        }

        String deathExplosionSoundPath = extractPath(jsonContent, "death_explosion");
        if (deathExplosionSoundPath != null) {
            game.setDeathExplosionSoundEffectPath(deathExplosionSoundPath);
        } else {
            game.clearDeathExplosionSoundEffectPath();
        }

        Boolean deathSoundEnabled = extractBoolean(jsonContent, "deathsound_enabled");
        game.setDeathSoundEnabled(deathSoundEnabled == null || deathSoundEnabled);

        Boolean deathSoundLooping = extractBoolean(jsonContent, "deathsound_loop");
        game.setDeathSoundLooping(deathSoundLooping != null && deathSoundLooping);

        Boolean musicEnabled = extractBoolean(jsonContent, "music_enabled");
        String musicPath = extractPath(jsonContent, "music");
        String midiSoundfontPath = extractOptionalPath(jsonContent, "midi_soundfont");
        if (game.getMusicHandler() != null) {
            game.getMusicHandler().setMidiSoundfontResourcePath(midiSoundfontPath);
            if (Boolean.FALSE.equals(musicEnabled)) {
                game.setCurrentThemeExpectedMusicPath(null);
                if (game.hasGameStarted()) {
                    game.getMusicHandler().clearInterruptedTrack();
                    game.getMusicHandler().stopCurrentTrack();
                }
            } else if (musicPath != null) {
                game.setCurrentThemeExpectedMusicPath(musicPath);
                if (!game.hasGameStarted() || game.isGameOver()) {
                    game.getMusicHandler().queueTrackWithoutPlaying(musicPath);
                } else {
                    game.getMusicHandler().selectTrack(musicPath);
                }
            } else {
                game.setCurrentThemeExpectedMusicPath(null);
            }
        }

        SwingUtilities.invokeLater(game::repaint);
    }

    private static void applyImagePath(SpaceInvadersUI game, String jsonContent, String key, ResourceApplier applier) {
        String resourcePath = extractPath(jsonContent, key);
        if (resourcePath == null) {
            return;
        }
        applier.apply(resourcePath);
    }

    private static String extractPath(String jsonContent, String key) {
        String quotedKey = Pattern.quote(key);
        Pattern pattern = Pattern.compile(String.format(STRING_FIELD_PATTERN_TEMPLATE.pattern(), quotedKey));
        Matcher matcher = pattern.matcher(jsonContent);
        if (!matcher.find()) {
            return null;
        }

        String normalized = normalizeResourcePath(matcher.group(1));
        if (ThemeImplementation.class.getResource(normalized) == null) {
            GameExceptions.showErrorDialog("Theme resource missing for key '" + key + "': " + normalized);
            return null;
        }
        return normalized;
    }

    private static String extractOptionalPath(String jsonContent, String key) {
        String quotedKey = Pattern.quote(key);
        Pattern pattern = Pattern.compile(String.format(STRING_FIELD_PATTERN_TEMPLATE.pattern(), quotedKey));
        Matcher matcher = pattern.matcher(jsonContent);
        if (!matcher.find()) {
            return null;
        }

        String normalized = normalizeResourcePath(matcher.group(1));
        if (ThemeImplementation.class.getResource(normalized) == null) {
            return null;
        }

        return normalized;
    }

    public static String readOptionalThemeResourcePath(String themeJsonResourcePath, String key) {
        if (themeJsonResourcePath == null || themeJsonResourcePath.isBlank()
                || key == null || key.isBlank()) {
            return null;
        }

        String normalizedThemePath = normalizeResourcePath(themeJsonResourcePath);
        String jsonContent = readResourceFile(normalizedThemePath);
        if (jsonContent == null) {
            return null;
        }

        return extractOptionalPath(jsonContent, key);
    }

    public static String readOptionalThemeString(String themeJsonResourcePath, String key) {
        if (themeJsonResourcePath == null || themeJsonResourcePath.isBlank()
                || key == null || key.isBlank()) {
            return null;
        }

        String normalizedThemePath = normalizeResourcePath(themeJsonResourcePath);
        String jsonContent = readResourceFile(normalizedThemePath);
        if (jsonContent == null) {
            return null;
        }

        String quotedKey = Pattern.quote(key);
        Pattern pattern = Pattern.compile(String.format(STRING_FIELD_PATTERN_TEMPLATE.pattern(), quotedKey));
        Matcher matcher = pattern.matcher(jsonContent);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    private static Boolean extractBoolean(String jsonContent, String key) {
        String quotedKey = Pattern.quote(key);
        Pattern pattern = Pattern.compile(String.format(BOOLEAN_FIELD_PATTERN_TEMPLATE.pattern(), quotedKey),
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(jsonContent);
        if (!matcher.find()) {
            return null;
        }

        return Boolean.parseBoolean(matcher.group(1));
    }

    private static String readResourceFile(String resourcePath) {
        try (InputStream input = ThemeImplementation.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                return null;
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            GameExceptions.handleWithDialog("Unable to read theme file " + resourcePath, e);
            return null;
        }
    }

    private static String normalizeResourcePath(String path) {
        String normalized = path.trim().replace('\\', '/');
        if (normalized.startsWith("src/")) {
            normalized = normalized.substring(3);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    @FunctionalInterface
    private interface ResourceApplier {
        void apply(String resourcePath);
    }
}
