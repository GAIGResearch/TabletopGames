package games.XIIScripta;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;

import games.backgammon.BGGameState;
import games.backgammon.MovePiece;
import games.backgammon.BGForwardModel;

import static java.util.stream.Collectors.toList;

public class XIIBoardView extends JComponent {

    private final int boardWidth = 900;
    private final int boardHeight = 500;
    private final int squareSize = 50;
    private final int margin = 30;
    private final int discRadius = 20;
    private final int discMargin = 5;
    private final int verticalGap = squareSize; // Add vertical gap between rows

    private int[][] piecesPerSpace = new int[2][38]; // [player][space], 1-36, 0=bar, 37=bearing off
    private int[] piecesOnBar = new int[2];
    private int[] piecesBorneOff = new int[2];
    private int[] diceValues = new int[2];
    private BGForwardModel forwardModel;
    private List<MovePiece> validActions = new ArrayList<>();

    int firstClick = -1;
    int secondClick = -1;

    // Helper to convert GUI space to game state space (reverse mapping)
    private int guiToGameStateSpace(int guiSpace) {
        if (guiSpace >= 1 && guiSpace <= 36) {
            return 37 - guiSpace;
        }
        if (guiSpace == 37)
            return -1;
        // Bar and bearing off zones remain unchanged
        return guiSpace;
    }

    public XIIBoardView(BGForwardModel model) {
        this.setPreferredSize(new Dimension(boardWidth, boardHeight));
        forwardModel = model;

        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                int x = evt.getX();
                int y = evt.getY();
                int col, space = -1;

                // Bar zone (left of middle row)
                if (x < margin + squareSize && y > margin + squareSize + verticalGap && y < margin + 2 * squareSize + verticalGap) {
                    space = 0;
                }
                // Bearing off zone (right of bottom row)
                else {
                    boolean onBoard = y < margin + 3 * (squareSize + verticalGap);
                    if (x > margin + 13 * squareSize && y > margin + 2 * (squareSize + verticalGap) && onBoard) {
                        space = 37;
                    }
                    // Board squares
                    else if (x >= margin + squareSize && x < margin + 13 * squareSize) {
                        // Top row (spaces 13-24, right to left)
                        if (y > margin && y < margin + squareSize) {
                            col = 12 - ((x - margin) / squareSize);
                            space = 13 + col;
                        }
                        // Middle row (spaces 1-12, left to right)
                        else if (y > margin + squareSize + verticalGap && y < margin + 2 * squareSize + verticalGap) {
                            col = (x - margin) / squareSize;
                            space = col;
                        }
                        // Bottom row (spaces 25-36, left to right)
                        else if (y > margin + 2 * (squareSize + verticalGap) && onBoard) {
                            col = (x - margin) / squareSize;
                            space = 24 + col;
                        }
                    }
                }

                if (evt.getButton() == MouseEvent.BUTTON1) {
                    if (firstClick == -1)
                        firstClick = space;
                    else
                        secondClick = space;
                } else {
                    firstClick = -1;
                    secondClick = -1;
                }
            }
        });
    }

    // When updating, map game state positions to GUI positions
    public synchronized void update(BGGameState state) {
        int nPlayers = state.getNPlayers();
        validActions = forwardModel.computeAvailableActions(state).stream()
                .filter(a -> a instanceof MovePiece)
                .map(MovePiece.class::cast)
                .collect(toList());

        for (int player = 0; player < nPlayers; player++) {
            // Map game state positions to GUI positions
            for (int guiSpace = 1; guiSpace <= 36; guiSpace++) {
                int gameStateSpace = guiToGameStateSpace(guiSpace);
                piecesPerSpace[player][guiSpace] = state.getPiecesOnPoint(player, gameStateSpace);
            }
            // Bar and bearing off zones
            piecesPerSpace[player][0] = state.getPiecesOnBar(player);
            piecesPerSpace[player][37] = state.getPiecesBorneOff(player);
            piecesOnBar[player] = state.getPiecesOnBar(player);
            piecesBorneOff[player] = state.getPiecesBorneOff(player);
        }
        diceValues = state.getAvailableDiceValues();

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Board background
        g2d.setColor(new Color(222, 184, 135));
        g2d.fillRect(0, 0, boardWidth, boardHeight);

        // Draw discs on squares
        for (int space = 1; space <= 36; space++) {
            int player = piecesPerSpace[0][space] > 0 ? 0 : (piecesPerSpace[1][space] > 0 ? 1 : -1);
            int[] pos = getSpacePosition(space);
            drawSquare(g2d, pos[0], pos[1], space);
            if (player == -1) continue;
            int numDiscs = piecesPerSpace[player][space];
            drawDiscs(g2d, pos[0], pos[1], numDiscs, player);
        }

        // Do NOT draw discs on bar or borne off zones

        // Draw dice in the center
        drawDice(g2d, boardWidth / 2, boardHeight * 3 / 4);

        // Number the spaces
        g2d.setColor(Color.BLACK);
        for (int space = 1; space <= 36; space++) {
            int[] pos = getSpacePosition(space);
            String text = String.valueOf(space);
            int tx = pos[0] + squareSize / 2 - g.getFontMetrics().stringWidth(text) / 2;
            int ty = pos[1] + squareSize - 5;
            g2d.drawString(text, tx, ty);
        }

        // Draw player bar/bear off counts at the bottom
        g2d.setColor(Color.BLACK);
        String p0Bar = "Player 0 Bar: " + piecesOnBar[0];
        String p1Bar = "Player 1 Bar: " + piecesOnBar[1];
        String p0Off = "Player 0 Borne Off: " + piecesBorneOff[0];
        String p1Off = "Player 1 Borne Off: " + piecesBorneOff[1];
        int yText = boardHeight - margin / 2;
        g2d.drawString(p0Bar, margin, yText);
        g2d.drawString(p1Bar, margin + 250, yText);
        g2d.drawString(p0Off, margin + 500, yText);
        g2d.drawString(p1Off, margin + 700, yText);
    }

    private void drawSquare(Graphics2D g2d, int x, int y, int space) {
        boolean highlight = false;
        boolean highlightRed = false;
        int gameStateSpace = guiToGameStateSpace(space);

        if (firstClick == -1) {
            for (MovePiece action : validActions) {
                if (action.from == gameStateSpace) {
                    highlight = true;
                    break;
                }
            }
        } else if (secondClick == -1) {
            int firstGameStateSpace = guiToGameStateSpace(firstClick);
            for (MovePiece action : validActions) {
                if (action.from == firstGameStateSpace && action.to == gameStateSpace) {
                    highlightRed = true;
                    break;
                }
            }
            if (space == firstClick) highlight = true;
        }
        g2d.setColor(highlightRed ? Color.RED : (highlight ? Color.YELLOW : Color.LIGHT_GRAY));
        g2d.fillRect(x, y, squareSize, squareSize);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, squareSize, squareSize);
    }

    private void drawDiscs(Graphics2D g2d, int x, int y, int numDiscs, int player) {
        for (int i = 0; i < numDiscs; i++) {
            int dx = x;
            int dy = y - i * (discRadius + discMargin);
            g2d.setColor(player == 0 ? Color.WHITE : Color.BLACK);
            g2d.fillOval(dx, dy, discRadius, discRadius);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(dx, dy, discRadius, discRadius);
        }
    }

    private int[] getSpacePosition(int space) {
        // Returns [x, y] for the top-left of the square for a given space, with vertical gaps
        if (space == 0) // bar
            return new int[]{margin, margin + squareSize + verticalGap};
        if (space == 37) // bearing off
            return new int[]{margin + 13 * squareSize, margin + 2 * (squareSize + verticalGap)};
        if (space >= 1 && space <= 12)
            return new int[]{margin + space * squareSize, margin + squareSize + verticalGap};
        if (space >= 13 && space <= 24)
            return new int[]{margin + (25 - space) * squareSize, margin};
        if (space >= 25 && space <= 36)
            return new int[]{margin + (space - 24) * squareSize, margin + 2 * (squareSize + verticalGap)};
        return new int[]{0, 0};
    }

    private void drawDice(Graphics2D g2d, int centerX, int centerY) {
        int dieSize = 40;
        int dieMargin = 10;
        for (int i = 0; i < diceValues.length; i++) {
            int x = centerX - (diceValues.length * (dieSize + dieMargin)) / 2 + i * (dieSize + dieMargin);
            int y = centerY - dieSize / 2;
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(x, y, dieSize, dieSize, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(x, y, dieSize, dieSize, 10, 10);
            drawDieFace(g2d, x, y, dieSize, diceValues[i]);
        }
    }

    private void drawDieFace(Graphics2D g2d, int x, int y, int size, int value) {
        int dotSize = size / 6;
        int offset = size / 4;
        g2d.setColor(Color.BLACK);
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
}
