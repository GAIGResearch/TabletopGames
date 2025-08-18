package games.dominion;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Card;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import games.GameType;
import games.dominion.DominionConstants.DeckType;
import games.dominion.actions.IDelayedAction;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

public class DominionGameState extends AbstractGameState implements IPrintable {

    Map<CardType, Integer> cardsIncludedInGame = new HashMap<>();
    // Then Decks for each player - Hand, Discard and Draw
    PartialObservableDeck<DominionCard>[] playerHands;
    PartialObservableDeck<DominionCard>[] playerDrawPiles;
    Deck<DominionCard>[] playerDiscards;
    Deck<DominionCard>[] playerTableaux;
    // Trash pile and other global decks
    Deck<DominionCard> trashPile;
    boolean[] defenceStatus;
    int buysLeftForCurrentPlayer = 1;
    int actionsLeftForCurrentPlayer = 1;
    int spentSoFar = 0;
    int additionalSpendAvailable = 0;
    List<IDelayedAction> delayedActions = new ArrayList<>();


    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players
     */
    public DominionGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        this.reset();
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Dominion;
    }

    public boolean removeCardFromTable(CardType type) {
        if (cardsIncludedInGame.getOrDefault(type, 0) > 0) {
            cardsIncludedInGame.put(type, cardsIncludedInGame.get(type) - 1);
            return true;
        }
        return false;
    }

    public void addCard(CardType type, int playerId, DeckType deckType) {
        DominionCard newCard = DominionCard.create(type);
        Deck<DominionCard> deck = getDeck(deckType, playerId);
        deck.add(newCard);
    }

    public int getEmptyDeckCount() {
        return Math.toIntExact(cardsIncludedInGame.values().stream().filter(i -> i == 0).count());
    }

    public boolean gameOver() {
        DominionParameters params = (DominionParameters) gameParameters;
        return cardsIncludedInGame.get(CardType.PROVINCE) == 0 ||
                getEmptyDeckCount() >= params.PILES_EXHAUSTED_FOR_GAME_END;
    }

    public boolean drawCard(int playerId) {
        return drawCard(playerId, DeckType.DRAW, playerId, DeckType.HAND);
    }

    public boolean drawCard(int fromPlayer, DeckType fromDeck, int toPlayer, DeckType toDeck) {
        Deck<DominionCard> source = getDeck(fromDeck, fromPlayer);
        Deck<DominionCard> destination = getDeck(toDeck, toPlayer);
        if (source.getSize() == 0) {
            // do stuff
            if (fromDeck == DeckType.DRAW) {
                Deck<DominionCard> discard = getDeck(DeckType.DISCARD, fromPlayer);
                if (discard.getSize() == 0)
                    return false;
                source.add(discard);
                discard.clear();
                source.shuffle(rnd);
            } else {
                return false;
            }
        }
        DominionCard cardDrawn = source.draw();
        destination.add(cardDrawn);
        return true;
    }

    public void moveCard(CardType type, int fromPlayer, DeckType fromDeck, int toPlayer, DeckType toDeck) {
        DominionCard cardToMove = getDeck(fromDeck, fromPlayer).stream()
                .filter(c -> c.cardType() == type)
                .findFirst().orElse(null);
        if (cardToMove == null)
            throw new IllegalArgumentException("Card of type " + type + " not found in deck " + fromDeck + " of player " + fromPlayer);

        moveCard(cardToMove, fromPlayer, fromDeck, toPlayer, toDeck);
    }

    public void moveCard(DominionCard cardToMove, int fromPlayer, DeckType fromDeck, int toPlayer, DeckType toDeck) {
        getDeck(fromDeck, fromPlayer).remove(cardToMove);
        getDeck(toDeck, toPlayer).add(cardToMove);
    }

    /**
     * The number of actions the current player has left
     */
    public int getActionsLeft() {
        return actionsLeftForCurrentPlayer;
    }

    public void changeActions(int delta) {
        actionsLeftForCurrentPlayer += delta;
    }

    /**
     * The number of buys the current player has left
     */
    public int getBuysLeft() {
        return buysLeftForCurrentPlayer;
    }

    public void changeBuys(int delta) {
        buysLeftForCurrentPlayer += delta;
    }

    public void spend(int delta) {
        spentSoFar += delta;
    }

    public void changeAdditionalSpend(int delta) {
        additionalSpendAvailable += delta;
    }

    /**
     * How much money does the player have available to spend?
     */
    public int getAvailableSpend(int playerID) {
        if (playerID != turnOwner) {
            return 0;
        }
        int totalTreasureInHand = playerHands[playerID].sumInt(DominionCard::treasureValue);
        return totalTreasureInHand - spentSoFar + additionalSpendAvailable;
    }

    public void addDelayedAction(IDelayedAction action) {
        delayedActions.add(action);
    }

    /**
     * Returns all components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state.
     *
     * @return - List of components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(Arrays.asList(playerHands));
        components.addAll(Arrays.asList(playerDiscards));
        components.addAll(Arrays.asList(playerTableaux));
        components.addAll(Arrays.asList(playerDrawPiles));
        components.add(trashPile);
        return components;
    }

    /**
     * Get the Deck of the specified type for the specified player.
     * DeckType.ALL is not valid
     */
    public Deck<DominionCard> getDeck(DeckType deck, int playerId) {
        switch (deck) {
            case HAND:
                return playerHands[playerId];
            case DRAW:
                return playerDrawPiles[playerId];
            case DISCARD:
                return playerDiscards[playerId];
            case TABLE:
                return playerTableaux[playerId];
            case TRASH:
                return trashPile;
        }
        throw new AssertionError("Unknown deck type " + deck);
    }

    /**
     * How many cards of the specified type are in the player's deck?
     * Use DeckType.ALL to count all cards in all decks.
     */
    public int cardsOfType(CardType type, int playerId, DeckType deck) {
        Deck<DominionCard> allCards;
        switch (deck) {
            case SUPPLY:
                return cardsIncludedInGame.getOrDefault(type, 0);
            case HAND:
            case TABLE:
            case DRAW:
            case DISCARD:
            case TRASH:
                allCards = getDeck(deck, playerId);
                break;
            case ALL:
                allCards = new Deck<>("temp", VISIBLE_TO_ALL);
                allCards.add(playerHands[playerId]);
                allCards.add(playerDiscards[playerId]);
                allCards.add(playerDrawPiles[playerId]);
                allCards.add(playerTableaux[playerId]);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + deck);
        }

        return (int) allCards.stream().filter(c -> c.cardType() == type).count();
    }

    public List<CardType> getCardsToBuy() {
        return cardsIncludedInGame.keySet().stream()
                .filter(c -> cardsIncludedInGame.get(c) > 0)
                .sorted(comparingInt(c -> -c.cost))
                .distinct()
                .collect(toList());
    }

    public List<CardType> cardsIncludedInGame() {
        return cardsIncludedInGame.keySet().stream()
                .sorted(comparingInt(c -> -c.cost))
                .collect(toList());
    }

    public Map<CardType, Integer> getCardsIncludedInGame() {
        return cardsIncludedInGame;
    }

    public void setDefended(int playerId) {
        defenceStatus[playerId] = true;
    }

    public boolean isDefended(int playerId) {
        return defenceStatus[playerId];
    }

    /**
     * Create a copy of the game state containing only those components the given player can observe (if partial
     * observable).
     *
     * @param playerId - player observing this game state.
     */
    @Override
    protected AbstractGameState _copy(int playerId) {
        DominionGameState retValue = new DominionGameState(((DominionParameters) gameParameters).shallowCopy(), nPlayers);
        for (CardType ct : cardsIncludedInGame.keySet()) {
            retValue.cardsIncludedInGame.put(ct, cardsIncludedInGame.get(ct));
        }
        for (int p = 0; p < nPlayers; p++) {
            if (playerId == -1) {
                retValue.playerHands[p] = playerHands[p].copy();
                retValue.playerDrawPiles[p] = playerDrawPiles[p].copy();
            } else if (playerId == p) {
                // need to shuffle drawpile separately
                retValue.playerHands[p] = playerHands[p].copy();
                retValue.playerDrawPiles[p] = playerDrawPiles[p].copy();
                retValue.playerDrawPiles[p].redeterminiseUnknown(redeterminisationRnd, p);
            } else {
                // need to combine and shuffle hands and drawpiles
                retValue.playerDrawPiles[p] = playerDrawPiles[p].copy();
                retValue.playerHands[p] = playerHands[p].copy();
                for (int i = 0; i < retValue.playerHands[p].getSize(); i++) {
                    // if we (the perspective player) can see the card, then we need to keep it in place
                    // if not then we move it to the *bottom* of the drawpile (this is the end of an ArrayList...so more efficient?)
                    if (!retValue.playerHands[p].getVisibilityForPlayer(i, playerId)) {
                        retValue.playerDrawPiles[p].add(retValue.playerHands[p].get(i), retValue.playerDrawPiles[p].getSize());
                    }
                }
                // we have now moved all the non-visible Hand cards into the Draw pile to reshuffle
                retValue.playerHands[p].clear(); // we will need to reconstruct this, including visibility status in a sec
                // we then reshuffle all the non-visible cards
                retValue.playerDrawPiles[p].redeterminiseUnknown(redeterminisationRnd, playerId);
                // we then remove cards from the top of the shuffled draw pile (in the region we know is not visible)
                for (int i = 0; i < playerHands[p].getSize(); i++) {
                    if (!playerHands[p].getVisibilityForPlayer(i, playerId)) {
                        // we then pick cards from the end of the drawpile List and add them to the Hand
                        // possibly more efficient picking from the end of an ArrayList?
                        retValue.playerHands[p].add(retValue.playerDrawPiles[p].pick(retValue.playerDrawPiles[p].getSize() - 1), i);
                    } else {
                        // we know what this card is, so copy over visibility status
                        retValue.playerHands[p].add(playerHands[p].get(i).copy(), playerHands[p].getVisibilityOfComponent(i).clone());
                    }
                }
            }
            retValue.playerDiscards[p] = playerDiscards[p].copy();
            retValue.playerTableaux[p] = playerTableaux[p].copy();
        }
        retValue.trashPile = trashPile.copy();
        retValue.buysLeftForCurrentPlayer = buysLeftForCurrentPlayer;
        retValue.actionsLeftForCurrentPlayer = actionsLeftForCurrentPlayer;
        retValue.spentSoFar = spentSoFar;
        retValue.additionalSpendAvailable = additionalSpendAvailable;

        retValue.defenceStatus = defenceStatus.clone();

        retValue.delayedActions = delayedActions.stream().map(IDelayedAction::copy).collect(toList());
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
    protected double _getHeuristicScore(int playerId) {
        if (getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            return -1.0;
        if (getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME)
            return 1.0;

        int score = getTotal(playerId, c -> c.victoryPoints(playerId, this));
        return score / 100.0;
    }

    /**
     * This provides the current score for the specified player.
     */
    @Override
    public double getGameScore(int playerId) {
        return getTotal(playerId, c -> c.victoryPoints(playerId, this));
    }

    /**
     * This provides the total value of the specified deck for the player using the provided cardValuer function
     */
    public int getTotal(int playerId, DeckType deck, Function<DominionCard, Integer> cardValuer) {
        return getDeck(deck, playerId).sumInt(cardValuer);
    }

    /**
     * This provides the total value across all decks for the player using the provided cardValuer function
     */
    public int getTotal(int playerId, Function<DominionCard, Integer> cardValuer) {
        int score = playerHands[playerId].sumInt(cardValuer);
        score += playerDiscards[playerId].sumInt(cardValuer);
        score += playerTableaux[playerId].sumInt(cardValuer);
        score += playerDrawPiles[playerId].sumInt(cardValuer);
        return score;
    }

    // How many cards does the player have in total?
    public int getTotalCards(int playerId) {
        return playerDrawPiles[playerId].getSize() + playerDiscards[playerId].getSize()
                + playerHands[playerId].getSize() + playerTableaux[playerId].getSize();
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
        return new ArrayList<>();
    }

    /**
     * Resets variables initialised for this game state.
     */
    @Override
    protected void reset() {
        playerHands = new PartialObservableDeck[nPlayers];
        playerDrawPiles = new PartialObservableDeck[nPlayers];
        playerDiscards = new Deck[nPlayers];
        playerTableaux = new Deck[nPlayers];

        trashPile = new Deck<>("Trash", VISIBLE_TO_ALL);
        for (int i = 0; i < nPlayers; i++) {
            boolean[] handVisibility = new boolean[nPlayers];
            handVisibility[i] = true;
            playerHands[i] = new PartialObservableDeck<>("Hand of Player " + i + 1, i, handVisibility);
            playerDrawPiles[i] = new PartialObservableDeck<>("Drawpile of Player " + i + 1, i, new boolean[nPlayers]);
            playerDiscards[i] = new Deck<>("Discard of Player " + i + 1, i, VISIBLE_TO_ALL);
            playerTableaux[i] = new Deck<>("Tableau of Player " + i + 1, i, VISIBLE_TO_ALL);
        }
        super.reset();
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
        if (!(o instanceof DominionGameState other)) return false;
        return cardsIncludedInGame.equals(other.cardsIncludedInGame) &&
                Arrays.equals(playerHands, other.playerHands) &&
                Arrays.equals(playerResults, other.playerResults) &&
                Arrays.equals(playerDiscards, other.playerDiscards) &&
                Arrays.equals(playerTableaux, other.playerTableaux) &&
                Arrays.equals(playerDrawPiles, other.playerDrawPiles) &&
                trashPile.equals(other.trashPile) &&
                buysLeftForCurrentPlayer == other.buysLeftForCurrentPlayer &&
                actionsLeftForCurrentPlayer == other.actionsLeftForCurrentPlayer &&
                spentSoFar == other.spentSoFar && additionalSpendAvailable == other.additionalSpendAvailable &&
                Arrays.equals(defenceStatus, other.defenceStatus) &&
                delayedActions.equals(other.delayedActions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(cardsIncludedInGame, trashPile, buysLeftForCurrentPlayer,
                actionsLeftForCurrentPlayer, spentSoFar, additionalSpendAvailable, delayedActions, super.hashCode());
        result = result + 743 * Arrays.hashCode(playerHands) + 353 * Arrays.hashCode(playerDiscards) +
                11 * Arrays.hashCode(playerTableaux) + 41 * Arrays.hashCode(playerDrawPiles) + Arrays.hashCode(defenceStatus);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder retValue = new StringBuilder();
        retValue.append(String.format("Turn: %d, Current Player: %d, Phase: %s%n", getRoundCounter(), getCurrentPlayer(), gamePhase));
        for (Map.Entry<CardType, Integer> s : cardsIncludedInGame.entrySet()) {
            retValue.append(String.format("\t%2d %s%n", s.getValue(), s.getKey()));
        }
        for (int p = 0; p < getNPlayers(); p++) {
            retValue.append(String.format("Player: %d, Score: %2.0f, Hand: %d, Deck: %d, Discard: %d, Actions: %d, Buys: %d%n",
                    p, getGameScore(p), playerHands[p].getSize(), playerDrawPiles[p].getSize(), playerDiscards[p].getSize(),
                    p == getCurrentPlayer() ? actionsLeftForCurrentPlayer : 0,
                    p == getCurrentPlayer() ? buysLeftForCurrentPlayer : 0));
            retValue.append("Tableau:\n\t");
            retValue.append(getDeck(DeckType.TABLE, p).stream().map(Card::toString).collect(Collectors.joining(", ")));
            retValue.append("\nHand:\n\t");
            retValue.append(getDeck(DeckType.HAND, p).stream().map(Card::toString).collect(Collectors.joining(", ")));
            retValue.append("\n");
        }
        retValue.append("\n\nHistory:\n\t");
        int historyLength = getHistoryAsText().size();
        retValue.append(getHistoryAsText().subList(Math.max(0, historyLength - 10), historyLength).stream().map(Objects::toString).collect(Collectors.joining("\n\t")));
        retValue.append("\n").append("Available Actions: \n");
        DominionForwardModel fm = new DominionForwardModel();
        retValue.append(fm._computeAvailableActions(this).stream().map(Objects::toString).collect(Collectors.joining(", ")));
        retValue.append("\n");
        return retValue.toString();
    }

    public enum DominionGamePhase implements IGamePhase {
        Play,
        Buy
    }
}
