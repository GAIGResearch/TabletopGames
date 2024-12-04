package games.explodingkittens.actions;

import core.AbstractGameState;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

public class Favor extends PlayInterruptibleCard {

    public final int targetPlayer;

    public Favor(int player, int target) {
        super (ExplodingKittensCard.CardType.FAVOR, player);
        targetPlayer = target;
    }

    @Override
    public void _execute(ExplodingKittensGameState state) {
        int cardIndex = state.getRnd().nextInt(state.getPlayerHand(targetPlayer).getSize());
        ExplodingKittensCard card = state.getPlayerHand(targetPlayer).get(cardIndex);
        state.getPlayerHand(targetPlayer).remove(card);
        state.getPlayerHand(state.getCurrentPlayer()).add(card);
    }

    @Override
    public Favor _copy() {
        return new Favor(cardPlayer, targetPlayer);
    }

    public boolean _equals(Object obj) {
        return obj instanceof Favor && ((Favor) obj).targetPlayer == targetPlayer;
    }

    @Override
    public int _hashCode() {
        return targetPlayer;
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