package gui.views;

import core.components.BoardNode;
import core.components.GraphBoard;

import java.awt.*;

public class GraphBoardView extends ComponentView {

    public GraphBoardView(GraphBoard board, int width, int height) {
        super(board, width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGraphBoard((Graphics2D)g, (GraphBoard) component, 0, 0, width, height);
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

}
