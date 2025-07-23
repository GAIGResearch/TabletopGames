package games.XIIScripta;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import games.backgammon.*;

import static java.util.stream.Collectors.toList;

public class XIIBoardView extends BGBoardView {

    protected final int squareSize = 50;
    protected final int verticalGap = squareSize; // Add vertical gap between rows

    public XIIBoardView(BGForwardModel model) {
        super(model);
        boardWidth = 900;
        boardHeight = 500;
        margin = 30;
        this.setPreferredSize(new Dimension(boardWidth, boardHeight));
        forwardModel = model;
        piecesPerPoint = new int[2][38]; // 2 players, 38 spaces (1-36, bar, bearing off)

        this.removeMouseListener(this.getMouseListeners()[0]); // Remove  existing mouse listener (from BGBoardView)

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
                System.out.printf("Clicked at (%d, %d), mapped to point %d%n", x, y, space);

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
                piecesPerPoint[player][guiSpace] = state.getPiecesOnPoint(player, gameStateSpace);
            }
            // Bar and bearing off zones
            piecesPerPoint[player][0] = state.getPiecesOnBar(player);
            piecesPerPoint[player][37] = state.getPiecesBorneOff(player);
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
            int player = piecesPerPoint[0][space] > 0 ? 0 : (piecesPerPoint[1][space] > 0 ? 1 : -1);
            int[] pos = getSpacePosition(space);
            drawSquare(g2d, pos[0], pos[1], space);
            if (player == -1) continue;
            int numDiscs = piecesPerPoint[player][space];
            drawDiscs(g2d, pos[0], pos[1], numDiscs, player, false);
        }

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

}
