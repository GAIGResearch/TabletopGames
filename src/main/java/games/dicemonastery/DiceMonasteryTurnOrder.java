package games.dicemonastery;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Component;
import core.turnorders.TurnOrder;
import games.dicemonastery.components.Monk;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase.BID;
import static games.dicemonastery.DiceMonasteryConstants.Phase.SACRIFICE;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static java.util.stream.Collectors.*;

public class DiceMonasteryTurnOrder extends TurnOrder {

    public DiceMonasteryTurnOrder(int nPlayers, DiceMonasteryParams params) {
        super(nPlayers, params.YEARS);
    }

    private DiceMonasteryTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    Season season = SPRING;
    ActionArea currentAreaBeingExecuted = null;
    int abbot = 0; // the current 'first player '
    List<Integer> playerOrderForCurrentArea;
    boolean turnOwnerTakenReward, turnOwnerPrayed;
    int actionPointsLeftForCurrentPlayer = 0;
    List<Integer> playersToMakeVikingDecisions = new ArrayList<>();

    @Override
    protected void _reset() {
        season = SPRING;
        turnOwner = 0;
        abbot = 0;
        actionPointsLeftForCurrentPlayer = 0;
        currentAreaBeingExecuted = null;
        turnOwnerTakenReward = false;
        turnOwnerPrayed = false;
    }

    @Override
    protected DiceMonasteryTurnOrder _copy() {
        DiceMonasteryTurnOrder retValue = new DiceMonasteryTurnOrder(nPlayers);
        retValue.season = season;
        retValue.roundCounter = roundCounter;
        retValue.abbot = abbot;
        retValue.currentAreaBeingExecuted = currentAreaBeingExecuted;
        retValue.playerOrderForCurrentArea = playerOrderForCurrentArea != null ? new ArrayList<>(playerOrderForCurrentArea) : null;
        retValue.turnOwnerTakenReward = turnOwnerTakenReward;
        retValue.turnOwnerPrayed = turnOwnerPrayed;
        retValue.actionPointsLeftForCurrentPlayer = actionPointsLeftForCurrentPlayer;
        retValue.playersToMakeVikingDecisions = new ArrayList<>(playersToMakeVikingDecisions);
        return retValue;
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.TURN_OVER, gameState, null));
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        switch (season) {
            case SPRING:
            case AUTUMN:
                if (state.getGamePhase() == Phase.PLACE_MONKS) {
                    // we move to the next player who still has monks to place
                    if (state.actionAreas.get(ActionArea.DORMITORY).size() == 0) {
                        // no monks left, so we move on to the next phase (which always starts with the MEADOW)
                        state.setGamePhase(Phase.USE_MONKS);
                        currentAreaBeingExecuted = ActionArea.MEADOW;
                        setUpPlayerOrderForCurrentArea(state); // impossible to have no monks placed at this point
                        turnOwner = playerOrderForCurrentArea.get(0);
                        initialiseUseMonkBooleans(state);
                        actionPointsLeftForCurrentPlayer = actionPoints(state, currentAreaBeingExecuted, turnOwner);
                        return;
                    }
                    turnOwner = nextPlayer(gameState);
                } else if (state.getGamePhase() == Phase.USE_MONKS) {
                    // first we check to see if we have finished using all monks; in which case move to next player
                    if (actionPointsLeftForCurrentPlayer == 0) {
                        // first move all Monks back to dormitory for the current player
                        for (Monk m : state.monksIn(currentAreaBeingExecuted, state.getCurrentPlayer())) {
                            state.moveMonk(m.getComponentID(), currentAreaBeingExecuted, DORMITORY);
                        }
                        // then move to next player
                        turnOwner = nextPlayer(state);
                        initialiseUseMonkBooleans(state);
                        actionPointsLeftForCurrentPlayer = actionPoints(state, currentAreaBeingExecuted, turnOwner);

                        if (state.monksIn(currentAreaBeingExecuted, -1).isEmpty()) {
                            // we have completed all actions for that area
                            if (setUpPlayerOrderForCurrentArea(state)) {
                                turnOwner = playerOrderForCurrentArea.get(0);
                                actionPointsLeftForCurrentPlayer = actionPoints(state, currentAreaBeingExecuted, turnOwner);
                                initialiseUseMonkBooleans(state);
                            } else {
                                // we have completed this phase
                                endRound(state);
                            }
                        }
                    }
                }
                break;
            case SUMMER:
                switch ((DiceMonasteryConstants.Phase) state.getGamePhase()) {
                    case BID:
                        turnOwner = (turnOwner + 1 + nPlayers) % nPlayers;
                        if (state.allBidsIn()) {
                            // we have completed SUMMER bidding
                            playersToMakeVikingDecisions = state.executeBids();
                            state.setGamePhase(SACRIFICE);
                            if (!playersToMakeVikingDecisions.isEmpty())
                                turnOwner = playersToMakeVikingDecisions.get(0);
                        }
                        break;
                    case SACRIFICE:
                        playersToMakeVikingDecisions.remove(0);
                        if (!playersToMakeVikingDecisions.isEmpty())
                            turnOwner = playersToMakeVikingDecisions.get(0);
                        break;
                }
                if (state.getGamePhase() == SACRIFICE && playersToMakeVikingDecisions.isEmpty()) {
                    // we have finished the raids, and all players have made sacrifice decisions
                    endRound(state);
                }
                break;
            case WINTER:
                turnOwner = (turnOwner + 1 + nPlayers) % nPlayers;
                // round over if we get back to abbot as first player
                if (turnOwner == abbot)
                    endRound(state);
                break;
            default:
                throw new AssertionError(String.format("Unknown Game Phase of %s in %s", state.getGamePhase(), season));
        }
    }

    private void initialiseUseMonkBooleans(DiceMonasteryGameState state) {
        turnOwnerTakenReward = false;
        if (state.availableBonusTokens(currentAreaBeingExecuted).isEmpty())
            playerTakesReward(state); // none to take
    }

    void playerTakesReward(DiceMonasteryGameState state) {
        turnOwnerTakenReward = true;
        turnOwnerPrayed = false;
        if (currentAreaBeingExecuted == CHAPEL || currentAreaBeingExecuted == LIBRARY || state.getResource(turnOwner, Resource.PRAYER, STOREROOM) == 0)
            turnOwnerPrayed = true; // No prayers in CHAPEL or LIBRARY as they cannot be used on anything, and if we don't have any
    }

    @Override
    public void endRound(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ROUND_OVER, state, null));
        switch (season) {
            case SPRING:
            case AUTUMN:
                state.springAutumnHousekeeping();
                break;
            case SUMMER:
                break;
            case WINTER:
                abbot = (abbot + 1 + nPlayers) % nPlayers;
                if (getYear() >= nMaxRounds) {
                    state.endGame();
                    return;
                }
                roundCounter++;  // increment year
                break;
        }
        season = season.next();
        if (season == SUMMER && getYear() == 1)
            season = season.next(); // we skip Summer in the first year
        state.checkAtLeastOneMonk();
        if (season == SUMMER)
            state.setGamePhase(BID);
        else
            state.setGamePhase(Phase.PLACE_MONKS);
        turnOwner = firstPlayerWithMonks(state);
        if (season == WINTER)
            state.winterHousekeeping();  // this occurs at the start of WINTER, as it includes the Christmas Feast
    }

    private int firstPlayerWithMonks(DiceMonasteryGameState state) {
        for (int p = 0; p < nPlayers; p++) {
            int player = (abbot + p + nPlayers) % nPlayers;
            if (!state.monksIn(DORMITORY, player).isEmpty())
                return player;
        }
        // should only reach here if NO player has any monks left! So we skip the entire season!
        // infinite recursion should not be possible due to finite number of turns
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
    //    System.out.printf("No monks at all in %s %d : %d %n", turnOrder.season, turnOrder.getYear(), state.getGameID());
        endRound(state);
        return turnOwner;
    }

    private int actionPoints(DiceMonasteryGameState state, ActionArea region, int player) {
        return state.monksIn(currentAreaBeingExecuted, turnOwner).stream().mapToInt(Monk::getPiety).sum();
    }

    void addActionPoints(int number) {
        actionPointsLeftForCurrentPlayer += number;
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        if (season == WINTER || season == SUMMER) {
            // one decision each in Summer/Winter (how much to bid / which monk to promote)
            return (turnOwner + 1 + nPlayers) % nPlayers;
        } else {
            switch ((Phase) state.getGamePhase()) {
                case PLACE_MONKS:
                    Collection<Component> monksInDormitory = state.actionAreas.get(ActionArea.DORMITORY).getAll(c -> c instanceof Monk);
                    if (monksInDormitory.size() == 0)
                        return playerOrderFor(MEADOW, state).get(0); // we move on to the MEADOW next
                    int nextPlayer = turnOwner;
                    do {
                        nextPlayer = (nPlayers + nextPlayer + 1) % nPlayers;
                    } while (state.monksIn(ActionArea.DORMITORY, nextPlayer).size() == 0);
                    return nextPlayer;
                // still monks left; so get the next player as usual, but skip any who have no monks left to place
                // (we have already moved the turn on once with super.endPlayerTurn(), so we just need to check
                // the current player has Monks to place.)
                case USE_MONKS:
                    if (actionPointsLeftForCurrentPlayer > 0)
                        return turnOwner;  // we still have monks for the current player to finish using
                    int currentIndex = playerOrderForCurrentArea.indexOf(turnOwner);
                    if (currentIndex + 1 == playerOrderForCurrentArea.size())
                        return abbot; // we have now finished this area, and we always start placing with the Abbot
                    return playerOrderForCurrentArea.get(currentIndex + 1);
            }
        }
        throw new AssertionError(String.format("Unexpected situation for Season %s and Phase %s", season, state.getGamePhase()));
    }

    private boolean setUpPlayerOrderForCurrentArea(DiceMonasteryGameState state) {
        // calculate piety order - player order by sum of the piety of all their monks in the space
        while (state.monksIn(currentAreaBeingExecuted, -1).size() == 0) {
            currentAreaBeingExecuted = currentAreaBeingExecuted.next();
            if (currentAreaBeingExecuted == ActionArea.MEADOW) {
                return false;
            }
        }

        playerOrderForCurrentArea = playerOrderFor(currentAreaBeingExecuted, state);
        return true;
    }

    private List<Integer> playerOrderFor(ActionArea area, DiceMonasteryGameState state) {
        Map<Integer, Integer> pietyPerPlayer = state.monksIn(area, -1).stream()
                .collect(groupingBy(Monk::getOwnerId, summingInt(Monk::getPiety)));
        return pietyPerPlayer.entrySet().stream()
                .sorted((e1, e2) -> {
                            // based on different in piety, with ties broken by turn order wrt to the abbot
                            int pietyDiff = e2.getValue() - e1.getValue();
                            if (pietyDiff == 0)
                                return (e1.getKey() + nPlayers - abbot) % nPlayers - (e2.getKey() + nPlayers - abbot) % nPlayers;
                            return pietyDiff;
                        }
                )
                .map(Map.Entry::getKey)
                .collect(toList());
    }


    public Season getSeason() {
        return season;
    }

    public int getYear() {
        return roundCounter + 1;
    }

    public int getAbbot() {
        return abbot;
    }

    public void setAbbot(int player) {
        abbot = player;
    }

    public ActionArea getCurrentArea() {
        return currentAreaBeingExecuted;
    }

    public int getActionPointsLeft() {
        return actionPointsLeftForCurrentPlayer;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DiceMonasteryTurnOrder) {
            DiceMonasteryTurnOrder other = (DiceMonasteryTurnOrder) o;
            return other.season == season && other.abbot == abbot &&
                    other.turnOwnerTakenReward == turnOwnerTakenReward &&
                    other.turnOwnerPrayed == turnOwnerPrayed &&
                    other.currentAreaBeingExecuted == currentAreaBeingExecuted &&
                    other.actionPointsLeftForCurrentPlayer == actionPointsLeftForCurrentPlayer &&
                    other.playerOrderForCurrentArea.equals(playerOrderForCurrentArea) &&
                    other.playersToMakeVikingDecisions.equals(playersToMakeVikingDecisions) &&
                    super.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * Objects.hash(season, abbot, currentAreaBeingExecuted,
                actionPointsLeftForCurrentPlayer, turnOwnerTakenReward, turnOwnerPrayed, playersToMakeVikingDecisions);
    }


}
