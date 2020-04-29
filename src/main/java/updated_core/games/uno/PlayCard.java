package updated_core.games.uno;

import components.Deck;
import updated_core.actions.IAction;
import updated_core.actions.IPrintable;
import updated_core.games.uno.cards.CardEffect;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

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
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        sourceDeck.remove(cardToBePlayed);
        targetDeck.add(cardToBePlayed);
        if (postEffect != null) postEffect.Execute(gs, turnOrder);
        return true;
    }

    @Override
    public void PrintToConsole() {
        System.out.println("Play card: " + cardToBePlayed.toString());
    }
}
