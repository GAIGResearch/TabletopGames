package games.sirius;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Deck;
import core.turnorders.TurnOrder;
import games.sirius.SiriusConstants.SiriusPhase;
import utilities.Pair;

import java.util.*;
import java.util.function.IntPredicate;

import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.*;

public class SiriusTurnOrder extends TurnOrder {

    int[] playerByRank; // this is indexed by rank and holds the current player at that rank
    int[] nextPlayer; // this is indexed by player and indicates the next player in rank. It is updated at the start of each round.
    Map<String, List<Boolean>> actionsTakenByPlayers; // used to track is certain action types have been taken (when these are only usable once)

    // We have different rules for different phases
    // During move selection we are formally simultaneous  (moveSelected)
    // While for everything else we go strictly in order of current ranking (nextPlayer)

    public SiriusTurnOrder(int nPlayers) {
        super(nPlayers);
        nextPlayer = new int[nPlayers];
        playerByRank = new int[nPlayers + 1];
        Arrays.setAll(playerByRank, i -> i - 1);
        initialiseActions();
        updatePlayerOrder();
    }

    private void initialiseActions() {
        actionsTakenByPlayers = new HashMap<>();
        List<Boolean> allFalse = new ArrayList<>(nPlayers);
        for (int i = 0; i < nPlayers; i++) allFalse.add(Boolean.FALSE);
        actionsTakenByPlayers.put("Favour", allFalse);
        actionsTakenByPlayers.put("Sold", new ArrayList<>(allFalse));
        actionsTakenByPlayers.put("Betrayed", new ArrayList<>(allFalse));
    }

    @Override
    public int nextPlayer(AbstractGameState gs) {
        return nextPlayerAndPhase((SiriusGameState) gs).a;
    }

    public Pair<Integer, SiriusPhase> nextPlayerAndPhase(SiriusGameState state) {
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        switch (phase) {
            case Move:
                // In this case move selection is simultaneous
                // so the next player is the first one who has not selected a move (and is not us)
                for (int i = 0; i < state.moveSelected.length; i++)
                    if (i != turnOwner && state.moveSelected[i] == -1) return new Pair<>(i, Move);
                // else we change phase, and the next player is the firstPlayer (who will always have an action available)
                return new Pair<>(getPlayerAtRank(1), Draw);
            case Draw:
                // The next player is whoever has a card to draw (or has not drawn one yet)
                int nextPlayerInPhase = getFirstMatchingPlayerFrom(nextPlayer[turnOwner], i -> {
                    Moon moon = state.getMoon(state.getLocationIndex(i));
                    switch (moon.moonType) {
                        case TRADING:
                            boolean canSell = !actionsTakenByPlayers.get("Sold").get(i) && state.getPlayerHand(i).stream().anyMatch(c -> c.cardType == AMMONIA || c.cardType == CONTRABAND);
                            boolean canBetray = !actionsTakenByPlayers.get("Betrayed").get(i) && state.getPlayerHand(i).stream().anyMatch(c -> c.cardType == SMUGGLER);
                            return canSell || canBetray;
                        case METROPOLIS:
                            // need to have not yet acted - in these locations we just get one action
                            return !actionsTakenByPlayers.get("Favour").get(i);
                        default:
                            // needs to have cards available to take
                            return moon.getDeck().getSize() > 0;
                    }
                });
                // if -1, then we shift phase so the next player will be the first one with a Favour card
                // but this takes place after roiling the rank, so that the current second player will go first
                // this may be nobody...in which case we go back to Move
                if (nextPlayerInPhase > -1)
                    return new Pair<>(nextPlayerInPhase, Draw);
                nextPlayerInPhase = getFirstMatchingPlayerFrom(getPlayerAtRank(2), i -> state.getPlayerHand(i).stream().anyMatch(c -> c.cardType == FAVOUR));
                if (nextPlayerInPhase > -1)
                    return new Pair<>(nextPlayerInPhase, Favour);
                return new Pair<>(0, Move);
            case Favour:
                // if there is no next player (i.e. -1), then the next is 0 for Move
                nextPlayerInPhase = getFirstMatchingPlayerFrom(nextPlayer[turnOwner], i -> !actionsTakenByPlayers.get("Favour").get(i) && state.getPlayerHand(i).stream().anyMatch(c -> c.cardType == FAVOUR));
                if (nextPlayerInPhase > -1)
                    return new Pair<>(nextPlayerInPhase, Favour);
                return new Pair<>(0, Move);
            default:
                throw new AssertionError("Unknown Phase " + phase);
        }
    }

    private int getFirstMatchingPlayerFrom(int startFrom, IntPredicate test) {
        // we go through in nextPlayer order from startFrom
        int player = startFrom;
        for (int i = 0; i < nPlayers; i++) {
            if (test.test(player)) return player;
            player = nextPlayer[player];
        }
        return -1;
    }

    public void setActionTaken(String ref, int player) {
        actionsTakenByPlayers.get(ref).set(player, true);
    }
    public boolean getActionTaken(String ref, int player) {
        return actionsTakenByPlayers.get(ref).get(player);
    }

    @Override
    public void endPlayerTurn(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.TURN_OVER, state, null));
        state.getPlayerTimer()[getCurrentPlayer(state)].incrementTurn();

        turnCounter++;

        Pair<Integer, SiriusPhase> nextPlayerAndPhase = nextPlayerAndPhase(state); // record this before we change the phase
        SiriusPhase nextPhase = nextPlayerAndPhase.b;
        int nextPlayer = nextPlayerAndPhase.a;

        if (nextPlayerAndPhase.b != phase) {
            // we end the phase here
            initialiseActions();
            switch (phase) {
                case Move:
                    if (nextPhase != Draw) {
                        throw new AssertionError("Impossible Phase to follow Move : " + nextPhase);
                    }
                    state.applyChosenMoves();
                    state.setGamePhase(SiriusPhase.Draw);
                    break;
                case Draw:
                    if (nextPhase != Move && nextPhase != Favour) {
                        throw new AssertionError("Impossible Phase to follow Draw : " + nextPhase);
                    }
                    // move first rank player to last, and shuffle the others
                    setRank(playerByRank[1], nPlayers);
                    updatePlayerOrder();
                    state.setGamePhase(nextPhase);
                    if (nextPhase == Move) { // this could happen if no-one has a Favour card
                        endRound(state);
                        Arrays.fill(state.moveSelected, -1);
                    }
                    break;
                case Favour:
                    if (nextPhase != Move) {
                        throw new AssertionError("Impossible Phase to follow Favour : " + nextPhase);
                    }
                    updatePlayerOrder(); // after Favour cards played
                    endRound(state);
                    Arrays.fill(state.moveSelected, -1);
                    state.setGamePhase(Move);
            }
        }

        turnOwner = nextPlayer;
    }

    // returns the current rank of the player for determining move order (1 is first, and so on)
    public int getRank(int player) {
        for (int r = 1; r <= nPlayers; r++) {
            if (playerByRank[r] == player)
                return r;
        }
        throw new AssertionError("Should be unreachable");
    }

    public void setRank(int player, int rank) {
        int oldRank = getRank(player);
        int inc = oldRank < rank ? 1 : -1;
        int diff = Math.abs(oldRank - rank);
        // we shuffle all the ranks up and move player to rank
        // We have to do this step last so that we have already shuffled out the previous occupant
        for (int i = 0; i <= diff; i++) {
            if (i == diff) {
                playerByRank[rank] = player;
            } else {
                playerByRank[i * inc + oldRank] = playerByRank[(i + 1) * inc + oldRank];
            }
        }
    }

    public void updatePlayerOrder() {
        // we set up nextPlayer to point to the next player
        for (int r = 1; r < nPlayers; r++) {
            nextPlayer[playerByRank[r]] = playerByRank[r + 1];
        }
        nextPlayer[playerByRank[nPlayers]] = playerByRank[1];  // we loop back to the start
    }

    public int getPlayerAtRank(int rank) {
        return playerByRank[rank];
    }

    @Override
    public void endRound(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        SiriusParameters params = (SiriusParameters) state.getGameParameters();
        state.getPlayerTimer()[getCurrentPlayer(state)].incrementRound();
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ROUND_OVER, state, null));

        // add cards - for all Moons with a linked Card Type
        for (Moon moon : state.getAllMoons()) {
            int drawLimit = moon.getDeckSize() == 0 ? params.cardsPerEmptyMoon : params.cardsPerNonEmptyMoon;
            if (moon.getMoonType().linkedCardType != null) {
                Deck<SiriusCard> drawDeck = state.getDeck(moon.moonType.linkedCardType, false);
                if (moon.moonType.linkedCardType != FAVOUR) {
                    // Except for Favour cards, which are always taken direct from the draw pile
                    for (int i = 0; i < drawLimit; i++) {
                        if (drawDeck.getSize() > 0)
                            moon.addCard(drawDeck.draw());
                    }
                }
                if (moon.getCartelOwner() > -1 && drawDeck.getSize() > 0)
                    state.addCardToHand(moon.getCartelOwner(), drawDeck.draw());
            }
        }
        roundCounter++;
    }


    @Override
    protected void _reset() {

    }

    @Override
    protected TurnOrder _copy() {
        SiriusTurnOrder retValue = new SiriusTurnOrder(nPlayers);
        retValue.nextPlayer = nextPlayer.clone();
        for (String key : actionsTakenByPlayers.keySet()) {
            retValue.actionsTakenByPlayers.put(key, new ArrayList<>(actionsTakenByPlayers.get(key)));
        }
        retValue.playerByRank = playerByRank.clone();
        return retValue;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + actionsTakenByPlayers.hashCode() + 31 * Arrays.hashCode(nextPlayer) + 31 * 31 * Arrays.hashCode(playerByRank);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SiriusTurnOrder) {
            SiriusTurnOrder other = (SiriusTurnOrder) obj;
            return Arrays.equals(nextPlayer, other.nextPlayer) && Arrays.equals(playerByRank, other.playerByRank) &&
                    actionsTakenByPlayers.equals(actionsTakenByPlayers) &&
                    super.equals(obj);
        }
        return false;
    }
}
