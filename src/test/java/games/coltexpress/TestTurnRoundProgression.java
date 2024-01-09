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
    ColtExpressTurnOrder turnOrder;

    ColtExpressForwardModel fm = new ColtExpressForwardModel();
    @Test
    public void testTurnOwnerProgressesInPlanningPhase() {

        ColtExpressParameters params = new ColtExpressParameters();
        params.setRandomSeed(6);
        state = new ColtExpressGameState(params, 3);
        turnOrder = (ColtExpressTurnOrder) state.getTurnOrder();
        fm.setup(state);
        state.getRounds().draw();
        state.getRounds().add(state.getRoundCard(ColtExpressTypes.RegularRoundCard.Bridge, 3));

        RoundCard card = state.getRounds().peek();
        assertEquals(NormalTurn, card.getTurnTypes()[0]);
        assertEquals(DoubleTurn, card.getTurnTypes()[1]);
        assertEquals(NormalTurn, card.getTurnTypes()[2]);

        assertEquals(0, turnOrder.getTurnOwner());
        assertEquals(0, turnOrder.getRoundCounter());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());
        assertEquals(DoubleTurn, turnOrder.getCurrentTurnType());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, turnOrder.getTurnOwner());
        assertEquals(PlanActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, turnOrder.getTurnOwner());
        assertEquals(0, turnOrder.getRoundCounter());
        assertEquals(ExecuteActions, state.getGamePhase());
    }

    @Test
    public void testTurnOwnerProgressionInExecutionPhase() {
        testTurnOwnerProgressesInPlanningPhase();

        assertEquals(0, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, turnOrder.getTurnOwner());
        assertEquals(0, turnOrder.getRoundCounter());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, turnOrder.getTurnOwner());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(2, turnOrder.getTurnOwner());
        assertEquals(0, turnOrder.getRoundCounter());
        assertEquals(ExecuteActions, state.getGamePhase());

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, turnOrder.getTurnOwner());
        assertEquals(1, turnOrder.getRoundCounter());
        assertEquals(PlanActions, state.getGamePhase());
    }
}
