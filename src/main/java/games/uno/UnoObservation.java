package games.uno;

import core.components.Deck;
import core.interfaces.IPrintable;
import games.uno.cards.UnoCard;
import core.interfaces.IObservation;

import java.util.ArrayList;

public class UnoObservation implements IPrintable, IObservation {

    String[] strings = new String[6];

    public UnoObservation(UnoCard currentCard, UnoCard.UnoCardColor currentColor, Deck<UnoCard> playerHand,
                          Deck<UnoCard> discardDeck, int playerID, ArrayList<Integer> cardsLeft) {
        String colorString = "";
        if (currentColor == UnoCard.UnoCardColor.Red)
            colorString = "Red";
        else if (currentColor == UnoCard.UnoCardColor.Green)
            colorString = "Green";
        else if (currentColor == UnoCard.UnoCardColor.Blue)
            colorString = "Blue";
        else if (currentColor == UnoCard.UnoCardColor.Yellow)
            colorString = "Yellow";

        strings[0] = "----------------------------------------------------";
        strings[1] = "Current Card: " + currentCard.toString() + " [" + colorString + "]";
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
    public void printToConsole() {
        for (String s : strings){
            System.out.println(s);
        }
    }
}
