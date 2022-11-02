package games.dicemonastery.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.dicemonastery.DiceMonasteryGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.ScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class DiceMonasteryGUI extends AbstractGUIManager {

    // Settings for display areas
    final static int playerAreaWidth = 360;
    final static int playerAreaHeight = 180;

    // Currently active player
    int activePlayer = -1;
    int humanId;

    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(220, 27, 67), 3);
    List<Border> playerViewBorders = new ArrayList<>();
    List<PlayerBoard> playerBoards;
    MainBoard mainBoard;

    public DiceMonasteryGUI(GamePanel p, Game game, ActionController ac, int humanID) {
        super(p, ac, 20);
        this.humanId = humanID;

        if (game != null && game.getGameState() != null) {
            AbstractGameState gameState = game.getGameState();
            // Initialise active player
            activePlayer = gameState.getCurrentPlayer();

            // Find required size of window
            int nPlayers = gameState.getNPlayers();
            int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : 3);
            double nVertAreas = 3.5;
            this.width = playerAreaWidth * nHorizAreas;
            this.height = (int) (playerAreaHeight * nVertAreas);

            JPanel mainGameArea = new JPanel();
            GridBagConstraints gbc = new GridBagConstraints();
            mainGameArea.setLayout(new GridBagLayout());

            mainBoard = new MainBoard();
            playerBoards = IntStream.range(0, nPlayers).mapToObj(PlayerBoard::new).collect(toList());

            // P1  *   P2
            // Main Board
            // P3  *   P4
            int[][] locations = {{0, 0}, {2, 0}, {0, 2}, {2, 2}};
            for (int i = 0; i < nPlayers; i++) {
                // Get agent name
                String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                String agentName = split[split.length - 1];

                // Create border, layouts and keep track of this view
                TitledBorder title = BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                        TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                PlayerBoard pb = playerBoards.get(i);
                playerViewBorders.add(title);
                pb.setBorder(title);
                gbc.gridx = locations[i][0];
                gbc.gridy = locations[i][1];
                gbc.weightx = 1.0;
                mainGameArea.add(pb, gbc);
            }

            // Discard and draw piles go in the center
            gbc.gridx = 0;
            gbc.gridy = 1;
            mainGameArea.add(mainBoard, gbc);

            // Top area will show state information
            JPanel infoPanel = createGameStateInfoPanel("Dice Monastery", gameState, width, defaultInfoPanelHeight);
            // Bottom area will show actions available
            JComponent actionPanel = createActionPanel(new ScreenHighlight[0], width, defaultActionPanelHeight, false);

            // Add all views to frame
            parent.add(mainGameArea, BorderLayout.CENTER);
            parent.add(infoPanel, BorderLayout.NORTH);
            parent.add(actionPanel, BorderLayout.SOUTH);

            parent.setPreferredSize(new Dimension(width, height));
            parent.revalidate();
            parent.setVisible(true);
            parent.repaint();
        }

    }


    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (gameState.getCurrentPlayer() != activePlayer) {
                activePlayer = gameState.getCurrentPlayer();
            }

            DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                PlayerBoard pb = playerBoards.get(i);
                pb.update(state);

                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    Border compound = BorderFactory.createCompoundBorder(
                            highlightActive, playerViewBorders.get(i));
                    pb.setBorder(compound);
                } else {
                    pb.setBorder(playerViewBorders.get(i));
                }
            }

            mainBoard.update(state);
        }
    }
}
