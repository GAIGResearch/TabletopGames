package games.sushigo.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Set;

public class SGGUIManager extends AbstractGUIManager {
    // Settings for display areas
    final static int playerAreaWidth = 350;
    final static int playerAreaHeight = 100;
    final static int SGCardWidth = 60;
    final static int SGCardHeight = 85;

    // List of player hand views
    SGPlayerView[] playerHands;

    // Currently active player
    int activePlayer = -1;

    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);
    Border highlightWin = BorderFactory.createLineBorder(new Color(31, 190, 58), 3);
    Border highlightLose = BorderFactory.createLineBorder(new Color(220, 73, 47), 3);
    Border[] playerViewBorders;
    Border[] playerViewWinBorders;
    Border[] playerViewLoseBorders;

    public SGGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);

        if (game != null) {
            AbstractGameState gameState = game.getGameState();
            if (gameState != null) {
                //Initialise active player
                activePlayer = gameState.getCurrentPlayer();

                // Find required size of window. nxn grid of player areas
                int nPlayers = gameState.getNPlayers();
                int perColumn = (int)Math.ceil(Math.sqrt(nPlayers));
                int nVertAreas = (int)Math.ceil((double)nPlayers / perColumn);
                this.width = playerAreaWidth * perColumn;
                this.height = playerAreaHeight * nVertAreas + defaultInfoPanelHeight;
                if (!humanID.isEmpty()) {
                    this.height += defaultActionPanelHeight;
                }

                SGGameState parsedGameState = (SGGameState) gameState;
                SGParameters parameters = (SGParameters) gameState.getGameParameters();

                // Create main game area that will hold all game views
                playerHands = new SGPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                playerViewWinBorders = new Border[nPlayers];
                playerViewLoseBorders = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setOpaque(false);
                mainGameArea.setLayout(new GridLayout(nVertAreas, perColumn));
                mainGameArea.setPreferredSize(new Dimension(playerAreaWidth * perColumn, playerAreaHeight * nVertAreas));

                // Player hands go on the edges
                for (int i = 0; i < nPlayers; i++) {
                    SGPlayerView playerHand = new SGPlayerView(parsedGameState.getPlayerHands().get(i), parsedGameState.getPlayedCards().get(i), i, humanID, parameters.getDataPath());
                    playerHand.setOpaque(false);
                    playerHand.setPreferredSize(new Dimension(playerAreaWidth, playerAreaHeight));

                    // Get agent name
                    String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                    String agentName = split[split.length - 1];

                    // Create border, layouts and keep track of this view
                    TitledBorder title = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                            TitledBorder.CENTER, TitledBorder.ABOVE_TOP);
                    TitledBorder titleWin = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "] - WIN",
                            TitledBorder.CENTER, TitledBorder.ABOVE_TOP);
                    TitledBorder titleLose = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "] - LOSE",
                            TitledBorder.CENTER, TitledBorder.ABOVE_TOP);
                    playerViewBorders[i] = title;
                    playerViewWinBorders[i] = titleWin;
                    playerViewLoseBorders[i] = titleLose;
                    playerHand.setBorder(title);

                    mainGameArea.add(playerHand);
                    playerHands[i] = playerHand;
                }

                // Add all views to frame
                parent.setLayout(new BorderLayout());

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Sushi Go!", gameState, width, defaultInfoPanelHeight);
                parent.add(infoPanel, BorderLayout.NORTH);

                // Bottom area will show actions available
                if (!humanID.isEmpty()) {
                    JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, false, null, null, null);
                    parent.add(actionPanel, BorderLayout.SOUTH);
                }

                // Center view will show game
                parent.add(mainGameArea, BorderLayout.CENTER);

                parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 20));
            }

        }
//        parent.setBackground(ImageIO.GetInstance().getImage("data/loveletter/bg.png"));
        parent.setBgColor(Color.white);
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
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

        gameInfo.setPreferredSize(new Dimension(width / 2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        createActionHistoryPanel(width / 2 - 10, height, humanPlayerIds);
        wrapper.add(historyContainer);
        return wrapper;
    }

    @Override
    public int getMaxActionSpace() {
        return 15;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (gameState.getCurrentPlayer() != activePlayer) {
                playerHands[activePlayer].playerHandView.setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }

            if (gameState.isNotTerminal()) {

                // Update decks and visibility
                SGGameState parsedGameState = (SGGameState) gameState;
                for (int i = 0; i < gameState.getNPlayers(); i++) {
                    playerHands[i].update(parsedGameState);
                    if (i == gameState.getCurrentPlayer()
                            || humanPlayerIds.contains(i)) {
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
            } else {
                // Highlight winner
                Set<Integer> winner = gameState.getWinners();
                for (int i = 0; i < gameState.getNPlayers(); i++) {
                    if (winner.contains(i)) {
                        Border compound = BorderFactory.createCompoundBorder(
                                highlightWin, playerViewWinBorders[i]);
                        playerHands[i].setBorder(compound);
                    } else {
                        Border compound = BorderFactory.createCompoundBorder(
                                highlightLose, playerViewLoseBorders[i]);
                        playerHands[i].setBorder(compound);
                    }
                }
            }

        }
    }
}
