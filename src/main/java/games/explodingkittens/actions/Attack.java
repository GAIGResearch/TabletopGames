package games.explodingkittens.actions;

import core.AbstractGameState;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class Attack extends PlayInterruptibleCard {

    public static int ADDITIONAL_TURNS = 2;

    public Attack(int player) {
        super (ExplodingKittensCard.CardType.ATTACK, player);
    }

    @Override
    public void _execute(ExplodingKittensGameState state) {
       int attackLevel = ADDITIONAL_TURNS;
       state.setSkip(true);
       if (state.getCurrentPlayerTurnsLeft() > 1) {
           attackLevel += state.getCurrentPlayerTurnsLeft();
           state.setCurrentPlayerTurnsLeft(1);
       }
         state.setNextAttackLevel(attackLevel);
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