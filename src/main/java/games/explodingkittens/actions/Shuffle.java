package games.explodingkittens.actions;

import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class Shuffle extends PlayInterruptibleCard {

    public Shuffle(int player) {
        super (ExplodingKittensCard.CardType.SHUFFLE, player);
    }

    @Override
    public void _execute(ExplodingKittensGameState state) {
        state.getDrawPile().shuffle(state.getRnd());
    }

    @Override
    public boolean _equals(Object obj) {
        return obj instanceof Shuffle;
    }

    @Override
    public int _hashCode() {
        return 909327409;
    }

    @Override
    public PlayInterruptibleCard _copy() {
        return new Shuffle(cardPlayer);
    }
}
