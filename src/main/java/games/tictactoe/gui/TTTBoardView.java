package games.tictactoe.gui;

import core.components.BoardNode;
import core.components.grid.SparseGrid;
import core.components.grid.SquareCoord;
import gui.IScreenHighlight;
import gui.views.ComponentView;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import static gui.GUI.defaultItemSize;

public class TTTBoardView extends ComponentView implements IScreenHighlight {

    Rectangle[] rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;
    final int gridSize;

    public TTTBoardView(SparseGrid<SquareCoord, BoardNode> gridBoard, int gridSize) {
        super(gridBoard, gridSize * defaultItemSize, gridSize * defaultItemSize);
        this.gridSize = gridSize;
        rects = new Rectangle[gridSize * gridSize];
        highlight = new ArrayList<>();

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
        @SuppressWarnings("unchecked")
        SparseGrid<SquareCoord, BoardNode> gridBoard = (SparseGrid<SquareCoord, BoardNode>) component;
        drawGridBoard((Graphics2D)g, gridBoard, 0, 0);

        if (!highlight.isEmpty()) {
            g.setColor(Color.green);
            Stroke s = ((Graphics2D) g).getStroke();
            ((Graphics2D) g).setStroke(new BasicStroke(3));

            Rectangle r = highlight.get(0);
            g.drawRect(r.x, r.y, r.width, r.height);
            ((Graphics2D) g).setStroke(s);
        }
    }

    public void drawGridBoard(Graphics2D g, SparseGrid<SquareCoord, BoardNode> gridBoard, int x, int y) {
        int width = gridSize * defaultItemSize;
        int height = gridSize * defaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int xC = x + j * defaultItemSize;
                int yC = y + i * defaultItemSize;
                drawCell(g, gridBoard.get(new SquareCoord(j, i)), xC, yC);

                // Save rect where cell is drawn
                int idx = i * gridSize + j;
                if (rects[idx] == null) {
                    rects[idx] = new Rectangle(xC, yC, defaultItemSize, defaultItemSize);
                }
            }
        }
    }

    private void drawCell(Graphics2D g, BoardNode element, int x, int y) {
        // Paint cell background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, defaultItemSize, defaultItemSize);
        g.setColor(Color.black);
        g.drawRect(x, y, defaultItemSize, defaultItemSize);

        // Paint element in cell
        if (element != null) {
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, defaultItemSize * 3 / 2));
            g.drawString(element.getComponentName(), x + defaultItemSize / 16, y + defaultItemSize - defaultItemSize / 16);
            g.setFont(f);
        }
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }

    @Override
    public void clearHighlights() {
        highlight.clear();
    }
}
