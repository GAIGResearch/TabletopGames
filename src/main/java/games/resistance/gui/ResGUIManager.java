package games.resistance.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.resistance.ResGameState;
import games.resistance.ResParameters;
import games.resistance.components.ResPlayerCards;
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
import java.util.Collections;
import java.util.Set;


/// Code Was Taken and adapted from SushiGo!
public class ResGUIManager extends AbstractGUIManager {
    // Settings for display areas
    final static int playerAreaWidth = 250;
    final static int playerAreaHeight = 130;
    final static int ResPlayerCardsWidth = 60;
    final static int ResPlayerCardsHeight = 85;

    // List of player hand views
    ResPlayerView[] playerHands;

    // Currently active player
    int activePlayer = -1;

    // Border highlight of active player
    Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);
    Border[] playerViewBorders;

    protected JLabel failedVoteCounter = new JLabel("Failed Vote Counter :" + 0);

    protected JLabel missionSuccessCounter = new JLabel("Mission Success Counter :" + 0);

    protected JLabel missionFailCounter = new JLabel("Mission Fail Counter :" + 0);

    public ResGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);
        if (game != null) {
            AbstractGameState gameState = game.getGameState();
            if (gameState != null) {
                //Initialise active player
                activePlayer = gameState.getCurrentPlayer();

                // Find required size of window
                int nPlayers = gameState.getNPlayers();
                int nHorizAreas = 1 + nPlayers;
                double nVertAreas = 3.5;
                this.width = playerAreaWidth * nHorizAreas;
                this.height = (int) (playerAreaHeight * nVertAreas);

                ResGameState parsedGameState = (ResGameState) gameState;
                ResParameters parameters = (ResParameters) gameState.getGameParameters();

                // Create main game area that will hold all game views
                playerHands = new ResPlayerView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();

                mainGameArea.setLayout(new BorderLayout());

                // Player hands go on the edges
                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.SOUTH};
                JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
                int next = 0;
                for (int i = 0; i < nPlayers; i++) {
                    ResPlayerView playerHand = new ResPlayerView(parsedGameState.getPlayerHandCards().get(i), i, humanID.stream().findFirst().orElse(0), parameters.getDataPath());
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


                ResParameters params = (ResParameters) gameState.getGameParameters();
                JPanel centerArea = new JPanel();
                centerArea.setLayout(new BoxLayout(centerArea, BoxLayout.Y_AXIS));
                Image backgroundImage;
                String imageName = params.dataPath + gameState.getNPlayers() + "missions.png";
                backgroundImage = ImageIO.GetInstance().getImage(imageName);

                if (backgroundImage == null) {
                    throw new AssertionError("Problem loading game data from  " + params.dataPath);
                }

                int newWidth = backgroundImage.getWidth(null) / 2; // Replace 2 with the desired scale factor
                int newHeight = backgroundImage.getHeight(null) / 2; // Replace 2 with the desired scale factor
                backgroundImage = backgroundImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                ResBoardView jp = new ResBoardView(backgroundImage);
                jp.setLayout(new GridBagLayout());
                jp.add(centerArea);
                mainGameArea.add(jp, BorderLayout.CENTER);
                mainGameArea.add(jp, BorderLayout.CENTER);

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("The Resistance", gameState, width, defaultInfoPanelHeight);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

                parent.setLayout(new BorderLayout());
                parent.add(mainGameArea, BorderLayout.CENTER);
                parent.add(infoPanel, BorderLayout.NORTH);
                parent.add(actionPanel, BorderLayout.SOUTH);
                parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 20));
            }

        }
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
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


            // Update decks and visibility
            ResGameState parsedGameState = (ResGameState) gameState;

            //missionSuccessText = createGameStateInfoPanel("Size of Mission Team needed : " + parsedGameState.gameBoard.getMissionSuccessValues()[parsedGameState.getRoundCounter()], gameState, width, 100);
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update(parsedGameState);
                if (((ResGameState) gameState).getPlayerHandCards().get(gameState.getCurrentPlayer()).get(2).cardType == ResPlayerCards.CardType.SPY) {
                    playerHands[i].playerHandView.setFront(true);
                    //playerHands[i].setFocusable(true);
                } else {
                    if (i == gameState.getCurrentPlayer()
                            || humanPlayerIds.contains(i)) {
                        playerHands[i].playerHandView.setFront(true);
                        playerHands[i].setFocusable(true);
                    } else {
                        playerHands[i].playerHandView.setFront(false);
                    }
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

        }
    }

    @Override
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setOpaque(false);
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(missionFailCounter);
        gameInfo.add(missionSuccessCounter);
        gameInfo.add(failedVoteCounter);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(gameStatus);

        gameInfo.setPreferredSize(new Dimension(width / 2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        createActionHistoryPanel(width / 2 - 10, height, humanPlayerIds);
        wrapper.add(historyContainer);

//        historyInfo.setPreferredSize(new Dimension(width / 2 - 10, height));
//        historyContainer = new JScrollPane(historyInfo);
//        historyContainer.setPreferredSize(new Dimension(width / 2 - 25, height));
//        wrapper.add(historyContainer);
        historyInfo.setOpaque(false);
        historyContainer.setOpaque(false);
        historyContainer.getViewport().setBackground(new Color(43, 108, 25, 111));
        historyContainer.getViewport().setOpaque(false);
//        historyInfo.setEditable(false);
        return wrapper;
    }

    protected void updateGameStateInfo(AbstractGameState gameState) {
        super.updateGameStateInfo(gameState);
        ResGameState resgs = (ResGameState) gameState;

        missionFailCounter.setText("Mission Fail Counter :" + Collections.frequency(resgs.getGameBoardValues(), false));
        missionSuccessCounter.setText("Mission Success Counter :" + Collections.frequency(resgs.getGameBoardValues(), true));
        failedVoteCounter.setText("Failed Vote Counter :" + resgs.getFailedVoteCounter());
    }

}
