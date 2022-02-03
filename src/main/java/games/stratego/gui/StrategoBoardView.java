package games.stratego.gui;

import core.components.GridBoard;
import core.components.Token;
import games.stratego.StrategoConstants;
import games.stratego.components.Piece;
import gui.views.ComponentView;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import static games.stratego.components.Piece.PieceType.BOMB;
import static games.stratego.components.Piece.PieceType.FLAG;
import static gui.GUI.defaultItemSize;

public class StrategoBoardView extends ComponentView {

    Rectangle[] rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;

    public StrategoBoardView(GridBoard<Piece> gridBoard) {
        super(gridBoard, gridBoard.getWidth() * defaultItemSize, gridBoard.getHeight() * defaultItemSize);
        rects = new Rectangle[gridBoard.getWidth() * gridBoard.getHeight()];
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
        int width = gridBoard.getWidth() * defaultItemSize;
        int height = gridBoard.getHeight() * defaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int xC = x + j * defaultItemSize;
                int yC = y + i * defaultItemSize;
                drawCell(g, gridBoard.getElement(j, i), xC, yC);

                // Save rect where cell is drawn
                int idx = i * gridBoard.getWidth() + j;
                if (rects[idx] == null) {
                    rects[idx] = new Rectangle(xC, yC, defaultItemSize, defaultItemSize);
                }
            }
        }
    }

    private void drawCell(Graphics2D g, Token element, int x, int y) {
        // Paint cell background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, defaultItemSize, defaultItemSize);

        Piece piece = (Piece) element;
        if (piece != null){
            g.setColor(piece.getPieceAlliance().getColor());
        } else {g.setColor(Color.black);}

        g.drawRect(x, y, defaultItemSize, defaultItemSize);

        if ((x==100 && y==200) || (x==100 && y==250) || (x==150 && y==200) || (x==150 && y==250)
                || (x==300 && y==200) || (x==300 && y==250) || (x==350 && y==200) || (x==350 && y==250)){
            String symbol = StrategoConstants.waterCell;
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, 30 * 3 / 2));
            g.drawString(symbol, x + defaultItemSize / 16, y + defaultItemSize - defaultItemSize / 16);
            g.setFont(f);
        }

        // Paint element in cell
        if (element != null) {
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, 9 * 3 / 2));
            String elementName = element.toString();
            if (piece.getPieceType() != FLAG && piece.getPieceType() != BOMB){
                elementName = piece.getPieceType().rankToString();
                g.setFont(new Font(f.getName(), Font.BOLD, 15 * 3 / 2));
            }
            g.drawString(elementName, x + defaultItemSize / 16, y + defaultItemSize - defaultItemSize / 16);
            g.setFont(f);
        }
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }
}
