package games.tricktaking;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;

/**
 * The current player plays a card from their hand to the current trick, recording any void it reveals (if
 * {@link ITrickTakingParameters#rememberVoids()}). Works on any state implementing {@link ITrickTakingState}.
 */
public class PlayCard extends AbstractAction {

    public final FrenchCard card;

    public PlayCard(FrenchCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ITrickTakingState state = (ITrickTakingState) gs;
        int player = gs.getCurrentPlayer();
        Trick trick = state.getCurrentTrick();
        state.getPlayerHand(player).remove(card);  // throws if the player does not hold the card
        if (((ITrickTakingParameters) gs.getGameParameters()).rememberVoids())
            state.getKnownVoids().record(player, trick, card);
        trick.play(card);
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
        return card.hashCode() + 730417;
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
