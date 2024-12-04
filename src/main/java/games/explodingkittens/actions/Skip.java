package games.explodingkittens.actions;

import core.AbstractGameState;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class Skip extends PlayInterruptibleCard {


    public Skip(int player) {
        super (ExplodingKittensCard.CardType.SKIP, player);
    }

    @Override
    public void _execute(ExplodingKittensGameState state) {
       state.setSkip(true);
    }

    @Override
    public Skip _copy() {
        return new Skip(cardPlayer);
    }

    public boolean _equals(Object obj) {
        return obj instanceof Skip;
    }

    @Override
    public int _hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    @Override
    public String toString() {
        return "Skip draw";
    }
}