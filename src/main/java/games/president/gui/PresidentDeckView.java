package games.president.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.president.PresidentUtils;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;
import java.util.Comparator;

/**
 * A player's hand, face up (lowest rank on the left) or face down.
 */
public class PresidentDeckView extends DeckView<FrenchCard> {

    static final String DATA_PATH = "data/FrenchCards/";
    private final Image backOfCard;
    private final Dimension size;

    public PresidentDeckView(int humanId, Deck<FrenchCard> deck, boolean visible, int width, int height) {
        super(humanId, deck, visible, PresidentGUIManager.CARD_WIDTH, PresidentGUIManager.CARD_HEIGHT,
                new Rectangle(0, 0, width, height));
        backOfCard = ImageIO.GetInstance().getImage(DATA_PATH + "gray_back.png");
        size = new Dimension(width, height);
        setOpaque(false);
        setDisplayOrder(Comparator.comparingInt((FrenchCard c) -> PresidentUtils.rank(c)).thenComparing(c -> c.suite));
    }

    @Override
    public void drawDeck(Graphics2D g) {
        @SuppressWarnings("unchecked") Deck<FrenchCard> deck = (Deck<FrenchCard>) component;
        if (deck == null || deck.getSize() == 0) return;
        drawnOrder = displayIndices(deck);
        int offset = Math.max((rect.width - itemWidth) / drawnOrder.length, 12);
        for (int k = 0; k < drawnOrder.length; k++)
            drawComponent(g, new Rectangle(rect.x + offset * k, rect.y, itemWidth, itemHeight), deck.get(drawnOrder[k]), front);
        if (!front) {
            g.setColor(Color.WHITE);
            g.drawString(drawnOrder.length + (drawnOrder.length == 1 ? " card" : " cards"), rect.x + 5, rect.y + itemHeight + 14);
        }
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard card, boolean front) {
        g.drawImage(front ? cardImage(card) : backOfCard, rect.x, rect.y, rect.width, rect.height, null);
    }

    static Image cardImage(FrenchCard card) {
        return ImageIO.GetInstance().getImage(DATA_PATH + PresidentUtils.rankName(card.number) + card.suite + ".png");
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
