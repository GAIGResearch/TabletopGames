package games.DiceMonastery;

import core.AbstractGameState;
import core.components.Component;
import core.turnorders.TurnOrder;

import java.util.*;

import static games.DiceMonastery.DiceMonasteryConstants.*;
import static games.DiceMonastery.DiceMonasteryConstants.Season.*;

public class DiceMonasteryTurnOrder extends TurnOrder {

    public DiceMonasteryTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    Season season = SPRING;
    int year = 1;

    @Override
    protected void _reset() {
        season = SPRING;
        year = 1;
        turnOwner = 0;
    }

    @Override
    protected DiceMonasteryTurnOrder _copy() {
        DiceMonasteryTurnOrder retValue = new DiceMonasteryTurnOrder(nPlayers);
        retValue.season = season;
        retValue.year = year;
        return retValue;
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        super.endPlayerTurn(gameState);
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        switch (season) {
            case SPRING:
            case AUTUMN:
                if (state.getGamePhase() == Phase.PLACE_MONKS) {
                    // we move to the next player who still has monks to place
                    if (state.actionAreas.get(ActionArea.DORMITORY).size() == 0) {
                        // no monks left, so we move on to the next phase
                        state.setGamePhase(Phase.USE_MONKS);
                        return;
                    }
                    // still monks left; so get the next player as usual, but skip any who have no monks left to place
                    // (we have already moved the turn on once with super.endPlayerTurn(), so we just need to check
                    // the current player has Monks to place.)
                    Collection<Component> monksInDormitory = state.actionAreas.get(ActionArea.DORMITORY).getComponents().values();
                    while (monksInDormitory.stream().noneMatch(c -> turnOwner == c.getOwnerId())) {
                        turnOwner = nextPlayer(gameState);
                    }
                } else if (state.getGamePhase() == Phase.USE_MONKS) {

                }
                break;
            case WINTER:
            case SUMMER:
                throw new AssertionError(String.format("Unknown Game Phase of %s in %s", season, state.getGamePhase()));

        }
    }

    public Season getSeason() {
        return season;
    }

    public int getYear() {
        return year;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }


}
