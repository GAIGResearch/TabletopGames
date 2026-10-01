package games.tricktaking.gui;

import core.components.Component;
import games.tricktaking.Trick;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The centre of the table: the trick in progress, in the order played, with who played each card and which card is
 * winning it. The three lines of text around the cards are supplied by the game. The cards are drawn with the given
 * {@link CardFace}.
 *
 * @param <C> the type of card
 * @param <S> the type of suit
 */
public class CardTrickView<C extends Component, S> extends JComponent {

    static final int gap = 10, margin = 12;
    static final int headerHeight = 44, footerHeight = 34;

    final CardFace<C, S> face;
    // each card's slot is wide enough for the "Player n (led)" label under it, even when the cards are narrow
    final int slotWidth;
    final int width, height;

    List<C> cards = new ArrayList<>();
    List<Integer> players = new ArrayList<>();
    int winningIndex = -1;
    String header = "", leadText = "", footer = "";
    boolean empty = true;

    /**
     * @param minWidth the least the panel may be - wide enough for the longest line of text the game shows
     */
    public CardTrickView(CardFace<C, S> face, int nPlayers, int minWidth) {
        this.face = face;
        this.slotWidth = Math.max(face.cardWidth() + gap, 90);
        this.width = Math.max(minWidth, margin * 2 + nPlayers * slotWidth);
        this.height = headerHeight + face.cardHeight() + 22 + footerHeight;
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
        int cardWidth = face.cardWidth(), cardHeight = face.cardHeight();
        for (int i = 0; i < cards.size(); i++) {
            int x = margin + i * slotWidth;
            face.drawFront(g2, cards.get(i), new Rectangle(x, y, cardWidth, cardHeight));
            if (i == winningIndex) {
                g2.setColor(new Color(220, 150, 20));
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(x - 2, y - 2, cardWidth + 4, cardHeight + 4, 8, 8);
                g2.setStroke(new BasicStroke(1));
            }
            g2.setColor(Color.black);
            g2.setFont(g2.getFont().deriveFont(i == winningIndex ? Font.BOLD : Font.PLAIN, 12f));
            g2.drawString("Player " + players.get(i) + (i == 0 ? " (led)" : ""), x, y + cardHeight + 16);
        }
        if (empty) {
            g2.setColor(Color.darkGray);
            g2.drawString("No cards played to this trick yet", margin, y + cardHeight / 2);
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
    public void update(Trick<C, S> trick, S trumps, String header, String leadText, String footer,
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
