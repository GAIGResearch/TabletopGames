package games.dominion;

import core.*;
import core.components.*;
import core.turnorders.*;
import games.dominion.cards.*;

import java.util.*;

public class DominionGameState extends AbstractGameState {

    Random rnd;
    int playerCount;
    DominionParameters params;

    // Counts of cards on the table should be fine
    Map<CardType, Integer> cardsAvailable;

    // Then Decks for each player - Hand, Discard and Draw

    // TODO: Convert these to use PartialObservableDecks
    Deck<Card>[] playerHands;
    Deck<Card>[] playerDrawPiles;
    Deck<Card>[] playerDiscards;

    // Trash pile and other global decks
    Deck<Card> trashPile;


    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - number of players
     */
    public DominionGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new DominionTurnOrder(nPlayers));
        rnd = new Random(gameParameters.getRandomSeed());
        playerCount = nPlayers;
        params = (DominionParameters) gameParameters;
        _reset();
    }

    /**
     * Returns all components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state.
     *
     * @return - List of components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        return null;
    }

    /**
     * Create a copy of the game state containing only those components the given player can observe (if partial
     * observable).
     *
     * @param playerId - player observing this game state.
     */
    @Override
    protected AbstractGameState _copy(int playerId) {
        DominionGameState retValue = new DominionGameState(gameParameters.copy(), playerCount);
        for (CardType ct : cardsAvailable.keySet()) {
            retValue.cardsAvailable.put(ct, cardsAvailable.get(ct));
        }
        for (int p = 0; p < playerCount; p++) {
            retValue.playerHands[p] = playerHands[p].copy();
            retValue.playerDrawPiles[p] = playerDrawPiles[p].copy();
            retValue.playerDiscards[p] = playerDiscards[p].copy();
        }
        retValue.trashPile = trashPile.copy();

        return retValue;
    }

    /**
     * Provide a simple numerical assessment of the current game state, the bigger the better.
     * Subjective heuristic function definition.
     *
     * @param playerId - player observing the state.
     * @return - double, score of current state.
     */
    @Override
    protected double _getScore(int playerId) {
        return 0;
    }

    /**
     * Provide a list of component IDs which are hidden in partially observable copies of games.
     * Depending on the game, in the copies these might be completely missing, or just randomized.
     *
     * @param playerId - ID of player observing the state.
     * @return - list of component IDs unobservable by the given player.
     */
    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return null;
    }

    /**
     * Resets variables initialised for this game state.
     */
    @Override
    protected void _reset() {
        playerHands = new Deck[playerCount];
        playerDrawPiles = new Deck[playerCount];
        playerDiscards = new Deck[playerCount];
        trashPile = new Deck("Trash");
        for (int i = 0; i < playerCount; i++) {
            playerHands[i] = new Deck<>("Hand of Player " + i + 1);
            for (int j = 0; j < 5; j++) {
                playerDrawPiles[i].add(new DominionCard(CardType.COPPER));
                playerDrawPiles[i].add(new DominionCard(CardType.ESTATE));
            }
            playerDrawPiles[i].shuffle(rnd);
            for (int k = 0; k < 5; k++) playerHands[i].add(playerDrawPiles[i].draw());
        }

        cardsAvailable = new HashMap<>(13);
        cardsAvailable.put(CardType.PROVINCE, 12);
        cardsAvailable.put(CardType.DUCHY, 12);
        cardsAvailable.put(CardType.ESTATE, 12);
        for (CardType ct : params.cardsUsed) {
            cardsAvailable.put(ct, 10);
        }
    }

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
