package games.explodingkittens.actions;

import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class SeeTheFuture extends PlayInterruptibleCard {

    public static int SEE_THE_FUTURE_CARDS = 3;

    public SeeTheFuture(int player) {
        super (ExplodingKittensCard.CardType.SEETHEFUTURE, player);
    }

    @Override
    public void _execute(ExplodingKittensGameState state) {
        // we set the visibility of the top 3 cards of the draw pile to the current player
        int cardsToSee = Math.min(SEE_THE_FUTURE_CARDS, state.getDrawPile().getSize());
        for (int i = 0; i < cardsToSee; i++) {
            state.getDrawPile().setVisibilityOfComponent(i, cardPlayer, true);
        }
    }

    @Override
    public boolean _equals(Object obj) {
        return obj instanceof SeeTheFuture;
    }

    @Override
    public int _hashCode() {
        return 907409;
    }

    @Override
    public PlayInterruptibleCard _copy() {
        return new SeeTheFuture(cardPlayer);
    }

}
