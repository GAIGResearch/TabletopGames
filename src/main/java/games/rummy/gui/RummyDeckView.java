package games.rummy.gui;

import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.rummy.RummyUtils;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

/**
 * A player's hand. Face up it is sorted by rank; face down it keeps its order and shows only the cards every player
 * knows, those taken from the discard pile. Known cards have a gold outline in either case.
 */
public class RummyDeckView extends DeckView<FrenchCard> {

    static final String DATA_PATH = "data/FrenchCards/";
    private static final Color KNOWN = new Color(240, 200, 60);
    private final Image backOfCard;
    private final Dimension size;

    public RummyDeckView(int humanId, Deck<FrenchCard> deck, boolean visible, int width, int height) {
        super(humanId, deck, visible, RummyGUIManager.CARD_WIDTH, RummyGUIManager.CARD_HEIGHT,
                new Rectangle(0, 0, width, height));
        backOfCard = ImageIO.GetInstance().getImage(DATA_PATH + "gray_back.png");
        size = new Dimension(width, height);
        setOpaque(false);
        setDisplayOrder(RummyUtils.MELD_ORDER);
    }

    @Override
    public void drawDeck(Graphics2D g) {
        @SuppressWarnings("unchecked") PartialObservableDeck<FrenchCard> hand = (PartialObservableDeck<FrenchCard>) component;
        if (hand == null || hand.getSize() == 0) return;
        drawnOrder = displayIndices(hand);
        // the cards sit side by side, overlapping only when the hand is too long for the view
        int offset = Math.min(itemWidth + 4, Math.max((rect.width - itemWidth) / drawnOrder.length, 12));
        for (int k = 0; k < drawnOrder.length; k++) {
            int i = drawnOrder[k];
            boolean known = knownToAll(hand.getVisibilityOfComponent(i));
            Rectangle r = new Rectangle(rect.x + offset * k, rect.y, itemWidth, itemHeight);
            drawComponent(g, r, hand.get(i), front || known);
            if (known) {
                g.setColor(KNOWN);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            }
        }
        if (!front) {
            g.setColor(Color.WHITE);
            g.drawString(drawnOrder.length + (drawnOrder.length == 1 ? " card" : " cards"), rect.x + 5, rect.y + itemHeight + 14);
        }
    }

    private static boolean knownToAll(boolean[] visibility) {
        for (boolean v : visibility)
            if (!v) return false;
        return true;
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard card, boolean front) {
        g.drawImage(front ? cardImage(card) : backOfCard, rect.x, rect.y, rect.width, rect.height, null);
    }

    static Image cardImage(FrenchCard card) {
        String rank = card.type == FrenchCard.FrenchCardType.Number ? String.valueOf(card.number) : card.type.name();
        return ImageIO.GetInstance().getImage(DATA_PATH + rank + card.suite + ".png");
    }

    static Image backImage() {
        return ImageIO.GetInstance().getImage(DATA_PATH + "gray_back.png");
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return size;
    }
}
