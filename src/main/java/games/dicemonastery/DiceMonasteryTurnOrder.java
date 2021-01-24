package games.dicemonastery;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Component;
import core.turnorders.TurnOrder;
import utilities.Utils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static java.util.stream.Collectors.*;

public class DiceMonasteryTurnOrder extends TurnOrder {

    public DiceMonasteryTurnOrder(int nPlayers, DiceMonasteryParams params) {
        super(nPlayers);
        nMaxRounds = params.YEARS;
    }

    private DiceMonasteryTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    Season season = SPRING;
    int year = 1;
    ActionArea currentAreaBeingExecuted = null;
    int abbot = 0; // the current 'first player '
    List<Integer> playerOrderForCurrentArea;
    int dominantPiety = 0;
    int actionPointsLeftForCurrentPlayer = 0;

    @Override
    protected void _reset() {
        season = SPRING;
        year = 1;
        turnOwner = 0;
        abbot = 0;
        actionPointsLeftForCurrentPlayer = 0;
        currentAreaBeingExecuted = null;
    }

    @Override
    protected DiceMonasteryTurnOrder _copy() {
        DiceMonasteryTurnOrder retValue = new DiceMonasteryTurnOrder(nPlayers);
        retValue.season = season;
        retValue.year = year;
        retValue.abbot = abbot;
        retValue.currentAreaBeingExecuted = currentAreaBeingExecuted;
        retValue.playerOrderForCurrentArea = playerOrderForCurrentArea != null ? new ArrayList<>(playerOrderForCurrentArea) : null;
        retValue.dominantPiety = dominantPiety;
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
                    // in this case we go through players in piety order (as calculated when we start processing the area)
                    if (nextPlayer(state) != turnOwner) {
                        turnOwner = nextPlayer(state);
                        actionPointsLeftForCurrentPlayer = actionPoints(state, currentAreaBeingExecuted, turnOwner);
                    }
                    if (turnOwner == -1) {
                        // we have completed all actions for that area
                        if (setUpPlayerOrderForCurrentArea(state)) {
                            turnOwner = playerOrderForCurrentArea.get(0);
                            actionPointsLeftForCurrentPlayer = actionPoints(state, currentAreaBeingExecuted, turnOwner);
                        } else {
                            // we have completed this phase
                            season = season.next();
                            if (season == SPRING)
                                year++;
                            if (year > nMaxRounds)
                                state.setGameStatus(Utils.GameResult.GAME_END);
                            abbot = (abbot + 1 + nPlayers) % nPlayers;
                            turnOwner = abbot;
                            state.setGamePhase(Phase.PLACE_MONKS);
                        }
                    }
                }
                break;
            case WINTER:
            case SUMMER:
                throw new AssertionError(String.format("Unknown Game Phase of %s in %s", season, state.getGamePhase()));

        }
    }

    private int actionPoints(DiceMonasteryGameState state, ActionArea region, int player) {
        return state.monksIn(currentAreaBeingExecuted, turnOwner).stream().mapToInt(Monk::getPiety).sum();
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        switch ((Phase) state.getGamePhase()) {
            case PLACE_MONKS:
                Collection<Component> monksInDormitory = state.actionAreas.get(ActionArea.DORMITORY).getAll(c -> c instanceof Monk);
                if (monksInDormitory.size() == 0)
                    return -1;
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
                    return -1; // we have now finished this area
                return playerOrderForCurrentArea.get(currentIndex + 1);
        }
        throw new AssertionError("Unexpected situation for Phase " + state.getGamePhase());
    }

    private boolean setUpPlayerOrderForCurrentArea(DiceMonasteryGameState state) {
        // calculate piety order - player order by sum of the piety of all their monks in the space
        while (state.monksIn(currentAreaBeingExecuted, -1).size() == 0) {
            currentAreaBeingExecuted = currentAreaBeingExecuted.next();
            if (currentAreaBeingExecuted == ActionArea.MEADOW) {
                return false;
            }
        }

        Map<Integer, Integer> pietyPerPlayer = state.monksIn(currentAreaBeingExecuted, -1).stream()
                .collect(groupingBy(Monk::getOwnerId, summingInt(Monk::getPiety)));
        playerOrderForCurrentArea = pietyPerPlayer.entrySet().stream()
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
        dominantPiety = pietyPerPlayer.get(playerOrderForCurrentArea.get(0));
        return true;
    }

    public Season getSeason() {
        return season;
    }

    public int getYear() {
        return year;
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
            return other.season == season && other.year == year && other.dominantPiety == dominantPiety &&
                    other.currentAreaBeingExecuted == currentAreaBeingExecuted && other.abbot == abbot &&
                    other.actionPointsLeftForCurrentPlayer == actionPointsLeftForCurrentPlayer &&
                    other.playerOrderForCurrentArea.equals(playerOrderForCurrentArea) &&
                    super.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * Objects.hash(season, year, abbot, currentAreaBeingExecuted,
                dominantPiety, actionPointsLeftForCurrentPlayer);
    }


}
