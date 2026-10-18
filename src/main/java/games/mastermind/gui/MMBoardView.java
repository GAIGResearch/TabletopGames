package games.mastermind.gui;

import core.components.BoardNode;
import core.components.GridBoard;
import gui.IScreenHighlight;
import gui.views.ComponentView;

import java.awt.*;
import java.util.ArrayList;

import static gui.GUI.defaultItemSize;

public class MMBoardView extends ComponentView implements IScreenHighlight {
    int scaledDefaultItemSize = defaultItemSize;
    // int scaledDefaultItemSize = (int) (defaultItemSize * 0.5);

    Rectangle[] rects; // For highlights + action trimming
    ArrayList<Rectangle> highlight;
    final int gridWidth, gridHeight;

    public MMBoardView(GridBoard gridBoard, int gridWidth, int gridHeight) {
        super(gridBoard, gridWidth*defaultItemSize, gridHeight*defaultItemSize);
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        rects = new Rectangle[gridWidth*gridHeight];
        highlight = new ArrayList<>();

//        addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                highlight.clear();
//                for (Rectangle r: rects) {
//                    if (r != null && r.contains()) {
//                    highlight.add(r);
//                        break;
//                    }
//                }
//            }
//        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        @SuppressWarnings("unchecked")
        GridBoard gridBoard = (GridBoard) component;
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

    public void drawGridBoard(Graphics2D g, GridBoard gridBoard, int x, int y) {
        int width = gridWidth * scaledDefaultItemSize;
        int height = gridHeight * scaledDefaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                int xC = x + j * scaledDefaultItemSize;
                int yC = y + i * scaledDefaultItemSize;
                drawCell(g, gridBoard.get(j, i), xC, yC);

                // Save rect where cell is drawn
                int idx = i * gridWidth + j;
                if (rects[idx] == null) {
                    rects[idx] = new Rectangle(xC, yC, scaledDefaultItemSize, scaledDefaultItemSize);
                }
            }
        }
    }

    private void drawCell(Graphics2D g, BoardNode element, int x, int y) {
        // Paint cell background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, scaledDefaultItemSize, scaledDefaultItemSize);
        g.setColor(Color.black);
        g.drawRect(x, y, scaledDefaultItemSize, scaledDefaultItemSize);

        // Paint element in cell
        if (element != null) {
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, scaledDefaultItemSize));
            g.drawString(element.getComponentName(), x + scaledDefaultItemSize / 16, y + scaledDefaultItemSize - scaledDefaultItemSize / 16);
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
