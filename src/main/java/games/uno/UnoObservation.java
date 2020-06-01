package games.uno;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.uno.cards.UnoCard;
import core.interfaces.IObservation;

import java.util.ArrayList;

public class UnoObservation implements IPrintable, IObservation {

    String[] strings = new String[6];

    public UnoObservation(UnoCard currentCard, String currentColor, Deck<UnoCard> playerHand,
                          Deck<UnoCard> discardDeck, int playerID, ArrayList<Integer> cardsLeft) {

        strings[0] = "----------------------------------------------------";
        strings[1] = "Current Card: " + currentCard.toString() + " [" + currentColor + "]";
        strings[2] = "----------------------------------------------------";

        strings[3] = "Player      : " + playerID;
        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand : ");

        for (UnoCard card : playerHand.getComponents()) {
            sb.append(card.toString());
            sb.append(" ");
        }
        strings[4] = sb.toString();
        strings[5] = "----------------------------------------------------";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        for (String s : strings){
            System.out.println(s);
        }
    }

    @Override
    public IObservation copy() {
        return null;
    }

    @Override
    public IObservation next(AbstractAction action) {
        return null;
    }
}
