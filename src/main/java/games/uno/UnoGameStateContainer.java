package games.uno;

import core.AbstractGameState;
import core.AbstractGameStateContainer;
import core.components.Deck;
import games.uno.cards.UnoCard;

import java.util.List;

public class UnoGameStateContainer extends AbstractGameStateContainer {
    List<Deck<UnoCard>> playerDecks;
    Deck<UnoCard> drawDeck;
    Deck<UnoCard> discardDeck;
    UnoCard currentCard;
    String currentColor;
    int[] playerScore;
    int[] expulsionRound;

    // Turn order data
    boolean skipTurn;
    int direction;
    public UnoGameStateContainer(UnoGameState gs) {
        super(gs);
        playerDecks = gs.playerDecks;
        drawDeck = gs.drawDeck;
        discardDeck = gs.discardDeck;
        currentCard = gs.currentCard;
        currentColor = gs.currentColor;
        playerScore = gs.playerScore;
        expulsionRound = gs.expulsionRound;

        // Turn order data
        skipTurn = gs.skipTurn;
        direction = gs.direction;
    }
}
