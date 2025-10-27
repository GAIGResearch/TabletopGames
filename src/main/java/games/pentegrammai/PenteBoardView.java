package games.pentegrammai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class PenteBoardView extends JComponent {

    protected int boardWidth = 600;
    protected int boardHeight = 390; // increased by 50% (was 260)
    protected int margin = 60;
    protected int spaceRadius = 22;
    protected int pieceRadius = 18;

    protected int firstClick = -1;
    protected int secondClick = -1;

    protected PenteForwardModel forwardModel;
    protected List<PenteMoveAction> validActions = new ArrayList<>();
    protected int currentPlayer = 0;
    protected int dieValue = 1;
    protected List<List<core.components.Token>> board;

    public PenteBoardView(PenteForwardModel model) {
        this.setPreferredSize(new Dimension(boardWidth, boardHeight));
        this.forwardModel = model;

        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                int x = evt.getX();
                int y = evt.getY();
                int pos = getSpaceAt(x, y);
                if (pos != -1) {
                    if (firstClick == -1) {
                        firstClick = pos;
                    } else if (secondClick == -1) {
                        secondClick = pos;
                    }
                } else {
                    firstClick = -1;
                    secondClick = -1;
                }
            }
        });
    }

    public synchronized void update(PenteGameState state) {
        this.currentPlayer = state.getCurrentPlayer();
        this.dieValue = state.die.getValue();
        this.board = state.board;
        // Compute valid actions
        this.validActions = forwardModel._computeAvailableActions(state).stream()
                .filter(a -> a instanceof PenteMoveAction)
                .map(a -> (PenteMoveAction) a)
                .toList();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw background (increased height)
        g2d.setColor(new Color(230, 210, 170));
        g2d.fillRect(0, 0, boardWidth, boardHeight);

        int nLines = 5;
        int lineSpacing = 54;
        int baseY = 40;
        int leftX = margin + 40;
        int rightX = boardWidth - margin - 40;

        // Calculate positions for each point
        Point[] pointCenters = new Point[10];
        for (int i = 0; i < nLines; i++) {
            int y = baseY + i * lineSpacing;
            // Left column: points 5-9 (top to bottom)
            pointCenters[5 + i] = new Point(leftX, y);
            // Right column: points 4-0 (top to bottom)
            pointCenters[4 - i] = new Point(rightX, y);
        }

        // Draw horizontal lines
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(4));
        for (int i = 0; i < nLines; i++) {
            Point right = pointCenters[4 - i];
            Point left = pointCenters[5 + i];
            g2d.drawLine(left.x, left.y, right.x, right.y);
        }

        // Draw spaces and numbers
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 14f));
        for (int i = 0; i < 10; i++) {
            int x = pointCenters[i].x - spaceRadius;
            int y = pointCenters[i].y - spaceRadius;

            // Highlight if valid move
            if (firstClick == -1) {
                for (PenteMoveAction a : validActions) {
                    if (a.from == i) {
                        g2d.setColor(Color.YELLOW);
                        g2d.fillOval(x - 4, y - 4, spaceRadius * 2 + 8, spaceRadius * 2 + 8);
                    }
                }
            } else if (secondClick == -1 && i == firstClick) {
                g2d.setColor(Color.ORANGE);
                g2d.fillOval(x - 4, y - 4, spaceRadius * 2 + 8, spaceRadius * 2 + 8);
            } else if (secondClick == -1) {
                for (PenteMoveAction a : validActions) {
                    if (a.from == firstClick && a.to == i) {
                        g2d.setColor(Color.RED);
                        g2d.fillOval(x - 4, y - 4, spaceRadius * 2 + 8, spaceRadius * 2 + 8);
                    }
                }
            }

            // Draw space
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillOval(x, y, spaceRadius * 2, spaceRadius * 2);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x, y, spaceRadius * 2, spaceRadius * 2);

            // Draw sacred marker if needed
            if (isSacred(i)) {
                g2d.setColor(new Color(180, 120, 40));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(x + 6, y + 6, spaceRadius * 2 - 12, spaceRadius * 2 - 12);
            }

            // Draw number for each space
            g2d.setColor(Color.BLACK);
            String label = String.valueOf(i);
            int labelY = pointCenters[i].y + 5; // vertically centered on the line

            if (i < 5) {
                // Right column: label to the left of the space
                int labelX = pointCenters[i].x - spaceRadius - 18;
                g2d.drawString(label, labelX, labelY);
            } else {
                // Left column: label to the right of the space
                int labelX = pointCenters[i].x + spaceRadius + 6;
                g2d.drawString(label, labelX, labelY);
            }
        }

        // Draw pieces, offset outward if multiple tokens
        if (board != null) {
            for (int i = 0; i < board.size(); i++) {
                Point center = pointCenters[i];
                int nTokens = board.get(i).size();
                for (int j = 0; j < nTokens; j++) {
                    core.components.Token t = board.get(i).get(j);
                    int owner = t.getOwnerId();
                    int dx = 0;
                    // Offset outward: left points offset left, right points offset right
                    if (i < 5) { // right points 0-4
                        dx = j * 10;
                    } else { // left points 5-9
                        dx = -j * 10;
                    }
                    int x = center.x - pieceRadius + dx;
                    int y = center.y - pieceRadius;
                    g2d.setColor(owner == 0 ? Color.WHITE : Color.BLACK);
                    g2d.fillOval(x, y, pieceRadius * 2, pieceRadius * 2);
                    g2d.setColor(Color.BLACK);
                    g2d.drawOval(x, y, pieceRadius * 2, pieceRadius * 2);
                }
            }
        }

        // Draw die as a graphic below the lines (background now covers this area)
        int dieY = baseY + nLines * lineSpacing;
        int dieX = boardWidth / 2 - 20;
        drawDie(g2d, dieX, dieY, 40, dieValue);

        // Draw player indicators below the die (background now covers this area)
        int markerY = dieY + 40;
        // Player 0 (White)
        g2d.setColor(Color.WHITE);
        g2d.fillOval(boardWidth / 2 - 80, markerY, 24, 24);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(boardWidth / 2 - 80, markerY, 24, 24);
        g2d.drawString("Player 0 (White)", boardWidth / 2 - 50, markerY + 17);

        // Player 1 (Black)
        g2d.setColor(Color.BLACK);
        g2d.fillOval(boardWidth / 2 + 80 - 24, markerY, 24, 24);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(boardWidth / 2 + 80 - 24, markerY, 24, 24);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Player 1 (Black)", boardWidth / 2 + 60, markerY + 17);
    }

    private void drawDie(Graphics2D g2d, int x, int y, int size, int value) {
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(x, y, size, size, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(x, y, size, size, 10, 10);

        int dotSize = size / 6;
        int offset = size / 4;

        g2d.setColor(Color.BLACK);
        // Center dot
        if (value == 1 || value == 3 || value == 5)
            g2d.fillOval(x + size / 2 - dotSize / 2, y + size / 2 - dotSize / 2, dotSize, dotSize);
        if (value >= 2) {
            g2d.fillOval(x + offset - dotSize / 2, y + offset - dotSize / 2, dotSize, dotSize);
            g2d.fillOval(x + size - offset - dotSize / 2, y + size - offset - dotSize / 2, dotSize, dotSize);
        }
        if (value >= 4) {
            g2d.fillOval(x + offset - dotSize / 2, y + size - offset - dotSize / 2, dotSize, dotSize);
            g2d.fillOval(x + size - offset - dotSize / 2, y + offset - dotSize / 2, dotSize, dotSize);
        }
        if (value == 6) {
            g2d.fillOval(x + offset - dotSize / 2, y + size / 2 - dotSize / 2, dotSize, dotSize);
            g2d.fillOval(x + size - offset - dotSize / 2, y + size / 2 - dotSize / 2, dotSize, dotSize);
        }
    }

    private int getSpaceAt(int x, int y) {
        int nLines = 5;
        int lineSpacing = 54;
        int baseY = 40;
        int leftX = margin + 40;
        int rightX = boardWidth - margin - 40;
        int[] xs = new int[10];
        int[] ys = new int[10];
        for (int i = 0; i < nLines; i++) {
            int yy = baseY + i * lineSpacing;
            xs[5 + i] = leftX;
            ys[5 + i] = yy;
            xs[4 - i] = rightX;
            ys[4 - i] = yy;
        }
        for (int i = 0; i < 10; i++) {
            int dx = x - xs[i];
            int dy = y - ys[i];
            if (dx * dx + dy * dy <= spaceRadius * spaceRadius) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSacred(int pos) {
        return pos == 2 || pos == 7;
    }
}
