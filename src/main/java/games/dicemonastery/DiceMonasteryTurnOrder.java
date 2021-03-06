package games.dicemonastery;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Component;
import core.turnorders.TurnOrder;

import java.util.*;
import java.util.stream.IntStream;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.DORMITORY;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.MEADOW;
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
    List<Integer> dominantPlayers;
    List<Integer> rewardsTaken;
    int actionPointsLeftForCurrentPlayer = 0;

    @Override
    protected void _reset() {
        season = SPRING;
        turnOwner = 0;
        abbot = 0;
        actionPointsLeftForCurrentPlayer = 0;
        currentAreaBeingExecuted = null;
        dominantPlayers = new ArrayList<>();
        rewardsTaken = new ArrayList<>();
    }

    @Override
    protected DiceMonasteryTurnOrder _copy() {
        DiceMonasteryTurnOrder retValue = new DiceMonasteryTurnOrder(nPlayers);
        retValue.season = season;
        retValue.roundCounter = roundCounter;
        retValue.abbot = abbot;
        retValue.currentAreaBeingExecuted = currentAreaBeingExecuted;
        retValue.playerOrderForCurrentArea = playerOrderForCurrentArea != null ? new ArrayList<>(playerOrderForCurrentArea) : null;
        retValue.dominantPlayers = new ArrayList<>(dominantPlayers);
        retValue.rewardsTaken = new ArrayList<>(rewardsTaken);
        retValue.actionPointsLeftForCurrentPlayer = actionPointsLeftForCurrentPlayer;
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
                        actionPointsLeftForCurrentPlayer = actionPoints(state, currentAreaBeingExecuted, turnOwner);
                        return;
                    }
                    turnOwner = nextPlayer(gameState);
                } else if (state.getGamePhase() == Phase.USE_MONKS) {
                    // first we check to see if we have finished using all monks; in which case move to next player
                    if (actionPointsLeftForCurrentPlayer == 0 && (!dominantPlayers.contains(turnOwner) || rewardsTaken.contains(turnOwner))) {
                        // first move all Monks back to dormitory for the current player
                        for (Monk m : state.monksIn(currentAreaBeingExecuted, state.getCurrentPlayer())) {
                            state.moveMonk(m.getComponentID(), currentAreaBeingExecuted, DORMITORY);
                        }
                        // then move to next player
                        turnOwner = nextPlayer(state);
                        actionPointsLeftForCurrentPlayer = actionPoints(state, currentAreaBeingExecuted, turnOwner);
                    }
                    if (state.monksIn(currentAreaBeingExecuted, -1).isEmpty()) {
                        // we have completed all actions for that area
                        if (setUpPlayerOrderForCurrentArea(state)) {
                            turnOwner = playerOrderForCurrentArea.get(0);
                            actionPointsLeftForCurrentPlayer = actionPoints(state, currentAreaBeingExecuted, turnOwner);
                        } else {
                            // we have completed this phase
                            season = season.next();
                            if (season == WINTER)
                                state.winterHousekeeping();
                            // and set the player back to the abbot
                            turnOwner = abbot;
                            state.setGamePhase(Phase.PLACE_MONKS);
                        }
                    }
                }
                break;
            case SUMMER:
                turnOwner = (turnOwner + 1 + nPlayers) % nPlayers;
                if (turnOwner == abbot) {
                    // we have completed SUMMER bidding
                    state.executeBids();
                    season = season.next();
                }
                break;
            case WINTER:
                if (state.monksIn(null, turnOwner).size() == 0) {
                    // get a free novice if you ever run out of monks
                    state.createMonk(1, turnOwner); // goes into the DORMITORY
                }
                turnOwner = (turnOwner + 1 + nPlayers) % nPlayers;
                // and then increment year
                if (turnOwner == abbot)
                    endRound(state);
                break;
            default:
                throw new AssertionError(String.format("Unknown Game Phase of %s in %s", state.getGamePhase(), season));
        }
    }

    @Override
    public void endRound(AbstractGameState state) {
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ROUND_OVER, state, null));

        roundCounter++;
        season = season.next();
        roundCounter++;
        if (getYear() > nMaxRounds) {
            ((DiceMonasteryGameState) state).endGame();
        }
        abbot = (abbot + 1 + nPlayers) % nPlayers;
        turnOwner = abbot;
    }

    private int actionPoints(DiceMonasteryGameState state, ActionArea region, int player) {
        return state.monksIn(currentAreaBeingExecuted, turnOwner).stream().mapToInt(Monk::getPiety).sum();
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
                    if (dominantPlayers.contains(turnOwner) && !rewardsTaken.contains(turnOwner)) {
                        // we now need to get the Dominance reward for the area
                        return turnOwner;
                    }
                    int currentIndex = playerOrderForCurrentArea.indexOf(turnOwner);
                    if (currentIndex + 1 == playerOrderForCurrentArea.size())
                        return abbot; // we have now finished this area, and we always start placing with the Abbot
                    return playerOrderForCurrentArea.get(currentIndex + 1);
            }
        }
        throw new AssertionError(String.format("Unexpected situation for Season %s and Phase %s", season, state.getGamePhase()));
    }

    public void setRewardTaken(int player) {
        rewardsTaken.add(player);
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
        int dominantPiety = state.monksIn(currentAreaBeingExecuted, playerOrderForCurrentArea.get(0)).stream().mapToInt(Monk::getPiety).sum();
        dominantPlayers = IntStream.range(0, nPlayers)
                .filter(p -> state.monksIn(currentAreaBeingExecuted, p).stream().mapToInt(Monk::getPiety).sum() == dominantPiety)
                .boxed()
                .collect(toList());
        rewardsTaken = new ArrayList<>();
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
                .mapToInt(Map.Entry::getKey)
                .boxed().collect(toList());
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
            return other.season == season && other.dominantPlayers.equals(dominantPlayers) &&
                    other.rewardsTaken.equals(rewardsTaken) &&
                    other.currentAreaBeingExecuted == currentAreaBeingExecuted && other.abbot == abbot &&
                    other.actionPointsLeftForCurrentPlayer == actionPointsLeftForCurrentPlayer &&
                    other.playerOrderForCurrentArea.equals(playerOrderForCurrentArea) &&
                    super.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * Objects.hash(season, abbot, currentAreaBeingExecuted,
                dominantPlayers, actionPointsLeftForCurrentPlayer, rewardsTaken);
    }


}
