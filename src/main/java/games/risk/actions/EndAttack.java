package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.risk.RiskGameState;
import games.risk.components.RiskCard;

/**
 * The current player stops attacking. If they captured a territory this turn, they draw a RISK card.
 */
public class EndAttack extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        if (state.hasCapturedThisTurn()) {
            Deck<RiskCard> drawDeck = state.getDrawDeck();
            // when the draw deck is empty the discard pile is shuffled to make a new one; with neither, no card
            if (drawDeck.getSize() == 0) {
                drawDeck.add(state.getDiscardPile());
                state.getDiscardPile().clear();
                drawDeck.shuffle(state.getRnd());
            }
            if (drawDeck.getSize() > 0)
                state.getHand(state.getCurrentPlayer()).add(drawDeck.draw());
        }
        return true;
    }

    @Override
    public EndAttack copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EndAttack;
    }

    @Override
    public int hashCode() {
        return 731213;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "EndAttack";
    }
}
