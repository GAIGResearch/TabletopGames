package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class Nope extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        state.setInPlay(ExplodingKittensCard.CardType.NOPE, state.getCurrentPlayer());
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Nope;
    }

    @Override
    public int hashCode() {
        return -42943;
    }

    @Override
    public String toString() {
        return "Nope";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
