package games.dotsboxes;

import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static gui.GUI.defaultItemSize;

public class DBGridBoardView extends JComponent {

    DBGameState dbgs;
    int width, height;

    int dotSize = 6;
    Color[] colors = new Color[] {
            new Color(220, 97, 79),
            new Color(54, 200, 103),
            new Color(21, 187, 220),
            new Color(220, 215, 44),
            new Color(147, 71, 220)
    };
    Color[] edgeColors = new Color[] {
            new Color(155, 79, 68),
            new Color(45, 154, 77),
            new Color(16, 125, 150),
            new Color(146, 141, 34),
            new Color(103, 50, 155)
    };

    Point start;
    DBEdge highlight;
    DBEdge highlightIP;

    public DBGridBoardView(DBGameState dbgs) {
        this.dbgs = dbgs;
        DBParameters dbp = (DBParameters) dbgs.getGameParameters();
        this.width = (dbp.gridWidth + 1) * defaultItemSize;
        this.height = (dbp.gridHeight + 2) * defaultItemSize;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (start == null) {
                        start = new Point(e.getX()/defaultItemSize, e.getY()/defaultItemSize);
                    } else {
                        // highlight edge and reset
                        Point end = new Point(e.getX()/defaultItemSize, e.getY()/defaultItemSize);
                        highlight = new DBEdge(new Vector2D(start.x, start.y), new Vector2D(end.x, end.y));
                        start = null;
                        highlightIP = null;
                    }
                } else {
                    start = null;
                    highlightIP = null;
                    highlight = null;
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (start != null) {
                    Point end = new Point(e.getX()/defaultItemSize, e.getY()/defaultItemSize);
                    highlightIP = new DBEdge(new Vector2D(start.x, start.y), new Vector2D(end.x, end.y));
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D)g0;
        drawGridBoard(g, dotSize/2, dotSize/2);

        if (highlightIP != null) {
            g.setColor(edgeColors[dbgs.getCurrentPlayer()]);
            Stroke s = g.getStroke();
            g.setStroke(new BasicStroke(3));
            g.drawLine(highlightIP.from.getX() * defaultItemSize + dotSize/2, highlightIP.from.getY() * defaultItemSize + dotSize/2,
                    highlightIP.to.getX() * defaultItemSize + dotSize/2, highlightIP.to.getY() * defaultItemSize + dotSize/2);
            g.setStroke(s);
        }
    }

    public void drawGridBoard(Graphics2D g, int x, int y) {
        // Draw cells
        for (DBCell c: dbgs.cells) {
            int xC = x + c.position.getX() * defaultItemSize;
            int yC = y + c.position.getY() * defaultItemSize;
            int owner = -1;
            if (dbgs.cellToOwnerMap.containsKey(c)) {
                owner = dbgs.cellToOwnerMap.get(c);
            }
            drawCell(g, c, owner, xC, yC, x, y);
        }
        // Draw edges
        Stroke s = g.getStroke();
        g.setStroke(new BasicStroke(3));
        for (DBEdge e: dbgs.edgeToOwnerMap.keySet()) {
            g.setColor(edgeColors[dbgs.edgeToOwnerMap.get(e)]);
            g.drawLine(e.from.getX() * defaultItemSize + x, e.from.getY() * defaultItemSize + y,
                    e.to.getX() * defaultItemSize + x, e.to.getY() * defaultItemSize + y);
        }
        g.setStroke(s);
    }

    private void drawCell(Graphics2D g, DBCell element, int owner, int x, int y, int offsetX, int offsetY) {
        if (owner != -1) {
            // Paint cell background, according to cell owner
            g.setColor(colors[owner]);
            g.fillRect(x, y, defaultItemSize, defaultItemSize);
            // Draw cell owner
            g.setColor(Color.black);
            g.drawString("" + owner, x+defaultItemSize/2, y+defaultItemSize/2);
        }

        // Draw 4 dots in the corners of the cell
        g.setColor(Color.black);
        g.fillOval(x - dotSize/2, y-dotSize/2, dotSize, dotSize);
        g.fillOval(x+defaultItemSize - dotSize/2, y-dotSize/2, dotSize, dotSize);
        g.fillOval(x-dotSize/2, y+defaultItemSize-dotSize/2, dotSize, dotSize);
        g.fillOval(x+defaultItemSize-dotSize/2, y+defaultItemSize-dotSize/2, dotSize, dotSize);

    }

    public void updateGameState(DBGameState dbgs) {
        this.dbgs = dbgs;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width + dotSize, height + dotSize);
    }

    public DBEdge getHighlight() {
        return highlight;
    }
}
