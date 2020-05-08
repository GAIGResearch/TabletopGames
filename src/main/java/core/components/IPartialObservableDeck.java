package core.components;


import java.util.ArrayList;

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
     * Returns all the cards that are visible to the given player. All other cards will be null.
     * @param playerID the player to check visibility for
     * @return all visible cards
     */
    ArrayList<T> getVisibleCards(int playerID);

    /**
     *
     * @param index index of the element to change visibility of
     * @param player index of player whose visibility should be changed
     * @param visibility visibility to be set
     */
    void setVisibilityOfCard(int index, int player, boolean visibility);

    void setVisibility(ArrayList<boolean[]> visibility);

    void setCards(ArrayList<T> cards, ArrayList<boolean[]> visibility);


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

    boolean add(PartialObservableDeck<T> deck);





}

