package games.wonders7;

import core.AbstractGameState;
import core.AbstractGameStateContainer;
import core.components.Deck;
import games.wonders7.cards.Wonder7Board;
import games.wonders7.cards.Wonder7Card;

import java.util.EnumMap;
import java.util.List;

public class Wonders7GameStateContainer extends AbstractGameStateContainer {
    int currentAge; // int from 1,2,3 of current age
    List<EnumMap<Wonders7Constants.Resource, Integer>> playerResources; // Each player's full resource counts
    List<Deck<Wonder7Card>> playerHands; // Player Hands
    List<Deck<Wonder7Card>> playedCards; // Player used cards
    Deck<Wonder7Card> ageDeck; // The 'draw deck' for the Age
    Deck<Wonder7Card> discardPile; // Discarded cards
    Deck<Wonder7Board> wonderBoardDeck; // The deck of wonder board that decide a players wonder
    Wonder7Board[] playerWonderBoard; // Every player's assigned Wonder Board
    public Wonders7GameStateContainer(Wonders7GameState gs) {
        super(gs);
        currentAge = gs.currentAge;
        playerResources = gs.playerResources;
        playerHands = gs.playerHands;
        playedCards = gs.playedCards;
        ageDeck = gs.ageDeck;
        discardPile = gs.discardPile;
        wonderBoardDeck = gs.wonderBoardDeck;
        playerWonderBoard = gs.playerWonderBoard;
    }
}
