package games.backgammon;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.actions.AbstractAction;
import games.descent2e.actions.Move;
import games.dotsboxes.AddGridCellEdge;
import games.dotsboxes.DBEdge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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
    private int[] diceValues = new int[2];
    private boolean[] diceUsed = new boolean[2];
    private BGForwardModel forwardModel;
    private List<MovePiece> validActions = new ArrayList<>();
    private int currentPlayer = 0;

    int firstClick = -1;
    int secondClick = -1;

    public BackgammonBoardView(BGForwardModel model) {
        this.setPreferredSize(new Dimension(boardWidth, boardHeight));
        forwardModel = model;

        // now add a MouseListener to listen for clicks
        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON1) {
                    // left-click
                    int x = evt.getX();
                    int y = evt.getY();
                    boolean topHalf = (y < boardHeight / 2);
                    // we now convert this x, y position to one of the 'points' on the board
                    // between 1 and 24, or 0 to bear-off and 25 for the bar

                    int point = (x - margin) / triangleBase;
                    if (topHalf) {
                        point = switch (point) {
                            case -1 -> -1;
                            case 12 -> 0;
                            default -> 12 - point;
                        };
                    } else {
                        // bottom half
                        point = switch (point) {
                            case -1 -> -1;
                            case 12 -> 25;
                            default -> point + 13;
                        };
                    }
                    // this is from the perspective of player 0
                    if (firstClick == -1)
                        firstClick = point;
                    else
                        secondClick = point;
                } else {
                    // we clear out the variables and reset
                    firstClick = -1;
                    secondClick = -1;
                }
            }
        });
    }

    public synchronized void update(BGGameState state) {
        int nPlayers = state.getNPlayers();

        validActions = forwardModel.computeAvailableActions(state).stream()
                .filter(a -> a instanceof MovePiece)
                .map(MovePiece.class::cast)
                .collect(toList());
        currentPlayer = state.getCurrentPlayer();

        // Update pieces on points
        for (int player = 0; player < nPlayers; player++) {
            piecesPerPoint[player] = state.getPlayerPieces(player);
            piecesOnBar[player] = state.getPiecesOnBar(player);
            piecesBorneOff[player] = state.getPiecesBorneOff(player);
        }
        diceValues = state.getDiceValues();
        diceUsed = state.diceUsed.clone();

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
        for (int i = 0; i < 24; i++) {
            drawTriangle(g2d, i);
        }

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
            int yStart = player == 1 ? margin + discMargin : boardHeight - margin - discMargin - discRadius;
            drawDiscs(g2d, x, yStart, piecesOnBar[player], player, player == 0);
        }

        // Display the number of discs borne off
        for (int player = 0; player < piecesBorneOff.length; player++) {
            String borneOffText = "Borne Off: " + piecesBorneOff[player];
            int x = boardWidth - g.getFontMetrics().stringWidth(borneOffText) - margin * 3; // Position next to the bar
            int y = player == 1 ? margin + boardHeight / 2 : boardHeight / 2 - margin;

            g2d.setColor(player == 0 ? Color.WHITE : Color.BLACK);
            g2d.drawString("Borne Off: " + piecesBorneOff[player], x, y);
        }
        // Draw the dice in the middle of the board
        drawDice(g2d, boardWidth / 2, boardHeight / 2);

        // Number the points (white / black text for the two players as they use different colors)
        // The base of each triangle should be labelled with whitePoint / blackPoint so that the text appears over/under the discs
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 12; i++) {
            String text = i + 1 + " / ";
            int x = margin + (11 - i) * triangleBase + triangleBase / 2 - g.getFontMetrics().stringWidth(text) / 2;
            int y = margin - g.getFontMetrics().getHeight() / 2;
            g2d.drawString(text, x, y);
            // then on the other side of the board
            text = i + 13 + " /  ";
            y = boardHeight - g.getFontMetrics().getHeight() / 2;
            x = margin + i * triangleBase + triangleBase / 2 - g.getFontMetrics().stringWidth(text) / 2;
            g2d.drawString(text, x, y);
        }
        g2d.setColor(Color.BLACK);
        for (int i = 0; i < 12; i++) {
            String text = " " + (i + 1);
            int x = margin + (11 - i) * triangleBase + triangleBase / 2 + g.getFontMetrics().stringWidth(text) / 2;
            int y = boardHeight - g.getFontMetrics().getHeight() / 2;
            g2d.drawString(text, x, y);
            // then on the other (top) side of the board
            text = " " + (i + 13);
            x = margin + i * triangleBase + triangleBase / 2 + g.getFontMetrics().stringWidth(text) / 2;
            y = margin - g.getFontMetrics().getHeight() / 2;
            g2d.drawString(text, x, y);
        }

    }

    private void drawDiscs(Graphics2D g2d, int x, int yStart, int numDiscs, int player, boolean fromTop) {
        for (int i = 0; i < numDiscs; i++) {
            int y = fromTop ? yStart + i * (discRadius + discMargin) : yStart - i * (discRadius + discMargin);
            g2d.setColor(player == 0 ? Color.WHITE : Color.BLACK);
            g2d.fillOval(x, y, discRadius, discRadius);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x, y, discRadius, discRadius);
        }
    }

    private void drawTriangle(Graphics2D g2d, int i) {
        // we start with i = 0 on the top row on the far right
        // and i = 23 on the bottom row on the far right
        // with the triangles proceeding in a horseshoe shape around the board

        boolean isDark = i % 2 == 0;
        boolean pointingDown = i < 12;
        int x = pointingDown ? boardWidth - triangleBase * (i + 2) : margin + triangleBase * (i - 12);
        int y = pointingDown ? margin : boardHeight - margin;

        int[] xPoints = new int[]{x, x + triangleBase / 2, x + triangleBase};
        int[] yPoints = pointingDown ? new int[]{y, y + triangleHeight, y} : new int[]{y, y - triangleHeight, y};

        g2d.setColor(isDark ? Color.DARK_GRAY : Color.LIGHT_GRAY);
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(xPoints, yPoints, 3);

        // Then, if firstClick is -1, we look through validActions to find the from position of all valid moves
        // and highlight the triangles that are valid moves
        // i and from/to in MovePiece use 0..23 for the points
        // firstClick and secondClick use 1..24 for the points
        int clickPlayerPerspective = currentPlayer == 0 ? i : (23 - i);
        if (firstClick == -1) {
            for (MovePiece action : validActions) {
                if (action.from == clickPlayerPerspective) {
                    g2d.setColor(Color.YELLOW);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawPolygon(xPoints, yPoints, 3);
                    break;
                }
            }
        } else if (secondClick == -1) {
            // Highlight the second click position
            // but only considering validActions for which from = firstClick
            for (MovePiece action : validActions) {
                int fromPoint = currentPlayer == 0 ? firstClick - 1 : 24 - firstClick;
                if (firstClick == 25 && currentPlayer == 0 || firstClick == 0 && currentPlayer == 1) {
                    fromPoint = -1; // Bar
                }
                if (action.from == fromPoint && action.to == clickPlayerPerspective) {
                    g2d.setColor(Color.RED);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawPolygon(xPoints, yPoints, 3);
                    break;
                }
            }
            if (i == firstClick - 1) {
                // also keep yellow if we have clicked on a triangle
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawPolygon(xPoints, yPoints, 3);
            }
        }
    }


    private void drawDice(Graphics2D g2d, int centerX, int centerY) {
        int dieSize = 40; // Size of each die
        int dieMargin = 10; // Margin between dice

        for (int i = 0; i < diceValues.length; i++) {
            int x = centerX - (diceValues.length * (dieSize + dieMargin)) / 2 + i * (dieSize + dieMargin);
            int y = centerY - dieSize / 2;

            // Set color based on whether the die has been used
            g2d.setColor(diceUsed[i] ? Color.LIGHT_GRAY : Color.WHITE);
            g2d.fillRoundRect(x, y, dieSize, dieSize, 10, 10);

            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(x, y, dieSize, dieSize, 10, 10);

            // Draw the die face
            drawDieFace(g2d, x, y, dieSize, diceValues[i]);
        }
    }

    private void drawDieFace(Graphics2D g2d, int x, int y, int size, int value) {
        int dotSize = size / 6; // Size of the dots
        int offset = size / 4; // Offset for the dots from the center

        g2d.setColor(Color.BLACK);

        // Draw dots based on the die value
        if (value == 1 || value == 3 || value == 5) {
            g2d.fillOval(x + size / 2 - dotSize / 2, y + size / 2 - dotSize / 2, dotSize, dotSize);
        }
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
}