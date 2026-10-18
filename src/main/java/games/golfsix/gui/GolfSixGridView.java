package games.golfsix.gui;

import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.golfsix.GolfSixParameters;
import games.tricktaking.gui.CardArt;

import javax.swing.*;
import java.awt.*;

import static games.golfsix.GolfSixParameters.COLUMNS;
import static games.golfsix.GolfSixParameters.GRID_SIZE;

/**
 * One player's grid of cards, each labelled with its position, and a status line underneath.
 */
public class GolfSixGridView extends JComponent {

    static final int gap = 8, border = 6, statusHeight = 22;

    final int playerId;
    PartialObservableDeck<FrenchCard> grid;
    boolean showFaceDown;
    String status = "";

    public GolfSixGridView(int playerId) {
        this.playerId = playerId;
    }

    /**
     * Refresh from the state. With showFaceDown the face-down cards are drawn face-up too.
     */
    public void update(PartialObservableDeck<FrenchCard> grid, boolean showFaceDown, String status) {
        this.grid = grid;
        this.showFaceDown = showFaceDown;
        this.status = status;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // the content sits inside the (titled) border
        Insets insets = getInsets();
        g2.translate(insets.left, insets.top);
        Dimension content = contentSize();
        // a dark panel behind the cards, so the status line is legible against the table
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(0, 0, content.width, content.height, 12, 12);
        if (grid == null) return;
        for (int pos = 0; pos < GRID_SIZE; pos++) {
            Rectangle rect = cardRect(pos);
            // a face-down card is face-down for everyone, its owner included
            boolean faceUp = grid.isComponentVisible(pos, playerId);
            if (faceUp || showFaceDown)
                CardArt.drawCardAt(g2, grid.get(pos), rect);
            else
                g2.drawImage(CardArt.backOfCard(), rect.x, rect.y, rect.width, rect.height, null);
            if (!faceUp && showFaceDown) {
                // shown only because the display is fully observable: shade it so it still reads as face-down
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
            // the position, as the ReplaceCard and TurnUp actions name it
            g2.setColor(new Color(20, 20, 20, 200));
            g2.fillOval(rect.x + 2, rect.y + rect.height - 22, 20, 20);
            g2.setColor(Color.white);
            g2.drawString(String.valueOf(pos), rect.x + 8, rect.y + rect.height - 7);
        }
        g2.setColor(Color.white);
        g2.drawString(status, border, content.height - 7);
        g2.translate(-insets.left, -insets.top);
    }

    private Rectangle cardRect(int position) {
        int column = position % COLUMNS;
        int row = position / COLUMNS;
        return new Rectangle(border + column * (CardArt.cardWidth + gap), border + row * (CardArt.cardHeight + gap),
                CardArt.cardWidth, CardArt.cardHeight);
    }

    private Dimension contentSize() {
        return new Dimension(2 * border + COLUMNS * CardArt.cardWidth + (COLUMNS - 1) * gap,
                2 * border + 2 * CardArt.cardHeight + gap + statusHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension content = contentSize();
        Insets insets = getInsets();
        return new Dimension(content.width + insets.left + insets.right, content.height + insets.top + insets.bottom);
    }

    // GridBagLayout and FlowLayout fall back to the minimum size when an area is too small
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /**
     * The points shown by the face-up cards: a column with both cards face-up counts as it will be scored, and a
     * single face-up card counts its value.
     */
    static int faceUpScore(PartialObservableDeck<FrenchCard> grid, int playerId, GolfSixParameters params) {
        int score = 0;
        for (int column = 0; column < COLUMNS; column++) {
            boolean topUp = grid.isComponentVisible(column, playerId);
            boolean bottomUp = grid.isComponentVisible(column + COLUMNS, playerId);
            FrenchCard top = grid.get(column), bottom = grid.get(column + COLUMNS);
            if (topUp && bottomUp && top.number == bottom.number)
                continue;
            if (topUp) score += params.cardValue(top);
            if (bottomUp) score += params.cardValue(bottom);
        }
        return score;
    }
}
