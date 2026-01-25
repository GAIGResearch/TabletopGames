package games.chinesecheckers.gui;

import games.chinesecheckers.components.CCNode;
import games.chinesecheckers.components.Peg;
import games.chinesecheckers.components.StarBoard;
import gui.IScreenHighlight;
import gui.views.ComponentView;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static gui.GUI.defaultItemSize;

public class CCGraphView extends ComponentView implements IScreenHighlight {

    Map<Integer, Rectangle> dots = new HashMap<>();
    StarBoard starBoard;

    public CCGraphView(StarBoard starBoard) {
        super(starBoard, defaultItemSize, defaultItemSize);

        this.starBoard = starBoard;
        for (CCNode node : starBoard.getBoardNodes()) {
            dots.put(node.getID(), new Rectangle(node.getX(), node.getY(), 10, 10));
        }
    }

    void drawNodes(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        for (Integer nodeID : dots.keySet()) {
            CCNode node = starBoard.getNodeById(nodeID);
            g2d.setColor(node.getBaseColour().toGraphicsColor());

            int x = node.getX();
            int y = node.getY();
            int scale = 30;
            int size = 15;
            boolean shift = false;

            if (y % 2 != 0) {
                shift = true;
            }

            x = x * scale;
            y = y * scale;

            if (shift) {
                g2d.fillOval(x + 72, y + 23, size, size);
            } else {
                g2d.fillOval(x + 57, y + 23, size, size);
            }

            for (int side = 0; side < 6; side++) {
                CCNode neighbour = starBoard.getNodeById(node.getNeighbourBySide(side));
                // now draw lines from this node to its neighbours
                if (neighbour != null) {
                    int neighbourX = neighbour.getX() * scale;
                    int neighbourY = neighbour.getY() * scale;

                    boolean neighbourShift = false;
                    if (neighbour.getY() % 2 != 0) {
                        neighbourShift = true;
                    }
                    if (neighbourShift) {
                        neighbourX += 72;
                        neighbourY += 23;
                    } else {
                        neighbourX += 57;
                        neighbourY += 23;
                    }
                    if (shift) {
                        g2d.drawLine(x + 79,
                                y + 30,
                                neighbourX + 7,
                                neighbourY + 7);
                    } else {
                        g2d.drawLine(x + 64,
                                y + 30,
                                neighbourX + 7,
                                neighbourY + 7);
                    }
                }

            }
        }
    }

    void drawPegs(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        for (int i = 0; i < starBoard.getBoardNodes().size(); i++) {
            if (starBoard.getBoardNodes().get(i).isNodeOccupied()) {
                Peg peg = (starBoard.getBoardNodes().get(i)).getOccupiedPeg();
                g2d.setColor(peg.getColour().toGraphicsColor());

                int x = dots.get(i).x;
                int y = dots.get(i).y;
                int scale = 30;
                int size = 20;
                boolean shift = false;

                if (y % 2 != 0) {
                    shift = true;
                }

                x = x * scale;
                y = y * scale;

                if (shift) {
                    g2d.fillOval(x + 70, y + 22, size, size);
                } else {
                    g2d.fillOval(x + 55, y + 22, size, size);
                }
            }
        }
    }

    void drawNumbers(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Font font = new Font("Verdana", Font.BOLD, 13);
        g2d.setFont(font);

        for (int i = 0; i < starBoard.getBoardNodes().size(); i++) {
            g2d.setColor(starBoard.getBoardNodes().get(i).getBaseColour().toGraphicsColor());

            int x = dots.get(i).x;
            int y = dots.get(i).y;
            int scale = 30;
            boolean shift = false;

            if (y % 2 != 0) {
                shift = true;
            }

            x = x * scale;
            y = y * scale;

            if (shift) {
                g2d.drawString(Integer.toString(i), x + 570, y + 28);
            } else {
                g2d.drawString(Integer.toString(i), x + 555, y + 28);
            }
        }
    }


    @Override
    public void clearHighlights() {

    }

    @Override
    protected void paintComponent(Graphics g) {
        drawNodes(g);
        drawPegs(g);
        drawNumbers(g);
    }
}
