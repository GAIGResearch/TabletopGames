package games.tricktaking.gui;

import core.components.Component;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

/**
 * How the shared trick-taking views draw one kind of card: its face and back, the size to draw it at, how its suits
 * are written, and the order to show a hand in. {@link CardArt#FRENCH} draws French cards from their images.
 *
 * @param <C> the type of card
 * @param <S> the type of suit
 */
public interface CardFace<C extends Component, S> {

    int cardWidth();

    int cardHeight();

    void drawFront(Graphics2D g, C card, Rectangle rect);

    void drawBack(Graphics2D g, Rectangle rect);

    /**
     * The suit in as few characters as possible, for status lines (e.g. the suits a player is void in).
     */
    String suitSymbol(S suit);

    /**
     * The suits, in the order a status line lists them.
     */
    List<S> suits();

    /**
     * The order to show a face-up hand in (display only).
     */
    Comparator<? super C> handOrder();
}
