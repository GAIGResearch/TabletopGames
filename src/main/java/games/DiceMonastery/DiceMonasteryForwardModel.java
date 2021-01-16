package games.DiceMonastery;

import core.*;
import core.actions.AbstractAction;

import java.util.*;

import static games.DiceMonastery.DiceMonasteryGameState.actionArea.*;
import static games.DiceMonastery.DiceMonasteryGameState.resource.*;
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

            state.gain(p, GRAIN, 2);
            state.gain(p, HONEY, 2);
            state.gain(p, WAX, 2);
            state.gain(p, SKEP, 2);
            state.gain(p, BREAD, 2);
        }

    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();

        int currentPlayer = turnOrder.getCurrentPlayer(state);
        switch (turnOrder.season) {
            case SPRING:
            case AUTUMN:
                // we place monks
                List<Monk> availableMonks = state.actionAreas.get(DORMITORY)
                        .getComponents().values().stream().map(c -> (Monk) c)
                        .filter(m -> m.getOwnerId() == currentPlayer)
                        .collect(toList());
                if (availableMonks.isEmpty()) {
                    throw new AssertionError("We have no monks left for player " + currentPlayer);
                }
                int mostPiousMonk = availableMonks.stream().mapToInt(Monk::getPiety).max().getAsInt();
                return Arrays.stream(DiceMonasteryGameState.actionArea.values())
                        .filter(a -> a != DORMITORY && a.dieMinimum <= mostPiousMonk)
                        .map(PlaceMonk::new).collect(toList());
            case SUMMER:
            case WINTER:
        }
        throw new AssertionError("Not yet implemented");
    }

    @Override
    protected DiceMonasteryForwardModel _copy() {
        // no mutable state
        return this;
    }
}
