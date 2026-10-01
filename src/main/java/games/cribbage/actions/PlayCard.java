package games.cribbage.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.cribbage.CribbageGameState;

import java.util.Objects;

/**
 * Play a card from the current player's hand in the play: it moves to the player's played cards and is added to
 * the current count. Scoring and passing the turn are done by the forward model.
 */
public class PlayCard extends AbstractAction {

    public final FrenchCard card;

    public PlayCard(FrenchCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CribbageGameState state = (CribbageGameState) gs;
        int player = state.getCurrentPlayer();
        state.getPlayerHand(player).remove(card);  // throws if the card is not in the hand
        state.getPlayedCards(player).add(card);
        state.getPlaySequence().add(card);
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
        return card.equals(that.card);
    }

    @Override
    public int hashCode() {
        return Objects.hash(card) + 590321;
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
