package games.loveletter.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import games.loveletter.LoveLetterForwardModel;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.loveletter.actions.*;
import games.loveletter.actions.deep.PlayCardDeep;
import games.loveletter.cards.LoveLetterCard;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;

public class LoveLetterGUIManager extends AbstractGUIManager {
    // Settings for display areas
    final static int playerAreaWidth = 300;
    final static int playerAreaHeight = 135;
    final static int llCardWidth = 90;
    final static int llCardHeight = 115;

    // Width and height of total window
    int width, height;
    // List of player hand + discard views
    LoveLetterPlayerView[] playerHands;
    // Draw pile view
    LoveLetterDeckView drawPile;
    LoveLetterDeckView reserve;

    // Currently active player
    int activePlayer = -1;

    int highlightPlayerIdx = 0;

    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(220, 27, 67), 3);
    Border[] playerViewBorders, playerViewBordersHighlight;

    LoveLetterGameState llgs;
    LoveLetterForwardModel fm;

    public LoveLetterGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        if (game != null) {
            AbstractGameState gameState = game.getGameState();
            fm = (LoveLetterForwardModel) game.getForwardModel();

            if (gameState != null) {
                llgs = (LoveLetterGameState)gameState;
                JTabbedPane pane = new JTabbedPane();
                JPanel main = new JPanel();
                main.setOpaque(false);
                main.setLayout(new BorderLayout());
                JPanel rules = new JPanel();
                pane.add("Main", main);
                pane.add("Rules", rules);
                JLabel ruleText = new JLabel(getRuleText());
                rules.add(ruleText);

                // Initialise active player
                activePlayer = gameState.getCurrentPlayer();

                // Find required size of window
                int nPlayers = gameState.getNPlayers();
                int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
                double nVertAreas = 4;
                this.width = playerAreaWidth * nHorizAreas;
                this.height = (int) (playerAreaHeight * nVertAreas);
                ruleText.setPreferredSize(new Dimension(width*2/3+60, height*2/3+100));

                parent.setBackground(ImageIO.GetInstance().getImage("data/loveletter/bg.png"));

                LoveLetterGameState llgs = (LoveLetterGameState) gameState;
                LoveLetterParameters llp = (LoveLetterParameters) gameState.getGameParameters();

                // Create main game area that will hold all game views
                playerHands = new LoveLetterPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                playerViewBordersHighlight = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setLayout(new BorderLayout());
                mainGameArea.setOpaque(false);

                // Player hands go on the edges
                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
                int next = 0;
                for (int i = 0; i < nPlayers; i++) {
                    LoveLetterPlayerView playerHand = new LoveLetterPlayerView(llgs.getPlayerHandCards().get(i),
                            llgs.getPlayerDiscardCards().get(i), i, humanID, llp.getDataPath());

                    // Get agent name
                    String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                    String agentName = split[split.length - 1];

                    // Create border, layouts and keep track of this view
                    TitledBorder title = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                            TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    playerViewBorders[i] = title;
                    playerViewBordersHighlight[i] = BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i]);
                    playerHand.setBorder(title);

                    sides[next].setOpaque(false);
                    sides[next].add(playerHand);
                    sides[next].setLayout(new GridBagLayout());
                    next = (next + 1) % (locations.length);
                    playerHands[i] = playerHand;
                    int p = i;
                    playerHands[i].addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            highlightPlayerIdx = p;
                        }
                    });
                }

                // Add GUI listener
                game.addListener(new LLGUIListener(fm, parent, playerHands));

                if (gameState.getNPlayers() == 2) {
                    // Add reserve
                    JLabel label = new JLabel("Reserve cards:");
                    reserve = new LoveLetterDeckView(-1, llgs.getReserveCards(), true, llp.getDataPath(),
                            new Rectangle(0, 0, playerAreaWidth, llCardHeight));
                    JPanel wrap = new JPanel();
                    wrap.setOpaque(false);
                    wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
                    wrap.add(label);
                    wrap.add(reserve);
                    sides[next].setOpaque(false);
                    sides[next].add(wrap);
                    sides[next].setLayout(new GridBagLayout());
                }
                for (int i = 0; i < locations.length; i++) {
                    mainGameArea.add(sides[i], locations[i]);
                }

                // Discard and draw piles go in the center
                JPanel centerArea = new JPanel();
                centerArea.setOpaque(false);
                centerArea.setLayout(new BoxLayout(centerArea, BoxLayout.Y_AXIS));
                drawPile = new LoveLetterDeckView(-1, llgs.getDrawPile(), gameState.getCoreGameParameters().alwaysDisplayFullObservable, llp.getDataPath(),
                        new Rectangle(0, 0, playerAreaWidth, llCardHeight));
                centerArea.add(new JLabel("Draw pile:"));
                centerArea.add(drawPile);
                JPanel jp = new JPanel();
                jp.setOpaque(false);
                jp.setLayout(new GridBagLayout());
                jp.add(centerArea);
                mainGameArea.add(jp, BorderLayout.CENTER);

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Love Letter", gameState, width, defaultInfoPanelHeight);
                infoPanel.setOpaque(false);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false);
                actionPanel.setOpaque(false);

                main.add(infoPanel, BorderLayout.NORTH);
                main.add(mainGameArea, BorderLayout.CENTER);
                main.add(actionPanel, BorderLayout.SOUTH);

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
        return 50;
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
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);

        gameInfo.setPreferredSize(new Dimension(width/2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        historyInfo.setOpaque(false);
        historyInfo.setPreferredSize(new Dimension(width/2 - 10, height));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setOpaque(false);
//        historyContainer.getViewport().setOpaque(false);
        historyContainer.getViewport().setBackground(new Color(229, 218, 209, 255));
        historyContainer.setPreferredSize(new Dimension(width/2 - 25, height));
        wrapper.add(historyContainer);
        return wrapper;
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
        pane.getViewport().setBackground(new Color(229, 218, 209, 255));
        pane.setPreferredSize(new Dimension(width, height));
        pane.getVerticalScrollBar().setUnitIncrement(16);
        pane.getHorizontalScrollBar().setUnitIncrement(16);
        if (boxLayout) {
            pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }
        return pane;
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
//            resetActionButtons();

            activePlayer = gameState.getCurrentPlayer();
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            int highlight = playerHands[activePlayer].handCards.getCardHighlight();
            Deck<LoveLetterCard> deck = ((LoveLetterGameState)gameState).getPlayerHandCards().get(activePlayer);
            if (deck.getSize() > 0) {
                if (highlight == -1 || highlight >= deck.getSize()) {
                    highlight = 0;
                    playerHands[activePlayer].handCards.setCardHighlight(highlight);
                }
                LoveLetterCard hCard = deck.get(highlight);

                int k = 0;
                for (AbstractAction action : actions) {
                    if (action instanceof PlayCard) {
                        PlayCard pc = (PlayCard) action;
                        if (pc.getTargetPlayer() == -1 || pc.getTargetPlayer() == highlightPlayerIdx) {
                            actionButtons[k].setVisible(true);
                            actionButtons[k].setButtonAction(action, action.getString(gameState));
                            k++;
                        }
                    }
                }
                for (int i = k; i < actionButtons.length; i++) {
                    actionButtons[i].setVisible(false);
                    actionButtons[i].setButtonAction(null, "");
                }
            } else {
                for (int i = 0; i < actions.size(); i++) {
                    actionButtons[i].setVisible(true);
                    actionButtons[i].setButtonAction(actions.get(i), gameState);
                }
                for (int i = actions.size(); i < actionButtons.length; i++) {
                    actionButtons[i].setVisible(false);
                    actionButtons[i].setButtonAction(null, "");
                }
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {

            // Update active player highlight
            if (gameState.getCurrentPlayer() != activePlayer) {
                playerHands[activePlayer].handCards.setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }

            // Update decks and visibility
            llgs = (LoveLetterGameState)gameState.copy();
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                boolean front = i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer
                        || humanPlayerIds.contains(i)
                        || gameState.getCoreGameParameters().alwaysDisplayFullObservable;
                playerHands[i].update(llgs, front);

                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    playerHands[i].setBorder(playerViewBordersHighlight[i]);
                } else {
                    playerHands[i].setBorder(playerViewBorders[i]);
                }
            }
            if (reserve != null)
                reserve.updateComponent(llgs.getReserveCards());
            drawPile.updateComponent(llgs.getDrawPile());
            drawPile.setFront(gameState.getCoreGameParameters().alwaysDisplayFullObservable);

        }
    }

    private String getRuleText() {
        String rules = "<html><center><h1>Love Letter</h1></center><br/><hr><br/>";
        rules += "<p>You try to earn the favour of the princess and get your love letter delivered to her. The closer you are (the higher your card number) at the end, the better. The closest player, or the only one left in the game, is the winner of the round. Win most rounds to win the game.</p><br/>";
        rules += "<p>On your turn, you draw a card to have 2 in hand, and then play one of the cards, discarding it and executing its effect.</p>";
        rules += "<p><b>Types of cards</b>: " +
                "<ul><li>Guard (1; x5): guess another player's card; if correct, that player has to discard their card and is eliminated.</li>" +
                "<li>Priest (2; x2): see another player's card.</li>" +
                "<li>Baron (3; x2): compare cards with another player; the player with the lower card is eliminated.</li>" +
                "<li>Handmaid (4; x2): the player is protected for 1 round and cannot be targeted by others' actions.</li>" +
                "<li>Prince (5; x2): choose a player to discard their card and draw another (can be yourself).</li>" +
                "<li>King (6; x1): choose a player to swap cards with.</li>" +
                "<li>Countess (7; x1): must be discarded if the other card in hand is a King or a Prince.</li>" +
                "<li>Princess (8; x1): player is eliminated if they discard this card.</li>" +
                "</ul></p><br/>";
        rules += "<hr><p><b>INTERFACE: </b> Find actions available at any time at the bottom of the screen. Each player has 2 components in their area: their hand (hidden; left) and their cards played/discarded (right). Click on cards in a deck to see them better / select them to see actions associated. Click on player areas (e.g. player names) to see actions targetting them.</p>";
        rules += "</html>";
        return rules;
    }
}
