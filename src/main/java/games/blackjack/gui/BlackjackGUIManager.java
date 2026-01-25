package games.blackjack.gui;

import gui.*;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Set;

public class BlackjackGUIManager extends AbstractGUIManager {
    final static int playerWidth = 300;
    final static int playerHeight = 130;
    final static int cardWidth = 90;
    final static int cardHeight = 115;

    int width, height;
    BlackjackPlayerView[] playerHands;

    int activePlayer = -1;

    Border highlightActive = BorderFactory.createLineBorder(new Color(47,132,220), 3);
    Border[] playerViewBorders;

    public BlackjackGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        if (game != null){
            AbstractGameState gameState = game.getGameState();
            if (gameState != null){
                JTabbedPane pane = new JTabbedPane();
                JPanel main = new JPanel();
                main.setOpaque(false);
                main.setLayout(new BorderLayout());
                JPanel rules = new JPanel();
                pane.add("Main", main);
                pane.add("Rules", rules);
                JLabel ruleText = new JLabel(getRuleText());
                rules.add(ruleText);
                rules.setBackground(new Color(43, 108, 25, 111));

                activePlayer = gameState.getCurrentPlayer();

                int nPlayers = gameState.getNPlayers();
                int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
                double nVertAreas = 3.5;
                this.width = playerWidth * nHorizAreas;
                this.height = (int) (playerHeight* nVertAreas);
                ruleText.setPreferredSize(new Dimension(width*2/3+60, height*2/3+100));

                BlackjackGameState bjgs = (BlackjackGameState) gameState;
                BlackjackParameters bjgp = (BlackjackParameters) gameState.getGameParameters();

                parent.setBackground(ImageIO.GetInstance().getImage("data/FrenchCards/table-background.jpg"));

                playerHands = new BlackjackPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setOpaque(false);
                mainGameArea.setLayout(new BorderLayout());

                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
                int next = 0;
                for (int i = 0; i < nPlayers; i++) {
                    BlackjackPlayerView playerHand = new BlackjackPlayerView(bjgs.getPlayerDecks().get(i), i, bjgp.getDataPath());
                    playerHand.setOpaque(false);

                    // Get agent name
                    String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                    String agentName = split[split.length - 1];

                    // Create border, layouts and keep track of this view
                    TitledBorder title;
                    if (i == bjgs.getDealerPlayer()) {
                        title = BorderFactory.createTitledBorder(
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "DEALER [" + agentName + "]",
                                TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    } else {
                        title = BorderFactory.createTitledBorder(
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                                TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    }
                    playerViewBorders[i] = title;
                    playerHand.setBorder(title);

                    sides[next].add(playerHand);
                    sides[next].setLayout(new GridBagLayout());
                    sides[next].setOpaque(false);
                    next = (next + 1) % (locations.length);
                    playerHands[i] = playerHand;
                }
                for (int i = 0; i < locations.length; i++) {
                    mainGameArea.add(sides[i], locations[i]);
                }

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Blackjack", gameState, width, defaultInfoPanelHeight);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false);

                // Add all views to frame
                main.add(mainGameArea, BorderLayout.CENTER);
                main.add(infoPanel, BorderLayout.NORTH);
                main.add(actionPanel, BorderLayout.SOUTH);

                pane.add("Main", main);
                pane.add("Rules", rules);

                parent.setLayout(new BorderLayout());
                parent.add(pane, BorderLayout.CENTER);
                parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20));
                parent.revalidate();
                parent.setVisible(true);
                parent.repaint();
            }
        }
    }

    @Override
    public int getMaxActionSpace() {
        return 15;
    }

    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height, boolean boxLayout) {
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        if (boxLayout) {
            actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        }

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights);
            actionButtons[i] = ab;
            actionButtons[i].setVisible(false);
            actionPanel.add(actionButtons[i]);
        }
        for (ActionButton actionButton : actionButtons) {
            actionButton.informAllActionButtons(actionButtons);
        }

        JScrollPane pane = new JScrollPane(actionPanel);
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.setPreferredSize(new Dimension(width, height));
        if (boxLayout) {
            pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }
        return pane;
    }

    @Override
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setOpaque(false);
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);

        gameInfo.setPreferredSize(new Dimension(width/2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        historyInfo.setPreferredSize(new Dimension(width/2 - 10, height));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setPreferredSize(new Dimension(width/2 - 25, height));
        wrapper.add(historyContainer);
        historyInfo.setOpaque(false);
        historyContainer.setOpaque(false);
        historyContainer.getViewport().setBackground(new Color(43, 108, 25, 111));
//        historyContainer.getViewport().setOpaque(false);
        historyInfo.setEditable(false);
        return wrapper;
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
                playerHands[i].update(bjgs);

                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    Border compound = BorderFactory.createCompoundBorder(
                            highlightActive, playerViewBorders[i]);
                    playerHands[i].setBorder(compound);
                } else {
                    playerHands[i].setBorder(playerViewBorders[i]);
                }
            }
        }
    }

    private String getRuleText() {
        String rules = "<html><center><h1>Blackjack</h1></center><br/><hr><br/>";
        rules += "<p>Players are each dealt 2 cards face up. The dealer is also dealt 2 cards, one up (exposed) and one down (hidden). " +
                "The value of number cards 2 through 10 is their pip value (2 through 10). " +
                "Face cards (jack, queen, and king) are all worth 10. " +
                "Aces can be worth 1 or 11. A hand's value is the sum of the card values. Players are allowed to draw additional cards to improve their hands. " +
                "A hand with an ace valued as 11 is called \"soft\", meaning that the hand will be guaranteed to not score more than 21 by taking an additional card. The value of the ace will become 1 to prevent the hand from exceeding 21. Otherwise, the hand is called \"hard\".\n" +
                "</p><br/><p>" +
                "Once all the players have completed their hands, it is the dealer's turn. The dealer hand will not be completed if all players have either exceeded the total of 21 or received blackjacks. The dealer then reveals the hidden card and must draw cards, one by one, until the cards total up to 17 points. " +
                "At 17 points or higher the dealer must stop. " +
                "The better hand is the hand where the sum of the card values is closer to 21 without exceeding 21. The detailed outcome of the hand follows:" +
                "</p><br/><ul><li>" +
                "If the player is dealt an ace and a ten-value card (called a \"blackjack\" or \"natural\"), and the dealer does not, the player wins." +
                "</li><li>If the player exceeds a sum of 21 (\"busts\"), the player loses, even if the dealer also exceeds 21." +
                "</li><li>If the dealer exceeds 21 (\"busts\") and the player does not, the player wins." +
                "</li><li>If the player attains a final sum higher than the dealer and does not bust, the player wins." +
                "</li><li>If both dealer and player receive a blackjack or any other hands with the same sum, this will be called a \"push\" and no one wins.</li></ul>";


        rules += "<hr><p><b>INTERFACE: </b> Choose action (Hit or Stand) at the bottom of the screen.</p>";
        rules += "</html>";
        return rules;
    }
}

