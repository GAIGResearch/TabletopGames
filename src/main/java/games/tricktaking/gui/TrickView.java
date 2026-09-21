package games.tricktaking.gui;

import core.components.FrenchCard;
import games.tricktaking.Trick;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The centre of the table: the trick in progress, in the order played, with who played each card and which card is
 * winning it. The three lines of text around the cards are supplied by the game.
 */
public class TrickView extends JComponent {

    static final int gap = 10, margin = 12;
    static final int headerHeight = 44, footerHeight = 34;

    final int width, height;

    List<FrenchCard> cards = new ArrayList<>();
    List<Integer> players = new ArrayList<>();
    int winningIndex = -1;
    String header = "", leadText = "", footer = "";
    boolean empty = true;

    public TrickView(int nPlayers) {
        this(nPlayers, 360);
    }

    /**
     * @param minWidth the least the panel may be - wide enough for the longest line of text the game shows
     */
    public TrickView(int nPlayers, int minWidth) {
        this.width = Math.max(minWidth, margin * 2 + nPlayers * (CardArt.cardWidth + gap));
        this.height = headerHeight + CardArt.cardHeight + 22 + footerHeight;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(255, 255, 255, 215));
        g2.fillRoundRect(0, 0, width - 1, height - 1, 14, 14);
        g2.setColor(Color.black);
        g2.drawRoundRect(0, 0, width - 1, height - 1, 14, 14);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString(header, margin, 20);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString(leadText, margin, 37);

        int y = headerHeight;
        for (int i = 0; i < cards.size(); i++) {
            int x = margin + i * (CardArt.cardWidth + gap);
            CardArt.drawCardAt(g2, cards.get(i), new Rectangle(x, y, CardArt.cardWidth, CardArt.cardHeight));
            if (i == winningIndex) {
                g2.setColor(new Color(220, 150, 20));
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(x - 2, y - 2, CardArt.cardWidth + 4, CardArt.cardHeight + 4, 8, 8);
                g2.setStroke(new BasicStroke(1));
            }
            g2.setColor(Color.black);
            g2.setFont(g2.getFont().deriveFont(i == winningIndex ? Font.BOLD : Font.PLAIN, 12f));
            g2.drawString("Player " + players.get(i) + (i == 0 ? " (led)" : ""), x, y + CardArt.cardHeight + 16);
        }
        if (empty) {
            g2.setColor(Color.darkGray);
            g2.drawString("No cards played to this trick yet", margin, y + CardArt.cardHeight / 2);
        }

        g2.setColor(Color.darkGray);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString(footer, margin, height - 12);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    // GridBagLayout falls back to the minimum size when an area is too short, which would collapse the view
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /**
     * Refresh from the trick and the game's own text.
     *
     * @param trick     the trick in progress
     * @param trumps    the trump suit, or null if there are none, used to outline the card that is winning
     * @param showEmpty whether to say that no card has been played yet (false once the game is over)
     */
    public void update(Trick trick, FrenchCard.Suite trumps, String header, String leadText, String footer,
                       boolean showEmpty) {
        cards = new ArrayList<>(trick.getComponents());
        players = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++)
            players.add(trick.playerOf(i));
        winningIndex = cards.isEmpty() ? -1 : players.indexOf(trick.winner(trumps));
        this.header = header;
        this.leadText = leadText;
        this.footer = footer;
        this.empty = cards.isEmpty() && showEmpty;
    }
}
