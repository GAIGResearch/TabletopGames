package uno.actions;

import actions.IAction;
import components.Deck;
import core.AbstractGameState;
import games.uno.cards.CardEffect;
import observations.IPrintable;
import turnorder.TurnOrder;
import uno.cards.UnoNumberCard;

public class PlayCard<T> implements IAction, IPrintable {

    private final Deck<T> sourceDeck;
    private final Deck<T> targetDeck;
    private final T cardToBePlayed;

    public PlayCard(T card, Deck<T> sourceDeck, Deck<T> targetDeck){
        cardToBePlayed = card;
        this.sourceDeck = sourceDeck;
        this.targetDeck = targetDeck;
    }

    // TODO
    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        if (cardToBePlayed instanceof UnoNumberCard) {
            sourceDeck.discard(cardToBePlayed);
            targetDeck.add(cardToBePlayed);
        }
        return true;
    }

    @Override
    public void PrintToConsole() {
        System.out.println("Play card: " + cardToBePlayed.toString());
    }
}

