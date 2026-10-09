package games.tricktaking;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;

/**
 * The current player plays a card from their hand to the current trick, recording any void it reveals (if
 * {@link ITrickTakingParameters#rememberVoids()}). Works on any state implementing {@link ITrickTakingState}.
 *
 * @param <C> the type of card, which must be immutable
 */
public class PlayCard<C extends Component> extends AbstractAction {

    public final C card;

    public PlayCard(C card) {
        this.card = card;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(AbstractGameState gs) {
        play((ITrickTakingState<C, ?>) gs, gs);
        return true;
    }

    private <S extends Enum<S>> void play(ITrickTakingState<C, S> state, AbstractGameState gs) {
        int player = gs.getCurrentPlayer();
        Trick<C, S> trick = state.getCurrentTrick();
        state.getPlayerHand(player).remove(card);  // throws if the player does not hold the card
        if (((ITrickTakingParameters) gs.getGameParameters()).rememberVoids())
            state.recordVoids(player, card);
        trick.play(card);
    }

    @Override
    public PlayCard<C> copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlayCard<?> that && card.equals(that.card);
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
