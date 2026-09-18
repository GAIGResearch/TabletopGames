package games.crazyeights.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.crazyeights.CZEGameState;

import java.util.Objects;

/**
 * Play a card from the hand onto the discard pile.
 * nominatedSuit becomes the suit to match: for an Eight it is the player's choice, for any other card it is the card's suit.
 */
public class PlayCard extends AbstractAction {

    public final FrenchCard card;
    public final FrenchCard.Suite nominatedSuit;

    public PlayCard(FrenchCard card, FrenchCard.Suite nominatedSuit) {
        this.card = card;
        this.nominatedSuit = nominatedSuit;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CZEGameState state = (CZEGameState) gs;
        state.getPlayerHands().get(state.getCurrentPlayer()).remove(card);  // throws if the card is not in the hand
        state.getDiscardPile().add(card);
        state.setCurrentSuit(nominatedSuit);
        return true;
    }

    @Override
    public PlayCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCard that)) return false;
        return card.equals(that.card) && nominatedSuit == that.nominatedSuit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(card, nominatedSuit) + 382903;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Play " + card + (card.suite == nominatedSuit ? "" : " nominating " + nominatedSuit);
    }
}
