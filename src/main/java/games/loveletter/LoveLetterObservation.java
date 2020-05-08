package games.loveletter;

import core.components.Deck;
import core.components.PartialObservableDeck;
import core.observations.IObservation;
import core.observations.IPrintable;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.List;

public class LoveLetterObservation implements IPrintable, IObservation {

    private final List<PartialObservableDeck<LoveLetterCard>> playerHandCards;
    private final List<Deck<LoveLetterCard>> playerDiscardCards;
    private final Deck<LoveLetterCard> drawPile;
    private final PartialObservableDeck<LoveLetterCard> reserveCards;
    private final boolean[] effectProtection;
    private final Utils.GameResult[] isAlive;
    private final int currentPlayer;
    private final LoveLetterGameState.GamePhase gamePhase;

    public LoveLetterObservation(List<PartialObservableDeck<LoveLetterCard>> playerDecks,
                                 List<Deck<LoveLetterCard>> playerDiscardCards,
                                 Deck<LoveLetterCard> drawPile,
                                 PartialObservableDeck<LoveLetterCard> reserveCards,
                                 boolean[] effectProtection,
                                 int currentPlayer,
                                 LoveLetterGameState.GamePhase gamePhase,
                                 Utils.GameResult[] isAlive){
        this.playerHandCards = playerDecks;
        this.playerDiscardCards = playerDiscardCards;
        this.drawPile = drawPile;
        this.reserveCards = reserveCards;
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
            System.out.print(playerHandCards.get(i).toString(currentPlayer));
            System.out.print(";\t Discarded: ");
            System.out.print(playerDiscardCards.get(i));
            System.out.print(";\t Protected: ");
            System.out.print(effectProtection[i]);
            System.out.print(";\t Status: ");
            System.out.println(isAlive[i]);
        }

        System.out.print("DrawPile" + ":");
        System.out.print(drawPile);
        System.out.println();

        System.out.print("ReserveCards" + ":");
        System.out.println(reserveCards.toString(currentPlayer));
        System.out.println();

        System.out.println("Current GamePhase: " + gamePhase);
        System.out.println();
    }
}
