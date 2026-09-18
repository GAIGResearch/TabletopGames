package games.crazyeights.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.crazyeights.CZEGameState;

import java.util.Objects;

/**
 * Play a card from the hand onto the discard pile. The card is identified by its value, not its position in the hand.
 * nominatedSuit becomes the suit to match: for an Eight it is the player's choice, for any other card it is the card's suit.
 */
public class PlayCard extends AbstractAction {

    public final int player;
    public final FrenchCard card;
    public final FrenchCard.Suite nominatedSuit;

    public PlayCard(int player, FrenchCard card, FrenchCard.Suite nominatedSuit) {
        this.player = player;
        this.card = card;
        this.nominatedSuit = nominatedSuit;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CZEGameState state = (CZEGameState) gs;
        state.getPlayerHands().get(player).remove(card);  // throws if the card is not in the hand
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
        return player == that.player && card.equals(that.card) && nominatedSuit == that.nominatedSuit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, card, nominatedSuit);
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
