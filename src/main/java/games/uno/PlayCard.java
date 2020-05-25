package games.uno;

import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import core.observations.IPrintable;
import games.uno.cards.CardEffect;

public class PlayCard<T> implements IAction, IPrintable {

    private final Deck<T> sourceDeck;
    private final Deck<T> targetDeck;
    private final T cardToBePlayed;

    private final IAction postEffect;

    public PlayCard(T card, Deck<T> sourceDeck, Deck<T> targetDeck){
        cardToBePlayed = card;
        this.sourceDeck = sourceDeck;
        this.targetDeck = targetDeck;
        postEffect = null;
    }

    public PlayCard(T card, Deck<T> sourceDeck, Deck<T> targetDeck, CardEffect postEffect){
        cardToBePlayed = card;
        this.sourceDeck = sourceDeck;
        this.targetDeck = targetDeck;
        this.postEffect = postEffect;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        sourceDeck.remove(cardToBePlayed);
        targetDeck.add(cardToBePlayed);
        if (postEffect != null) postEffect.execute(gs);
        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play card: " + cardToBePlayed.toString());
    }
}
