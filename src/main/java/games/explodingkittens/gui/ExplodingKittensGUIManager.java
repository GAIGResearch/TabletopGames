package games.explodingkittens.gui;

import gui.AbstractGUIManager;
import gui.GamePanel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.explodingkittens.ExplodingKittensParameters;
import games.explodingkittens.ExplodingKittensGameState;
import gui.ScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;


public class ExplodingKittensGUIManager extends AbstractGUIManager {
    // Settings for display areas
    final static int playerAreaWidth = 300;
    final static int playerAreaHeight = 135;
    final static int ekCardWidth = 90;
    final static int ekCardHeight = 110;

    // List of player hand views
    ExplodingKittensDeckView[] playerHands;
    // Discard pile view
    ExplodingKittensDiscardView discardPile;
    // Draw pile view
    ExplodingKittensDeckView drawPile;

    // Currently active player
    int activePlayer = -1;
    // ID of human player
    int humanID;
    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(220, 169, 11), 3);
    Border[] playerViewBorders;

    public ExplodingKittensGUIManager(GamePanel parent, Game game, ActionController ac, int humanID) {
        super(parent, ac, 25);
        this.humanID = humanID;

        if (game != null) {
            AbstractGameState gameState = game.getGameState();
            if (gameState != null) {
                // Initialise active player
                activePlayer = gameState.getCurrentPlayer();

                // Find required size of window
                int nPlayers = gameState.getNPlayers();
                int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
                double nVertAreas = 5;
                this.width = playerAreaWidth * nHorizAreas;
                this.height = (int) (playerAreaHeight * nVertAreas) + 20;

                ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;
                ExplodingKittensParameters ekgp = (ExplodingKittensParameters) gameState.getGameParameters();

                // Create main game area that will hold all game views
                playerHands = new ExplodingKittensDeckView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setLayout(new BorderLayout());

                // Player hands go on the edges
                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
                int next = 0;
                for (int i = 0; i < nPlayers; i++) {
                    ExplodingKittensDeckView playerHand = new ExplodingKittensDeckView(humanID, ekgs.getPlayerHandCards().get(i), false, ekgp.getDataPath());

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
                discardPile = new ExplodingKittensDiscardView(ekgs.getDiscardPile(), ekgs.getActionStack(), true, ekgp.getDataPath());
                drawPile = new ExplodingKittensDeckView(-1, ekgs.getDrawPile(), gameState.getCoreGameParameters().alwaysDisplayFullObservable, ekgp.getDataPath());
                centerArea.add(drawPile);
                centerArea.add(discardPile);
                JPanel jp = new JPanel();
                jp.setLayout(new GridBagLayout());
                jp.add(centerArea);
                mainGameArea.add(jp, BorderLayout.CENTER);

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Exploding Kittens", gameState, width, defaultInfoPanelHeight);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new ScreenHighlight[0], width, defaultActionPanelHeight, false, true, null);

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

    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (gameState.getCurrentPlayer() != activePlayer) {
                playerHands[activePlayer].setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }

            // Update decks and visibility
            ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].updateComponent(ekgs.getPlayerHandCards().get(i));
                if (i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer
                        || i == humanID
                        || gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
                    playerHands[i].setFront(true);
                    playerHands[i].setFocusable(true);
                } else {
                    playerHands[i].setFront(false);
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
            discardPile.updateComponent(ekgs.getDiscardPile());
            discardPile.setFocusable(true);
            drawPile.updateComponent(ekgs.getDrawPile());
            if (activePlayer == humanID || gameState.getCoreGameParameters().alwaysDisplayFullObservable)
                drawPile.setFront(true);

        }
    }

}
