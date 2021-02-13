package games.dicemonastery.test;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dicemonastery.*;
import games.dicemonastery.actions.*;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase;
import static games.dicemonastery.DiceMonasteryConstants.Phase.PLACE_MONKS;
import static games.dicemonastery.DiceMonasteryConstants.Phase.USE_MONKS;
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
            assertEquals(2, state.getResource(p, SKEP, STOREROOM));
            assertEquals(0, state.getResource(p, CALF_SKIN, STOREROOM));
            assertEquals(0, state.getResource(p, VELLUM, STOREROOM));
            assertEquals(0, state.getResource(p, CANDLE, STOREROOM));

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

        assertEquals(0, turnOrder.getAbbot());
        assertEquals(0, state.getCurrentPlayer());
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
        assertEquals(0, turnOrder.getAbbot());
        assertEquals(0, state.getCurrentPlayer());
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
    public void whenAPlayerRunsOutOfActionPointsTheirMonksGoToTheDormitory() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        int firstPlayer = state.getCurrentPlayer();
        int monksInMeadow = state.monksIn(MEADOW, firstPlayer).size();
        do {
            fm.next(state, new SowWheat());
        } while (state.getCurrentPlayer() == firstPlayer && fm.computeAvailableActions(state).contains(new SowWheat()));

        if (state.getDominantPlayers().contains(firstPlayer)) {
            fm.next(state, new Pass(true));
        }

        assertEquals(monksInMeadow, state.monksIn(DORMITORY, firstPlayer).size());
        assertEquals(0, state.monksIn(MEADOW, firstPlayer).size());
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
        boolean rewardTurn = false;
        do {
            if (currentArea != turnOrder.getCurrentArea()) {
                currentArea = turnOrder.getCurrentArea();
                Map<Integer, Integer> pietyPerPlayer = state.monksIn(currentArea, -1).stream()
                        .collect(groupingBy(Monk::getOwnerId, summingInt(Monk::getPiety)));
                int maxPiety = pietyPerPlayer.values().stream().max(Integer::compareTo).get();
                int actualPiety = pietyPerPlayer.getOrDefault(state.getCurrentPlayer(), 0);
                assertEquals(maxPiety, actualPiety);
                lastPlayer = state.getCurrentPlayer();
                System.out.printf("Starting %s with player %d and piety %d\n", currentArea, lastPlayer, maxPiety);
            } else {
                if (!playerSwitchExpected) {
                    assertEquals(lastPlayer, state.getCurrentPlayer());
                } else {
                    assertNotSame(lastPlayer, state.getCurrentPlayer());
                    lastPlayer = state.getCurrentPlayer();
                }
            }
            AbstractAction actionChosen = rnd.getAction(state, fm.computeAvailableActions(state));
            if (actionChosen instanceof VisitMarket) {
                playerSwitchExpected = false;
            } else if (state.currentActionInProgress() instanceof VisitMarket) {
                playerSwitchExpected = turnOrder.getActionPointsLeft() == 0;
            } else {
                playerSwitchExpected = actionChosen instanceof Pass ||
                        actionChosen instanceof PromoteMonk || actionChosen instanceof GainVictoryPoints ||
                        ((UseMonk) actionChosen).getActionPoints() == turnOrder.getActionPointsLeft();
            }
            if (playerSwitchExpected && !rewardTurn && state.getDominantPlayers().contains(lastPlayer)) {
                // wait one more turn for reward decision
                playerSwitchExpected = false;
                rewardTurn = true;
            } else {
                rewardTurn = false;
            }
            System.out.printf("Action: %s, Player: %d, actionPointsLeft: %d, Reward: %s, Switch: %s\n", actionChosen, lastPlayer, turnOrder.getActionPointsLeft(), rewardTurn, playerSwitchExpected);
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

    @Test
    public void foodIsRemovedToFeedMonksAtYearEnd() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        advanceToWinterAndRemoveAllFood();

        int monksP2 = state.monksIn(null, 2).size();

        state.addResource(1, BERRIES, 1);
        state.addResource(1, BREAD, 10);
        state.addResource(1, HONEY, 2);
        state.addResource(1, GRAIN, 20);
        state.addResource(2, BERRIES, 1);
        state.addResource(2, BREAD, 2);
        state.addResource(2, HONEY, 10);

        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        assertEquals(WINTER, turnOrder.getSeason());

        assertEquals(0, state.getResource(1, BERRIES, STOREROOM));
        assertEquals(0, state.getResource(1, BREAD, STOREROOM));
        assertEquals(2, state.getResource(1, HONEY, STOREROOM));
        assertEquals(0, state.getResource(2, BERRIES, STOREROOM));
        assertEquals(0, state.getResource(2, BREAD, STOREROOM));
        assertEquals(10 + 3 - monksP2, state.getResource(2, HONEY, STOREROOM));
    }

    private void advanceToWinterAndRemoveAllFood() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (!(turnOrder.getSeason() == AUTUMN && turnOrder.getCurrentArea() == CHAPEL && state.monksIn(CHAPEL, -1).size() == 1));

        assertEquals(AUTUMN, turnOrder.getSeason());


        for (int player = 0; player < turnOrder.nPlayers(); player++) {
            // clear out all food supplies for players 1 and 2
            while (state.getResource(player, BERRIES, STOREROOM) > 0)
                state.moveCube(player, BERRIES, STOREROOM, SUPPLY);
            while (state.getResource(player, HONEY, STOREROOM) > 0)
                state.moveCube(player, HONEY, STOREROOM, SUPPLY);
            while (state.getResource(player, BREAD, STOREROOM) > 0)
                state.moveCube(player, BREAD, STOREROOM, SUPPLY);
        }
    }

    @Test
    public void unfedMonksDeclineInPiety() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        advanceToWinterAndRemoveAllFood();

        state.addVP(10 - state.getVictoryPoints(0), 0);

        state.createMonk(1, 0);  // ensure we have at least one Piety 1 monk
        List<Monk> monksP0 = state.monksIn(null, 0);
        int totalPips = monksP0.stream().mapToInt(Monk::getPiety).sum();
        int totalOners = (int) monksP0.stream().filter(m -> m.getPiety() == 1).count();
        assertTrue(totalOners > 0);

        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        assertEquals(WINTER, turnOrder.getSeason());

        int newPips = state.monksIn(null, 0).stream().mapToInt(Monk::getPiety).sum();

        assertEquals(totalPips - monksP0.size() + totalOners, newPips);
        assertEquals(10 - monksP0.size(), state.getVictoryPoints(0));
    }

    @Test
    public void victoryPointsCannotGoBelowZero() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        state.addVP(-100, 0);
        assertEquals(0, state.getVictoryPoints(0));
    }

    @Test
    public void allSurplusPerishablesAreRemovedAtYearEnd() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        advanceToWinterAndRemoveAllFood();

        state.addResource(0, BERRIES, 20);
        state.addResource(0, BREAD, 20);
        state.addResource(0, HONEY, 20);
        state.addResource(0, CALF_SKIN, 20 - state.getResource(0, CALF_SKIN, STOREROOM));
        state.addResource(0, BEER, 20 - state.getResource(0, BEER, STOREROOM));
        state.addResource(0, GRAIN, 20 - state.getResource(0, GRAIN, STOREROOM));

        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));

        assertEquals(0, state.getResource(0, BERRIES, STOREROOM));
        assertEquals(0, state.getResource(0, BREAD, STOREROOM));
        assertEquals(0, state.getResource(0, CALF_SKIN, STOREROOM));
        assertEquals(20, state.getResource(0, HONEY, STOREROOM));
        assertEquals(20, state.getResource(0, BEER, STOREROOM));
        assertEquals(20, state.getResource(0, GRAIN, STOREROOM));
    }

    @Test
    public void gainAFreeNoviceIfNoMonksLeftAtEndOfTurn() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        advanceToWinterAndRemoveAllFood();
        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));

        state.monksIn(DORMITORY, 0).forEach(state::retireMonk);

        assertEquals(0, state.getCurrentPlayer());

        assertEquals(0, state.monksIn(DORMITORY, 0).size());
        fm.next(state, new DoNothing()); // don't promote anyone
        assertEquals(1, state.monksIn(DORMITORY, 0).size());
        assertEquals(1, state.monksIn(DORMITORY, 0).get(0).getPiety());
    }
}
