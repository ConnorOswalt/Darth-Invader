package spaceinvaders;

import java.awt.GraphicsEnvironment;
import javax.swing.*;
import spaceinvaders.UI.SpaceInvadersUI;
import spaceinvaders.scores.LeaderboardPanel;


public class Main {
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Darth Invaders requires a graphical display. Run it locally or configure an X11 display.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Darth Invaders");
            SpaceInvadersUI game = new SpaceInvadersUI();
            frame.add(game);
            frame.setSize(600, 700);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Get menu bar and add Scores menu
            JMenuBar menuBar = game.createMenuBar();
            JMenu scoresMenu = new JMenu("Scores");
            JMenuItem viewLeaderboardItem = new JMenuItem("View Leaderboard");
            viewLeaderboardItem.addActionListener(e -> LeaderboardPanel.showLeaderboard(game.getScoreManager(), game));
            scoresMenu.add(viewLeaderboardItem);
            
            // Add separator
            scoresMenu.addSeparator();
            
            // Add checkbox to enable/disable leaderboard
            JCheckBoxMenuItem enableLeaderboardItem = new JCheckBoxMenuItem("Enable Leaderboard", true);
            enableLeaderboardItem.addActionListener(e -> {
                LeaderboardPanel.setLeaderboardEnabled(enableLeaderboardItem.isSelected());
                viewLeaderboardItem.setEnabled(enableLeaderboardItem.isSelected());
            });
            scoresMenu.add(enableLeaderboardItem);

            JMenu sillyMenu = new JMenu("Silly");
            JCheckBoxMenuItem sillinessModeItem = new JCheckBoxMenuItem("Silliness Mode", true);
            sillinessModeItem.addActionListener(e -> game.setSillinessModeEnabled(sillinessModeItem.isSelected()));
            sillyMenu.add(sillinessModeItem);
            
            menuBar.add(scoresMenu);
            menuBar.add(sillyMenu);
            
            frame.setJMenuBar(menuBar);
            frame.setVisible(true);
        });
    }
}
