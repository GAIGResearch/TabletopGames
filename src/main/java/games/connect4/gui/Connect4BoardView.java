package games.connect4.gui;
import core.components.BoardNode;
import core.components.GridBoard;
import games.connect4.Connect4Constants;
import gui.IScreenHighlight;
import gui.views.ComponentView;
import utilities.Pair;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;

import static gui.GUI.defaultItemSize;

public class Connect4BoardView extends ComponentView implements IScreenHighlight {

    Rectangle[] rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;
    LinkedList<Pair<Integer, Integer>> winningCells;

    public Connect4BoardView(GridBoard gridBoard) {
        super(gridBoard, gridBoard.getWidth() * defaultItemSize, gridBoard.getHeight() * defaultItemSize);
        rects = new Rectangle[gridBoard.getWidth()];
        highlight = new ArrayList<>();
        winningCells = new LinkedList<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left click, highlight cell
                    for (Rectangle r: rects) {
                        if (r != null && r.contains(e.getPoint())) {
                            highlight.clear();
                            highlight.add(r);
                            break;
                        }
                    }
                } else {
                    // Remove highlight
                    highlight.clear();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoard((Graphics2D)g, (GridBoard) component, 0, 0);

        if (highlight.size() > 0) {
            g.setColor(Color.green);
            Stroke s = ((Graphics2D) g).getStroke();
            ((Graphics2D) g).setStroke(new BasicStroke(3));

            Rectangle r = highlight.get(0);
            g.drawRect(r.x, r.y, r.width, r.height);
            ((Graphics2D) g).setStroke(s);
        }

        if(winningCells.size() > 0)
            drawWinningCells((Graphics2D) g);
    }

    public void drawGridBoard(Graphics2D g, GridBoard gridBoard, int x, int y) {
        int width = gridBoard.getWidth() * defaultItemSize;
        int height = (gridBoard.getHeight()+1) * defaultItemSize;

        //Draw cells background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw column indicators
        for (int j = 0; j < gridBoard.getWidth(); j++) {
            int xC = x + j * defaultItemSize;
            int yC = y;
            drawCell(g, null, xC, yC);

            // Save rect where cell is drawn
            if (rects[j] == null) {
                rects[j] = new Rectangle(xC, yC, defaultItemSize, defaultItemSize);
            }
        }


        // Draw main cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int xC = x + j * defaultItemSize;
                int yC = y + (i+1) * defaultItemSize;
                drawCell(g, gridBoard.getElement(j, i), xC, yC);

            }
        }
    }

    private void drawCell(Graphics2D g, BoardNode element, int x, int y) {


        // Paint element in cell
        if (element != null) {

            // Paint cell background
            g.setColor(Color.lightGray);
            g.fillRect(x, y, defaultItemSize, defaultItemSize);
            g.setColor(Color.black);
            g.drawRect(x, y, defaultItemSize, defaultItemSize);

            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, defaultItemSize * 3 / 2));
            if(!element.getComponentName().equals(Connect4Constants.emptyCell))
                g.drawString(element.getComponentName(), x + defaultItemSize / 16, y + defaultItemSize - defaultItemSize / 16);
            g.setFont(f);
        }else
        {
            // Paint cell background
            g.setColor(Color.pink);
            g.fillRect(x, y, defaultItemSize, defaultItemSize);
            g.setColor(Color.black);
            g.drawRect(x, y, defaultItemSize, defaultItemSize);

            // Paint column indicator
            g.drawString("[Col: " + ((x/defaultItemSize)+1) + "]", x + defaultItemSize / 16, y + defaultItemSize - defaultItemSize / 16);
        }

    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }

    public void drawWinningCells(Graphics2D g) {
        g.setColor(Color.cyan);
        for (Pair<Integer, Integer> wC : winningCells)
            g.drawRect(wC.a * defaultItemSize, (wC.b+1) * defaultItemSize, defaultItemSize, defaultItemSize);
        g.setColor(Color.black);
    }

    public void setWinningCells(LinkedList<Pair<Integer, Integer>> winningCells) {
        this.winningCells = winningCells;
    }

    @Override
    public void clearHighlights() {
        highlight.clear();
    }
}
