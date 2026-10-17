package games.skitgubbe.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.skitgubbe.SkitgubbeGameState;

/**
 * The current player plays a card to the trick: from their hand in phase one, from their collected cards in phase
 * two.
 */
public class PlayCard extends AbstractAction {

    public final FrenchCard card;

    public PlayCard(FrenchCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SkitgubbeGameState state = (SkitgubbeGameState) gs;
        int player = state.getCurrentPlayer();
        Deck<FrenchCard> from = state.getGamePhase() == SkitgubbeGameState.Phase.PHASE_ONE
                ? state.getPlayerHand(player) : state.getCollectedCards(player);
        if (!from.getComponents().remove(card))
            throw new IllegalArgumentException("Player " + player + " does not hold " + card);
        state.getTrick().add(card);
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
        return card.hashCode() + 518823;
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
