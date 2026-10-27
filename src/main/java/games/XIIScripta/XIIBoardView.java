package games.XIIScripta;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import games.backgammon.*;
import games.backgammon.actions.MovePiece;
import games.backgammon.gui.BGBoardView;

import static java.util.stream.Collectors.toList;

public class XIIBoardView extends BGBoardView {

    protected final int squareSize = 50;
    protected final int verticalGap = squareSize; // Add vertical gap between rows

    // Stone / Roman-board palette
    private static final Color STONE_BG = new Color(196, 190, 176);      // weathered stone tablet
    private static final Color STONE_SQUARE = new Color(221, 216, 204);  // lighter inset square
    private static final Color STONE_BORDER = new Color(120, 114, 100);  // carved grout line
    private static final Color GROUP_DIVIDER = new Color(88, 82, 70);    // heavy line between groups of six
    private static final Color TRAY_STONE = new Color(184, 177, 162);    // off-board trays

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
                int vcol = (x - margin) / squareSize; // visual column index from the left margin

                // Bar zone (column 0, left of middle row)
                if (vcol == 0 && y > margin + squareSize + verticalGap && y < margin + 2 * squareSize + verticalGap) {
                    space = 0;
                }
                // Bearing off zone (right of bottom row)
                else {
                    boolean onBoard = y < margin + 3 * (squareSize + verticalGap);
                    if (x >= margin + 14 * squareSize && y > margin + 2 * (squareSize + verticalGap) && onBoard) {
                        space = 37;
                    }
                    // Board squares (visual columns 1-6 and 8-13; column 7 is the rosette gap)
                    else if (x >= margin + squareSize && x < margin + 14 * squareSize && vcol != 7) {
                        col = vcol <= 6 ? vcol : vcol - 1; // logical box column 1..12
                        // Top row (spaces 13-24, right to left)
                        if (y > margin && y < margin + squareSize) {
                            space = 25 - col;
                        }
                        // Middle row (spaces 1-12, left to right)
                        else if (y > margin + squareSize + verticalGap && y < margin + 2 * squareSize + verticalGap) {
                            space = col;
                        }
                        // Bottom row (spaces 25-36, left to right)
                        else if (y > margin + 2 * (squareSize + verticalGap) && onBoard) {
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Board background (weathered stone tablet)
        g2d.setColor(STONE_BG);
        g2d.fillRect(0, 0, boardWidth, boardHeight);

        // Draw all the squares first
        for (int space = 1; space <= 36; space++) {
            int[] pos = getSpacePosition(space);
            drawSquare(g2d, pos[0], pos[1], space);
        }

        // Frame each row's two groups of six and mark the divide with a rosette
        drawGroupDividers(g2d);

        // Direction-of-travel arrows (under the discs)
        drawDirectionArrows(g2d);

        // Draw the discs, centred on each square
        for (int space = 1; space <= 36; space++) {
            int player = piecesPerPoint[0][space] > 0 ? 0 : (piecesPerPoint[1][space] > 0 ? 1 : -1);
            if (player == -1) continue;
            int[] pos = getSpacePosition(space);
            drawDiscs(g2d, pos[0], pos[1], piecesPerPoint[player][space], player, false);
        }

        // Draw the bar (entry) and bear-off trays
        drawBarAndBearOff(g2d);

        // Draw dice in the center
        drawDice(g2d, boardWidth / 2, boardHeight * 3 / 4);

        // Number the spaces (drawn over the discs so they stay legible)
        g2d.setColor(Color.BLACK);
        for (int space = 1; space <= 36; space++) {
            int[] pos = getSpacePosition(space);
            String text = String.valueOf(37-space);
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
        g2d.setColor(highlightRed ? new Color(196, 92, 78) : (highlight ? new Color(214, 196, 120) : STONE_SQUARE));
        g2d.fillRect(x, y, squareSize, squareSize);
        g2d.setColor(STONE_BORDER);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(x, y, squareSize, squareSize);
    }

    private int[] getSpacePosition(int space) {
        // Returns [x, y] for the top-left of the square for a given space, with vertical gaps
        int midY = margin + squareSize + verticalGap;
        int topY = margin;
        int botY = margin + 2 * (squareSize + verticalGap);
        if (space == 0) // bar
            return new int[]{margin, midY};
        if (space == 37) // bearing off (right of the rosette-widened bottom row)
            return new int[]{columnX(13), botY};
        if (space >= 1 && space <= 12)
            return new int[]{columnX(space), midY};
        if (space >= 13 && space <= 24)
            return new int[]{columnX(25 - space), topY};
        if (space >= 25 && space <= 36)
            return new int[]{columnX(space - 24), botY};
        return new int[]{0, 0};
    }

    // x of the left edge of a box, given its logical column (1..12) within a row. The rosette
    // occupies its own column between the two groups of six, so columns 7-12 are pushed one
    // place to the right to leave a clean 6 - rosette - 6 layout.
    private int columnX(int col) {
        int visualCol = col <= 6 ? col : col + 1;
        return margin + visualCol * squareSize;
    }

    // Centre of a square, given its space index
    private int[] getSpaceCentre(int space) {
        int[] pos = getSpacePosition(space);
        return new int[]{pos[0] + squareSize / 2, pos[1] + squareSize / 2};
    }

    /**
     * Draw a stack of pieces as overlapping "saucers", centred on the square. The
     * {@code x}/{@code yStart} passed in are the top-left of the square.
     */
    @Override
    protected void drawDiscs(Graphics2D g2d, int x, int yStart, int numDiscs, int player, boolean fromTop) {
        int cx = x + squareSize / 2;                    // horizontal centre of the square
        int cy = yStart + (squareSize - 12) / 2;        // biased up a little to leave room for the number
        drawSaucerPile(g2d, cx, cy, squareSize - 16, numDiscs, player);
    }

    /**
     * Draw {@code numDiscs} pieces as overlapping "saucers" (flattened ellipses), centred on
     * ({@code cx}, {@code cy}) and tightly stacked within {@code maxExtent} pixels vertically.
     * Stacks too tall to fit are capped and labelled with the total count.
     */
    private void drawSaucerPile(Graphics2D g2d, int cx, int cy, int maxExtent, int numDiscs, int player) {
        if (numDiscs <= 0) return;

        int discWidth = squareSize - 16;        // wide, saucer-like
        int discHeight = 14;                    // ...and flat
        int idealStep = 7;                      // gap between stacked saucers

        int step = idealStep;
        int drawn = numDiscs;
        if (numDiscs > 1 && discHeight + (numDiscs - 1) * step > maxExtent) {
            step = Math.max(1, (maxExtent - discHeight) / (numDiscs - 1));
            if (step < 4) {                     // too compressed: cap and show a count instead
                step = 4;
                drawn = Math.max(1, 1 + (maxExtent - discHeight) / step);
            }
        }
        boolean showCount = drawn < numDiscs;

        int stackHeight = discHeight + (drawn - 1) * step;
        int topY = cy - stackHeight / 2;

        Color faceColor = player == 0 ? Color.WHITE : new Color(45, 45, 45);
        Color rimColor = player == 0 ? new Color(120, 120, 120) : Color.BLACK;

        // Draw bottom saucer first so higher saucers overlap the ones below
        for (int i = 0; i < drawn; i++) {
            int dx = cx - discWidth / 2;
            int dy = topY + (drawn - 1 - i) * step;
            g2d.setColor(faceColor);
            g2d.fillOval(dx, dy, discWidth, discHeight);
            g2d.setStroke(new BasicStroke(1));
            g2d.setColor(rimColor);
            g2d.drawOval(dx, dy, discWidth, discHeight);
            // inner ring to give the saucer some depth
            g2d.drawOval(dx + discWidth / 6, dy + discHeight / 4, discWidth * 2 / 3, discHeight / 2);
        }

        if (showCount) {
            String count = String.valueOf(numDiscs);
            Font old = g2d.getFont();
            g2d.setFont(old.deriveFont(Font.BOLD, 13f));
            int tw = g2d.getFontMetrics().stringWidth(count);
            int ty = topY + (discHeight + g2d.getFontMetrics().getAscent()) / 2 - 1;
            g2d.setColor(player == 0 ? Color.BLACK : Color.WHITE);
            g2d.drawString(count, cx - tw / 2, ty);
            g2d.setFont(old);
        }
    }

    /**
     * Draw the bar (entry) tray at the far left of the middle row and the bear-off tray to the
     * right of the bottom row. Each tray shows both players' piles (player 0 above, player 1
     * below) since both can occupy them at the same time.
     */
    private void drawBarAndBearOff(Graphics2D g2d) {
        int[] bar = getSpacePosition(0);
        int[] off = getSpacePosition(37);
        drawTray(g2d, bar[0], bar[1], "BAR", piecesPerPoint[0][0], piecesPerPoint[1][0], null);

        // Highlight the OFF tray when bearing-off moves are available, matching regular square logic
        Color offHighlight = null;
        if (firstClick == -1) {
            for (MovePiece action : validActions) {
                if (action.to == -1) { offHighlight = new Color(214, 196, 120); break; }
            }
        } else {
            int fromGs = guiToGameStateSpace(firstClick);
            for (MovePiece action : validActions) {
                if (action.from == fromGs && action.to == -1) { offHighlight = new Color(196, 92, 78); break; }
            }
        }
        drawTray(g2d, off[0], off[1], "OFF", piecesPerPoint[0][37], piecesPerPoint[1][37], offHighlight);
    }

    private void drawTray(Graphics2D g2d, int x, int y, String label, int p0, int p1, Color highlight) {
        g2d.setColor(highlight != null ? highlight : TRAY_STONE);
        g2d.fillRect(x, y, squareSize, squareSize);
        g2d.setColor(STONE_BORDER);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(x, y, squareSize, squareSize);

        int cx = x + squareSize / 2;
        int half = squareSize / 2 - 6;
        drawSaucerPile(g2d, cx, y + squareSize / 4, half, p0, 0);          // player 0 (white), upper half
        drawSaucerPile(g2d, cx, y + 3 * squareSize / 4, half, p1, 1);      // player 1 (black), lower half

        // Label just above the tray
        Font old = g2d.getFont();
        g2d.setFont(old.deriveFont(Font.BOLD, 11f));
        g2d.setColor(new Color(90, 70, 40));
        int tw = g2d.getFontMetrics().stringWidth(label);
        g2d.drawString(label, cx - tw / 2, y - 4);
        g2d.setFont(old);
    }

    // Draws the two connector arrows that show how movement wraps between the rows.
    private void drawDirectionArrows(Graphics2D g2d) {
        g2d.setColor(new Color(50, 90, 200, 210));

        int[] topRow = getSpacePosition(13);
        int[] midRow = getSpacePosition(1);

        int x12 = getSpaceCentre(12)[0];
        int topCentreY = getSpaceCentre(13)[1];
        int botCentreY = getSpaceCentre(25)[1];

        // Connector up the right side (space 12 -> 13)
        drawArrow(g2d, x12, midRow[1] - 2, x12, topRow[1] + squareSize + 2);

        // Connector down the left side (space 24 -> 25), routed through the left margin so it
        // clears the bar tray that sits at the left of the middle row
        int leftX = margin / 2;
        int square24LeftX = getSpacePosition(24)[0];
        int square25LeftX = getSpacePosition(25)[0];
        drawArrowPath(g2d,
                new int[]{square24LeftX, leftX, leftX, square25LeftX},
                new int[]{topCentreY, topCentreY, botCentreY, botCentreY});
    }

    /**
     * Frame each row's two groups of six squares and place a Roman-style rosette on the divide,
     * so the line of twelve clearly reads as two sixes (as on the carved boards).
     */
    private void drawGroupDividers(Graphics2D g2d) {
        int leftStart = margin + squareSize;            // x of the first square in a row
        int rightStart = margin + 8 * squareSize;       // x of the seventh square (past the rosette gap)
        int rosetteCx = margin + 7 * squareSize + squareSize / 2;  // centre of the rosette column
        int[] rowTops = {getSpacePosition(13)[1], getSpacePosition(1)[1], getSpacePosition(25)[1]};

        for (int yTop : rowTops) {
            g2d.setColor(GROUP_DIVIDER);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(leftStart, yTop, 6 * squareSize, squareSize);   // left six
            g2d.drawRect(rightStart, yTop, 6 * squareSize, squareSize);  // right six
            drawRosette(g2d, rosetteCx, yTop + squareSize / 2, squareSize / 2 - 4);
        }
    }

    // A simple four-pointed Roman rosette, as carved between the groups of six.
    private void drawRosette(Graphics2D g2d, int cx, int cy, int r) {
        g2d.setColor(new Color(236, 232, 221));
        g2d.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g2d.setColor(GROUP_DIVIDER);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(cx - r, cy - r, 2 * r, 2 * r);

        int o = r - 3;          // distance to the star's points
        int i = Math.max(2, r / 3);   // distance to the star's waist
        int[] xs = {cx, cx + i, cx + o, cx + i, cx, cx - i, cx - o, cx - i};
        int[] ys = {cy - o, cy - i, cy, cy + i, cy + o, cy + i, cy, cy - i};
        g2d.setColor(new Color(128, 118, 98));
        g2d.fillPolygon(xs, ys, 8);

        g2d.setColor(new Color(236, 232, 221));
        g2d.fillOval(cx - 2, cy - 2, 4, 4);
    }

    // Straight arrow from (x1,y1) to (x2,y2)
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        drawArrowPath(g2d, new int[]{x1, x2}, new int[]{y1, y2});
    }

    // Poly-line arrow with a head on the final segment
    private void drawArrowPath(Graphics2D g2d, int[] xs, int[] ys) {
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < xs.length - 1; i++)
            g2d.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        int n = xs.length;
        double ang = Math.atan2(ys[n - 1] - ys[n - 2], xs[n - 1] - xs[n - 2]);
        int head = 12;
        int xa = (int) (xs[n - 1] - head * Math.cos(ang - Math.PI / 7));
        int ya = (int) (ys[n - 1] - head * Math.sin(ang - Math.PI / 7));
        int xb = (int) (xs[n - 1] - head * Math.cos(ang + Math.PI / 7));
        int yb = (int) (ys[n - 1] - head * Math.sin(ang + Math.PI / 7));
        g2d.fillPolygon(new int[]{xs[n - 1], xa, xb}, new int[]{ys[n - 1], ya, yb}, 3);
    }

}
