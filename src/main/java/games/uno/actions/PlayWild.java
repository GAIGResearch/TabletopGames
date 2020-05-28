package games.uno.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.interfaces.IPrintable;
import games.uno.UnoGameState;
import games.uno.cards.UnoCard;

public class PlayWild extends DrawCard implements IPrintable {

    private final UnoCard.UnoCardColor color;

    public PlayWild(int deckFrom, int deckTo, int fromIndex, UnoCard.UnoCardColor color) {
        super(deckFrom, deckTo, fromIndex);
        this.color = color;
    }


    @Override
    public boolean execute(AbstractGameState gameState) {
        super.execute(gameState);
        UnoCard cardToBePlayed = (UnoCard) getCard(gameState);
        ((UnoGameState) gameState).updateCurrentCard(cardToBePlayed, color);
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
