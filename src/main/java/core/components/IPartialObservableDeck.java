package core.components;

import java.util.ArrayList;
import java.util.Random;


/**
 * Class for a deck of elements which are partial observable to the players.
 * A deck is defined as a "group of cards". Examples of decks are:
 *   * A hand of cards
 *   * A deck to draw from
 *   * Cards played on the player's area
 *   * Discard pile
 */
public interface IPartialObservableDeck<T> extends IDeck<T> {

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
     * Creates a copy of this object.
     * @return a copy of the IDeck.
     */
    IPartialObservableDeck<T> copy();

    /**
     * Returns a unique ID for this deck.
     * @return an ID of the IDeck.
     */
    String getID();

    /**
     * Removes the specified element from the deck
     */
    public boolean remove(T el);

    /**
     * Removes all the cards from the deck.
     */
    void clear();

    /**
     * Adds a card to a deck on the given index
     * @param c card to add
     * @param index where to add it
     * @return true if it was correctly added.
     */
    boolean add(T c, int index);

    /**
     * Adds a card to a deck on the first position of the deck
     * @param c card to add
     * @return true if it was correctly added.
     */
    boolean add(T c);


    /**
     * Adds a card to a deck on the first position of the deck and lets you specify its visibility.
     * @param c card to add
     * @param visibilityPerPlayer visibility of the card to be added
     * @return true if add was successful
     */
    boolean add(T c, boolean[] visibilityPerPlayer);

    /**
     * Adds a card to a deck on the given index and lets you specify its visibility.
     * @param c card to add
     * @param visibilityPerPlayer visibility of the card to be added
     * @return true if add was successful
     */
    boolean add(T c, int index, boolean[] visibilityPerPlayer);


    /**
     * Returns all the cards in this deck.
     * @return all the cards in this deck.
     */
    ArrayList<T> getCards();

    /**
     * Returns all the cards that are visible to the given player. All other cards will be null.
     * @param playerID the player to check visibility for
     * @return all visible cards
     */
    ArrayList<T> getVisibleCards(int playerID);

    /**
     * Shuffles the order of elements in the deck.
     */
    void shuffle();

    /**
     *
     * @param index index of the element to change visibility of
     * @param player index of player whose visibility should be changed
     * @param visibility visibility to be set
     */
    boolean setVisibility(int index, int player, boolean visibility);
}

