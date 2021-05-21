package games.coltexpress.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.components.Compartment;
import gui.ScreenHighlight;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

import static core.CoreConstants.ALWAYS_DISPLAY_CURRENT_PLAYER;
import static core.CoreConstants.ALWAYS_DISPLAY_FULL_OBSERVABLE;

public class ColtExpressGUI extends AbstractGUI {
    // Settings for display area sizes
    final static int playerAreaWidth = 250;
    final static int playerAreaWidthScroll = 290;
    final static int playerAreaHeight = 150;
    final static int playerAreaHeightScroll = 150;
    final static int ceCardWidth = 50;
    final static int ceCardHeight = 60;
    final static int roundCardWidth = 80;
    final static int roundCardHeight = 50;
    final static int trainCarWidth = 130;
    final static int trainCarHeight = 80;
    final static int playerSize = 40;
    final static int lootSize = 20;

    // Player views
    ColtExpressPlayerView[] playerHands;
    // Planned actions deck view
    ColtExpressDeckView plannedActions;
    // Main train view
    ColtExpressTrainView trainView;

    // Currently active player
    int activePlayer = -1;
    // ID of human player
    int humanID;
    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(220, 169, 11), 3);
    Border[] playerViewBorders;

    public ColtExpressGUI(Game game, ActionController ac, int humanID) {
        super(ac, 25);
        this.humanID = humanID;

        if (game != null) {
            AbstractGameState gameState = game.getGameState();
            if (gameState != null) {
                activePlayer = gameState.getCurrentPlayer();
                int nPlayers = gameState.getNPlayers();
                double nHorizAreas = 1.5 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
                double nVertAreas = 3.5;
                this.width = (int) (playerAreaWidth * nHorizAreas);
                this.height = (int) (playerAreaHeight * nVertAreas);

                ColtExpressGameState cegs = (ColtExpressGameState) gameState;
                ColtExpressParameters cep = (ColtExpressParameters) gameState.getGameParameters();

                // Create main game area that will hold all game views
                JPanel mainGameArea = new JPanel();
                mainGameArea.setLayout(new BorderLayout());

                // Player hands go on the edges
                playerHands = new ColtExpressPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
                int next = 0;
                for (int i = 0; i < nPlayers; i++) {
                    ColtExpressPlayerView playerHand = new ColtExpressPlayerView(i, cep.getDataPath(), cegs.getPlayerCharacters());

                    // Get agent name
                    String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                    String agentName = split[split.length - 1];

                    // Create border, layouts and keep track of this view
                    TitledBorder title = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                            TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    playerViewBorders[i] = title;
                    playerHand.setBorder(title);

                    sides[next].add(playerHand);
                    sides[next].setLayout(new GridBagLayout());
                    next = (next + 1) % (locations.length);
                    playerHands[i] = playerHand;
                }
                for (int i = 0; i < locations.length; i++) {
                    mainGameArea.add(sides[i], locations[i]);
                }

                // Planned actions + train + rounds go in the center
                JPanel centerArea = new JPanel();
                centerArea.setLayout(new BoxLayout(centerArea, BoxLayout.Y_AXIS));
                List<Compartment> train = ((ColtExpressGameState) gameState).getTrainCompartments();
                trainView = new ColtExpressTrainView(train, cep.getDataPath(), cegs.getPlayerCharacters());
                plannedActions = new ColtExpressDeckView(cegs.getPlannedActions(), true, cep.getDataPath(), cegs.getPlayerCharacters());
                centerArea.add(trainView);
                centerArea.add(plannedActions);
                JPanel jp = new JPanel();
                jp.setLayout(new GridBagLayout());
                jp.add(centerArea);
                mainGameArea.add(jp, BorderLayout.CENTER);

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Colt Express", gameState, width, defaultInfoPanelHeight);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new ScreenHighlight[0], width, defaultActionPanelHeight, false, true);

                getContentPane().add(mainGameArea, BorderLayout.CENTER);
                getContentPane().add(infoPanel, BorderLayout.NORTH);
                getContentPane().add(actionPanel, BorderLayout.SOUTH);
            }
        }

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState, boolean actionTaken) {
        if (gameState != null) {
            if (gameState.getCurrentPlayer() != activePlayer) {
                activePlayer = gameState.getCurrentPlayer();
            }

            // Update decks and visibility
            ColtExpressGameState cegs = (ColtExpressGameState)gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update((ColtExpressGameState) gameState, humanID);

                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    Border compound = BorderFactory.createCompoundBorder(
                            highlightActive, playerViewBorders[i]);
                    playerHands[i].setBorder(compound);
                } else {
                    playerHands[i].setBorder(playerViewBorders[i]);
                }
            }
            plannedActions.updateComponent(cegs.getPlannedActions());
            int activePlayer = (ALWAYS_DISPLAY_CURRENT_PLAYER || ALWAYS_DISPLAY_FULL_OBSERVABLE? player.getPlayerID(): player.getPlayerID()==humanID? player.getPlayerID():-1);
            plannedActions.informActivePlayer(player.getPlayerID());

            // Show planned actions from the first played
            if (gameState.getGamePhase() == ColtExpressGameState.ColtExpressGamePhase.ExecuteActions) {
                plannedActions.setFirstOnTop(true);
            } else {
                plannedActions.setFirstOnTop(false);
            }

            // Update train view
            trainView.update(cegs);

            // Update actions
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

}
