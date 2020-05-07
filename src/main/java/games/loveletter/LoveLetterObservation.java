package games.loveletter;

import core.components.Deck;
import core.components.IDeck;
import core.observations.IObservation;
import core.observations.IPrintable;
import games.explodingkittens.cards.ExplodingKittenCard;
import games.loveletter.cards.LoveLetterCard;

import java.util.List;

public class LoveLetterObservation implements IPrintable, IObservation {

    private final List<PartialObservableDeck<LoveLetterCard>> playerHandCards;
    private final List<Deck<LoveLetterCard>> playerDiscardCards;
    private final Deck<LoveLetterCard> drawPile;
    private final Deck<LoveLetterCard> discardPile;
    private final boolean[] effectProtection;
    private final boolean[] isAlive;
    private final int currentPlayer;
    private final LoveLetterGameState.GamePhase gamePhase;

    public LoveLetterObservation(List<PartialObservableDeck<LoveLetterCard>> playerDecks,
                                 List<Deck<LoveLetterCard>> playerDiscardCards,
                                 Deck<LoveLetterCard> drawPile,
                                 Deck<LoveLetterCard> discardPile,
                                 boolean[] effectProtection,
                                 int currentPlayer,
                                 LoveLetterGameState.GamePhase gamePhase,
                                 boolean[] isAlive){
        this.playerHandCards = playerDecks;
        this.playerDiscardCards = playerDiscardCards;
        this.drawPile = drawPile;
        this.discardPile = discardPile;
        this.effectProtection = effectProtection;
        this.currentPlayer = currentPlayer;
        this.gamePhase = gamePhase;
        this.isAlive = isAlive;
    }


    public void printToConsole() {
        System.out.println("Love Letter Game-State");
        System.out.println("======================");

        for (int i = 0; i < playerHandCards.size(); i++){
            if (currentPlayer == i)
                System.out.print(">>> Player " + i + ":");
            else
                System.out.print("Player " + i + ": ");
            printDeck(playerHandCards.get(i));
            System.out.print(";\t Discarded: ");
            printDeck(playerDiscardCards.get(i));
            System.out.print(";\t Protected: ");
            System.out.print(effectProtection[i]);
            System.out.print(";\t Alive: ");
            System.out.println(isAlive[i]);
        }

        System.out.print("DrawPile" + ":");
        printDeck(drawPile);
        System.out.println();

        System.out.print("DiscardPile" + ":");
        printDeck(discardPile);
        System.out.println();

        System.out.println("Current GamePhase: " + gamePhase);
        System.out.println();
    }

    public void printDeck(IDeck<LoveLetterCard> deck){
        StringBuilder sb = new StringBuilder();
        for (LoveLetterCard card : deck.getCards()){
            sb.append(card.cardType.toString());
            sb.append(",");
        }
        if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
        System.out.print(sb.toString());
    }

    public void printDeck(IPartialObservableDeck<LoveLetterCard> deck){
        StringBuilder sb = new StringBuilder();
        for (LoveLetterCard card : deck.getVisibleCards(currentPlayer)){
            if (card == null)
                sb.append("UNKNOWN");
            else
                sb.append(card.cardType.toString());
            sb.append(",");
        }
        if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
        System.out.print(sb.toString());
    }



}
