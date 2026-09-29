package games.coltexpress;

import core.AbstractPlayer;
import games.coltexpress.*;
import games.coltexpress.cards.RoundCard;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.Arrays;
import java.util.List;

import static games.coltexpress.ColtExpressGameState.ColtExpressGamePhase.*;
import static games.coltexpress.cards.RoundCard.TurnType.*;
import static org.junit.Assert.*;

public class TestTurnRoundProgression {


    ColtExpressGameState state;

    ColtExpressForwardModel fm = new ColtExpressForwardModel();
    @Test
    public void testTurnOwnerProgressesInPlanningPhase() {

        ColtExpressParameters params = new ColtExpressParameters();
        params.setRandomSeed(6);
        state = new ColtExpressGameState(params, 3);
        fm.setup(state);
        state.getRounds().draw();
        state.getRounds().add(state.getRoundCard(ColtExpressTypes.RegularRoundCard.Bridge, 3));

        RoundCard card = state.getRounds().peek();
        // setup initialised the first turn from the card we have just replaced
        state.currentTurnType = card.getTurnTypes()[0];
        assertEquals(NormalTurn, card.getTurnTypes()[0]);
        assertEquals(DoubleTurn, card.getTurnTypes()[1]);
        assertEquals(NormalTurn, card.getTurnTypes()[2]);

        assertEquals(0, state.getTurnOwner());
        assertEquals(0, state.getRoundCounter());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());
        assertEquals(DoubleTurn, state.getCurrentTurnType());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, state.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getTurnOwner());
        assertEquals(0, state.getRoundCounter());
        assertEquals(ExecuteActions, state.getGamePhase());
    }

    @Test
    public void testTurnOwnerProgressionInExecutionPhase() {
        testTurnOwnerProgressesInPlanningPhase();

        assertEquals(0, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getTurnOwner());
        assertEquals(0, state.getRoundCounter());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, state.getTurnOwner());
        assertEquals(0, state.getRoundCounter());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getTurnOwner());
        assertEquals(1, state.getRoundCounter());
        assertEquals(PlanActions, state.getGamePhase());
    }
}
