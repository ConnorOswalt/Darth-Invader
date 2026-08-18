package spaceinvaders.DataHandlers.MenuImplementations;

import spaceinvaders.DataHandlers.PreferencesManager;
import spaceinvaders.GameExceptions;
import spaceinvaders.UI.SpaceInvadersUI;

import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class BulletImplementation {
    public void handleBulletSelection(ActionEvent e) {
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
                    "Enter project resource path (example: /resources/Bullet/laser.png):");

            if (customPath == null || customPath.isBlank()) {
                return;
            }

            String normalizedPath = normalizeResourcePath(customPath);
            if (BulletImplementation.class.getResource(normalizedPath) == null) {
                GameExceptions.showErrorDialog("Invalid project resource path: " + customPath);
                return;
            }

            game.imageSelection.setBulletImageFromResourcePath(normalizedPath);
            PreferencesManager.saveCurrentSelections(game);
            game.repaint();
            return;
        }

        game.imageSelection.setBulletImageFromResourcePath(selectedPath);
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
