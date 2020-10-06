package games.dominion;

import core.*;
import core.components.*;
import core.interfaces.IGamePhase;
import games.dominion.cards.*;
import utilities.Utils;

import java.util.*;

public class DominionGameState extends AbstractGameState {

    public enum DominionGamePhase implements IGamePhase {
        Play,
        Buy,
        CleanUp
    }

    Random rnd;
    int playerCount;
    DominionParameters params;

    // Counts of cards on the table should be fine
    Map<CardType, Integer> cardsAvailable;

    // Then Decks for each player - Hand, Discard and Draw
    // TODO: Convert these to use PartialObservableDecks
    Deck<DominionCard>[] playerHands;
    Deck<DominionCard>[] playerDrawPiles;
    Deck<DominionCard>[] playerDiscards;

    DominionPlayerState[] playerStates;

    // Trash pile and other global decks
    Deck<DominionCard> trashPile;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players
     */
    public DominionGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new DominionTurnOrder(nPlayers));
        DominionParameters params = (DominionParameters) gameParameters;
        rnd = new Random(gameParameters.getRandomSeed());
        playerCount = nPlayers;
        _reset();
    }

    public DominionPlayerState getPlayerState(int playerID) {
        return playerStates[playerID];
    }

    public boolean removeCardFromTable(CardType type) {
        if (CardType.infiniteSupplyCards.contains(type)) {
            return true;
        }
        if (cardsAvailable.getOrDefault(type, 0) > 0) {
            cardsAvailable.put(type, cardsAvailable.get(type));
        }
        return false;
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
            retValue.playerStates[p] = playerStates[p].copy();
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
        double score = Utils.summariseDeck(playerDiscards[playerId], DominionCard::victoryPoints);
        score += Utils.summariseDeck(playerDrawPiles[playerId], DominionCard::victoryPoints);
        score += Utils.summariseDeck(playerHands[playerId], DominionCard::victoryPoints);
        return score;
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
        playerStates = new DominionPlayerState[playerCount];
        trashPile = new Deck("Trash");
        for (int i = 0; i < playerCount; i++) {
            playerStates[i] = new DominionPlayerState(i);
            playerHands[i] = new Deck<>("Hand of Player " + i + 1);
            for (int j = 0; j < 5; j++) {
                playerDrawPiles[i].add(DominionCard.create(CardType.COPPER));
                playerDrawPiles[i].add(DominionCard.create(CardType.ESTATE));
            }
            playerDrawPiles[i].shuffle(rnd);
            for (int k = 0; k < 5; k++) playerHands[i].add(playerDrawPiles[i].draw());
        }

        cardsAvailable = new HashMap<>(13);
        cardsAvailable.put(CardType.PROVINCE, 12);
        cardsAvailable.put(CardType.DUCHY, 12);
        cardsAvailable.put(CardType.ESTATE, 12);
        for (CardType ct : params.cardsUsed.keySet()) {
            cardsAvailable.put(ct, params.cardsUsed.get(ct));
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
        if (this == o) return true;
        if (!(o instanceof DominionGameState)) return false;
        DominionGameState other = (DominionGameState) o;
        return cardsAvailable.equals(other.cardsAvailable) &&
                Arrays.equals(playerHands, other.playerHands) &&
                Arrays.equals(playerDiscards, other.playerDiscards) &&
                Arrays.equals(playerDrawPiles, other.playerDrawPiles) &&
                Arrays.equals(playerStates, other.playerStates) &&
                trashPile.equals(other.trashPile);
    }
}
