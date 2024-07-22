package games.mastermind.gui;

import core.components.GridBoard;
import core.components.Token;
import games.mastermind.MMGameState;
import games.mastermind.MMParameters;
import gui.IScreenHighlight;
import gui.views.ComponentView;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import static gui.GUI.defaultItemSize;

public class MMBoardView extends ComponentView implements IScreenHighlight {
    int scaledDefaultItemSize = (int) (defaultItemSize * 0.5);
    Rectangle[] rects; // For highlights + action trimming
    ArrayList<Rectangle> highlight;

    public MMBoardView(GridBoard<Token> gridBoard) {
        super(gridBoard, gridBoard.getWidth()*defaultItemSize, gridBoard.getHeight()*defaultItemSize);
        rects = new Rectangle[gridBoard.getWidth()*gridBoard.getHeight()];
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
        drawGridBoard((Graphics2D)g, (GridBoard<Token>) component, 0, 0);

        if (highlight.size() > 0) {
            g.setColor(Color.green);
            Stroke s = ((Graphics2D) g).getStroke();
            ((Graphics2D) g).setStroke(new BasicStroke(3));

            Rectangle r = highlight.get(0);
            g.drawRect(r.x, r.y, r.width, r.height);
            ((Graphics2D) g).setStroke(s);
        }
    }

    public void drawGridBoard(Graphics2D g, GridBoard<Token> gridBoard, int x, int y) {
        int width = gridBoard.getWidth() * scaledDefaultItemSize;
        int height = gridBoard.getHeight() * scaledDefaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int xC = x + j * scaledDefaultItemSize;
                int yC = y + i * scaledDefaultItemSize;
                drawCell(g, gridBoard.getElement(j, i), xC, yC);

                // Save rect where cell is drawn
                int idx = i * gridBoard.getWidth() + j;
                if (rects[idx] == null) {
                    rects[idx] = new Rectangle(xC, yC, scaledDefaultItemSize, scaledDefaultItemSize);
                }
            }
        }
    }

    private void drawCell(Graphics2D g, Token element, int x, int y) {
        // Paint cell background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, scaledDefaultItemSize, scaledDefaultItemSize);
        g.setColor(Color.black);
        g.drawRect(x, y, scaledDefaultItemSize, scaledDefaultItemSize);

        // Paint element in cell
        if (element != null) {
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, scaledDefaultItemSize));
            g.drawString(element.toString(), x + scaledDefaultItemSize / 16, y + scaledDefaultItemSize - scaledDefaultItemSize / 16);
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
