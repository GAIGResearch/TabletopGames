package games.hearts.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.hearts.HeartsGameState;
import games.hearts.HeartsParameters;
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
import java.util.Arrays;
import java.util.Set;

public class HeartsGUIManager extends AbstractGUIManager {
    final static int playerWidth = 300;
    final static int playerHeight = 130;
    final static int cardWidth = 90;
    final static int cardHeight = 115;

    private String lastHistoryEntry = null;

    int width, height;
    HeartsPlayerView[] playerHands;
    HeartsPlayerTrickView[] playerTricks;


    private HeartsGameState gameState;

    int activePlayer = -1;

    Border highlightActive = BorderFactory.createLineBorder(new Color(47,132,220), 3);
    Border[] playerViewBorders;

    public HeartsGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
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
                this.height = (int) (playerHeight * nVertAreas * 2); // double the height
                parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight * 2 + 20));
                ruleText.setPreferredSize(new Dimension(width*2/3+60, height*2/3+100));

                HeartsGameState hgs = (HeartsGameState) gameState;
                HeartsParameters bjgp = (HeartsParameters) gameState.getGameParameters();

                parent.setBackground(ImageIO.GetInstance().getImage("data/FrenchCards/table-background.jpg"));

                playerHands = new HeartsPlayerView[nPlayers];
                playerTricks = new HeartsPlayerTrickView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setOpaque(false);
                mainGameArea.setLayout(new BorderLayout());


                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[nPlayers];


                int rows = nPlayers / 2 + nPlayers % 2;
                int cols = nPlayers / rows;

                mainGameArea.setLayout(new GridLayout(rows, cols));

                for (int i = 0; i < nPlayers; i++) {
                    HeartsPlayerView playerHand = new HeartsPlayerView(hgs.getPlayerDecks().get(i), i, bjgp.getDataPath());
                    HeartsPlayerTrickView playerTrick = new HeartsPlayerTrickView(hgs.getPlayerTrickDecks().get(i), i, bjgp.getDataPath());
                    playerHand.setOpaque(false);
                    playerTrick.setOpaque(false);


                    String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                    String agentName = split[split.length - 1];


                    TitledBorder title;

                        title = BorderFactory.createTitledBorder(
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                                TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);

                    playerViewBorders[i] = title;
                    playerHand.setBorder(title);
                    playerTrick.setBorder(title);

                    sides[i] = new JPanel();
                    sides[i].add(playerHand);
                    sides[i].add(playerTrick);
                    sides[i].setLayout(new GridLayout(1,2));
                    sides[i].setOpaque(false);
                    playerHands[i] = playerHand;
                    playerTricks[i] = playerTrick;

                    mainGameArea.add(sides[i]);
                }


                JPanel infoPanel = createGameStateInfoPanel("Hearts", gameState, width, defaultInfoPanelHeight);

                JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false);


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


    @Override
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

        if (gameState == null) {
            System.out.println("gameState is null");
        } else {


            playerHands[gameState.getCurrentPlayer()].setFront(true);
            playerTricks[gameState.getCurrentPlayer()].setFront(true);
            playerStatus.setText(Arrays.toString(gameState.getPlayerResults()));



            if (gameState.getCurrentPlayer() != activePlayer) {
                playerHands[activePlayer].setCardHighlight(-1);
                playerHands[activePlayer].setFront(false);
                activePlayer = gameState.getCurrentPlayer();

            }


            HeartsGameState hgs = (HeartsGameState) gameState;
            if (hgs.isNotTerminal()) {
                this.gameState = hgs;
            }


            for(int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update(this.gameState);


                if (i == gameState.getCurrentPlayer()) {
                    Border compound = BorderFactory.createCompoundBorder(
                            highlightActive, playerViewBorders[i]);
                    playerHands[i].setBorder(compound);
                } else {
                    playerHands[i].setBorder(playerViewBorders[i]);
                }
            }






            String newHistory = String.join("\n", gameState.getHistoryAsText());
            if (!newHistory.equals(lastHistoryEntry)) {
                historyInfo.setText(newHistory);
                lastHistoryEntry = newHistory;
            }


            historyInfo.setCaretPosition(historyInfo.getDocument().getLength());


        }
    }

    private String getRuleText() {
        String rules = "<html><center><h1>Hearts</h1></center><br/><hr><br/>";
        rules = "<html><p>Hearts is a trick taking game where the objective is to avoid scoring points. The game is played over several rounds, and the player with the fewest points at the end of the game wins.</p>" +
                "<ul><li>Each round starts with players passing three cards to another player. The direction of passing alternates each round. In the first round, players pass to the left. In the second round, they pass to the right. In the third round, they pass across. There is no passing in the fourth round, and then the cycle repeats.</li>" +
                "<li>After the pass, play starts with the player holding the 2 of clubs leading the trick by playing it. Each player, in turn, must follow suit if possible. If a player does not have any cards of the leading suit, they can play any other card. The player who played the highest value card of the leading suit wins the trick and leads the next one.</li>" +
                "<li>The player cannot play a Heart or the Queen of Spades in the first trick, and cannot play them in other tricks unless they have been 'broken', i.e., played in a previous trick. Hearts are broken with the first Heart played in the game.</li>" +
                "<li>Each Heart card in a trick scores 1 point, and the Queen of Spades scores 13. However, if a player manages to take all scoring cards in a round (a move known as 'shooting the moon'), they score 0 points and each other player scores 26 points.</li>" +
                "<li>The game ends when a player reaches or exceeds 50 points at the end of a round, and the player with the fewest points is the winner.</li></ul>" +
                "<hr><p><b>INTERFACE: </b> Choose a card to play from your hand at the bottom of the screen.</p>";
        rules += "</html>";


        return rules;
    }
}