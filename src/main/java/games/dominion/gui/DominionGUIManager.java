package games.dominion.gui;

import core.*;
import games.dominion.*;
import gui.IScreenHighlight;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.Set;

public class DominionGUIManager extends AbstractGUIManager {
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
    Border[] playerViewBorders;

    DominionPlayerView[] playerViews;
    DominionDeckView trashPile;
    DominionMarketView marketView;

    public DominionGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);
        // Now we set up the GUI

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

            DominionGameState state = (DominionGameState) gameState;
            DominionParameters params = (DominionParameters) gameState.getGameParameters();

            // Create main game area that will hold all game views
            playerViews = new DominionPlayerView[nPlayers];
            playerViewBorders = new Border[nPlayers];
            JPanel mainGameArea = new JPanel();
            mainGameArea.setLayout(new BorderLayout());

            // Player hands go on the edges
            String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
            JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
            int next = 0;
            for (int i = 0; i < nPlayers; i++) {
                DominionPlayerView playerView = new DominionPlayerView(i, humanId, params.getDataPath(), state);

                // Get agent name
                String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                String agentName = split[split.length - 1];

                // Create border, layouts and keep track of this view
                TitledBorder title = BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                        TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                playerViewBorders[i] = title;
                playerView.setBorder(title);

                sides[next].add(playerView);
                sides[next].setLayout(new GridBagLayout());
                next = (next + 1) % (locations.length);
                playerViews[i] = playerView;
            }
            for (int i = 0; i < locations.length; i++) {
                mainGameArea.add(sides[i], locations[i]);
            }

            // Discard and draw piles go in the center
            JPanel centerArea = new JPanel();
            centerArea.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
            trashPile = new DominionDeckView(-1, state.getDeck(DominionConstants.DeckType.TRASH, -1),
                    true, params.getDataPath(), new Rectangle(0, 0, cardWidth, cardHeight));
            centerArea.add(trashPile);
            marketView = new DominionMarketView(state);
            centerArea.add(marketView);
      //      centerArea.add(marketView.dataDisplay);
            mainGameArea.add(centerArea, BorderLayout.CENTER);

            // Top area will show state information
            JPanel infoPanel = createGameStateInfoPanel("Dominion", gameState, width, defaultInfoPanelHeight);
            // Bottom area will show actions available
            JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, true, null, null, null);

            // Add all views to frame
            parent.setLayout(new BorderLayout());
            parent.add(mainGameArea, BorderLayout.CENTER);
            parent.add(infoPanel, BorderLayout.NORTH);
            parent.add(actionPanel, BorderLayout.SOUTH);
            parent.revalidate();
            parent.setVisible(true);
            parent.repaint();
        }

    }

    @Override
    public int getMaxActionSpace() {
        return 20;
    }

    /**
     * Updates all GUI elements. Must be implemented by subclass.
     *  @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (gameState.getCurrentPlayer() != activePlayer) {
                playerViews[activePlayer].playerHand.setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }

            // Update decks and visibility
            DominionGameState state = (DominionGameState) gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerViews[i].update(state);
                if (i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer
                        || i == humanId
                        || gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
                    playerViews[i].playerHand.setFront(true);
                    playerViews[i].playerHand.setFocusable(true);
                } else {
                    playerViews[i].playerHand.setFront(false);
                }

                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    Border compound = BorderFactory.createCompoundBorder(
                            highlightActive, playerViewBorders[i]);
                    playerViews[i].setBorder(compound);
                } else {
                    playerViews[i].setBorder(playerViewBorders[i]);
                }
            }

            // trashPile should not need to be formally updated, as it is linked to the Game trashpile
            // so the later repaint() should do the trick.

            // MarketView however needs to be updated
            marketView.update(state);

        }
    }
}
