package uno;

import components.Deck;
import uno.cards.UnoCard;
import observations.IPrintable;
import observations.Observation;

public class UnoObservation implements IPrintable, Observation {

    String[] strings = new String[5];

    public UnoObservation(UnoCard currentCard, UnoCard.UnoCardColor currentColor, int currentNumber, Deck<UnoCard> playerHand) {

    }

    /*
    public UnoObservation(UnoCard currentCard, Deck<UnoCard> playerHand, Deck<UnoCard> discardPile, int[] cardsPerPlayer, int cardsInDeck){
        strings[0] = "Current Card: " + currentCard.toString();

        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand: ");

        for (UnoCard card : playerHand.getCards()) {
            sb.append(card.toString());
            sb.append("\t");
        }
        strings[1] = sb.toString();

        sb = new StringBuilder();
        sb.append("Discard Pile: ");
        for (UnoCard card : discardPile.getCards()) {
            sb.append(card.toString());
            sb.append("\t");
        }
        strings[2] = sb.toString();

        strings[3] = "Cards per player: " + Arrays.toString(cardsPerPlayer);
        strings[4] = "Remaining cards in draw pile: " + Integer.toString(cardsInDeck);
    }
*/

    @Override
    public void PrintToConsole() {
        for (String s : strings){
            System.out.println(s);
        }
    }
}
