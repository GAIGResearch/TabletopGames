package gui.views;

import core.components.BoardNode;
import core.components.GraphBoard;

import javax.swing.*;
import java.awt.*;

public class GraphBoardView extends JComponent {
    GraphBoard graphBoard;
    int width, height;

    public GraphBoardView(GraphBoard gridBoard, int width, int height) {
        updateBoard(gridBoard);
        this.width = width;
        this.height = height;
    }

    public void updateBoard(GraphBoard graphBoard) {
        this.graphBoard = graphBoard;
        if (graphBoard != null) {
            setToolTipText("Component ID: " + graphBoard.getComponentID());
        }
    }

    public GraphBoard getGraphBoard() {
        return graphBoard;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGraphBoard((Graphics2D)g, graphBoard, 0, 0, width, height);
    }

    public static void drawGraphBoard(Graphics2D g, GraphBoard graphBoard, int x, int y, int width, int height) {

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw board nodes
        for (BoardNode bn: graphBoard.getBoardNodes()) {
            // TODO
        }
    }

    public static void drawGraphBoard(Graphics2D g, GraphBoard graphBoard, Rectangle rect) {
        drawGraphBoard(g, graphBoard, rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
