package games.skitgubbe.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

/**
 * A row of cards - a hand or a player's collected cards - face up (sorted) or face down, overlapping to fit.
 */
public class SkitgubbeDeckView extends DeckView<FrenchCard> {

    static final String DATA_PATH = "data/FrenchCards/";
    private final Image backOfCard;
    private final Dimension size;

    public SkitgubbeDeckView(int humanId, Deck<FrenchCard> deck, boolean visible, int width, int height) {
        super(humanId, deck, visible, SkitgubbeGUIManager.CARD_WIDTH, SkitgubbeGUIManager.CARD_HEIGHT,
                new Rectangle(0, 0, width, height));
        backOfCard = backImage();
        size = new Dimension(width, height);
        setOpaque(false);
        setDisplayOrder(FrenchCard.HAND_DISPLAY_ORDER);
    }

    @Override
    public void drawDeck(Graphics2D g) {
        @SuppressWarnings("unchecked") Deck<FrenchCard> deck = (Deck<FrenchCard>) component;
        if (deck == null || deck.getSize() == 0) return;
        drawnOrder = displayIndices(deck);
        // a collected pile can hold most of the pack, so the cards overlap as far as needed to fit the row
        int offset = drawnOrder.length == 1 ? 0 : Math.min(itemWidth + 4, (rect.width - itemWidth) / (drawnOrder.length - 1));
        for (int k = 0; k < drawnOrder.length; k++)
            drawComponent(g, new Rectangle(rect.x + offset * k, rect.y, itemWidth, itemHeight), deck.get(drawnOrder[k]), front);
        if (!front) {
            g.setColor(Color.WHITE);
            g.drawString(drawnOrder.length + (drawnOrder.length == 1 ? " card" : " cards"),
                    rect.x + offset * (drawnOrder.length - 1) + itemWidth + 8, rect.y + itemHeight / 2);
        }
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard card, boolean front) {
        g.drawImage(front ? cardImage(card) : backOfCard, rect.x, rect.y, rect.width, rect.height, null);
    }

    static Image cardImage(FrenchCard card) {
        String rank = switch (card.number) {
            case 11 -> "Jack";
            case 12 -> "Queen";
            case 13 -> "King";
            case 14 -> "Ace";
            default -> String.valueOf(card.number);
        };
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
