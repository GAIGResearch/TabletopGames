package games.root.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.root.RootGameState;
import games.root.RootParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Iterator;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public class RootGUIManager extends AbstractGUIManager {
    final static int playerAreaWidth = 400;
    final static int playerAreaHeight = 350;

    final static int cardWidth = 70;//55;
    final static int cardHeight = 95;//75;
    int activePlayer = -1;
    int humanId;
    private MapPanel mapPanel;

    Border[] playerViewBorders;
    MarquisePlayerView catPlayerView;
    EyriePlayerView eyriePlayerView;
    WoodlandPlayerView woodlandPlayerView;
    VagabondPlayerView vagabondPlayerView;
    JPanel infoPanel;
    JLabel victoryConditions;
    CommonView commonView;

    public RootGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;

        if (game.getGameState() != null) {
            AbstractGameState gameState = game.getGameState();
            activePlayer = gameState.getCurrentPlayer();

            int nPlayers = gameState.getNPlayers();

            this.width = 1600;
            this.height = 1100;

            RootGameState state = (RootGameState) gameState;
            RootParameters parameters = (RootParameters) gameState.getGameParameters();


            playerViewBorders = new Border[nPlayers];
            JPanel mainGameArea = new JPanel();
            mainGameArea.setLayout(new GridLayout(2, 2));

            JPanel[] sides = new JPanel[]{new JPanel(), new JPanel(), new JPanel(), new JPanel()};
            for (int i = 0; i < nPlayers; i++) {
                if (!humanPlayerIds.isEmpty() && humanPlayerIds.contains(i)) {
                    humanId = i;
                } else {
                    Iterator<Integer> iterator = humanPlayerIds.iterator();
                    if (iterator.hasNext()) {
                        humanId = iterator.next();
                    }
                }
                switch (state.getPlayerFaction(i)) {
                    case MarquiseDeCat:
                        catPlayerView = new MarquisePlayerView(i, humanId, parameters.getDataPath(), state);
                        String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                        String agentName = split[split.length - 1];

                        // Create border, layouts and keep track of this view
                        TitledBorder title = BorderFactory.createTitledBorder(
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                                TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                        playerViewBorders[i] = title;
                        catPlayerView.setBorder(title);
                        sides[i].add(catPlayerView);
                        break;
                    case WoodlandAlliance:
                        woodlandPlayerView = new WoodlandPlayerView(i, humanId, parameters.getDataPath(), state);
                        String[] split1 = game.getPlayers().get(i).getClass().toString().split("\\.");
                        String agentName1 = split1[split1.length - 1];

                        // Create border, layouts and keep track of this view
                        TitledBorder title1 = BorderFactory.createTitledBorder(
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName1 + "]",
                                TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                        playerViewBorders[i] = title1;
                        woodlandPlayerView.setBorder(title1);
                        sides[i].add(woodlandPlayerView);
                        break;
                    case EyrieDynasties:
                        eyriePlayerView = new EyriePlayerView(i, humanId, parameters.dataPath, state);
                        String[] split2 = game.getPlayers().get(i).getClass().toString().split("\\.");
                        String agentName2 = split2[split2.length - 1];

                        // Create border, layouts and keep track of this view
                        TitledBorder title2 = BorderFactory.createTitledBorder(
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName2 + "]",
                                TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                        playerViewBorders[i] = title2;
                        eyriePlayerView.setBorder(title2);
                        sides[i].add(eyriePlayerView);
                        break;
                    case Vagabond:
                        vagabondPlayerView = new VagabondPlayerView(i, humanId, parameters.getDataPath(), state);
                        String[] split3 = game.getPlayers().get(i).getClass().toString().split("\\.");
                        String agentName3 = split3[split3.length - 1];

                        // Create border, layouts and keep track of this view
                        TitledBorder title3 = BorderFactory.createTitledBorder(
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName3 + "]",
                                TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                        playerViewBorders[i] = title3;
                        vagabondPlayerView.setBorder(title3);
                        sides[i].add(vagabondPlayerView);
                        ;
                        break;

                }
                sides[i].setPreferredSize(new Dimension(playerAreaWidth, playerAreaHeight));

            }
            for (int e =0; e<4;e++){
                mainGameArea.add(sides[e]);
            }
            commonView = new CommonView(0,0,parameters.getDataPath(), state);

            mapPanel = new MapPanel((RootGameState) game.getGameState());
            mapPanel.setPreferredSize(new Dimension(800, 700));
            infoPanel = createGameStateInfoPanel("Root", game.getGameState(), 800, 200);
            JPanel TopPanel = new JPanel();
            TopPanel.setLayout(new GridLayout(1, 2));
            TopPanel.add(infoPanel);
            TopPanel.add(commonView);
            JPanel CenterPanel = new JPanel();
            CenterPanel.setLayout(new GridLayout(1, 2));
            CenterPanel.add(mapPanel);
            CenterPanel.add(mainGameArea);
            parent.setLayout(new BorderLayout());
            parent.add(TopPanel, BorderLayout.NORTH);
            parent.add(CenterPanel, BorderLayout.CENTER);
            Component actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, true, null, null, null);
            parent.add(actionPanel, BorderLayout.SOUTH);
            parent.setBgColor(Color.white);
            parent.revalidate();
            parent.setVisible(true);
            parent.repaint();

        }

    }

    /**
     * Defines how many action button objects will be created and cached for usage if needed. Less is better, but
     * should not be smaller than the number of actions available to players in any game state.
     *
     * @return maximum size of the action space (maximum actions available to a player for any decision point in the game)
     */
    @Override
    public int getMaxActionSpace() {
        // Estimate the maximum number of actions
        return 500;
    }

    /**
     * Updates all GUI elements given current game state and player that is currently acting.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        // Update the GUI elements based on the current game state
        if (gameState != null) {
            mapPanel.repaint();
            if (gameState.getCurrentPlayer() != activePlayer) {
                activePlayer = gameState.getCurrentPlayer();
                if (humanPlayerIds.contains(activePlayer)) {
                    humanId = activePlayer;
                }
                switch (activePlayer) {
                    case 0:
                        catPlayerView.playerHand.setCardHighlight(-1);
                    case 1:
                        eyriePlayerView.playerHand.setCardHighlight(-1);
                    case 2:
                        if (gameState.getNPlayers() > 2) woodlandPlayerView.playerHand.setCardHighlight(-1);
                    case 3:
                        if (gameState.getNPlayers() > 3) vagabondPlayerView.playerHand.setCardHighlight(-1);
                }
            }

            RootGameState state = (RootGameState) gameState;
            commonView.update(state);
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                switch (i) {
                    case 0:
                        catPlayerView.update(state);
                        if (i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer || humanPlayerIds.contains(i) || gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
                            catPlayerView.playerHand.setFront(true);
                            //catPlayerView.playerHand.setFocusable(true);
                        } else {
                            catPlayerView.playerHand.setFront(false);
                        }

                        // Highlight active player
                        if (i == gameState.getCurrentPlayer()) {
                            catPlayerView.setBorder(playerViewBorders[i]);
                        } else {
                            catPlayerView.setBorder(playerViewBorders[i]);
                        }
                        break;
                    case 1:
                        eyriePlayerView.update(state);
                        if (i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer || humanPlayerIds.contains(i) || gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
                            eyriePlayerView.playerHand.setFront(true);
                            //eyriePlayerView.playerHand.setFocusable(true);
                        } else {
                            eyriePlayerView.playerHand.setFront(false);
                        }

                        // Highlight active player
                        if (i == gameState.getCurrentPlayer()) {
                            eyriePlayerView.setBorder(playerViewBorders[i]);
                        } else {
                            eyriePlayerView.setBorder(playerViewBorders[i]);
                        }
                        break;
                    case 2:
                        woodlandPlayerView.update(state);
                        if (i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer || humanPlayerIds.contains(i) || gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
                            woodlandPlayerView.playerHand.setFront(true);
                            woodlandPlayerView.playerHand.setFocusable(true);
                        } else {
                            woodlandPlayerView.playerHand.setFront(false);
                        }

                        // Highlight active player
                        if (i == gameState.getCurrentPlayer()) {
                            woodlandPlayerView.setBorder(playerViewBorders[i]);
                        } else {
                            woodlandPlayerView.setBorder(playerViewBorders[i]);
                        }
                        break;
                    case 3:
                        vagabondPlayerView.update(state);
                        if (i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer || humanPlayerIds.contains(i) || gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
                            vagabondPlayerView.playerHand.setFront(true);
                            vagabondPlayerView.playerHand.setFocusable(true);
                        } else {
                            vagabondPlayerView.playerHand.setFront(false);
                        }

                        // Highlight active player
                        if (i == gameState.getCurrentPlayer()) {
                            vagabondPlayerView.setBorder(playerViewBorders[i]);
                        } else {
                            vagabondPlayerView.setBorder(playerViewBorders[i]);
                        }
                        break;
                }
            }
        }
    }

    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        RootGameState gs = (RootGameState) gameState;
        JPanel gameInfo = new JPanel();
        gameInfo.setOpaque(false);
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        victoryConditions = new JLabel();
        updateGameStateInfo(gameState);

        StringBuilder victoryCon = new StringBuilder("VC: ");
        for (int i = 0; i< gs.getNPlayers(); i++){
            victoryCon.append(gs.getPlayerVictoryCondition(i).toString()).append(",");
        }
        victoryConditions.setText(victoryCon.toString());
        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(victoryConditions);
        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);

        gameInfo.setPreferredSize(new Dimension(width / 3 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        createActionHistoryPanel((width / 3 - 10) * 2, height, humanPlayerIds);
        wrapper.add(historyContainer);
        return wrapper;
    }
    @Override
    protected void updateGameStateInfo(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        StringBuilder victoryCon = new StringBuilder("VC: ");
        for (int i = 0; i< gs.getNPlayers(); i++){
            victoryCon.append(gs.getPlayerVictoryCondition(i).toString()).append(",");
        }
        victoryConditions.setText(victoryCon.toString());
        super.updateGameStateInfo(gameState);
    }
}



