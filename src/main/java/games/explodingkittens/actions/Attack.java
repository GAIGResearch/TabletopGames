package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class Attack extends AbstractAction {

    public static int ADDITIONAL_TURNS = 2;

    @Override
    public boolean execute(AbstractGameState gs) {
       ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
       state.setInPlay(ExplodingKittensCard.CardType.ATTACK, state.getCurrentPlayer());
       int attackLevel = ADDITIONAL_TURNS;
       state.setSkip(true);
       if (state.getCurrentPlayerTurnsLeft() > 1) {
           attackLevel += state.getCurrentPlayerTurnsLeft();
           state.setCurrentPlayerTurnsLeft(1);
       }
         state.setNextAttackLevel(attackLevel);
        return true;
    }

    @Override
    public Attack copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Attack;
    }

    @Override
    public int hashCode() {
        return 202849;
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