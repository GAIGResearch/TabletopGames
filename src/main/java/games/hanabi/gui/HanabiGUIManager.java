package games.hanabi.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;

import games.hanabi.HanabiGameState;
import games.hanabi.HanabiParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.CardView;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class HanabiGUIManager extends AbstractGUIManager {
    final static int playerAreaWidth = 300;
    final static int playerAreaHeight = 130;
    final static int hanabiCardWidth = 90;
    final static int hanabiCardHeight = 115;

    // List of player hand views
    HanabiPlayerView[] playerHands;
    // Discard pile view
//    HanabiDeckView discardPile;
    JLabel cardsPlayed;
    JLabel failCounter;
    JLabel hintCounter;
    JLabel drawDeck;
    JLabel discardDeck;

    CardView[] currentCards;

    // Currently active player
    int activePlayer = -1;
    // ID of human player
    int humanID;

    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);
    Border[] playerViewBorders;

    public HanabiGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);

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

                HanabiGameState hbgs = (HanabiGameState) gameState;
                HanabiParameters hbp = (HanabiParameters) gameState.getGameParameters();

                // Create main game area that will hold all game views
                playerHands = new HanabiPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setLayout(new BorderLayout());

                // Player hands go on the edges
                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
                int next = 0;
                for (int i = 0; i < nPlayers; i++) {
                    HanabiPlayerView playerHand = new HanabiPlayerView(hbgs, hbgs.getPlayerDecks().get(i), i, i, hbp.getDataPath());

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
//                discardPile = new HanabiDeckView(hbgs, -1, hbgs.getDiscardDeck(), true, hbp.getDataPath(), new Rectangle(0, 0, hanabiCardWidth, hanabiCardHeight));
//                drawPile = new HanabiDeckView(hbgs,-1, hbgs.getDrawDeck(), gameState.getCoreGameParameters().alwaysDisplayFullObservable, hbp.getDataPath(), new Rectangle(0, 0, hanabiCardWidth, hanabiCardHeight));
//                centerArea.add(drawPile);
//                centerArea.add(discardPile);
//                centerArea.add(failCounter);
//                centerArea.add(hintCounter);
                JPanel jp = new JPanel();
                jp.setLayout(new GridBagLayout());
                jp.add(centerArea);
                mainGameArea.add(jp, BorderLayout.CENTER);

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Hanabi", gameState, width, defaultInfoPanelHeight+50);
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
    }

    @Override
    public int getMaxActionSpace() {
        return 200;
    }

    @Override
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        cardsPlayed = new JLabel("Played: ");
        failCounter = new JLabel("Fails: 0");
        hintCounter = new JLabel("Hints: 0");
        drawDeck = new JLabel("Cards in deck: 0");
        discardDeck = new JLabel("Discards: ");

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(cardsPlayed);
        gameInfo.add(failCounter);
        gameInfo.add(hintCounter);
        gameInfo.add(drawDeck);
        gameInfo.add(discardDeck);

        gameInfo.setPreferredSize(new Dimension(width / 2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        createActionHistoryPanel(width/2 - 10, height, new HashSet<>());
        wrapper.add(historyContainer);
        return wrapper;
    }

    @Override
    protected void updateGameStateInfo(AbstractGameState gameState) {
        super.updateGameStateInfo(gameState);
        cardsPlayed.setText("Played: " + ((HanabiGameState)gameState).getCurrentCard().toString());
        hintCounter.setText("Hints: " + ((HanabiGameState)gameState).getHintCounter().getValue());
        failCounter.setText("Fails: " + ((HanabiGameState)gameState).getFailCounter().getValue());
        drawDeck.setText("Cards in deck: " + ((HanabiGameState)gameState).getDrawDeck().getSize());
        discardDeck.setText("Discards: " + ((HanabiGameState)gameState).getDiscardDeck().toString());
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (gameState.getCurrentPlayer() != activePlayer) {
                playerHands[activePlayer].playerHandView.setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }

            // Update decks and visibility
            HanabiGameState ugs = (HanabiGameState)gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update((HanabiGameState) gameState);
                if (i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer
                        || i == humanID
                        || gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
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
//            discardPile.updateComponent(ugs.getDiscardDeck());
//            discardPile.setFocusable(true);
//            drawPile.updateComponent(ugs.getDrawDeck());
//            failCounter.updateComponent(ugs.getFailCounter());
//            hintCounter.updateComponent(ugs.getHintCounter());
//            if (gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
//                drawPile.setFront(true);
//            }

        }
    }
}
