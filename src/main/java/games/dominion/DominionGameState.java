package games.dominion;

import core.*;
import core.components.*;
import core.interfaces.IGamePhase;
import games.dominion.actions.IExtendedSequence;
import games.dominion.cards.*;
import games.dominion.DominionConstants.*;
import utilities.Utils;

import java.util.*;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;

public class DominionGameState extends AbstractGameState {

    public enum DominionGamePhase implements IGamePhase {
        Play,
        Buy
    }

    Random rnd;
    int playerCount;
    boolean winLose = false;

    // Counts of cards on the table should be fine
    Map<CardType, Integer> cardsIncludedInGame;

    // Then Decks for each player - Hand, Discard and Draw
    PartialObservableDeck<DominionCard>[] playerHands;
    PartialObservableDeck<DominionCard>[] playerDrawPiles;
    Deck<DominionCard>[] playerDiscards;
    Deck<DominionCard>[] playerTableaux;

    boolean[] defenceStatus;

    int buysLeftForCurrentPlayer = 1;
    int actionsLeftForCurrentPlayer = 1;
    int spentSoFar = 0;
    int additionalSpendAvailable = 0;

    // Trash pile and other global decks
    Deck<DominionCard> trashPile;

    Stack<IExtendedSequence> actionsInProgress = new Stack<>();

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players
     */
    public DominionGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new DominionTurnOrder(nPlayers));
        rnd = new Random(gameParameters.getRandomSeed());
        playerCount = nPlayers;
        defenceStatus = new boolean[nPlayers];  // defaults to false
        this._reset();
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

    public void endOfTurn(int playerID) {
        if (!actionsInProgress.empty())
            throw new AssertionError("Should not have an action in progress beyond the Play phase (yet)");
        if (playerID != getCurrentPlayer())
            throw new AssertionError("Not yet supported");
        // 1) put hand and cards played into discard
        // 2) draw 5 new cards
        // 3) shuffle and move discard if we run out
        Deck<DominionCard> hand = playerHands[playerID];
        Deck<DominionCard> discard = playerDiscards[playerID];
        Deck<DominionCard> table = playerTableaux[playerID];

        discard.add(hand);
        discard.add(table);
        table.clear();
        hand.clear();
        for (int i = 0; i < 5; i++)
            drawCard(playerID);

        defenceStatus = new boolean[playerCount];  // resets to false

        actionsLeftForCurrentPlayer = 1;
        spentSoFar = 0;
        additionalSpendAvailable = 0;
        buysLeftForCurrentPlayer = 1;
        setGamePhase(DominionGameState.DominionGamePhase.Play);
        getTurnOrder().endPlayerTurn(this);
    }

    public boolean gameOver() {
        return cardsIncludedInGame.get(CardType.PROVINCE) == 0 ||
                cardsIncludedInGame.values().stream().filter(i -> i == 0).count() >= 3;
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

    public boolean moveCard(CardType type, int fromPlayer, DeckType fromDeck, int toPlayer, DeckType toDeck) {
        DominionCard cardToMove = getDeck(fromDeck, fromPlayer).stream()
                .filter(c -> c.cardType() == type)
                .findFirst().orElse(null);
        if (cardToMove == null)
            return false;

        return moveCard(cardToMove, fromPlayer, fromDeck, toPlayer, toDeck);
    }

    public boolean moveCard(DominionCard cardToMove, int fromPlayer, DeckType fromDeck, int toPlayer, DeckType toDeck) {
        boolean cardFound = getDeck(fromDeck, fromPlayer).remove(cardToMove);
        if (cardFound) {
            getDeck(toDeck, toPlayer).add(cardToMove);
            return true;
        }
        return false;
    }

    public int actionsLeft() {
        return actionsLeftForCurrentPlayer;
    }

    public void changeActions(int delta) {
        actionsLeftForCurrentPlayer += delta;
    }

    public int buysLeft() {
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

    public int availableSpend(int playerID) {
        if (playerID != turnOrder.getTurnOwner())
            throw new AssertionError("Not yet supported");
        int totalTreasureInHand = playerHands[playerID].sumInt(DominionCard::treasureValue);
        return totalTreasureInHand - spentSoFar + additionalSpendAvailable;
    }

    public IExtendedSequence currentActionInProgress() {
        return actionsInProgress.isEmpty() ? null : actionsInProgress.peek();
    }

    public boolean isActionInProgress() {
        return !actionsInProgress.empty();
    }

    public void setActionInProgress(IExtendedSequence action) {
        if (gamePhase != DominionGamePhase.Play)
            throw new AssertionError("ExtendedActions are currently only supported during the Play action phase");
        if (action == null && !actionsInProgress.isEmpty())
            actionsInProgress.pop();
        else
            actionsInProgress.push(action);
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
                allCards = new Deck<>("temp");
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

    public List<CardType> cardsToBuy() {
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
        DominionGameState retValue = new DominionGameState(gameParameters.copy(), playerCount);
        for (CardType ct : cardsIncludedInGame.keySet()) {
            retValue.cardsIncludedInGame.put(ct, cardsIncludedInGame.get(ct));
        }
        for (int p = 0; p < playerCount; p++) {
            if (playerId == -1) {
                retValue.playerHands[p] = playerHands[p].copy();
                retValue.playerDrawPiles[p] = playerDrawPiles[p].copy();
            } else if (playerId == p) {
                // need to shuffle drawpile separately
                retValue.playerHands[p] = playerHands[p].copy();
                retValue.playerDrawPiles[p] = playerDrawPiles[p].copy();
                retValue.playerDrawPiles[p].shuffleVisible(rnd, p, false);
            } else {
                // need to combine and shuffle hands and drawpiles
                retValue.playerDrawPiles[p] = playerDrawPiles[p].copy();
                retValue.playerHands[p] = playerHands[p].copy();
                for (int i = 0; i < retValue.playerHands[p].getSize(); i++) {
                    // if we (the perspective player) can see the card, then we need to keep it in place
                    if (!retValue.playerHands[p].getVisibilityForPlayer(i, playerId)) {
                        retValue.playerDrawPiles[p].add(retValue.playerHands[p].get(i));
                    }
                }
                retValue.playerHands[p].clear(); // we will need to reconstruct this, including visibility status in a sec
                // TODO: Currently this assumes playerDrawPile is non-observable. this will change once certain action cards introduced
                retValue.playerDrawPiles[p].shuffle(rnd);
                for (int i = 0; i < playerHands[p].getSize(); i++) {
                    if (!playerHands[p].getVisibilityForPlayer(i, playerId)) {
                        retValue.playerHands[p].add(retValue.playerDrawPiles[p].draw(), i);
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

        retValue.actionsInProgress = new Stack<>();
        actionsInProgress.forEach(
                a -> retValue.actionsInProgress.push(a.copy())
        );
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
        double divisor = winLose ? 100.0 : 1.0;
        if (winLose) {
            if (getPlayerResults()[playerId] == Utils.GameResult.LOSE)
                return -1.0;
            if (getPlayerResults()[playerId] == Utils.GameResult.WIN)
                return 1.0;
        }
        int score = playerHands[playerId].sumInt(DominionCard::victoryPoints);
        score += playerDiscards[playerId].sumInt(DominionCard::victoryPoints);
        score += playerTableaux[playerId].sumInt(DominionCard::victoryPoints);
        score += playerDrawPiles[playerId].sumInt(DominionCard::victoryPoints);
        return score / divisor;
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
    protected void _reset() {
        playerHands = new PartialObservableDeck[playerCount];
        playerDrawPiles = new PartialObservableDeck[playerCount];
        playerDiscards = new Deck[playerCount];
        playerTableaux = new Deck[playerCount];
        trashPile = new Deck<>("Trash");
        for (int i = 0; i < playerCount; i++) {
            boolean[] handVisibility = new boolean[playerCount];
            handVisibility[i] = true;
            playerHands[i] = new PartialObservableDeck<>("Hand of Player " + i + 1, handVisibility);
            playerDrawPiles[i] = new PartialObservableDeck<>("Drawpile of Player " + i + 1, new boolean[playerCount]);
            playerDiscards[i] = new Deck<>("Discard of Player " + i + 1);
            playerTableaux[i] = new Deck<>("Tableau of Player " + i + 1);
            for (int j = 0; j < 7; j++)
                playerDrawPiles[i].add(DominionCard.create(CardType.COPPER));
            for (int j = 0; j < 3; j++)
                playerDrawPiles[i].add(DominionCard.create(CardType.ESTATE));
            playerDrawPiles[i].shuffle(rnd);
            for (int k = 0; k < 5; k++) playerHands[i].add(playerDrawPiles[i].draw());
        }
        actionsLeftForCurrentPlayer = 1;
        buysLeftForCurrentPlayer = 1;
        spentSoFar = 0;

        cardsIncludedInGame = new HashMap<>(16);
        cardsIncludedInGame.put(CardType.PROVINCE, 12);
        cardsIncludedInGame.put(CardType.DUCHY, 12);
        cardsIncludedInGame.put(CardType.ESTATE, 12);
        cardsIncludedInGame.put(CardType.GOLD, 1000);
        cardsIncludedInGame.put(CardType.SILVER, 1000);
        cardsIncludedInGame.put(CardType.COPPER, 1000);
        DominionParameters params = (DominionParameters) gameParameters;
        for (CardType ct : params.cardsUsed.keySet()) {
            cardsIncludedInGame.put(ct, params.cardsUsed.get(ct));
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
        return cardsIncludedInGame.equals(other.cardsIncludedInGame) &&
                Arrays.equals(playerHands, other.playerHands) &&
                Arrays.equals(playerResults, other.playerResults) &&
                Arrays.equals(playerDiscards, other.playerDiscards) &&
                Arrays.equals(playerTableaux, other.playerTableaux) &&
                Arrays.equals(playerDrawPiles, other.playerDrawPiles) &&
                actionsInProgress.equals(other.actionsInProgress) &&
                trashPile.equals(other.trashPile) &&
                buysLeftForCurrentPlayer == other.buysLeftForCurrentPlayer &&
                actionsLeftForCurrentPlayer == other.actionsLeftForCurrentPlayer &&
                spentSoFar == other.spentSoFar && additionalSpendAvailable == other.additionalSpendAvailable &&
                Arrays.equals(defenceStatus, other.defenceStatus) &&
                gamePhase == other.gamePhase && gameStatus == other.gameStatus;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(cardsIncludedInGame, trashPile, buysLeftForCurrentPlayer, gamePhase, gameStatus,
                actionsLeftForCurrentPlayer, spentSoFar, additionalSpendAvailable, actionsInProgress);
        result = result + 31 * Arrays.hashCode(playerResults) + 743 * Arrays.hashCode(playerHands) + 353 * Arrays.hashCode(playerDiscards) +
        11 * Arrays.hashCode(playerTableaux) + 41 * Arrays.hashCode(playerDrawPiles) + Arrays.hashCode(defenceStatus);
        return result;
    }
}
