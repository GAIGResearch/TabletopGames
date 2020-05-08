package uno;

import components.Deck;
import uno.cards.UnoCard;
import observations.IPrintable;
import observations.Observation;

public class UnoObservation implements IPrintable, Observation {

    String[] strings = new String[4];

    public UnoObservation(UnoCard currentCard, UnoCard.UnoCardColor currentColor, int currentNumber, Deck<UnoCard> playerHand) {
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
    public void PrintToConsole() {
        for (String s : strings){
            System.out.println(s);
        }
    }
}
