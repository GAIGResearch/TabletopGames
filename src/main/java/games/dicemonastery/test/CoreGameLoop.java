package games.dicemonastery.test;

import core.actions.*;
import games.dicemonastery.*;
import games.dicemonastery.actions.*;
import org.junit.*;
import players.simple.RandomPlayer;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;


public class CoreGameLoop {

    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    DiceMonasteryGame game = new DiceMonasteryGame(fm, new DiceMonasteryGameState(new DiceMonasteryParams(3), 4));
    RandomPlayer rnd = new RandomPlayer();

    @Test
    public void gameSetup() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        for (int p = 0; p < 4; p++) {
            assertEquals(2, state.getResource(p, GRAIN, STOREROOM));
            assertEquals(2, state.getResource(p, WAX, STOREROOM));
            assertEquals(2, state.getResource(p, HONEY, STOREROOM));
            assertEquals(2, state.getResource(p, BREAD, STOREROOM));
            assertEquals(6, state.getResource(p, SHILLINGS, STOREROOM));
            assertEquals(1, state.getResource(p, PRAYERS, STOREROOM));

            assertEquals(4 + 3 + 2 + 2 + 1 + 1, state.getGameScore(p), 0.01);
            assertEquals(6, state.monksIn(DORMITORY, p).size());
        }
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(SPRING, ((DiceMonasteryTurnOrder) state.getTurnOrder()).getSeason());
        assertEquals(1, ((DiceMonasteryTurnOrder) state.getTurnOrder()).getYear());

        assertEquals(24, state.monksIn(DORMITORY, -1).size());
    }

    @Test
    public void placeMonkActionsGeneratedCorrectly() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(6, actions.size());
        for (ActionArea a : ActionArea.values()) {
            if (a.dieMinimum > 0)
                assertTrue(actions.contains(new PlaceMonk(0, a)));
        }

        fm.next(state, new PlaceMonk(0, MEADOW));
        actions = fm.computeAvailableActions(state);
        assertEquals(4, actions.size());
        assertTrue(actions.stream().allMatch(a -> a.toString().contains("Choose Monk")));

        assertEquals(24, state.monksIn(DORMITORY, -1).size());
        assertEquals(6, state.monksIn(DORMITORY, 0).size());
        assertEquals(0, state.monksIn(MEADOW, -1).size());
        assertEquals(0, state.monksIn(MEADOW, 0).size());

        fm.next(state, actions.get(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(23, state.monksIn(DORMITORY, -1).size());
        assertEquals(5, state.monksIn(DORMITORY, 0).size());
        assertEquals(4 + 3 + 2 + 2 + 1 + 1, state.getGameScore(0), 0.01);
        assertEquals(1, state.monksIn(MEADOW, -1).size());
        assertEquals(1, state.monksIn(MEADOW, 0).size());

        for(Monk m : state.monksIn(null, -1)) {
            assertTrue (state.getMonkLocation(m.getComponentID()).dieMinimum <= m.getPiety());
        }
    }

    @Test
    public void varyingNumbersOfMonksWorksWhenPlacing() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.createMonk(5, 3);

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 1);

        // at this point we should have 1 monks still to place for P3, and 0 each for all other players
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Phase.PLACE_MONKS, state.getGamePhase());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // PlaceMonk
        fm.next(state, fm.computeAvailableActions(state).get(0)); // ChooseMonk
        assertEquals(USE_MONKS, state.getGamePhase());
    }

    @Test
    public void seasonMovesOnAfterPlacingAndUsingAllMonks() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        do {
            assertEquals(USE_MONKS, state.getGamePhase());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.monksIn(DORMITORY, -1).size() < 24);

        assertEquals(1, turnOrder.getAbbot());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(AUTUMN, turnOrder.getSeason());
    }

    @Test
    public void areaIsSkippedIfNoMonksInIt() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        for (Monk m : state.monksIn(WORKSHOP, -1)) {
            state.moveMonk(m.getComponentID(), WORKSHOP, DORMITORY);
        }
        for (Monk m : state.monksIn(CHAPEL, -1)) {
            state.moveMonk(m.getComponentID(), CHAPEL, DORMITORY);
        }
        // Now have no monks in CHAPEL or WORKSHOP
        Map<ActionArea, Integer> monksInArea = Arrays.stream(ActionArea.values())
                .collect(toMap(a -> a, a -> state.monksIn(a, -1).size()));

        int totalMonks = 24 - state.monksIn(DORMITORY, -1).size();
        Set<ActionArea> areasProcessed = new HashSet<>();
        do {
            areasProcessed.add(turnOrder.getCurrentArea());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }while (state.monksIn(DORMITORY, -1).size() < 24);

        assertEquals(4, areasProcessed.size());
        assertFalse(areasProcessed.contains(CHAPEL));
        assertFalse(areasProcessed.contains(WORKSHOP));

        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(AUTUMN, turnOrder.getSeason());
        assertEquals(1, turnOrder.getAbbot());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void meadowIsSkippedIfEmpty() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        do {
            List<AbstractAction> available = fm.computeAvailableActions(state).stream()
                    .filter(a -> !(a instanceof PlaceMonk) || ((PlaceMonk) a).destination != MEADOW).collect(toList());
            fm.next(state, rnd.getAction(state, available));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        assertEquals(0, state.monksIn(MEADOW, -1).size());
        assertEquals(KITCHEN, turnOrder.getCurrentArea());
    }

    @Test
    public void gameEndsAfterThreeYears() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.isNotTerminal());

        assertEquals(4, turnOrder.getYear());
        assertEquals(SPRING, turnOrder.getSeason());
    }

    @Test
    public void playerOrderToUseMonksInPietyOrder() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        // we now want to check that the first player has the most piety in that area
        // and that this changes each time we move to a new area

        ActionArea currentArea = null;
        int lastPlayer = -1;
        boolean playerSwitchExpected = false;
        do {
            if (currentArea != turnOrder.getCurrentArea()) {
                currentArea = turnOrder.getCurrentArea();
                Map<Integer, Integer> pietyPerPlayer = state.monksIn(currentArea, -1).stream()
                        .collect(groupingBy(Monk::getOwnerId, summingInt(Monk::getPiety)));
                int maxPiety = pietyPerPlayer.values().stream().max(Integer::compareTo).get();
                int actualPiety = pietyPerPlayer.getOrDefault(state.getCurrentPlayer(), 0);
                assertEquals(maxPiety, actualPiety);
                lastPlayer = state.getCurrentPlayer();
            } else {
                if (!playerSwitchExpected) {
                    assertEquals(lastPlayer, state.getCurrentPlayer());
                } else {
                    assertNotSame(lastPlayer, state.getCurrentPlayer());
                    lastPlayer = state.getCurrentPlayer();
                }
            }
            AbstractAction actionChosen = rnd.getAction(state, fm.computeAvailableActions(state));
            playerSwitchExpected = actionChosen instanceof Pass || ((UseMonk) actionChosen).getActionPoints() == turnOrder.getActionPointsLeft();
            fm.next(state, actionChosen);
        } while (state.getGamePhase() == USE_MONKS);
    }

    @Test
    public void playerOrderToUseMonksBreaksTiesByPlayerOrder() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        turnOrder.setAbbot(1);

        // then we move monks to ensure a tie between players 0 and 1...player 1 should then go first
        // we also repeat for players 2, 3...player 2 should go first
        for (Monk monk : state.monksIn(DORMITORY, 0)) {
            state.moveMonk(monk.getComponentID(), DORMITORY, MEADOW);
        }
        for (Monk monk : state.monksIn(DORMITORY, 1)) {
            state.moveMonk(monk.getComponentID(), DORMITORY, MEADOW);
        }
        for (Monk monk : state.monksIn(DORMITORY, 2)) {
            state.moveMonk(monk.getComponentID(), DORMITORY, GATEHOUSE);
        }
        for (Monk monk : state.monksIn(DORMITORY, 3)) {
            state.moveMonk(monk.getComponentID(), DORMITORY, GATEHOUSE);
        }
        fm.next(state, new DoNothing()); // this will move on phase and current player
        assertEquals(USE_MONKS, state.getGamePhase());
        assertEquals(MEADOW, turnOrder.getCurrentArea());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, turnOrder.nextPlayer(state));

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (turnOrder.getCurrentArea() != GATEHOUSE);

        assertEquals(2, state.getCurrentPlayer());
        assertEquals(2, turnOrder.nextPlayer(state));
    }
}
