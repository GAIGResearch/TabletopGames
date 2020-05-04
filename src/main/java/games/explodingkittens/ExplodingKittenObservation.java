package games.explodingkittens;

import core.components.Deck;
import core.components.IDeck;
import core.observations.IPrintable;
import games.explodingkittens.cards.ExplodingKittenCard;
import core.observations.IObservation;

import java.util.List;

public class ExplodingKittenObservation implements IPrintable, IObservation {

    private List<Deck<ExplodingKittenCard>> playerHandCards;
    private Deck<ExplodingKittenCard> drawPile;
    private Deck<ExplodingKittenCard> discardPile;
    private int currentPlayer;

    public ExplodingKittenObservation(List<Deck<ExplodingKittenCard>> playerDecks,
                                      Deck<ExplodingKittenCard> drawPile,
                                      Deck<ExplodingKittenCard> discardPile,
                                      int currentPlayer){
        playerHandCards = playerDecks;
        this.drawPile = drawPile;
        this.discardPile = discardPile;
        this.currentPlayer = currentPlayer;
    }

    @Override
    public void printToConsole() {
        for (int i = 0; i < playerHandCards.size(); i++){
            if (currentPlayer == i)
                System.out.print(">>> Player " + i + ":");
            else
                System.out.print("Player " + i + ":");
            printDeck(playerHandCards.get(i));
        }

        System.out.print("DrawPile" + ":");
        printDeck(drawPile);

        System.out.print("DiscardPile" + ":");
        printDeck(discardPile);
    }

    public void printDeck(IDeck<ExplodingKittenCard> deck){
        StringBuilder sb = new StringBuilder();
        for (ExplodingKittenCard card : deck.getCards(currentPlayer)){
            if (card == null)
                sb.append("UNKNOWN");
            else
                sb.append(card.cardType.toString());
            sb.append(",");
        }
        if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
        System.out.println(sb.toString());
    }


}
