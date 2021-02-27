package games.dicemonastery.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.dicemonastery.DiceMonasteryGameState;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class DiceMonasteryGUI extends AbstractGUI {

    // Settings for display areas
    final static int playerAreaWidth = 360;
    final static int playerAreaHeight = 180;
    final static int cardWidth = 90;
    final static int cardHeight = 60;

    // Currently active player
    int activePlayer = -1;
    int humanId;

    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(220, 27, 67), 3);
    List<Border> playerViewBorders = new ArrayList<>();
    List<PlayerBoard> playerBoards;
    MainBoard mainBoard;

    public DiceMonasteryGUI(Game game, ActionController ac, int humanID) {
        super(ac, 20);
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
            mainGameArea.setLayout(new BorderLayout());

            mainBoard = new MainBoard();
            playerBoards = IntStream.range(0, nPlayers).mapToObj(PlayerBoard::new).collect(toList());

            // Player hands go on the edges
            String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
            JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
            int next = 0;

            for (int i = 0; i < nPlayers; i++) {
                // Get agent name
                String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                String agentName = split[split.length - 1];

                // Create border, layouts and keep track of this view
                TitledBorder title = BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                        TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                playerViewBorders.add(title);
                PlayerBoard pb = playerBoards.get(i);
                pb.setBorder(title);
                sides[next].add(pb);
                sides[next].setLayout(new GridBagLayout());
                next = (next + 1) % (locations.length);
            }
            for (int i = 0; i < locations.length; i++) {
                mainGameArea.add(sides[i], locations[i]);
            }

            // Discard and draw piles go in the center
            JPanel centerArea = new JPanel();
            centerArea.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
            mainGameArea.add(centerArea, BorderLayout.CENTER);

            // Top area will show state information
            JPanel infoPanel = createGameStateInfoPanel("Dice Monastery", gameState, width, defaultInfoPanelHeight);
            // Bottom area will show actions available
            JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight, false);

            // Add all views to frame
            getContentPane().add(mainGameArea, BorderLayout.CENTER);
            getContentPane().add(infoPanel, BorderLayout.NORTH);
            getContentPane().add(actionPanel, BorderLayout.SOUTH);
        }

        setFrameProperties();
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

            // Update actions
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }
}
