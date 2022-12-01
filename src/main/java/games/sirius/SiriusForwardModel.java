package games.sirius;

import core.*;
import core.actions.*;
import games.sirius.SiriusConstants.SiriusPhase;
import games.sirius.actions.MoveToMoon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static games.sirius.SiriusConstants.SiriusPhase.Move;
import static java.util.stream.Collectors.toList;

public class SiriusForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SiriusGameState state = (SiriusGameState) firstState;
        SiriusParameters params = (SiriusParameters) state.getGameParameters();
        for (int i = 0; i < params.ammonia; i++) {
            state.ammoniaDeck.add(new SiriusCard("Ammonia", 1));
        }
        for (int i = 0; i < params.superAmmonia; i++) {
            state.ammoniaDeck.add(new SiriusCard("Super Ammonia", 2));
        }
        for (int i = 0; i < params.ammonia; i++) {
            state.ammoniaDeck.add(new SiriusCard("Hyper Ammonia", 3));
        }
        state.ammoniaDeck.shuffle(state.rnd);
        state.moons.add(new Moon("Sirius", MoonType.TRADING, state.rnd));
        state.moons.add(new Moon("Mining_1", MoonType.MINING, state.rnd));
        state.moons.add(new Moon("Mining_2", MoonType.MINING, state.rnd));

        state.playerLocations = new int[state.getNPlayers()];
        state.moveSelected = new int[state.getNPlayers()];
        Arrays.fill(state.moveSelected, -1);
        // All players start on Sirius
        state.playerAreas = IntStream.range(0, state.getNPlayers()).mapToObj(PlayerArea::new).collect(toList());

        state.setGamePhase(Move);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        SiriusGameState state = (SiriusGameState) currentState;
        action.execute(state);

        SiriusTurnOrder turnOrder = (SiriusTurnOrder) state.getTurnOrder();
        turnOrder.endPlayerTurn(state);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SiriusGameState state = (SiriusGameState) gameState;
        SiriusPhase phase = (SiriusPhase) state.getGamePhase();
        List<AbstractAction> retValue = new ArrayList<>();
        int player = state.getCurrentPlayer();
        switch (phase) {
            case Move:
                int currentLocation = state.getLocationIndex(player);
                retValue = IntStream.range(0, state.moons.size()).filter(i -> i != currentLocation).mapToObj(MoveToMoon::new).collect(toList());
                break;
            case Draw:
                // TODO:
        }
        return retValue;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return this; // immutable
    }
}
