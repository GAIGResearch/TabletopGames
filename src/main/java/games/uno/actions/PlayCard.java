package games.uno.actions;


import core.AbstractGameState;
import core.actions.IAction;
import core.components.Deck;
import core.observations.IPrintable;
import games.uno.cards.UnoCard;
import games.uno.cards.UnoDrawTwoCard;
import games.uno.cards.UnoReverseCard;
import games.uno.cards.UnoSkipCard;
import games.uno.UnoGameState;

public class PlayCard<T> implements IAction, IPrintable {

    private final Deck<T> discardDeck;
    private final Deck<T> playerDeck;
    private final T cardToBePlayed;

    public PlayCard(T card, Deck<T> discardDeck, Deck<T> playerDeck){
        cardToBePlayed = card;
        this.discardDeck = discardDeck;
        this.playerDeck = playerDeck;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        playerDeck.remove(cardToBePlayed);
        discardDeck.add(cardToBePlayed);
        ((UnoGameState) gameState).updateCurrentCard((UnoCard) cardToBePlayed);

        if (cardToBePlayed instanceof UnoReverseCard)
            ((UnoGameState) gameState).reverseTurn();
        else if (cardToBePlayed instanceof UnoSkipCard)
            ((UnoGameState) gameState).skipTurn();
        else if (cardToBePlayed instanceof UnoDrawTwoCard)
            ((UnoGameState) gameState).drawTwo();

        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play card: " + cardToBePlayed.toString());
    }
}

