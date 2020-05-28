package games.explodingkittens.actions;

import core.actions.IAction;
import core.components.Card;
import core.components.IDeck;
import core.AbstractGameState;


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
    public boolean execute(AbstractGameState gs) {
        boolean success = sourceDeck.remove(card);
        targetDeck.add(card);
        return success;
    }

    @Override
    public Card getCard() {
        return null;
    }
}
