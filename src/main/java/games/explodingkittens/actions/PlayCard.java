package games.explodingkittens.actions;

import core.actions.IAction;
import core.components.Card;
import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;


public class PlayCard<T extends Component> implements IAction {

    T card;
    final Deck<T> sourceDeck;
    final Deck<T> targetDeck;

    public PlayCard(T card, Deck<T> sourceDeck, Deck<T> targetDeck){
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
