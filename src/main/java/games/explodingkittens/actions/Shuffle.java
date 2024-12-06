package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class Shuffle extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        state.setInPlay(ExplodingKittensCard.CardType.SHUFFLE, state.getCurrentPlayer());
        state.getDrawPile().shuffle(state.getRnd());
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Shuffle;
    }

    @Override
    public int hashCode() {
        return 909327409;
    }

    @Override
    public Shuffle copy() {
        return this;
    }

    @Override
    public String toString() {
        return "Shuffle the draw pile";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
