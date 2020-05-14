package games.uno.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Deck;
import core.observations.IPrintable;
import games.uno.UnoGameState;
import games.uno.cards.UnoCard;

public class PlayWild<T>  implements IAction, IPrintable {

    private final Deck<T>              discardDeck;
    private final Deck<T>              playerDeck;
    private final T                    cardToBePlayed;
    private final UnoCard.UnoCardColor color;

    public PlayWild(T card, Deck<T> discardDeck, Deck<T> playerDeck, UnoCard.UnoCardColor color){
        cardToBePlayed   = card;
        this.discardDeck = discardDeck;
        this.playerDeck  = playerDeck;
        this.color       = color;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        playerDeck.remove(cardToBePlayed);
        discardDeck.add(cardToBePlayed);
        ((UnoGameState) gameState).updateCurrentCard((UnoCard) cardToBePlayed, color);
        return true;
    }

    @Override
    public void printToConsole() {
        if (color == UnoCard.UnoCardColor.Red)
            System.out.println("Wild. Change to color RED");
        else if (color == UnoCard.UnoCardColor.Green)
            System.out.println("Wild. Change to color GREEN");
        else if (color == UnoCard.UnoCardColor.Blue)
            System.out.println("Wild. Change to color BLUE");
        else if (color == UnoCard.UnoCardColor.Yellow)
            System.out.println("Wild. Change to color YELLOW");
    }
}
