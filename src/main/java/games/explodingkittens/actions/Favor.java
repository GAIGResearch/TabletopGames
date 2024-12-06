package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class Favor extends AbstractAction {

    public final int targetPlayer;

    public Favor(int player, int target) {
        targetPlayer = target;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        state.setInPlay(ExplodingKittensCard.CardType.FAVOR, state.getCurrentPlayer());
        int cards = state.getPlayerHand(targetPlayer).getSize();
        if (cards > 0) { // edge cases make this possible
            state.setActionInProgress(new ChoiceOfCardToGive(targetPlayer, state.getCurrentPlayer()));
        }
        return true;
    }

    @Override
    public Favor copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Favor && ((Favor) obj).targetPlayer == targetPlayer;
    }

    @Override
    public int hashCode() {
        return targetPlayer + 4792;
    }


    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    @Override
    public String toString() {
        return "Take card from player " + targetPlayer;
    }
}