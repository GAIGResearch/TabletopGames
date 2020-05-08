package uno.actions;

import actions.IAction;
import components.Deck;
import core.AbstractGameState;
import observations.IPrintable;
import turnorder.TurnOrder;
import uno.cards.UnoCard;

public class NoCards implements IAction, IPrintable {
    private final Deck<UnoCard> sourceDeck;
    private final Deck<UnoCard> targetDeck;

    public NoCards(Deck<UnoCard> sourceDeck, Deck<UnoCard> targetDeck){
        this.sourceDeck = sourceDeck;
        this.targetDeck = targetDeck;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        UnoCard card = sourceDeck.draw();
        targetDeck.add(card);
        return true;
    }

    @Override
    public void PrintToConsole() {
        System.out.println("No playable cards. You must draw a card.");
    }
}
