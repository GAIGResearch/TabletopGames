package games.uno.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.uno.UnoGameParameters;
import games.uno.UnoGameState;
import gui.ScreenHighlight;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static core.CoreConstants.ALWAYS_DISPLAY_CURRENT_PLAYER;
import static core.CoreConstants.ALWAYS_DISPLAY_FULL_OBSERVABLE;

public class UnoGUI extends AbstractGUI {
    // Settings for display areas
    final static int playerAreaWidth = 300;
    final static int playerAreaHeight = 130;
    final static int unoCardWidth = 90;
    final static int unoCardHeight = 115;

    // List of player hand views
    UnoPlayerView[] playerHands;
    // Discard pile view
    UnoDeckView discardPile;
    // Draw pile view
    UnoDeckView drawPile;

    // Currently active player
    int activePlayer = -1;
    // ID of human player
    int humanID;

    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);
    Border[] playerViewBorders;

    public UnoGUI(Game game, ActionController ac, int humanID) {
        super(ac, 15);
        this.humanID = humanID;

        if (game != null) {
            AbstractGameState gameState = game.getGameState();
            if (gameState != null) {
                // Initialise active player
                activePlayer = gameState.getCurrentPlayer();

                // Find required size of window
                int nPlayers = gameState.getNPlayers();
                int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
                double nVertAreas = 3.5;
                this.width = playerAreaWidth * nHorizAreas;
                this.height = (int) (playerAreaHeight * nVertAreas);

                UnoGameState ugs = (UnoGameState) gameState;
                UnoGameParameters ugp = (UnoGameParameters) gameState.getGameParameters();

                // Create main game area that will hold all game views
                playerHands = new UnoPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setLayout(new BorderLayout());

                // Player hands go on the edges
                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
                int next = 0;
                for (int i = 0; i < nPlayers; i++) {
                    UnoPlayerView playerHand = new UnoPlayerView(ugs.getPlayerDecks().get(i), i, humanID, ugp.getDataPath());

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

                // Discard and draw piles go in the center
                JPanel centerArea = new JPanel();
                centerArea.setLayout(new BoxLayout(centerArea, BoxLayout.Y_AXIS));
                discardPile = new UnoDeckView(-1, ugs.getDiscardDeck(), true, ugp.getDataPath(), new Rectangle(0, 0, unoCardWidth, unoCardHeight));
                drawPile = new UnoDeckView(-1, ugs.getDrawDeck(), ALWAYS_DISPLAY_FULL_OBSERVABLE, ugp.getDataPath(), new Rectangle(0, 0, unoCardWidth, unoCardHeight));
                centerArea.add(drawPile);
                centerArea.add(discardPile);
                JPanel jp = new JPanel();
                jp.setLayout(new GridBagLayout());
                jp.add(centerArea);
                mainGameArea.add(jp, BorderLayout.CENTER);

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Uno", gameState, width, defaultInfoPanelHeight);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new ScreenHighlight[0], width, defaultActionPanelHeight, false, true);

                // Add all views to frame
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
                playerHands[activePlayer].playerHandView.setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }

            // Update decks and visibility
            UnoGameState ugs = (UnoGameState)gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update((UnoGameState) gameState);
                if (i == gameState.getCurrentPlayer() && ALWAYS_DISPLAY_CURRENT_PLAYER
                        || i == humanID
                        || ALWAYS_DISPLAY_FULL_OBSERVABLE) {
                    playerHands[i].playerHandView.setFront(true);
                    playerHands[i].setFocusable(true);
                } else {
                    playerHands[i].playerHandView.setFront(false);
                }

                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    Border compound = BorderFactory.createCompoundBorder(
                            highlightActive, playerViewBorders[i]);
                    playerHands[i].setBorder(compound);
                } else {
                    playerHands[i].setBorder(playerViewBorders[i]);
                }
            }
            discardPile.updateComponent(ugs.getDiscardDeck());
            discardPile.setFocusable(true);
            drawPile.updateComponent(ugs.getDrawDeck());
            if (ALWAYS_DISPLAY_FULL_OBSERVABLE) {
                drawPile.setFront(true);
            }

            // Update actions
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

}
