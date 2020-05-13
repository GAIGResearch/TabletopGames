package uno;

import core.components.Deck;
import core.observations.IObservation;
import core.observations.IPrintable;
import uno.cards.UnoCard;

public class UnoObservation implements IPrintable, IObservation {

    String[] strings = new String[4];

    public UnoObservation(UnoCard currentCard, Deck<UnoCard> playerHand) {
        strings[0] = "----------------------------------------------------";
        strings[1] = "Current Card: " + currentCard.toString();
        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand: ");

        for (UnoCard card : playerHand.getCards()) {
            sb.append(card.toString());
            sb.append(" ");
        }
        strings[2] = sb.toString();
        strings[3] = "----------------------------------------------------";
    }

    @Override
    public void printToConsole() {
        for (String s : strings){
            System.out.println(s);
        }
    }
}
