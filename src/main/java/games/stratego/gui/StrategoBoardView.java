package games.stratego.gui;

import core.components.GridBoard;
import games.stratego.StrategoConstants;
import games.stratego.StrategoGameState;
import games.stratego.components.Piece;
import gui.views.ComponentView;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;

import static gui.GUI.defaultItemSize;

public class StrategoBoardView extends ComponentView {

    Rectangle[] rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;
    StrategoGameState gs;
    HashSet<Integer> humanPlayerID;

    public StrategoBoardView(StrategoGameState gs) {
        super(gs.getGridBoard(), gs.getGridBoard().getWidth() * defaultItemSize,
                gs.getGridBoard().getHeight() * defaultItemSize);
        rects = new Rectangle[gs.getGridBoard().getWidth() * gs.getGridBoard().getHeight()];
        highlight = new ArrayList<>();
        this.gs = gs;
        humanPlayerID = new HashSet<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && highlight.size() < 2) {  // Can select up to 2 squares, then clear
                    // Left click, highlight cell
                    for (Rectangle r: rects) {
                        if (r != null && r.contains(e.getPoint())) {
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

            for(Rectangle r: highlight) {
                g.drawRect(r.x, r.y, r.width, r.height);
            }
            ((Graphics2D) g).setStroke(s);
        }
    }

    public void drawGridBoard(Graphics2D g, GridBoard gridBoard, int x, int y) {
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
                drawCell(g, (Piece) gridBoard.getElement(j, i), xC, yC);

                // Save rect where cell is drawn
                int idx = i * gridBoard.getWidth() + j;
                if (rects[idx] == null) {
                    rects[idx] = new Rectangle(xC, yC, defaultItemSize, defaultItemSize);
                }
            }
        }
    }

    private void drawCell(Graphics2D g, Piece piece, int x, int y) {
        // Paint cell background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, defaultItemSize, defaultItemSize);

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
        if (piece != null) {
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, 9 * 3 / 2));
            String elementName = piece.toString();
            if (gs.getCoreGameParameters().partialObservable && !gs.getCoreGameParameters().alwaysDisplayFullObservable && !piece.isPieceKnown()) {
                // Need to hide piece?
                if (!gs.getCoreGameParameters().alwaysDisplayCurrentPlayer || piece.getOwnerId() != gs.getCurrentPlayer()) {
                    if (!humanPlayerID.contains(piece.getOwnerId())) {
                        elementName = "?";
                    }
                }
            }
//            if (piece.getPieceType() != FLAG && piece.getPieceType() != BOMB){
//                elementName = piece.getPieceType().rankToString();
//                g.setFont(new Font(f.getName(), Font.BOLD, 15 * 3 / 2));
//            }
            g.drawString(elementName, x + defaultItemSize / 16, y + defaultItemSize - defaultItemSize / 16);
            g.setFont(f);
        }
    }

    public void setHumanPlayerID(HashSet<Integer> humanPlayerID) {
        this.humanPlayerID = humanPlayerID;
    }

    public void addHumanPlayerID(int human) {
        this.humanPlayerID.add(human);
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }
}
