package games.tricktaking.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.CardView;
import gui.views.DeckView;

import java.awt.*;

/**
 * Draws a Deck of FrenchCards using the images in data/FrenchCards, with {@code front} deciding whether card faces
 * or card backs are shown.
 */
public class FrenchCardDeckView extends DeckView<FrenchCard> {

    final Image backOfCard;

    public FrenchCardDeckView(int humanPlayer, Deck<FrenchCard> d, boolean visible, Rectangle rect) {
        super(humanPlayer, d, visible, CardArt.cardWidth, CardArt.cardHeight, rect);
        this.backOfCard = CardArt.backOfCard();
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard card, boolean front) {
        CardView.drawCard(g, rect, card, CardArt.cardImage(card), backOfCard, front);
    }
}
