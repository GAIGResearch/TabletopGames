package games.catan.gui;

import core.AbstractGameState;
import games.catan.CatanBoard;
import games.catan.CatanGameState;

import javax.swing.*;
import java.awt.*;

public class CatanBoardView extends JComponent {
    CatanBoard board;
    CatanGameState gs;

    public CatanBoardView(CatanGameState gs){
        this.gs = gs;

    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
    }

    private void drawBoard(Graphics2D g) {
        board = gs.getBoard();
    }
}
