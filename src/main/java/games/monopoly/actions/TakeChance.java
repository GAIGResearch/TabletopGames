package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.components.MonopolyCard;

/**
 * The current player draws a Chance card and carries it out, rather than paying the fine (PayOrChance).
 */
public class TakeChance extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        MonopolyCard card = state.drawCard(MonopolyCard.Pile.CHANCE);
        card.effect.apply(state, state.getCurrentPlayer(), card);
        return true;
    }

    @Override
    public TakeChance copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TakeChance;
    }

    @Override
    public int hashCode() {
        return 640231;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Take a Chance";
    }

    @Override
    public String toString() {
        return "TakeChance";
    }
}
