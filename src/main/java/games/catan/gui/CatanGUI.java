package games.catan.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.catan.CatanGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class CatanGUI extends AbstractGUIManager {
    CatanGameState gs;
    CatanBoardView boardView;
    PlayerPanel[] playerPanels;

    JPanel gameInfo;
    JLabel scoreLabel;
    JLabel victoryPointsLabel;
    JLabel diceRollLabel;
    JLabel knightCount;
    JLabel longestRoad;
    JLabel playerResources;
    JLabel devCards;
    JLabel playerColourLabel;

    public CatanGUI(GamePanel parent, Game game, ActionController ac, int humanId) {
        super(parent, game, ac, humanId);
        if (game == null) return;
        this.gs = (CatanGameState) game.getGameState();

        boardView = new CatanBoardView(gs);

        // Bottom area will show actions available
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], 400, defaultActionPanelHeight, false);

        parent.setLayout(new BorderLayout());
        parent.setBackground(Color.white);
        parent.add(createGameStateInfoPanel(gs), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        // each player have their own panel
        playerPanels = new PlayerPanel[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            playerPanels[i] = new PlayerPanel(i, game.getPlayers().get(i).toString());
        }

        mainPanel.add(Box.createRigidArea(new Dimension(5,0)));

        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane0 = new JScrollPane(playerPanels[0]);
        scrollPane0.setPreferredSize(new Dimension(400,500));
        leftPanel.add(scrollPane0);
        JScrollPane scrollPane1 = new JScrollPane(playerPanels[1]);
        scrollPane1.setPreferredSize(new Dimension(400,500));
        leftPanel.add(scrollPane1);
        mainPanel.add(leftPanel);

        mainPanel.add(boardView);
        mainPanel.add(Box.createRigidArea(new Dimension(5,0)));

        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane2 = new JScrollPane(playerPanels[2]);
        scrollPane2.setPreferredSize(new Dimension(400,500));
        rightPanel.add(scrollPane2);
        if (playerPanels.length > 3) {
            JScrollPane scrollPane3 = new JScrollPane(playerPanels[3]);
            scrollPane3.setPreferredSize(new Dimension(400,500));
            rightPanel.add(scrollPane3);
        }
        mainPanel.add(rightPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(5,0)));

        parent.add(mainPanel, BorderLayout.CENTER);
        parent.add(actionPanel, BorderLayout.SOUTH);

        parent.revalidate();
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        return 25;
    }
//
//    public CatanGUI(Game game, ActionController ac, GamePanel gp) {
//        super(gp, ac, 25);
//        gs = (CatanGameState) game.getGameState();
//        board = gs.getBoard();
//        gp.setPreferredSize(new Dimension(1000, 600));
//
//        boardView = new CatanBoardView(gs, 500, 500);
//
//        // Bottom area will show actions available
//        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], 400, defaultActionPanelHeight, false);
//
//        gp.setLayout(new FlowLayout());
//        gp.add(createGameStateInfoPanel(gs), new FlowLayout(FlowLayout.LEADING));
//
//        // each player have their own panel
//        playerPanels = new PlayerPanel[gs.getNPlayers()];
//        for (int i = 0; i < gs.getNPlayers(); i++) {
//            playerPanels[i] = new PlayerPanel(i);
//        }
//
//        JPanel leftPanel = new JPanel();
//        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
//        leftPanel.add(playerPanels[0]);
//        leftPanel.add(playerPanels[1]);
//        gp.add(leftPanel, new FlowLayout(FlowLayout.LEFT));
//
//        gp.add(boardView, new FlowLayout(FlowLayout.CENTER));
//
//        JPanel rightPanel = new JPanel();
//        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
//        rightPanel.add(playerPanels[2]);
//        rightPanel.add(playerPanels[3]);
//        gp.add(rightPanel, new FlowLayout(FlowLayout.RIGHT));
//
//        gp.add(actionPanel, new FlowLayout(FlowLayout.TRAILING));
//
//        gp.revalidate();
//        gp.repaint();
//
//    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        scoreLabel.setText("Score: " + Arrays.toString(gs.getScores()));
        diceRollLabel.setText("Dice Roll: " + ((CatanGameState) gameState).getRollValue());
        knightCount.setText("Knights: " + Arrays.toString(gs.getKnights()));
        longestRoad.setText("Longest Road: " + gs.getLongestRoadOwner() + " with length " + gs.getLongestRoadLength());
        victoryPointsLabel.setText("VictoryPoints: " + Arrays.toString(gs.getVictoryPoints()));

//        playerResources.setText("<html>Resources: ");
//        playerResources.setText(playerResources.getText() + "<br/>K : [B, L, O, G, W]");
//        for (int i = 0 ; i < gameState.getNPlayers(); i++){
//            playerResources.setText(playerResources.getText() + "<br/>" + i + " : " + Arrays.toString(gs.getPlayerResources(i)));
//        }
//        playerResources.setText(playerResources.getText() + "</html>");
//
//        devCards.setText("<html>Dev. Cards: ");
        for (int i = 0; i < gameState.getNPlayers(); i++) {
//            devCards.setText(devCards.getText() + "<br/>" + i + " : " + Arrays.toString(gs.getPLayerDevCards(i)));
            playerPanels[i]._update((CatanGameState) gameState);
        }
//        devCards.setText(devCards.getText() + "</html>");

        parent.repaint();
    }


    protected JPanel createGameStateInfoPanel(AbstractGameState gameState) {
        gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>Catan</h1></html>"));

        updateGameStateInfo(gameState);

        playerColourLabel = new JLabel("Current Player Colour: Red");
        knightCount = new JLabel("Knights: " + Arrays.toString(gs.getKnights()));
        longestRoad = new JLabel("Longest Road: " + gs.getLongestRoadOwner() + " with length " + gs.getLongestRoadLength());
        victoryPointsLabel = new JLabel("VictoryPoints: " + Arrays.toString(gs.getVictoryPoints()));
        scoreLabel = new JLabel("Scores: " + Arrays.toString(gs.getScores()));
        diceRollLabel = new JLabel("Dice Roll: " + ((CatanGameState) gameState).getRollValue());

        playerResources = new JLabel("<html>Resources: ");
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            playerResources.setText(playerResources.getText() + "<br/>" + i + " : " + gs.getPlayerResources(i).toString());
        }
        playerResources.setText(playerResources.getText() + "</html>");

        devCards = new JLabel("<html>Dev. Cards: ");
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            devCards.setText(devCards.getText() + "<br/>" + i + " : " + gs.getPlayerDevCards(i).toString());
        }
        devCards.setText(devCards.getText() + "</html>");

        gameInfo.add(gameStatus);
//        gameInfo.add(playerStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(diceRollLabel);

        gameInfo.setPreferredSize(new Dimension(900, 150));

        JPanel wrapper = new JPanel();
        wrapper.add(gameInfo, BorderLayout.WEST);
//        wrapper.setLayout(new GridBagLayout());
        return wrapper;
    }

}

