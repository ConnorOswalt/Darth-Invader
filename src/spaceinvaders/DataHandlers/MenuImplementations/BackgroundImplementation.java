package spaceinvaders.DataHandlers.MenuImplementations;

import spaceinvaders.DataHandlers.PreferencesManager;
import spaceinvaders.GameExceptions;
import spaceinvaders.UI.SpaceInvadersUI;

import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class BackgroundImplementation {
    public static final String STARS_BACKGROUND_OPTION = "__stars_background__";

    public void handleBackgroundSelection(ActionEvent e) {
        if (!(e.getSource() instanceof JMenuItem)) {
            return;
        }

        JMenuItem selectedItem = (JMenuItem) e.getSource();
        String selectedPath = selectedItem.getName();

        SpaceInvadersUI game = SpaceInvadersUI.getActiveInstance();
        if (game == null) {
            return;
        }

        if (STARS_BACKGROUND_OPTION.equals(selectedPath)) {
            game.imageSelection.enableStarsBackground(game);
            PreferencesManager.saveCurrentSelections(game);
            game.repaint();
            return;
        }

        if (selectedPath == null || selectedPath.isBlank()) {
            String customPath = JOptionPane.showInputDialog(
                    null,
                    "Enter project resource path (example: /resources/Background/stars.gif):");

            if (customPath == null || customPath.isBlank()) {
                return;
            }

            String normalizedPath = normalizeResourcePath(customPath);
            if (BackgroundImplementation.class.getResource(normalizedPath) == null) {
                GameExceptions.showErrorDialog("Invalid project resource path: " + customPath);
                return;
            }

            game.imageSelection.setBackgroundImageFromResourcePath(normalizedPath);
            PreferencesManager.saveCurrentSelections(game);
            game.repaint();
            return;
        }

        game.imageSelection.setBackgroundImageFromResourcePath(selectedPath);
        PreferencesManager.saveCurrentSelections(game);
        game.repaint();
    }

    private String normalizeResourcePath(String customPath) {
        String normalized = customPath.trim().replace('\\', '/');
        if (normalized.startsWith("src/")) {
            normalized = normalized.substring(3);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }
}
