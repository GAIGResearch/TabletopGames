package games.coltexpress.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import players.ActionController;
import players.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

import static core.CoreConstants.ALWAYS_DISPLAY_CURRENT_PLAYER;
import static core.CoreConstants.ALWAYS_DISPLAY_FULL_OBSERVABLE;

public class ColtExpressGUI extends AbstractGUI {
    final static int playerAreaWidth = 300;
    final static int playerAreaHeight = 130;
    final static int ceCardWidth = 90;
    final static int ceCardHeight = 115;

    int width, height;
    ColtExpressPlayerView[] playerHands;
    ColtExpressDeckView plannedActions;

    int activePlayer = -1;
    int humanID;

    public ColtExpressGUI(AbstractGameState gameState, ActionController ac, int humanID) {
        super(ac, 15);
        this.humanID = humanID;

        if (gameState != null) {
            activePlayer = gameState.getCurrentPlayer();
            int nPlayers = gameState.getNPlayers();
            int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
            double nVertAreas = 3.5;
            this.width = playerAreaWidth * nHorizAreas;
            this.height = (int)(playerAreaHeight * nVertAreas);

            ColtExpressGameState cegs = (ColtExpressGameState) gameState;
            ColtExpressParameters cep = (ColtExpressParameters) gameState.getGameParameters();

            // Create main game area that will hold all game views
            playerHands = new ColtExpressPlayerView[nPlayers];
            JPanel mainGameArea = new JPanel();
            mainGameArea.setLayout(new BorderLayout());

            // Player hands go on the edges
            String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
            JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
            int next = 0;
            for (int i = 0; i < nPlayers; i++) {
                ColtExpressPlayerView playerHand = new ColtExpressPlayerView(cegs.getPlayerDecks().get(i), i,
                        cep.getDataPath(), cegs.getPlayerCharacters());
                sides[next].add(playerHand);
                sides[next].setLayout(new GridBagLayout());
                next = (next+1) % (locations.length);
                playerHands[i] = playerHand;
            }
            for (int i = 0; i < locations.length; i++) {
                mainGameArea.add(sides[i], locations[i]);
            }

            // Discard and draw piles go in the center
            JPanel centerArea = new JPanel();
            centerArea.setLayout(new BoxLayout(centerArea, BoxLayout.Y_AXIS));
            plannedActions = new ColtExpressDeckView(cegs.getPlannedActions(), true, cep.getDataPath(), cegs.getPlayerCharacters());
            centerArea.add(plannedActions);
            JPanel jp = new JPanel();
            jp.setLayout(new GridBagLayout());
            jp.add(centerArea);
            mainGameArea.add(jp, BorderLayout.CENTER);

            // Top area will show state information
            JPanel infoPanel = createGameStateInfoPanel("Uno", gameState, width, defaultInfoPanelHeight);
            // Bottom area will show actions available
            JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight, false);

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
                playerHands[activePlayer].setCardHighlight(-1);
            }

            // Update decks and visibility
            ColtExpressGameState cegs = (ColtExpressGameState)gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update((ColtExpressGameState) gameState);
                if (i == gameState.getCurrentPlayer() && ALWAYS_DISPLAY_CURRENT_PLAYER
                        || i == humanID
                        || ALWAYS_DISPLAY_FULL_OBSERVABLE) {
                    playerHands[i].setFront(true);
                } else {
                    playerHands[i].setFront(false);
                }
            }
            plannedActions.updateComponent(cegs.getPlannedActions());

            // Update actions
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20);
    }
}
