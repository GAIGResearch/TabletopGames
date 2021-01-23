package games.dicemonastery;

import core.*;
import core.actions.AbstractAction;
import games.dicemonastery.actions.PlaceMonk;
import games.dicemonastery.actions.UseMonk;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static java.util.stream.Collectors.*;

public class DiceMonasteryForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) firstState;

        for (int p = 0; p < state.getNPlayers(); p++) {
            state.createMonk(4, p);
            state.createMonk(3, p);
            state.createMonk(2, p);
            state.createMonk(2, p);
            state.createMonk(1, p);
            state.createMonk(1, p);

            state.addResource(p, GRAIN, 2);
            state.addResource(p, HONEY, 2);
            state.addResource(p, WAX, 2);
            state.addResource(p, SKEP, 2);
            state.addResource(p, BREAD, 2);

            state.addResource(p, SHILLINGS, 6);
            state.addResource(p, PRAYERS, 1);
        }

        state.setGamePhase(Phase.PLACE_MONKS);
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        super.endGame(gameState);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) currentState;

        if (!state.actionsInProgress.isEmpty()) {
            // we just register the action with the currently active action
            state.actionsInProgress.peek().registerActionTaken(state, action);
        }

        action.execute(state);

        // we may be in an extended action, so update that
        if (!state.actionsInProgress.isEmpty()) {
            // we just register the action taken with the currently active action
            // and then remove anything which is now complete
            int loopCount = 0;
            while (!state.actionsInProgress.isEmpty() && state.actionsInProgress.peek().executionComplete(state)) {
                state.actionsInProgress.pop();
                loopCount++;
                if (loopCount > 100) {
                    throw new AssertionError("WTF?");
                }
            }
        }

        if (state.isActionInProgress())
            return;

        // We only consider the next phase once any extended actions are complete
        state.getTurnOrder().endPlayerTurn(state);

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;

        if (state.isActionInProgress()) {
            return state.actionsInProgress.peek().followOnActions(state);
        }

        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        int currentPlayer = turnOrder.getCurrentPlayer(state);
        switch (turnOrder.season) {
            case SPRING:
            case AUTUMN:
                if (state.getGamePhase() == Phase.PLACE_MONKS) {
                    // we place monks
                    List<Monk> availableMonks = state.monksIn(DORMITORY, currentPlayer);
                    if (availableMonks.isEmpty()) {
                        throw new AssertionError("We have no monks left for player " + currentPlayer);
                    }
                    int mostPiousMonk = availableMonks.stream().mapToInt(Monk::getPiety).max().getAsInt();
                    return Arrays.stream(ActionArea.values())
                            .filter(a -> a != DORMITORY && a.dieMinimum <= mostPiousMonk)
                            .map(a -> new PlaceMonk(currentPlayer, a)).collect(toList());
                } else if (state.getGamePhase() == Phase.USE_MONKS) {
                    List<Monk> availableMonks = state.monksIn(turnOrder.currentAreaBeingExecuted, currentPlayer);
                    if (availableMonks.isEmpty()) {
                        throw new AssertionError("We have no monks left for player " + currentPlayer);
                    }
                    return availableMonks.stream().map(m -> new UseMonk(m.getComponentID(), turnOrder.currentAreaBeingExecuted)).collect(toList());
                }
            case SUMMER:
            case WINTER:
        }
        throw new AssertionError("Not yet implemented combination " + turnOrder.season + " : " + state.getGamePhase());
    }

    @Override
    protected DiceMonasteryForwardModel _copy() {
        // no mutable state
        return this;
    }
}
