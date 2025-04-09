package games.backgammon;

import javax.swing.*;
import java.awt.*;

public class BackgammonBoardView extends JComponent {

    private final int boardWidth = 800;
    private final int boardHeight = 400;
    private final int triangleBase = 60;
    private final int triangleHeight = 150;
    private final int margin = 20;
    int discRadius = 20;
    int discMargin = 5;

    private int[][] piecesPerPoint = new int[2][24]; // [player][point]
    private int[] piecesOnBar = new int[2];      // [player]
    private int[] piecesBorneOff = new int[2];   // [player]

    public BackgammonBoardView() {
        this.setPreferredSize(new Dimension(boardWidth, boardHeight));
    }


    public synchronized void update(BGGameState state) {
        int nPlayers = state.getNPlayers();

        // Update pieces on points
        for (int player = 0; player < nPlayers; player++) {
            piecesPerPoint[player] = state.getPlayerPieces(player);
            piecesOnBar[player] = state.getPiecesOnBar(player);
            piecesBorneOff[player] = state.getPiecesBorneOff(player);
        }

        // Repaint the board to reflect the updated state
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw the board background
        g2d.setColor(new Color(222, 184, 135)); // Light brown for the board
        g2d.fillRect(0, 0, boardWidth, boardHeight);

        // Draw the triangles
        drawTriangles(g2d);

        // Draw the discs on the triangles
        // point is measured from the perspective of player 0
        for (int point = 0; point < piecesPerPoint[0].length; point++) {
            // which player (if any) has discs on this point
            int player = piecesPerPoint[0][point] > 0 ? 0 : (piecesPerPoint[1][23 - point] > 0 ? 1 : -1);
            if (player == -1) continue; // No discs on this point

            int numDiscs = piecesPerPoint[player][player == 0 ? point : (23 - point)];
            boolean topRowOfTriangles = point < 12;
            int position = topRowOfTriangles ? (11 - point) : (point - 12);
            // Calculate the x position based on the triangle base and margin
            int x = margin + position * triangleBase + triangleBase / 2 - discRadius / 2;
            // Calculate the y position based on the triangle height and margin
            int yStart = topRowOfTriangles ? margin + discMargin : boardHeight - margin - discMargin - discRadius;
            drawDiscs(g2d, x, yStart, numDiscs, player, topRowOfTriangles);
        }

        // Draw the discs on the bar
        for (int player = 0; player < piecesOnBar.length; player++) {
            int x = margin + 12 * triangleBase + triangleBase / 2 - discRadius / 2;
            int yStart = player == 0 ? margin + discMargin : boardHeight - margin - discMargin - discRadius;
            drawDiscs(g2d, x, yStart, piecesOnBar[player], player, player == 0);
        }

        // Display the number of discs borne off
        for (int player = 0; player < piecesBorneOff.length; player++) {
            String borneOffText = "Borne Off: " + piecesBorneOff[player];
            int x = boardWidth - g.getFontMetrics().stringWidth(borneOffText) - margin * 3; // Position next to the bar
            int y = player == 0 ? margin + boardHeight / 2 : boardHeight / 2 - margin;

            g2d.setColor(player == 0 ? Color.WHITE : Color.BLACK);
            g2d.drawString("Borne Off: " + piecesBorneOff[player], x, y);
        }
    }

    private void drawDiscs(Graphics2D g2d, int x, int yStart, int numDiscs, int player, boolean fromTop) {
        for (int i = 0; i < numDiscs; i++) {
            int y = fromTop ? yStart + i * (discRadius + discMargin) : yStart - i * (discRadius + discMargin);
            g2d.setColor(player == 0 ? Color.WHITE : Color.BLACK);
            g2d.fillOval(x, y, discRadius, discRadius);
            g2d.setColor(Color.CYAN);
            g2d.drawOval(x, y, discRadius, discRadius);
        }
    }

    private void drawTriangles(Graphics2D g2d) {
        int xStart = margin;
        int yTop = margin;
        int yBottom = boardHeight - margin;

        // Draw top row of triangles (12 points)
        for (int i = 0; i < 12; i++) {
            drawTriangle(g2d, xStart + i * triangleBase, yTop, true, i % 2 == 0);
        }

        // Draw bottom row of triangles (12 points)
        for (int i = 0; i < 12; i++) {
            drawTriangle(g2d, xStart + i * triangleBase, yBottom, false, i % 2 == 0);
        }
    }

    private void drawTriangle(Graphics2D g2d, int x, int y, boolean pointingUp, boolean isDark) {
        int[] xPoints = {x, x + triangleBase / 2, x + triangleBase};
        int[] yPoints;

        if (pointingUp) {
            yPoints = new int[]{y, y + triangleHeight, y};
        } else {
            yPoints = new int[]{y, y - triangleHeight, y};
        }



        g2d.setColor(isDark ? Color.DARK_GRAY : Color.LIGHT_GRAY);
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(xPoints, yPoints, 3);
    }
}