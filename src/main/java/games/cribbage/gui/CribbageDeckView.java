package games.cribbage.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

/**
 * Draws a Deck of FrenchCards using the images in data/FrenchCards. Cards are face-up when {@code front} is set,
 * or (for the crib, a PartialObservableDeck) when the human viewer can see that card.
 */
public class CribbageDeckView extends DeckView<FrenchCard> {

    final Image backOfCard;
    boolean bottomFirst;

    public CribbageDeckView(int humanPlayer, Deck<FrenchCard> d, boolean visible, Rectangle rect) {
        super(humanPlayer, d, visible, CribbageGUIManager.cardWidth, CribbageGUIManager.cardHeight, rect);
        this.backOfCard = ImageIO.GetInstance().getImage(CribbageGUIManager.dataPath + "gray_back.png");
    }

    /**
     * Lay the cards out bottom card first: for a pile added to one card at a time (the played cards), that is the
     * order they were played in.
     */
    public void setBottomCardFirst(boolean bottomFirst) {
        this.bottomFirst = bottomFirst;
    }

    @Override
    protected int[] displayIndices(Deck<FrenchCard> deck) {
        int[] order = super.displayIndices(deck);
        if (!bottomFirst) return order;
        int[] reversed = new int[order.length];
        for (int i = 0; i < order.length; i++)
            reversed[i] = order[order.length - 1 - i];
        return reversed;
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard card, boolean front) {
        CardView.drawCard(g, rect, card, cardImage(card), backOfCard, front);
    }

    /** Draw one card, face-up or face-down, in the given rectangle. */
    static void drawCardAt(Graphics2D g, FrenchCard card, Rectangle rect, boolean front) {
        Image back = ImageIO.GetInstance().getImage(CribbageGUIManager.dataPath + "gray_back.png");
        CardView.drawCard(g, rect, card, cardImage(card), back, front);
    }

    /**
     * Image file names are &lt;number&gt;&lt;suit&gt;.png for spot cards and &lt;type&gt;&lt;suit&gt;.png for the
     * others - so an Ace is "AceHearts.png", not "14Hearts.png".
     */
    static Image cardImage(FrenchCard card) {
        if (card == null) return null;
        String name = card.type == FrenchCard.FrenchCardType.Number
                ? card.number + card.suite.name()
                : card.type.name() + card.suite.name();
        return ImageIO.GetInstance().getImage(CribbageGUIManager.dataPath + name + ".png");
    }
}
