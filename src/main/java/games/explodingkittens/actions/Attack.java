package games.explodingkittens.actions;

import core.AbstractGameState;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class Attack extends PlayInterruptibleCard {


    public Attack(int player) {
        super (ExplodingKittensCard.CardType.ATTACK, player);
    }

    @Override
    public void _execute(ExplodingKittensGameState state) {
       state.incrementExtraTurns();
       state.setSkip(true);
    }

    @Override
    public Attack _copy() {
        return new Attack(cardPlayer);
    }

    public boolean _equals(Object obj) {
        return obj instanceof Attack;
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
        return "Force next player to take 2 turns";
    }
}