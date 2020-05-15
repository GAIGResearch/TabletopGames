package core.components;

import java.util.ArrayList;
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
    void setID(String id);

    /**
     * Maximum number of cards this deck may contain.
     */
    int getCapacity();

    /**
     * Maximum number of cards this deck may contain.
     */
    void setCapacity(int capacity);

    boolean isOverCapacity();

    /**
     * Returns all the cards in this deck.
     * @return all the cards in this deck.
     */
    ArrayList<T> getCards();

    void setCards(ArrayList<T> cards);


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
     * Creates a copy of this object.
     * @return a copy of the IDeck.
     */
    IDeck<T> copy();

    /**
     * Returns a unique ID for this deck.
     * @return an ID of the IDeck.
     */
    String getID();

    T peek(int idx);

    /**
     * Adds a card to a deck on the given index and uses the default visibility.
     * @param c card to add
     * @param index where to add it
     * @return true if it was correctly added.
     */
    boolean add(T c, int index);

    /**
     * Adds a card to a deck on the first position of the deck and uses the default visibility.
     * @param c card to add
     * @return true if it was correctly added.
     */
    boolean add(T c);

    boolean add(Deck<T> d);

    boolean remove(T card);

    boolean remove(int idx);

    /**
     * Removes all the cards from the deck.
     */
    void clear();

    /**
     * Shuffles the order of elements in the deck.
     */
    void shuffle();
}
