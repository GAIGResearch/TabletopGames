package games.tricktaking.gui;

import core.components.Component;
import core.components.Deck;
import gui.views.DeckView;

import java.awt.*;

/**
 * Draws a Deck of cards with the given {@link CardFace}, with {@code front} deciding whether card faces or card backs
 * are shown.
 */
public class CardDeckView<C extends Component> extends DeckView<C> {

    final CardFace<C, ?> face;

    public CardDeckView(CardFace<C, ?> face, int humanPlayer, Deck<C> d, boolean visible, Rectangle rect) {
        super(humanPlayer, d, visible, face.cardWidth(), face.cardHeight(), rect);
        this.face = face;
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, C card, boolean front) {
        if (front)
            face.drawFront(g, card, rect);
        else
            face.drawBack(g, rect);
    }
}
