package games.monopoly.gui;

import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.monopoly.*;
import games.monopoly.actions.*;
import games.monopoly.components.MonopolyCard;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The board: the 40 squares round the edge, each with its owner, buildings and whether it is mortgaged, and the
 * players' tokens. In the middle, the last roll of the dice, the decision being made, and the card piles.
 */
public class MonopolyBoardView extends JComponent {

    static final int CORNER = 84;
    static final int CELL = 56;
    static final int SIZE = 2 * CORNER + 9 * CELL;
    static final int BAND = 15;
    static final int TOKEN = 14;
    static final Color[] PLAYER_COLOURS = {
            new Color(220, 40, 40), new Color(40, 90, 220), new Color(245, 200, 30), new Color(40, 160, 60),
            new Color(150, 60, 190), new Color(240, 130, 30), new Color(30, 190, 190), new Color(120, 120, 120)
    };
    static final Color BOARD = new Color(205, 230, 208);
    static final Color MORTGAGED = new Color(90, 90, 90, 110);

    private MonopolyGameState state;
    // the top card of each pile (Chance, then Community Chest) at the last update, and the card drawn last. The state
    // does not record which card was drawn last, and the bottom of a pile is not known until a card has gone back
    private final MonopolyCard[] top = new MonopolyCard[2];
    private final MonopolyCard[] lastDrawn = new MonopolyCard[2];

    public MonopolyBoardView() {
        Dimension size = new Dimension(SIZE, SIZE);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    public void update(MonopolyGameState state) {
        this.state = state;
        List<Deck<MonopolyCard>> piles = List.of(state.getChanceDeck(), state.getCommunityChestDeck());
        for (int i = 0; i < 2; i++) {
            MonopolyCard now = piles.get(i).peek();
            // the top card has changed, so the old top card has been drawn (unless a new game has begun)
            if (top[i] != null && !top[i].equals(now))
                lastDrawn[i] = state.getTurnCounter() == 0 && state.getRoundCounter() == 0 ? null : top[i];
            top[i] = now;
        }
        repaint();
    }

    static Color playerColour(int player) {
        return PLAYER_COLOURS[player % PLAYER_COLOURS.length];
    }

    /**
     * The square's cell on the board: GO in the bottom right corner, then clockwise (left along the bottom, up the
     * left side, right along the top and down the right side). The layout is for a board of 40 squares, 10 to a side.
     */
    static Rectangle cell(int index) {
        int side = index / 10, k = index % 10;
        int far = SIZE - CORNER;
        if (k == 0)
            return switch (side) {
                case 0 -> new Rectangle(far, far, CORNER, CORNER);
                case 1 -> new Rectangle(0, far, CORNER, CORNER);
                case 2 -> new Rectangle(0, 0, CORNER, CORNER);
                default -> new Rectangle(far, 0, CORNER, CORNER);
            };
        int along = CORNER + (k - 1) * CELL;
        return switch (side) {
            case 0 -> new Rectangle(far - k * CELL, far, CELL, CORNER);
            case 1 -> new Rectangle(0, far - k * CELL, CORNER, CELL);
            case 2 -> new Rectangle(along, 0, CELL, CORNER);
            default -> new Rectangle(far, along, CORNER, CELL);
        };
    }

    /**
     * The street's colour band, on the edge of its cell nearest the middle of the board.
     */
    static Rectangle band(int index) {
        Rectangle c = cell(index);
        return switch (index / 10) {
            case 0 -> new Rectangle(c.x, c.y, c.width, BAND);
            case 1 -> new Rectangle(c.x + c.width - BAND, c.y, BAND, c.height);
            case 2 -> new Rectangle(c.x, c.y + c.height - BAND, c.width, BAND);
            default -> new Rectangle(c.x, c.y, BAND, c.height);
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (state == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(BOARD);
        g2.fillRect(0, 0, SIZE, SIZE);
        MonopolyBoard board = state.getBoard();
        for (MonopolySquare s : board.squares())
            drawSquare(g2, s, board.currency());
        drawTokens(g2, board);
        drawCentre(g2, board.currency());
        g2.setColor(Color.BLACK);
        g2.drawRect(0, 0, SIZE - 1, SIZE - 1);
    }

    private void drawSquare(Graphics2D g2, MonopolySquare s, String currency) {
        Rectangle c = cell(s.index());
        g2.setColor(new Color(250, 250, 242));
        g2.fillRect(c.x, c.y, c.width, c.height);
        Rectangle text = new Rectangle(c);
        if (s.type() == SquareType.STREET) {
            Rectangle b = band(s.index());
            g2.setColor(Color.decode(s.group().colour()));
            g2.fill(b);
            g2.setColor(Color.BLACK);
            g2.draw(b);
            drawBuildings(g2, b, state.getBuildings(s));
            text = remainder(c, b);
        }
        int owner = s.type().isProperty() ? state.getOwner(s) : -1;
        // the owner's colour as a frame inside the cell
        if (owner != -1) {
            g2.setColor(playerColour(owner));
            g2.setStroke(new BasicStroke(4));
            g2.drawRect(text.x + 2, text.y + 2, text.width - 4, text.height - 4);
            g2.setStroke(new BasicStroke(1));
        }
        g2.setColor(Color.BLACK);
        List<String> lines = new ArrayList<>(wrap(g2, label(s), text.width - 8, 9f));
        String value = s.type().isProperty() ? currency + s.price()
                : s.tax() > 0 ? "Pay " + currency + s.tax() : "";
        drawLines(g2, lines, value, text);
        if (owner != -1 && state.isMortgaged(s)) {
            g2.setColor(MORTGAGED);
            g2.fillRect(c.x, c.y, c.width, c.height);
            g2.setColor(Color.WHITE);
            float size = 11f;
            g2.setFont(getFont().deriveFont(Font.BOLD, size));
            while (g2.getFontMetrics().stringWidth("MORTGAGED") > c.width - 6)
                g2.setFont(getFont().deriveFont(Font.BOLD, size -= 0.5f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("MORTGAGED", c.x + (c.width - fm.stringWidth("MORTGAGED")) / 2,
                    c.y + c.height / 2 + 4);
        }
        g2.setColor(Color.BLACK);
        g2.drawRect(c.x, c.y, c.width, c.height);
    }

    /**
     * The name shown on the square: the board's name, without the number that tells two Chance or Community Chest
     * squares apart.
     */
    static String label(MonopolySquare s) {
        return switch (s.type()) {
            case CHANCE -> "Chance";
            case COMMUNITY_CHEST -> "Community Chest";
            case JAIL -> "Jail / Just Visiting";
            default -> s.name();
        };
    }

    private static Rectangle remainder(Rectangle cell, Rectangle band) {
        if (band.width == cell.width)
            return new Rectangle(cell.x, band.y == cell.y ? cell.y + BAND : cell.y, cell.width, cell.height - BAND);
        return new Rectangle(band.x == cell.x ? cell.x + BAND : cell.x, cell.y, cell.width - BAND, cell.height);
    }

    /**
     * Houses as small green squares in the band, or a hotel as a red bar.
     */
    private static void drawBuildings(Graphics2D g2, Rectangle b, int n) {
        if (n == 0) return;
        boolean across = b.width > b.height;
        if (n == MonopolyParameters.HOTEL) {
            g2.setColor(new Color(200, 20, 20));
            Rectangle h = across ? new Rectangle(b.x + b.width / 2 - 14, b.y + 3, 28, b.height - 6)
                    : new Rectangle(b.x + 3, b.y + b.height / 2 - 14, b.width - 6, 28);
            g2.fill(h);
            g2.setColor(Color.BLACK);
            g2.draw(h);
            return;
        }
        int size = BAND - 6, gap = 3;
        int length = n * size + (n - 1) * gap;
        for (int i = 0; i < n; i++) {
            int x = across ? b.x + (b.width - length) / 2 + i * (size + gap) : b.x + 3;
            int y = across ? b.y + 3 : b.y + (b.height - length) / 2 + i * (size + gap);
            g2.setColor(new Color(20, 150, 40));
            g2.fillRect(x, y, size, size);
            g2.setColor(Color.BLACK);
            g2.drawRect(x, y, size, size);
        }
    }

    private void drawLines(Graphics2D g2, List<String> lines, String value, Rectangle r) {
        FontMetrics fm = g2.getFontMetrics();
        int y = r.y + fm.getAscent() + 4;
        for (String line : lines) {
            g2.drawString(line, r.x + (r.width - fm.stringWidth(line)) / 2, y);
            y += fm.getHeight() - 1;
        }
        if (!value.isEmpty()) {
            g2.setFont(getFont().deriveFont(Font.BOLD, 9f));
            fm = g2.getFontMetrics();
            g2.drawString(value, r.x + (r.width - fm.stringWidth(value)) / 2, r.y + r.height - 6);
        }
    }

    /**
     * Splits the text into lines no wider than the width, at a font size from the given one down to 7 points, so
     * that each word fits. Sets the font on g2.
     */
    private List<String> wrap(Graphics2D g2, String text, int width, float size) {
        String[] words = text.split(" ");
        for (float s = size; ; s -= 0.5f) {
            g2.setFont(getFont().deriveFont(Font.PLAIN, s));
            FontMetrics fm = g2.getFontMetrics();
            boolean fits = true;
            for (String w : words)
                if (fm.stringWidth(w) > width) fits = false;
            if (!fits && s > 7f) continue;
            List<String> lines = new ArrayList<>();
            String line = "";
            for (String w : words) {
                String next = line.isEmpty() ? w : line + " " + w;
                if (fm.stringWidth(next) > width && !line.isEmpty()) {
                    lines.add(line);
                    line = w;
                } else
                    line = next;
            }
            lines.add(line);
            return lines;
        }
    }

    /**
     * Each player's token as a numbered disc, side by side in the square's cell. In Jail, a token has bars across
     * it (Just Visiting tokens on the same square have none).
     */
    private void drawTokens(Graphics2D g2, MonopolyBoard board) {
        int[] onSquare = new int[board.nSquares()];
        g2.setFont(getFont().deriveFont(Font.BOLD, 10f));
        FontMetrics fm = g2.getFontMetrics();
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (state.isBankrupt(p)) continue;
            int index = state.getPosition(p).index();
            Rectangle c = cell(index);
            Rectangle r = state.getBoard().square(index).type() == SquareType.STREET ? remainder(c, band(index)) : c;
            // in rows upwards from just above the price, clear of the name where the cell has room
            int i = onSquare[index]++;
            int perRow = Math.max(1, (r.width - 6) / (TOKEN + 2));
            int x = r.x + 4 + (i % perRow) * (TOKEN + 2);
            int y = r.y + r.height - 18 - TOKEN - (i / perRow) * (TOKEN + 2);
            g2.setColor(playerColour(p));
            g2.fillOval(x, y, TOKEN, TOKEN);
            g2.setColor(Color.BLACK);
            g2.drawOval(x, y, TOKEN, TOKEN);
            String n = String.valueOf(p);
            g2.setColor(p == 2 ? Color.BLACK : Color.WHITE);
            g2.drawString(n, x + (TOKEN - fm.stringWidth(n)) / 2, y + TOKEN - 4);
            if (state.isInJail(p)) {
                g2.setColor(Color.BLACK);
                for (int b = 1; b <= 2; b++)
                    g2.drawLine(x + b * TOKEN / 3, y - 2, x + b * TOKEN / 3, y + TOKEN + 2);
            }
        }
    }

    private void drawCentre(Graphics2D g2, String currency) {
        int left = CORNER + 16, width = SIZE - 2 * CORNER - 32;
        int y = CORNER + 34;
        g2.setColor(new Color(200, 30, 30));
        g2.setFont(getFont().deriveFont(Font.BOLD, 30f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("MONOPOLY", left + (width - fm.stringWidth("MONOPOLY")) / 2, y);

        // the dice of the last roll
        y += 20;
        int[] dice = state.getDice();
        int die = 40;
        int dx = left + width / 2 - die - 6;
        for (int d = 0; d < 2; d++)
            if (dice[d] > 0)
                drawDie(g2, dx + d * (die + 12), y, die, dice[d]);
        y += die + 26;

        g2.setColor(Color.BLACK);
        g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
        for (String line : wrap(g2, decisionText(currency), width, 13f)) {
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g2.drawString(line, left, y);
            y += 17;
        }
        y += 10;
        y = drawPile(g2, "Chance", state.getChanceDeck().getSize(), lastDrawn[0], left, y, width);
        drawPile(g2, "Community Chest", state.getCommunityChestDeck().getSize(), lastDrawn[1], left, y + 8, width);
    }

    private static void drawDie(Graphics2D g2, int x, int y, int size, int value) {
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, size, size, 8, 8);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(x, y, size, size, 8, 8);
        int p = size / 4, r = 7;
        // pips by position: 0 1 2 / 3 4 5 / 6 7 8
        int[][] pips = {{}, {4}, {0, 8}, {0, 4, 8}, {0, 2, 6, 8}, {0, 2, 4, 6, 8}, {0, 2, 3, 5, 6, 8}};
        for (int pip : pips[value])
            g2.fillOval(x + p * (1 + pip % 3) - r / 2, y + p * (1 + pip / 3) - r / 2, r, r);
    }

    private int drawPile(Graphics2D g2, String name, int size, MonopolyCard last, int left, int y, int width) {
        g2.setColor(Color.BLACK);
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g2.drawString(name + " (" + size + " cards)", left, y);
        y += 16;
        if (last != null) {
            for (String line : wrap(g2, "Last drawn: " + last.text, width, 11f)) {
                g2.drawString(line, left, y);
                y += 14;
            }
        }
        return y;
    }

    /**
     * What is being decided now, and by whom.
     */
    private String decisionText(String currency) {
        if (!state.isNotTerminal()) {
            for (int p = 0; p < state.getNPlayers(); p++)
                if (state.getOrdinalPosition(p) == 1)
                    return "Game over. Player " + p + " wins.";
            return "Game over";
        }
        int p = state.getCurrentPlayer();
        IExtendedSequence pending = state.currentActionInProgress();
        if (pending instanceof Auction a) {
            String high = a.getHighBidder() == -1 ? "no bids yet"
                    : "high bid " + currency + a.getHighBid() + " by Player " + a.getHighBidder();
            return "Auction of " + a.square.name() + ": " + high + ". Player " + p + " to bid or pass.";
        }
        if (pending instanceof RaiseMoney r)
            return "Player " + r.player + " must raise " + currency + r.amount + " to pay "
                    + (r.creditor == -1 ? "the Bank" : "Player " + r.creditor) + ".";
        if (pending instanceof BuyDecision b)
            return "Player " + p + " may buy " + b.square.name() + " for " + currency + b.square.price() + ".";
        if (pending instanceof PayOrChance c)
            return "Player " + p + " pays " + currency + c.amount + " or takes a Chance.";
        if (pending instanceof IncomeTaxChoice t)
            return "Player " + p + " pays " + t.square.name() + ": " + currency + t.square.tax() + " or a percentage.";
        if (state.getGamePhase() == MonopolyGamePhase.ROLL)
            return "Player " + p + " to roll" + (state.isInJail(p) ? " (in Jail)." : ".");
        return "Player " + p + " may build, sell, mortgage or unmortgage, then "
                + (state.hasAnotherRoll() ? "roll again (doubles)." : "end the turn.");
    }
}
