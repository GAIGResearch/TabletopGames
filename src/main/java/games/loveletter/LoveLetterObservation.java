package games.loveletter;

import core.components.Deck;
import core.components.IDeck;
import core.observations.IObservation;
import core.observations.IPrintable;
import games.explodingkittens.cards.ExplodingKittenCard;
import games.loveletter.cards.LoveLetterCard;

import java.util.List;

public class LoveLetterObservation implements IPrintable, IObservation {

    private final List<Deck<LoveLetterCard>> playerHandCards;
    private final Deck<LoveLetterCard> drawPile;
    private final Deck<LoveLetterCard> discardPile;
    private final int currentPlayer;

    public LoveLetterObservation(List<Deck<LoveLetterCard>> playerDecks,
                                 Deck<LoveLetterCard> drawPile,
                                 Deck<LoveLetterCard> discardPile,
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

    public void printDeck(IDeck<LoveLetterCard> deck){
        StringBuilder sb = new StringBuilder();
        for (LoveLetterCard card : deck.getCards(currentPlayer)){
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
