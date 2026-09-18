package games.agram.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.agram.AgramGameState;

/**
 * Play a card from the hand to the current trick.
 */
public class PlayCard extends AbstractAction {

    public final FrenchCard card;

    public PlayCard(FrenchCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        AgramGameState state = (AgramGameState) gs;
        int player = state.getCurrentPlayer();
        FrenchCard.Suite lead = state.getLeadSuit();
        if (lead != null && card.suite != lead)
            state.getKnownVoids(player).add(lead);  // failing to follow suit shows everyone the player has none
        state.getPlayerHands().get(player).remove(card);
        state.getCurrentTrick().addToBottom(card);  // index 0 stays the lead card
        return true;
    }

    @Override
    public PlayCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlayCard that && card.equals(that.card);
    }

    @Override
    public int hashCode() {
        return card.hashCode() + 518201;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Play " + card;
    }
}
