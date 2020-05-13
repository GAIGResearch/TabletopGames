package uno;

import core.components.Deck;
import core.observations.IObservation;
import core.observations.IPrintable;
import uno.cards.UnoCard;

public class UnoObservation implements IPrintable, IObservation {

    String[] strings = new String[6];

    public UnoObservation(UnoCard currentCard, Deck<UnoCard> playerHand, int playerID) {
        strings[0] = "----------------------------------------------------";
        strings[1] = "Current Card: " + currentCard.toString();
        strings[2] = "----------------------------------------------------";

        strings[3] = "Player      : " + playerID;
        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand : ");

        for (UnoCard card : playerHand.getCards()) {
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
