package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class SeeTheFuture extends AbstractAction {

    public static int SEE_THE_FUTURE_CARDS = 3;

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        state.setInPlay(ExplodingKittensCard.CardType.ATTACK, state.getCurrentPlayer());

        // we set the visibility of the top 3 cards of the draw pile to the current player
        int cardsToSee = Math.min(SEE_THE_FUTURE_CARDS, state.getDrawPile().getSize());
        for (int i = 0; i < cardsToSee; i++) {
            state.getDrawPile().setVisibilityOfComponent(i, state.getCurrentPlayer(), true);
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SeeTheFuture;
    }

    @Override
    public int hashCode() {
        return 907409;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "See the future";
    }

    @Override
    public SeeTheFuture copy() {
        return this;
    }

}
