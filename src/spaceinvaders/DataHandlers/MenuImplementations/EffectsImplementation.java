package spaceinvaders.DataHandlers.MenuImplementations;

import spaceinvaders.DataHandlers.PreferencesManager;
import spaceinvaders.UI.SpaceInvadersUI;

import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;

public class EffectsImplementation {
    public void handleEffectsSelection(ActionEvent e) {
        if (!(e.getSource() instanceof JMenuItem)) {
            return;
        }

        JMenuItem selectedItem = (JMenuItem) e.getSource();
        String command = selectedItem.getName();

        SpaceInvadersUI game = SpaceInvadersUI.getActiveInstance();
        if (game == null || command == null) {
            return;
        }

        if ("enable_explosions".equals(command)) {
            game.setExplosionsEnabled(true);
            PreferencesManager.saveCurrentSelections(game);
            return;
        }

        if ("disable_explosions".equals(command)) {
            game.setExplosionsEnabled(false);
            PreferencesManager.saveCurrentSelections(game);
        }
    }
}
