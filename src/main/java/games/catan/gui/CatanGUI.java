package games.catan.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.catan.CatanBoard;
import games.catan.CatanGameState;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;

public class CatanGUI extends AbstractGUI {
    CatanGameState gs;
    CatanBoard board;
    CatanBoardView boardView;

    public CatanGUI(Game game, ActionController ac) {
        super(ac, 25);
        gs = (CatanGameState)game.getGameState();
        board = gs.getBoard();

        boardView = new CatanBoardView(gs);

        getContentPane().add(boardView, BorderLayout.CENTER);

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {

    }
}
