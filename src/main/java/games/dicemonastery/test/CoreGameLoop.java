package games.dicemonastery.test;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dicemonastery.DiceMonasteryConstants.Resource;
import games.dicemonastery.*;
import games.dicemonastery.actions.*;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.*;
import java.util.stream.IntStream;

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
            assertEquals(1, state.getResource(p, PRAYER, STOREROOM));
            assertEquals(2, state.getResource(p, SKEP, STOREROOM));
            assertEquals(0, state.getResource(p, CALF_SKIN, STOREROOM));
            assertEquals(0, state.getResource(p, VELLUM, STOREROOM));
            assertEquals(0, state.getResource(p, CANDLE, STOREROOM));

            assertEquals(4 + 3 + 2 + 2 + 1 + 1, state.monksIn(DORMITORY, p).stream().mapToInt(Monk::getPiety).sum(), 0.01);
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
        assertTrue(actions.stream().allMatch(a -> a instanceof ChooseMonk));

        assertEquals(24, state.monksIn(DORMITORY, -1).size());
        assertEquals(6, state.monksIn(DORMITORY, 0).size());
        assertEquals(0, state.monksIn(MEADOW, -1).size());
        assertEquals(0, state.monksIn(MEADOW, 0).size());

        fm.next(state, actions.get(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(23, state.monksIn(DORMITORY, -1).size());
        assertEquals(5, state.monksIn(DORMITORY, 0).size());
        assertEquals(4 + 3 + 2 + 2 + 1 + 1, state.monksIn(null, 0).stream().mapToInt(Monk::getPiety).sum(), 0.01);
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
        assertEquals(AUTUMN, turnOrder.getSeason()); // skip SUMMER in first year
    }

    @Test
    public void areaIsSkippedIfNoMonksInIt() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);
        assertEquals(0, state.monksIn(DORMITORY, -1).size());

        for (Monk m : state.monksIn(WORKSHOP, -1)) {
            state.moveMonk(m.getComponentID(), WORKSHOP, DORMITORY);
        }
        for (Monk m : state.monksIn(CHAPEL, -1)) {
            state.moveMonk(m.getComponentID(), CHAPEL, DORMITORY);
        }
        Set<ActionArea> areasWithMonks = Arrays.stream(ActionArea.values())
                .filter(a -> a != RETIRED && a != GRAVEYARD && a != DORMITORY)
                .filter(a -> state.monksIn(a, -1).size() > 0).collect(toSet());
        assertTrue(areasWithMonks.size() <= 4);
        // Now have no monks in CHAPEL or WORKSHOP
        Set<ActionArea> areasProcessed = new HashSet<>();
        do {
            areasProcessed.add(turnOrder.getCurrentArea());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.monksIn(DORMITORY, -1).size() < 24);

        assertEquals(areasWithMonks, areasProcessed);
        assertFalse(areasProcessed.contains(CHAPEL));
        assertFalse(areasProcessed.contains(WORKSHOP));

        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(AUTUMN, turnOrder.getSeason());  // skip SUMMER in first year
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
    public void springHousekeepingFermentsAlcohol() {
        // we set up stocks of ProtoBeer and Mead, and check they move as expected
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        state.addResource(0, PROTO_BEER_1, 1);
        state.addResource(1, PROTO_BEER_2, 2);
        state.addResource(2, PROTO_MEAD_1, 3);
        state.addResource(3, PROTO_MEAD_2, 4);
        fm.next(state, new PlaceMonk(0, CHAPEL)); // ensure we have one monk at least in the CHAPEL, so that we can stop before Housekeeping
        do {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            fm.next(state, rnd.getAction(state, available));
        } while (state.monksIn(DORMITORY, -1).size() > 1);
        // This should leave us with one monk left in the Dormitory

        do {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            fm.next(state, rnd.getAction(state, available));
        } while (!state.monksIn(LIBRARY, -1).isEmpty()  || !state.monksIn(GATEHOUSE, -1).isEmpty());
        // This should leave us with just the CHAPEL to process, and that has no mechanism to gain Resources
        // so we can now track what resources we have at the end of Spring

        assertEquals(SPRING, turnOrder.getSeason());
        List<Map<Resource, Integer>> springResources = new ArrayList<>();
        for (int player = 0; player < 4; player++) {
            springResources.add(getResourcesFor(state, player));
        }

        // Now advance into Autumn (no Summer in year 1)
        do {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            fm.next(state, rnd.getAction(state, available));
        } while (turnOrder.getSeason() != AUTUMN);

        List<Map<Resource, Integer>> summerResources = new ArrayList<>();
        for (int player = 0; player < 4; player++) {
            summerResources.add(getResourcesFor(state, player));
        }

        List<Resource> nonAlcoholicResources = Arrays.stream(Resource.values())
                .filter(r -> !r.name().contains("BEER") && !r.name().contains("MEAD"))
                .collect(toList());
        nonAlcoholicResources.remove(SHILLINGS);  // could change due to BONUS_TOKENS
        nonAlcoholicResources.remove(PRAYER);  // could change due to BONUS_TOKENS
        for (int player = 0; player < 4; player++) {
     //       System.out.println("Player : " + player);
            Map<Resource, Integer> spring = springResources.get(player);
            Map<Resource, Integer> summer = summerResources.get(player);
            for (Resource r : nonAlcoholicResources) {
       //         System.out.println("Resource : " + r);
                assertEquals(spring.get(r), summer.get(r));
            }
            assertEquals(spring.get(BEER) + spring.get(PROTO_BEER_2), summer.get(BEER).intValue());
            assertEquals(spring.get(PROTO_BEER_1).intValue(), summer.get(PROTO_BEER_2).intValue());
            assertEquals(spring.get(MEAD) + spring.get(PROTO_MEAD_2), summer.get(MEAD).intValue());
            assertEquals(spring.get(PROTO_MEAD_1).intValue(), summer.get(PROTO_MEAD_2).intValue());
            assertEquals(0, summer.get(PROTO_MEAD_1).intValue());
            assertEquals(0, summer.get(PROTO_BEER_1).intValue());
        }

    }

    private Map<Resource, Integer> getResourcesFor(DiceMonasteryGameState state, int player) {
        return Arrays.stream(Resource.values())
                .collect(toMap(r -> r, r -> state.getResource(player, r, STOREROOM)));
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
        assertTrue(Arrays.stream(state.getPlayerResults()).noneMatch(r -> r == Utils.GameResult.GAME_ONGOING));
    }

    @Test
    public void whenAPlayerRunsOutOfActionPointsTheirMonksGoToTheDormitory() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        do {
            AbstractAction action = rnd.getAction(state, fm.computeAvailableActions(state));
            //    System.out.printf("Current Player %d has %d in DORMITORY about to %s%n",
            //            state.getCurrentPlayer(), state.monksIn(DORMITORY, state.getCurrentPlayer()).size(), action.getString(state));
            fm.next(state, action);
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        int firstPlayer = state.getCurrentPlayer();
        int monksInMeadow = state.monksIn(MEADOW, firstPlayer).size();
        fm.next(state, fm.computeAvailableActions(state).get(0)); // take one of the tokens
        if (state.isActionInProgress())
            fm.next(state, fm.computeAvailableActions(state).get(0)); // and promote a monk
        fm.next(state, new Pray(0)); // decline to Pray
        do {
            fm.next(state, new SowWheat());
        } while (state.getCurrentPlayer() == firstPlayer && fm.computeAvailableActions(state).contains(new SowWheat()));

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
                playerSwitchExpected = actionChosen instanceof Pass || actionChosen instanceof PromoteAllMonks ||
                        (actionChosen instanceof UseMonk && ((UseMonk) actionChosen).getActionPoints() == turnOrder.getActionPointsLeft());
            }
            System.out.printf("Action: %s, Player: %d, actionPointsLeft: %d, Switch: %s\n", actionChosen, lastPlayer, turnOrder.getActionPointsLeft(), playerSwitchExpected);
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
        advanceToJustBeforeStartofWinterandRemoveFood();

        int monksP2 = state.monksIn(null, 2).size();

        state.addResource(1, BERRIES, 1);
        state.addResource(1, BREAD, 10);
        state.addResource(1, HONEY, 2);
        state.addResource(1, GRAIN, 20);
        state.addResource(2, BERRIES, 1);
        state.addResource(2, BREAD, 2);
        state.addResource(2, HONEY, 10);

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state))); // last Action of Autumn -> WINTER
        } while (turnOrder.getSeason() != WINTER);
        assertEquals(WINTER, turnOrder.getSeason());

        assertEquals(0, state.getResource(1, BERRIES, STOREROOM));
        assertEquals(0, state.getResource(1, BREAD, STOREROOM));
        assertEquals(2, state.getResource(1, HONEY, STOREROOM));
        assertEquals(0, state.getResource(2, BERRIES, STOREROOM));
        assertEquals(0, state.getResource(2, BREAD, STOREROOM));
        assertEquals(10 + 3 - monksP2, state.getResource(2, HONEY, STOREROOM));
    }

    private void advanceToJustBeforeStartofWinterandRemoveFood() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (turnOrder.getSeason() != AUTUMN);
        fm.next(state, new PlaceMonk(0, CHAPEL)); // ensure we have at least one in the CHAPEL in the AUTUMN
        do {
            AbstractAction action = rnd.getAction(state, fm.computeAvailableActions(state));
    //        System.out.printf("Player: %d, %s%n", state.getCurrentPlayer(), action);
            fm.next(state, action);
        } while (!(turnOrder.getSeason() == AUTUMN && turnOrder.getCurrentArea() == CHAPEL));

        assertEquals(AUTUMN, turnOrder.getSeason());
        assertEquals(1, turnOrder.getYear());

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
        assertEquals(1, turnOrder.getYear());
        advanceToJustBeforeStartofWinterandRemoveFood();

        state.addVP(10 - state.getVictoryPoints(1), 1);
        state.putToken(CHAPEL, DiceMonasteryConstants.BONUS_TOKEN.DEVOTION, 0);
        state.putToken(CHAPEL, DiceMonasteryConstants.BONUS_TOKEN.DEVOTION, 1);

        state.createMonk(1, 1);  // ensure we have at least one Piety 1 monk
        List<Monk> monksP1 = state.monksIn(null, 1);
        int totalPips = monksP1.stream().mapToInt(Monk::getPiety).sum();
        int totalOners = (int) monksP1.stream().filter(m -> m.getPiety() == 1).count();
        assertTrue(totalOners > 0);
        int monksInChapelWhoWillPipUp = (int) state.monksIn(CHAPEL, 1).stream().filter(m -> m.getPiety() < 6).count();
        int monksInChapelWhoWillRetire = (int) state.monksIn(CHAPEL, 1).stream().filter(m -> m.getPiety() == 6).count();
        int pietyOneMonksInChapel =  (int) state.monksIn(CHAPEL, 1).stream().filter(m -> m.getPiety() == 1).count();
        totalOners -= pietyOneMonksInChapel; // they won't be oners when they get to feeding time

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state))); // last few actions of Autumn -> WINTER
        } while (turnOrder.getSeason() != WINTER);
        assertEquals(WINTER, turnOrder.getSeason());

        int newPips = state.monksIn(null, 1).stream().mapToInt(Monk::getPiety).sum();
        assertEquals(totalPips - monksP1.size() + totalOners + monksInChapelWhoWillPipUp - monksInChapelWhoWillRetire * 6, newPips);
        assertEquals(10 - monksP1.size(), state.getVictoryPoints(1));
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
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        advanceToJustBeforeStartofWinterandRemoveFood();

        state.addResource(0, BERRIES, 20);
        state.addResource(0, BREAD, 20);
        state.addResource(0, HONEY, 20);
        state.addResource(0, CALF_SKIN, 20 - state.getResource(0, CALF_SKIN, STOREROOM));
        state.addResource(0, BEER, 20 - state.getResource(0, BEER, STOREROOM));
        state.addResource(0, PROTO_BEER_1,  - state.getResource(0, PROTO_BEER_1, STOREROOM));
        state.addResource(0, PROTO_BEER_2,  - state.getResource(0, PROTO_BEER_2, STOREROOM));
        state.addResource(0, GRAIN, 20 - state.getResource(0, GRAIN, STOREROOM));

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state))); // last Action of Autumn -> WINTER
        } while (turnOrder.getSeason() != WINTER);

        assertEquals(0, state.getResource(0, BERRIES, STOREROOM));
        assertEquals(0, state.getResource(0, BREAD, STOREROOM));
        assertEquals(0, state.getResource(0, CALF_SKIN, STOREROOM));
        assertEquals(20, state.getResource(0, HONEY, STOREROOM));
        assertEquals(20, state.getResource(0, BEER, STOREROOM));
        assertEquals(20, state.getResource(0, GRAIN, STOREROOM));
    }

    @Test
    public void lackOfAnyMonksWillSkipPlaceMonksPhaseForAPlayer() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.monksIn(DORMITORY, 1).forEach(state::retireMonk);  // retire all of P1's monks so that their turn will be skipped

        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state))); // P0 PlaceMonk
        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state))); // P0 ChooseMonk

        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.monksIn(null, 1).size());
    }

    @Test
    public void monkGainedForFreeAtStartOfSeason() {
        // if the Abbot loses their last monk to the vikings in summer
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (turnOrder.getSeason() != SUMMER);
        // repeat until all of Player 0's monks are back in the dormitory
        // before we do the Bidding we remove all of P0's monks
        state.monksIn(DORMITORY, 0).forEach(state::retireMonk);
        assertEquals(0, state.monksIn(DORMITORY, 0).size());

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (turnOrder.getSeason() != AUTUMN);
        assertEquals(1, state.monksIn(DORMITORY, 0).size());
    }

    @Test
    public void gainAFreeNoviceIfLastMonkIsPromoted() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        advanceToJustBeforeStartofWinterandRemoveFood();

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state))); // last Action of Autumn -> WINTER
        } while (turnOrder.getSeason() != WINTER);

        assertEquals(0, state.getCurrentPlayer());
        state.monksIn(DORMITORY, 1).forEach(state::retireMonk);  // retire all of P1's monks
        state.createMonk(6, 1); // and give them a P6 monk to retire
        assertEquals(1, state.monksIn(DORMITORY, 1).size());
        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new PromoteMonk(6, DORMITORY), fm.computeAvailableActions(state).get(0));
        fm.next(state, new PromoteMonk(6, DORMITORY));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.monksIn(DORMITORY, 1).size());

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (turnOrder.getSeason() != SPRING);

        // and check that a free monk has been assigned
        assertEquals(1, state.monksIn(DORMITORY, 1).size());
        assertEquals(1, state.monksIn(DORMITORY, 1).get(0).getPiety());
    }


    private void advanceToSummer() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();

        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (!(turnOrder.getSeason() == SUMMER));

        assertEquals(2, turnOrder.getYear());
    }

    private void emptyAllStores() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        for (int p = 0; p < state.getNPlayers(); p++) {
            for (Resource r : Resource.values()) {
                state.addResource(p, r, -state.getResource(p, r, STOREROOM));
            }
        }
    }

    @Test
    public void summerActionsCorrect() {
        advanceToSummer();
        emptyAllStores();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new SummerBid(0, 0), actions.get(0));

        state.addResource(1, BEER, 1);
        state.addResource(1, MEAD, 1);
        actions = fm.computeAvailableActions(state);
        assertEquals(4, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof SummerBid));
        assertTrue(actions.contains(new SummerBid(0, 0)));
        assertTrue(actions.contains(new SummerBid(0, 1)));
        assertTrue(actions.contains(new SummerBid(1, 0)));
        assertTrue(actions.contains(new SummerBid(1, 1)));

        state.addResource(1, BEER, 1);
        state.addResource(1, MEAD, 1);
        actions = fm.computeAvailableActions(state);
        assertEquals(9, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof SummerBid));
        assertTrue(actions.contains(new SummerBid(0, 0)));
        assertTrue(actions.contains(new SummerBid(0, 1)));
        assertTrue(actions.contains(new SummerBid(1, 0)));
        assertTrue(actions.contains(new SummerBid(1, 1)));
        assertTrue(actions.contains(new SummerBid(0, 2)));
        assertTrue(actions.contains(new SummerBid(1, 2)));
        assertTrue(actions.contains(new SummerBid(2, 0)));
        assertTrue(actions.contains(new SummerBid(2, 1)));
        assertTrue(actions.contains(new SummerBid(2, 2)));

        state.addResource(1, BEER, 4);
        state.addResource(1, MEAD, -1);
        actions = fm.computeAvailableActions(state);
        assertEquals(8, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof SummerBid));
        assertTrue(actions.contains(new SummerBid(0, 0)));
        assertTrue(actions.contains(new SummerBid(0, 1)));
        assertTrue(actions.contains(new SummerBid(2, 0)));
        assertTrue(actions.contains(new SummerBid(2, 1)));
        assertTrue(actions.contains(new SummerBid(4, 0)));
        assertTrue(actions.contains(new SummerBid(4, 1)));
        assertTrue(actions.contains(new SummerBid(6, 0)));
        assertTrue(actions.contains(new SummerBid(6, 1)));
    }

    @Test
    public void summerBidsUniqueForAllPlayers() {
        advanceToSummer();
        emptyAllStores();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();

        for (int p = 0; p < state.getNPlayers(); p++) {
            state.addResource(p, BEER, 10);
            state.addResource(p, MEAD, 10);
        }
        int[] startVP = IntStream.range(0, 4).map(state::getVictoryPoints).toArray();
        int[] startMonks = IntStream.range(0, 4).map(p -> state.monksIn(DORMITORY, p).size()).toArray();
        fm.next(state, new SummerBid(1, 5));
        assertEquals(10, state.getResource(1, BEER, STOREROOM));
        assertEquals(10, state.getResource(1, MEAD, STOREROOM));
        fm.next(state, new SummerBid(0, 5));
        fm.next(state, new SummerBid(3, 2));
        fm.next(state, new SummerBid(1, 0));
        assertEquals(10, state.getResource(0, BEER, STOREROOM));
        assertEquals(10, state.getResource(0, MEAD, STOREROOM));

        assertEquals(AUTUMN, turnOrder.getSeason());
        assertEquals(startVP[0], state.getVictoryPoints(0));
        assertEquals(startVP[1] + 6, state.getVictoryPoints(1));
        assertEquals(startVP[2] + 4, state.getVictoryPoints(2));
        assertEquals(startVP[3] + 2, state.getVictoryPoints(3));

        // Then check that
        assertEquals(10, state.getResource(0, BEER, STOREROOM));
        assertEquals(10, state.getResource(0, MEAD, STOREROOM));
        assertEquals(9, state.getResource(1, BEER, STOREROOM));
        assertEquals(5, state.getResource(1, MEAD, STOREROOM));
        assertEquals(10, state.getResource(2, BEER, STOREROOM));
        assertEquals(5, state.getResource(2, MEAD, STOREROOM));
        assertEquals(7, state.getResource(3, BEER, STOREROOM));
        assertEquals(8, state.getResource(3, MEAD, STOREROOM));

        assertEquals(startMonks[0] - 1, state.monksIn(DORMITORY, 0).size());
        assertEquals(startMonks[1], state.monksIn(DORMITORY, 1).size());
        assertEquals(startMonks[2], state.monksIn(DORMITORY, 2).size());
        assertEquals(startMonks[3], state.monksIn(DORMITORY, 3).size());
    }

    @Test
    public void summerBidTiesForFirstAndThird() {
        advanceToSummer();
        emptyAllStores();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();

        for (int p = 0; p < state.getNPlayers(); p++) {
            state.addResource(p, BEER, 10);
            state.addResource(p, MEAD, 10);
        }
        int[] startVP = IntStream.range(0, 4).map(state::getVictoryPoints).toArray();
        int[] startMonks = IntStream.range(0, 4).map(p -> state.monksIn(DORMITORY, p).size()).toArray();
        fm.next(state, new SummerBid(1, 0));
        fm.next(state, new SummerBid(0, 1));
        fm.next(state, new SummerBid(0, 1));
        fm.next(state, new SummerBid(1, 0));

        assertEquals(AUTUMN, turnOrder.getSeason());
        assertEquals(startVP[0], state.getVictoryPoints(0));
        assertEquals(startVP[1], state.getVictoryPoints(1));
        assertEquals(startVP[2] + 6, state.getVictoryPoints(2));
        assertEquals(startVP[3] + 6, state.getVictoryPoints(3));

        // Then check that
        assertEquals(10, state.getResource(0, BEER, STOREROOM));
        assertEquals(10, state.getResource(0, MEAD, STOREROOM));
        assertEquals(10, state.getResource(1, BEER, STOREROOM));
        assertEquals(10, state.getResource(1, MEAD, STOREROOM));
        assertEquals(10, state.getResource(2, BEER, STOREROOM));
        assertEquals(9, state.getResource(2, MEAD, STOREROOM));
        assertEquals(10, state.getResource(3, BEER, STOREROOM));
        assertEquals(9, state.getResource(3, MEAD, STOREROOM));

        assertEquals(startMonks[0] - 1, state.monksIn(DORMITORY, 0).size());
        assertEquals(startMonks[1] - 1, state.monksIn(DORMITORY, 1).size());
        assertEquals(startMonks[2], state.monksIn(DORMITORY, 2).size());
        assertEquals(startMonks[3], state.monksIn(DORMITORY, 3).size());
    }

    @Test
    public void summerBidTiesForSecond() {
        advanceToSummer();
        emptyAllStores();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();

        for (int p = 0; p < state.getNPlayers(); p++) {
            state.addResource(p, BEER, 10);
            state.addResource(p, MEAD, 10);
        }
        int[] startVP = IntStream.range(0, 4).map(state::getVictoryPoints).toArray();
        int[] startMonks = IntStream.range(0, 4).map(p -> state.monksIn(DORMITORY, p).size()).toArray();
        fm.next(state, new SummerBid(1, 0));
        fm.next(state, new SummerBid(0, 2));
        fm.next(state, new SummerBid(0, 1));
        fm.next(state, new SummerBid(2, 0));

        assertEquals(AUTUMN, turnOrder.getSeason());
        assertEquals(startVP[0] + 4, state.getVictoryPoints(0));
        assertEquals(startVP[1], state.getVictoryPoints(1));
        assertEquals(startVP[2] + 6, state.getVictoryPoints(2));
        assertEquals(startVP[3] + 4, state.getVictoryPoints(3));

        // Then check that
        assertEquals(8, state.getResource(0, BEER, STOREROOM));
        assertEquals(10, state.getResource(0, MEAD, STOREROOM));
        assertEquals(10, state.getResource(1, BEER, STOREROOM));
        assertEquals(10, state.getResource(1, MEAD, STOREROOM));
        assertEquals(10, state.getResource(2, BEER, STOREROOM));
        assertEquals(8, state.getResource(2, MEAD, STOREROOM));
        assertEquals(10, state.getResource(3, BEER, STOREROOM));
        assertEquals(9, state.getResource(3, MEAD, STOREROOM));

        assertEquals(startMonks[0], state.monksIn(DORMITORY, 0).size());
        assertEquals(startMonks[1] - 1, state.monksIn(DORMITORY, 1).size());
        assertEquals(startMonks[2], state.monksIn(DORMITORY, 2).size());
        assertEquals(startMonks[3], state.monksIn(DORMITORY, 3).size());
    }

}
