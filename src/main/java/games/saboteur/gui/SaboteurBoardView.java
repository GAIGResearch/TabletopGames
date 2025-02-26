package games.saboteur.gui;

import games.saboteur.SaboteurGameState;
import core.components.PartialObservableGridBoard;
import games.saboteur.components.PathCard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static games.saboteur.gui.SaboteurGUIManager.boardSize;

public class SaboteurBoardView extends JComponent {
    SaboteurGameState gs;
    SaboteurGUIManager gui;
    PartialObservableGridBoard board;
    Dimension size;
    Point panPos;

    public static int cellWidth = 30;
    public static int cellHeight = 50;
    public static int pathSize = 8;
    public static int centerGap = 4;

    public SaboteurBoardView(SaboteurGUIManager gui, SaboteurGameState gs) {
        this.gs = gs;
        this.gui = gui;
        board = gs.getGridBoard();
        size = new Dimension(board.getWidth() * cellWidth, board.getHeight() * cellHeight);
        panPos = new Point(-size.width/2 + boardSize/2, -size.height/2 + boardSize/2);  // Start focused on center

        addMouseListener(new MouseAdapter() {
            Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Middle (wheel) click, pan around
                    start = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && start != null) {
                    // Middle (wheel) click, pan around
                    Point end = e.getPoint();
                    panPos.x += end.x - start.x;
                    panPos.y += end.y - start.y;
                    start = null;
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int i = 0; i < board.getHeight(); i++) {
            for (int j = 0; j < board.getWidth(); j++) {
                PathCard card = (PathCard) board.getElement(j, i);
                if (card != null) drawPathCard((Graphics2D) g, card, panPos.x + j * cellWidth, panPos.y + i * cellHeight);
                if (gui.gridHighlight != null && gui.gridHighlight.x == j && gui.gridHighlight.y == i) {
                    g.setColor(Color.green);
                    g.drawRect(panPos.x + j * cellWidth, panPos.y + i * cellHeight, cellWidth, cellHeight);
                }
            }
        }
    }

    public static void drawPathCard (Graphics2D g, PathCard card, int pX, int pY) {
        int mX = pX + cellWidth/2;
        int mY = pY + cellHeight/2;

        // Draw card background and outline
        g.setColor(Color.white);
        g.fillRoundRect(pX, pY, cellWidth, cellHeight, 2, 2);
        g.setColor(Color.black);
        g.drawRoundRect(pX, pY, cellWidth, cellHeight, 2, 2);

        // Draw paths on card
        g.setColor(Color.gray);
        boolean[] dirs = card.getDirections();
        if (card.type != PathCard.PathCardType.Edge) {
            if (dirs[0]) {
                // North - center
                g.fillRect(mX - pathSize /2, pY, pathSize, cellHeight/2);
            }
            if (dirs[1]) {
                // South - center
                g.fillRect(mX - pathSize /2, mY, pathSize, cellHeight/2);
            }
            if (dirs[2]) {
                // West - center
                g.fillRect(pX, mY - pathSize /2, cellWidth/2, pathSize);
            }
            if (dirs[3]) {
                // East - center
                g.fillRect(mX, mY - pathSize /2, cellWidth/2, pathSize);
            }
        } else {
            if (dirs[0]) {
                // North - center
                g.fillRect(mX - pathSize /2, pY, pathSize, cellHeight/2 - centerGap);
            }
            if (dirs[1]) {
                // South - center
                g.fillRect(mX - pathSize /2, mY + centerGap, pathSize, cellHeight/2 - centerGap);
            }
            if (dirs[2]) {
                // West - center
                g.fillRect(pX, mY - pathSize /2, cellWidth/2 - centerGap, pathSize);
            }
            if (dirs[3]) {
                // East - center
                g.fillRect(mX + centerGap, mY - pathSize /2, cellWidth/2 - centerGap, pathSize);
            }
        }

        if (card.type == PathCard.PathCardType.Goal) {
            // Draw treasure in center of card
            if (card.hasTreasure()) {
                g.setColor(Color.yellow);
            } else {
                g.setColor(Color.black);
            }
            g.fillOval(pX + cellWidth/2, pY + cellWidth/2, 10, 10);
        }

        // Draw starting card 'S'
        if (card.type == PathCard.PathCardType.Start) {
            g.setColor(Color.black);
            g.drawString("S", mX, mY);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(boardSize, boardSize);
    }
}
