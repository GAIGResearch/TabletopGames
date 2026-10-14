package games.chinesecheckers.gui;

import core.components.grid.HexCoord;
import games.chinesecheckers.components.Peg;
import games.chinesecheckers.components.StarBoard;
import gui.HexLayout;
import gui.IScreenHighlight;
import gui.views.ComponentView;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static gui.GUI.defaultItemSize;

/**
 * The star: the nodes in their base colours, joined to their neighbours, with their numbers (as the actions name
 * them), and the pegs on them.
 */
public class CCGraphView extends ComponentView implements IScreenHighlight {

    static final int WIDTH = 520, HEIGHT = 560, MARGIN = 10;

    final HexLayout layout;

    public CCGraphView(StarBoard starBoard) {
        super(starBoard, defaultItemSize, defaultItemSize);
        List<HexCoord> positions = new ArrayList<>();
        for (int node = 0; node < starBoard.nNodes(); node++)
            positions.add(starBoard.getPosition(node));
        layout = HexLayout.fitting(positions, WIDTH, HEIGHT, MARGIN, 100);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        StarBoard board = (StarBoard) component;
        double size = layout.size();
        int dot = (int) (size * 0.8), peg = (int) (size * 1.1);

        // the links, each drawn once from the node to its east, south-east and south-west
        g.setColor(Color.gray);
        for (int node = 0; node < board.nNodes(); node++) {
            HexCoord h = board.getPosition(node);
            for (int d = 0; d < 3; d++)
                if (board.getNode(h.neighbour(d)) >= 0)
                    g.draw(new Line2D.Double(layout.centre(h), layout.centre(h.neighbour(d))));
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(8, (int) (size * 0.45))));
        FontMetrics fm = g.getFontMetrics();
        for (int node = 0; node < board.nNodes(); node++) {
            Point2D c = layout.centre(board.getPosition(node));
            int x = (int) Math.round(c.getX()), y = (int) Math.round(c.getY());
            g.setColor(board.getBaseColour(node).toGraphicsColor());
            g.fillOval(x - dot / 2, y - dot / 2, dot, dot);
            Peg p = board.getPeg(node);
            if (p != null) {
                g.setColor(p.colour().toGraphicsColor());
                g.fillOval(x - peg / 2, y - peg / 2, peg, peg);
                g.setColor(Color.black);
                g.drawOval(x - peg / 2, y - peg / 2, peg, peg);
            }
            // the number above the node
            String label = Integer.toString(node);
            g.setColor(Color.darkGray);
            g.drawString(label, x - fm.stringWidth(label) / 2, (int) (y - size * 0.55));
        }
    }

    @Override
    public void clearHighlights() {
    }
}
