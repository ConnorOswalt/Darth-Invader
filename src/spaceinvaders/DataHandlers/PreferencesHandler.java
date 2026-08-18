package spaceinvaders.DataHandlers;

import spaceinvaders.GameExceptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads and writes cosmetic player preferences (skins, background, effects)
 * to the same per-user data directory used for the score file.
 */
public class PreferencesHandler {
    private static final String APPLICATION_DIRECTORY = "Darth-Invader";
    private static final String PREFERENCES_FILE_NAME = "preferences.properties";
    private final Path preferencesFile;

    public PreferencesHandler() {
        this(resolveDefaultPreferencesFile());
    }

    PreferencesHandler(Path preferencesFile) {
        this.preferencesFile = preferencesFile;
    }

    private static Path resolveDefaultPreferencesFile() {
        String appDataDirectory = System.getenv("APPDATA");
        Path dataDirectory = appDataDirectory == null || appDataDirectory.isBlank()
                ? Path.of(System.getProperty("user.home"), ".darth-invader")
                : Path.of(appDataDirectory, APPLICATION_DIRECTORY);
        return dataDirectory.resolve(PREFERENCES_FILE_NAME);
    }

    public Properties load() {
        Properties properties = new Properties();
        if (!Files.isRegularFile(preferencesFile)) {
            return properties;
        }

        try (InputStream input = Files.newInputStream(preferencesFile)) {
            properties.load(input);
        } catch (IOException e) {
            GameExceptions.logWarning("Failed to load preferences: " + e.getMessage());
        }
        return properties;
    }

    public void save(Properties properties) {
        try {
            Path parent = preferencesFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(preferencesFile)) {
                properties.store(output, "Darth Invader player preferences");
            }
        } catch (IOException e) {
            GameExceptions.logWarning("Failed to save preferences: " + e.getMessage());
        }
    }
}
