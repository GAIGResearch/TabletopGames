package components;

import java.util.Random;

/**
 * Class for a deck of cards.
 * A deck is defined as a "group of cards". Examples of decks are:
 *   * A hand of cards
 *   * A deck to draw from
 *   * Cards played on the player's area
 *   * Discard pile
 */
public interface IDeck<T>
{
    /**
     * Maximum number of cards this deck may contain.
     */
    int getCapacity();

    /**
     * Shuffles the deck
     */
    void shuffle(Random rnd);

    /**
     * Draws the first card of the deck
     * @return the first card of the deck
     */
    T draw();

    /**
     * Picks a random card from the IDeck
     * @return a random card from the IDeck
     */
    T pick();

    /**
     * Picks the card in position idx from the deck
     * @param idx the index of the card in the deck
     * @return the card in position idx from the deck
     */
    T pick(int idx);

    /**
     * Draws the last card of the deck
     * @return the last card of the deck
     */
    T pickLast();

    /**
     * Peeks (without drawing) the first card of the deck
     * @return The card peeked.
     */
    T peek();

    /**
     * Peeks (without drawing) amount cards of the deck starting from card idx
     * @return The cards peeked, following the order of the deck. If there are not
     * enough cards to be picked, the array will only contain those available. If no
     * cards are available, returns an empty array.
     */
    T[] peek(int idx, int amount);

    /**
     * Creates a copy of this obejct.
     * @return a copy of the IDeck.
     */
    IDeck<T> copy();


}
