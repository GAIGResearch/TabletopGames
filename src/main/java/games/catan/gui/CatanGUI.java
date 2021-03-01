package games.catan.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.catan.CatanGameState;
import games.catan.CatanTile;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;

public class CatanGUI extends AbstractGUI {
    CatanGameState gs;
    CatanTile[][] board;
    CatanBoardView boardView;

    JPanel gameInfo;
    JLabel scoreLabel;
    JLabel victoryPointsLabel;
    JLabel diceRollLabel;
    JLabel knigthCount;
    JLabel longestRoad;
    JLabel playerResources;
    JLabel devCards;

    public CatanGUI(Game game, ActionController ac) {
        super(ac, 25);
        gs = (CatanGameState)game.getGameState();
        board = gs.getBoard();
        setSize(500, 500);

        boardView = new CatanBoardView(gs);

        // Bottom area will show actions available
        JComponent actionPanel = createActionPanel(new Collection[0], 400, defaultActionPanelHeight, false);

        // Add all views to frame
//        JPanel buttons = new JPanel();
//        JButton button1 = new JButton("button1");
//        buttons.add(button1);
        getContentPane().add(boardView, BorderLayout.CENTER);
        getContentPane().add(createGameStateInfoPanel(gs), BorderLayout.EAST);
//        getContentPane().add(buttons, BorderLayout.SOUTH);

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        scoreLabel.setText("Scores: " + Arrays.toString(gs.getScores()));
        diceRollLabel.setText("Dice Roll: " + ((CatanGameState)gameState).getRollValue());
        knigthCount.setText("Knights: " + Arrays.toString(gs.getKnights()));
        longestRoad.setText("Longest Road: " + gs.getLongestRoadOwner() + " with length " + gs.getLongestRoadLength());
        victoryPointsLabel.setText("VictoryPoints: " + Arrays.toString(gs.getVictoryPoints()));

        playerResources.setText("<html>Resources: ");
        for (int i = 0 ; i < gameState.getNPlayers(); i++){
            playerResources.setText(playerResources.getText() + "<br/>" + i + " : " + Arrays.toString(gs.getPlayerResources(i)));
        }
        playerResources.setText(playerResources.getText() + "</html>");

        devCards.setText("<html>Dev. Cards: ");
        for (int i = 0 ; i < gameState.getNPlayers(); i++){
            devCards.setText(devCards.getText() + "<br/>" + i + " : " + Arrays.toString(gs.getPLayerDevCards(i)));
        }
        devCards.setText(devCards.getText() + "</html>");

        repaint();
    }

    protected JPanel createGameStateInfoPanel(AbstractGameState gameState) {
        System.out.println("info panel");

        gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>Catan</h1></html>"));

        updateGameStateInfo(gameState);

        knigthCount = new JLabel("Knights: " + Arrays.toString(gs.getKnights()));
        longestRoad = new JLabel("Longest Road: " + gs.getLongestRoadOwner() + " with length " + gs.getLongestRoadLength());
        victoryPointsLabel = new JLabel("VictoryPoints: " + Arrays.toString(gs.getVictoryPoints()));
        scoreLabel = new JLabel("Scores: " + Arrays.toString(gs.getScores()));
        diceRollLabel = new JLabel("Dice Roll: " + ((CatanGameState)gameState).getRollValue());

        playerResources = new JLabel("<html>Resources: ");
        for (int i = 0 ; i < gameState.getNPlayers(); i++){
            playerResources.setText(playerResources.getText() + "<br/>" + i + " : " + Arrays.toString(gs.getPlayerResources(i)));
        }
        playerResources.setText(playerResources.getText() + "</html>");

        devCards = new JLabel("<html>Dev. Cards: ");
        for (int i = 0 ; i < gameState.getNPlayers(); i++){
            devCards.setText(devCards.getText() + "<br/>" + i + " : " + Arrays.toString(gs.getPLayerDevCards(i)));
        }
        devCards.setText(devCards.getText() + "</html>");

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turnOwner);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(knigthCount);
        gameInfo.add(longestRoad);
        gameInfo.add(victoryPointsLabel);
        gameInfo.add(scoreLabel);
        gameInfo.add(diceRollLabel);
        gameInfo.add(playerResources);
        gameInfo.add(devCards);

        gameInfo.setPreferredSize(new Dimension(300, 600));

        JPanel wrapper = new JPanel();
        wrapper.add(gameInfo);
        wrapper.setLayout(new GridBagLayout());
        return wrapper;
    }
}
