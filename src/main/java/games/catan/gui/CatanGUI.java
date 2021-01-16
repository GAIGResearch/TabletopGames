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
    JLabel diceRollLabel;

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
        repaint();
    }

    protected JPanel createGameStateInfoPanel(AbstractGameState gameState) {
        System.out.println("info panel");

        gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>Catan</h1></html>"));

        updateGameStateInfo(gameState);

        scoreLabel = new JLabel("Scores: " + Arrays.toString(gs.getScores()));
        diceRollLabel = new JLabel("Dice Roll: " + ((CatanGameState)gameState).getRollValue());

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turnOwner);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(scoreLabel);
        gameInfo.add(diceRollLabel);

        gameInfo.setPreferredSize(new Dimension(300, defaultInfoPanelHeight+5));

        JPanel wrapper = new JPanel();
        wrapper.add(gameInfo);
        wrapper.setLayout(new GridBagLayout());
        return wrapper;
    }
}
