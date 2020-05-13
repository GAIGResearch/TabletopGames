package uno.actions;


import core.AbstractGameState;
import core.actions.IAction;
import core.components.Deck;
import core.observations.IPrintable;
import uno.UnoGameState;
import uno.cards.UnoCard;
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

    @Override
    public boolean execute(AbstractGameState gameState) {
        if (cardToBePlayed instanceof UnoNumberCard) {
            sourceDeck.remove(cardToBePlayed);
            targetDeck.add(cardToBePlayed);

            ((UnoGameState) gameState).updateCurrentCard((UnoCard) cardToBePlayed);
        }
        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play card: " + cardToBePlayed.toString());
    }
}

