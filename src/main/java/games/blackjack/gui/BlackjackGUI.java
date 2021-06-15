package games.blackjack.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Collection;

import static core.CoreConstants.ALWAYS_DISPLAY_CURRENT_PLAYER;
import static core.CoreConstants.ALWAYS_DISPLAY_FULL_OBSERVABLE;

public class BlackjackGUI extends AbstractGUI {
    final static int playerWidth = 300;
    final static int playerHeight = 130;
    final static int cardWidth = 90;
    final static int cardHeight = 115;

    int width, height;
    BlackjackPlayerView[] playerHands;

    int activePlayer = -1;
    int humanID;

    Border highlightActive = BorderFactory.createLineBorder(new Color(47,132,220,3));
    Border[] playerViewBorders;

    public BlackjackGUI(Game game, ActionController ac, int humanID) {
        super(ac, 15);
        this.humanID = humanID;

        if (game != null){
            AbstractGameState gameState = game.getGameState();
            if (gameState != null){
                activePlayer = gameState.getCurrentPlayer();

                int nPlayers = gameState.getNPlayers();
                int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
                double nVertAreas = 3.5;
                this.width = playerWidth * nHorizAreas;
                this.height = (int) (playerHeight* nVertAreas);

                BlackjackGameState bjgs = (BlackjackGameState) gameState;
                BlackjackParameters bjgp = (BlackjackParameters) gameState.getGameParameters();

                playerHands = new BlackjackPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setLayout(new BorderLayout());

                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
                int next = 0;
                for (int i = 0; i < nPlayers; i++) {
                    BlackjackPlayerView playerHand = new BlackjackPlayerView(bjgs.PlayerDecks().get(i), i, bjgp.getDataPath());

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
                JPanel jp = new JPanel();
                jp.setLayout(new GridBagLayout());
                jp.add(centerArea);
                mainGameArea.add(jp, BorderLayout.CENTER);

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Blackjack", gameState, width, defaultInfoPanelHeight);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight, false);

                // Add all views to frame
                getContentPane().add(mainGameArea, BorderLayout.CENTER);
                getContentPane().add(infoPanel, BorderLayout.NORTH);
                getContentPane().add(actionPanel, BorderLayout.SOUTH);
            }
        }
        setFrameProperties();
            }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (gameState.getCurrentPlayer() != activePlayer) {
                playerHands[activePlayer].setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }

            // Update decks and visibility
            BlackjackGameState bjgs = (BlackjackGameState) gameState;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update((BlackjackGameState) gameState);
                if (i == gameState.getCurrentPlayer() && ALWAYS_DISPLAY_CURRENT_PLAYER
                        || i == humanID
                        || ALWAYS_DISPLAY_FULL_OBSERVABLE) {
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
/*            drawdeck.updateComponent(bjgs.DrawDeck());
            if (ALWAYS_DISPLAY_FULL_OBSERVABLE) {
                drawdeck.setFront(true);
            }*/

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

