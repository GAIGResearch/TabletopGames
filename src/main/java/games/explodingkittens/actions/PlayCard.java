package games.explodingkittens.actions;

import actions.IAction;
import components.IDeck;
import core.AbstractGameState;
import turnorder.TurnOrder;


public class PlayCard<T> implements IAction {

    T card;
    final IDeck<T> sourceDeck;
    final IDeck<T> targetDeck;

    public PlayCard(T card, IDeck<T> sourceDeck, IDeck<T> targetDeck){
        this.card = card;
        this.sourceDeck = sourceDeck;
        this.targetDeck = targetDeck;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        boolean succes = sourceDeck.remove(card);
        targetDeck.add(card);
        return succes;
    }
}
