package uno.actions;


import core.AbstractGameState;
import core.actions.IAction;
import core.components.Deck;
import core.observations.IPrintable;
import uno.cards.UnoCard;

public class NoCards implements IAction, IPrintable {
    private final Deck<UnoCard> sourceDeck;
    private final Deck<UnoCard> targetDeck;

    public NoCards(Deck<UnoCard> sourceDeck, Deck<UnoCard> targetDeck){
        this.sourceDeck = sourceDeck;
        this.targetDeck = targetDeck;
    }

    // TODO if the card drawn is playable, then play it
    @Override
    public boolean execute(AbstractGameState gs) {
        UnoCard card = sourceDeck.draw();
        targetDeck.add(card);
        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println("No playable cards. You must draw a card.");
    }
}
