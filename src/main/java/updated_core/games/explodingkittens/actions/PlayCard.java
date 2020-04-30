package updated_core.games.explodingkittens.actions;

import components.IDeck;
import updated_core.actions.IAction;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;


public class PlayCard<T> implements IAction{

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
