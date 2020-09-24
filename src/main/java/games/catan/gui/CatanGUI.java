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
import java.util.Collection;

public class CatanGUI extends AbstractGUI {
    CatanGameState gs;
    CatanTile[][] board;
    CatanBoardView boardView;

    public CatanGUI(Game game, ActionController ac) {
        super(ac, 25);
        gs = (CatanGameState)game.getGameState();
        board = gs.getBoard();
        setSize(500, 500);

        boardView = new CatanBoardView(gs);

//        JPanel mainGameArea = new JPanel();
//        mainGameArea.setPreferredSize(new Dimension(500, 500));
//        mainGameArea.add(boardView);

        // Bottom area will show actions available
        JComponent actionPanel = createActionPanel(new Collection[0], 400, defaultActionPanelHeight, false);

        // Add all views to frame
        JPanel buttons = new JPanel();
        JButton button1 = new JButton("button1");
        buttons.add(button1);
        getContentPane().add(boardView, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
//        getContentPane().add(boardView, BorderLayout.CENTER);

        setFrameProperties();
        System.out.println("End of Catan GUI");
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        repaint();
        System.out.println("repaint");
    }
}
