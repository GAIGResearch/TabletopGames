package games.dicemonastery;

import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.GameType;
import games.dicemonastery.*;
import games.dicemonastery.DiceMonasteryConstants.Resource;
import games.dicemonastery.actions.*;
import games.dicemonastery.components.Monk;
import games.dicemonastery.components.Treasure;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase;
import static games.dicemonastery.DiceMonasteryConstants.Phase.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;


public class CoreGameLoopTests {

    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    Game game = GameType.DiceMonastery.createGameInstance(4, new DiceMonasteryParams(3));
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
        assertEquals(SPRING, state.getSeason());
        assertEquals(1,  state.getYear());

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
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
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
        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        do {
            assertEquals(USE_MONKS, state.getGamePhase());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.monksIn(DORMITORY, -1).size() < 24);

        assertEquals(0, state.getFirstPlayer());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(AUTUMN, state.getSeason()); // skip SUMMER in first year
    }

    @Test
    public void areaIsSkippedIfNoMonksInIt() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
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
            areasProcessed.add(state.getCurrentArea());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.monksIn(DORMITORY, -1).size() < 24);

        assertEquals(areasWithMonks, areasProcessed);
        assertFalse(areasProcessed.contains(CHAPEL));
        assertFalse(areasProcessed.contains(WORKSHOP));

        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(AUTUMN, state.getSeason());  // skip SUMMER in first year
        assertEquals(0, state.getFirstPlayer());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void meadowIsSkippedIfEmpty() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        do {
            List<AbstractAction> available = fm.computeAvailableActions(state).stream()
                    .filter(a -> !(a instanceof PlaceMonk) || ((PlaceMonk) a).destination != MEADOW).collect(toList());
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        assertEquals(0, state.monksIn(MEADOW, -1).size());
        assertEquals(KITCHEN, state.getCurrentArea());
    }

    @Test
    public void springHousekeepingFermentsAlcohol() {
        // we set up stocks of ProtoBeer and Mead, and check they move as expected
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        state.addResource(0, PROTO_BEER_1, 1);
        state.addResource(1, PROTO_BEER_2, 2);
        state.addResource(2, PROTO_MEAD_1, 3);
        state.addResource(3, PROTO_MEAD_2, 4);
        fm.next(state, new PlaceMonk(0, CHAPEL)); // ensure we have one monk at least in the CHAPEL, so that we can stop before Housekeeping
        do {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, -1).size() > 1);
        // This should leave us with one monk left in the Dormitory

        do {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (!state.monksIn(LIBRARY, -1).isEmpty()  || !state.monksIn(GATEHOUSE, -1).isEmpty());
        // This should leave us with just the CHAPEL to process, and that has no mechanism to gain Resources
        // so we can now track what resources we have at the end of Spring

        assertEquals(SPRING, state.getSeason());
        List<Map<Resource, Integer>> springResources = new ArrayList<>();
        for (int player = 0; player < 4; player++) {
            springResources.add(getResourcesFor(state, player));
        }

        // Now advance into Autumn (no Summer in year 1)
        do {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getSeason() != AUTUMN);

        List<Map<Resource, Integer>> summerResources = new ArrayList<>();
        for (int player = 0; player < 4; player++) {
            summerResources.add(getResourcesFor(state, player));
        }

        List<Resource> nonAlcoholicResources = Arrays.stream(Resource.values())
                .filter(r -> !r.name().contains("BEER") && !r.name().contains("MEAD"))
                .collect(toList());
        nonAlcoholicResources.remove(SHILLINGS);  // could change due to BONUS_TOKENS
        nonAlcoholicResources.remove(PRAYER);  // could change due to BONUS_TOKENS
        nonAlcoholicResources.removeAll(Arrays.asList(VIVID_BLUE_PIGMENT, VIVID_GREEN_PIGMENT, VIVID_PURPLE_PIGMENT, VIVID_RED_PIGMENT));  // possible from pilgrimage
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
    public void gameEndsAfterFourYears() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.isNotTerminal());

        assertEquals(4, state.getYear());
        assertEquals(WINTER, state.getSeason());
        assertTrue(Arrays.stream(state.getPlayerResults()).noneMatch(r -> r == CoreConstants.GameResult.GAME_ONGOING));
    }

    @Test
    public void whenAPlayerRunsOutOfActionPointsTheirMonksGoToTheDormitory() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        do {
            AbstractAction action = rnd._getAction(state, fm.computeAvailableActions(state));
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

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        // we now want to check that the first player has the most piety in that area
        // and that this changes each time we move to a new area

        ActionArea currentArea = null;
        int lastPlayer = -1;
        boolean playerSwitchExpected = false;
        do {
            if (currentArea != state.getCurrentArea()) {
                currentArea = state.getCurrentArea();
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
            AbstractAction actionChosen = rnd._getAction(state, fm.computeAvailableActions(state));
            if (actionChosen instanceof VisitMarket) {
                playerSwitchExpected = false;
            } else if (state.currentActionInProgress() instanceof VisitMarket) {
                playerSwitchExpected = state.getActionPointsLeft() == 0;
            } else {
                playerSwitchExpected = actionChosen instanceof Pass || (actionChosen instanceof PromoteMonk && ((PromoteMonk) actionChosen).useAllAP) ||
                        (actionChosen instanceof UseMonk && ((UseMonk) actionChosen).getActionPoints() == state.getActionPointsLeft());
            }
            System.out.printf("Action: %s, Player: %d, actionPointsLeft: %d, Switch: %s\n", actionChosen, lastPlayer, state.getActionPointsLeft(), playerSwitchExpected);
            fm.next(state, actionChosen);
        } while (state.getGamePhase() == USE_MONKS);
    }

    @Test
    public void playerOrderToUseMonksBreaksTiesByPlayerOrder() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        state.setFirstPlayer(1);

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
        assertEquals(MEADOW, state.getCurrentArea());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.nextPlayer());

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getCurrentArea() != GATEHOUSE);

        assertEquals(2, state.getCurrentPlayer());
        assertEquals(2, state.nextPlayer());
    }

    @Test
    public void foodIsRemovedToFeedMonksAtYearEnd() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        advanceToJustBeforeStartofWinterandRemoveFood();

        state.addResource(1, BREAD, 10);
        state.addResource(1, HONEY, 2);
        state.addResource(1, GRAIN, 20);
        state.addResource(2, BREAD, 2);
        state.addResource(2, HONEY, 10);

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // last Action of Autumn -> WINTER
        } while (state.getSeason() != WINTER);
        assertEquals(WINTER, state.getSeason());

        int monksP2 = state.monksIn(null, 2).size();
        int monksOnPilgrimage = state.monksIn(PILGRIMAGE, 2).size();

        assertEquals(0, state.getResource(1, BREAD, STOREROOM));
        assertEquals(2, state.getResource(1, HONEY, STOREROOM));
        assertEquals(0, state.getResource(2, BREAD, STOREROOM));
        assertEquals(10 + 2 - monksP2 + monksOnPilgrimage, state.getResource(2, HONEY, STOREROOM));
    }

    private void advanceToJustBeforeStartofWinterandRemoveFood() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getSeason() != AUTUMN);
        fm.next(state, new PlaceMonk(0, CHAPEL)); // ensure we have at least one in the CHAPEL in the AUTUMN
        do {
            AbstractAction action = rnd._getAction(state, fm.computeAvailableActions(state));
    //        System.out.printf("Player: %d, %s%n", state.getCurrentPlayer(), action);
            fm.next(state, action);
        } while (!(state.getSeason() == AUTUMN && state.getCurrentArea() == CHAPEL));

        assertEquals(AUTUMN, state.getSeason());
        assertEquals(1, state.getYear());

        for (int player = 0; player < state.getNPlayers(); player++) {
            // clear out all food supplies for players 1 and 2
            while (state.getResource(player, HONEY, STOREROOM) > 0)
                state.moveCube(player, HONEY, STOREROOM, SUPPLY);
            while (state.getResource(player, BREAD, STOREROOM) > 0)
                state.moveCube(player, BREAD, STOREROOM, SUPPLY);
        }
    }

    @Test
    public void unfedMonksDeclineInPiety() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        assertEquals(1, state.getYear());
        advanceToJustBeforeStartofWinterandRemoveFood();

        state.addVP(10 - state.getVictoryPoints(1), 1);
        state.putToken(CHAPEL, DiceMonasteryConstants.BONUS_TOKEN.DEVOTION, 0);
        state.putToken(CHAPEL, DiceMonasteryConstants.BONUS_TOKEN.DEVOTION, 1);

        state.createMonk(1, 1);  // ensure we have at least one Piety 1 monk
        List<Monk> monksP1 = state.monksIn(null, 1);
        int totalPips = monksP1.stream().mapToInt(Monk::getPiety).sum();
        int totalOners = (int) monksP1.stream().filter(m -> m.getPiety() == 1).count();
        assertTrue(totalOners > 0);
        int monksInChapel = (int) state.monksIn(CHAPEL, 1).stream().filter(m -> m.getPiety() < 6).count();
        int monksInChapelWhoWillRetire = (int) state.monksIn(CHAPEL, 1).stream().filter(m -> m.getPiety() == 6).count();
        int pietyOneMonksInChapel = (int) state.monksIn(CHAPEL, 1).stream().filter(m -> m.getPiety() == 1).count();

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // last few actions of Autumn -> WINTER
        } while (state.getSeason() != WINTER);
        assertEquals(WINTER, state.getSeason());
        int monksOnPilgrimage = state.monksIn(PILGRIMAGE, 1).size();
        assertEquals(monksP1.size() - monksOnPilgrimage, state.monksIn(DORMITORY, 1).size());

        int monksInChapelWhoWillPipUp = monksInChapel > 0 ? 1 : 0;
        if (pietyOneMonksInChapel > 0 && pietyOneMonksInChapel == monksInChapel)
            totalOners--;
        int error = pietyOneMonksInChapel > 0 && pietyOneMonksInChapel != monksInChapel ? 1 : 0;
        // in this case we may have an answer 1 less than expected - if the piety 1 monk is promoted
        int newPips = state.monksIn(null, 1).stream().mapToInt(Monk::getPiety).sum();
        System.out.printf("Actual: %d, Start: %d, Monks: %d (%d), InChapel: %d (%d, %d), Pilgrims: %d, Error: %d%n ", newPips, totalPips, monksP1.size(), totalOners, monksInChapel, pietyOneMonksInChapel, monksInChapelWhoWillRetire, monksOnPilgrimage, error);
        assertEquals(totalPips - monksP1.size() + totalOners + monksInChapelWhoWillPipUp + monksOnPilgrimage - monksInChapelWhoWillRetire * 6, newPips, error);
        assertEquals(10 - monksP1.size() + monksOnPilgrimage, state.getVictoryPoints(1), 1);
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
        advanceToJustBeforeStartofWinterandRemoveFood();

        state.addResource(0, BREAD, 20);
        state.addResource(0, HONEY, 20);
        state.addResource(0, CALF_SKIN, 20 - state.getResource(0, CALF_SKIN, STOREROOM));
        state.addResource(0, BEER, 20 - state.getResource(0, BEER, STOREROOM));
        state.addResource(0, PROTO_BEER_1,  - state.getResource(0, PROTO_BEER_1, STOREROOM));
        state.addResource(0, PROTO_BEER_2,  - state.getResource(0, PROTO_BEER_2, STOREROOM));
        state.addResource(0, GRAIN, 20 - state.getResource(0, GRAIN, STOREROOM));

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // last Action of Autumn -> WINTER
        } while (state.getSeason() != WINTER);

        assertEquals(0, state.getResource(0, BREAD, STOREROOM));
 //       assertEquals(0, state.getResource(0, CALF_SKIN, STOREROOM));
        assertEquals(20, state.getResource(0, HONEY, STOREROOM));
        assertEquals(20, state.getResource(0, BEER, STOREROOM));
        assertEquals(20, state.getResource(0, GRAIN, STOREROOM));
    }

    @Test
    public void lackOfAnyMonksWillSkipPlaceMonksPhaseForAPlayer() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.monksIn(DORMITORY, 1).forEach(state::retireMonk);  // retire all of P1's monks so that their turn will be skipped

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // P0 PlaceMonk
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // P0 ChooseMonk

        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.monksIn(null, 1).size());
    }

    @Test
    public void monkGainedForFreeAtStartOfSeason() {
        // if the Abbot loses their last monk to the vikings in summer
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getSeason() != SUMMER);
        // repeat until all of Player 0's monks are back in the dormitory
        // before we do the Bidding we remove all of P0's monks
        state.monksIn(DORMITORY, 0).forEach(state::retireMonk);
        state.monksIn(PILGRIMAGE, 0).forEach(state::retireMonk);
        assertEquals(0, state.monksIn(DORMITORY, 0).size());

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getSeason() != AUTUMN);
        assertEquals(1, state.monksIn(DORMITORY, 0).size());
    }

    @Test
    public void gainAFreeNoviceIfLastMonkIsPromoted() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        advanceToJustBeforeStartofWinterandRemoveFood();

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // last Action of Autumn -> WINTER
        } while (state.getSeason() != WINTER);

        assertEquals(0, state.getCurrentPlayer());
        state.monksIn(DORMITORY, 1).forEach(state::retireMonk);  // retire all of P1's monks
        state.monksIn(PILGRIMAGE, 1).forEach(state::retireMonk);  // retire all of P1's monks on pilgrimage
        state.createMonk(6, 1); // and give them a P6 monk to retire
        assertEquals(1, state.monksIn(DORMITORY, 1).size());
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));  // p0 promotes a random monk
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new PromoteMonk(6, DORMITORY), fm.computeAvailableActions(state).get(0));
        fm.next(state, new PromoteMonk(6, DORMITORY));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.monksIn(DORMITORY, 1).size());

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getSeason() != SPRING);

        // and check that a free monk has been assigned
        assertEquals(1, state.monksIn(DORMITORY, 1).size());
        assertEquals(1, state.monksIn(DORMITORY, 1).get(0).getPiety());
    }


    private void advanceToSummerAndRemoveTreasure() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (!(state.getSeason() == SUMMER));

        for (int p = 0; p < 4; p++)
            state.getTreasures(p).clear();

        assertEquals(2, state.getYear());
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
        advanceToSummerAndRemoveTreasure();
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
        advanceToSummerAndRemoveTreasure();
        emptyAllStores();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        assertEquals(BID, state.getGamePhase());
        for (int p = 0; p < state.getNPlayers(); p++) {
            state.addResource(p, BEER, 10);
            state.addResource(p, MEAD, 10);
        }
        int[] startVP = IntStream.range(0, 4).map(state::getVictoryPoints).toArray();
        int[] startMonks = IntStream.range(0, 4).map(p -> state.monksIn(DORMITORY, p).size()).toArray();
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new SummerBid(1, 5));
        assertEquals(10, state.getResource(1, BEER, STOREROOM));
        assertEquals(10, state.getResource(1, MEAD, STOREROOM));
        fm.next(state, new SummerBid(0, 5));
        fm.next(state, new SummerBid(3, 2));
        fm.next(state, new SummerBid(1, 0));  // Player 0
        assertEquals(10, state.getResource(0, BEER, STOREROOM));
        assertEquals(10, state.getResource(0, MEAD, STOREROOM));

        assertEquals(SUMMER, state.getSeason());
        assertEquals(SACRIFICE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
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

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.get(0) instanceof KillMonk);
        fm.next(state, actions.get(0));

        assertEquals(AUTUMN, state.getSeason());
        assertEquals(PLACE_MONKS, state.getGamePhase());

        assertEquals(startMonks[0] - 1, state.monksIn(DORMITORY, 0).size());
        assertEquals(startMonks[1], state.monksIn(DORMITORY, 1).size());
        assertEquals(startMonks[2], state.monksIn(DORMITORY, 2).size());
        assertEquals(startMonks[3], state.monksIn(DORMITORY, 3).size());
    }

    @Test
    public void summerBidTiesForFirstAndThird() {
        advanceToSummerAndRemoveTreasure();
        emptyAllStores();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        for (int p = 0; p < state.getNPlayers(); p++) {
            state.addResource(p, BEER, 10);
            state.addResource(p, MEAD, 10);
        }
        int[] startVP = IntStream.range(0, 4).map(state::getVictoryPoints).toArray();
        int[] startMonks = IntStream.range(0, 4).map(p -> state.monksIn(DORMITORY, p).size()).toArray();
        fm.next(state, new SummerBid(1, 0)); // player 1
        fm.next(state, new SummerBid(0, 1));
        fm.next(state, new SummerBid(0, 1));
        fm.next(state, new SummerBid(1, 0)); // player 0

        assertEquals(SUMMER, state.getSeason());
        assertEquals(SACRIFICE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());

        assertEquals(startVP[0], state.getVictoryPoints(0));
        assertEquals(startVP[1], state.getVictoryPoints(1));
        assertEquals(startVP[2] + 5, state.getVictoryPoints(2));
        assertEquals(startVP[3] + 5, state.getVictoryPoints(3));

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.get(0) instanceof KillMonk);
        fm.next(state, actions.get(0));
        assertEquals(1, state.getCurrentPlayer());
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.get(0) instanceof KillMonk);
        fm.next(state, actions.get(0));

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
        advanceToSummerAndRemoveTreasure();
        emptyAllStores();
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

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

        assertEquals(SUMMER, state.getSeason());
        assertEquals(SACRIFICE, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());

        assertEquals(startVP[0] + 3, state.getVictoryPoints(0));
        assertEquals(startVP[1], state.getVictoryPoints(1));
        assertEquals(startVP[2] + 6, state.getVictoryPoints(2));
        assertEquals(startVP[3] + 3, state.getVictoryPoints(3));

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
        assertEquals(startMonks[1], state.monksIn(DORMITORY, 1).size());
        assertEquals(startMonks[2], state.monksIn(DORMITORY, 2).size());
        assertEquals(startMonks[3], state.monksIn(DORMITORY, 3).size());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.get(0) instanceof KillMonk);
        fm.next(state, actions.get(0));

        assertEquals(startMonks[0], state.monksIn(DORMITORY, 0).size());
        assertEquals(startMonks[1] - 1, state.monksIn(DORMITORY, 1).size());
        assertEquals(startMonks[2], state.monksIn(DORMITORY, 2).size());
        assertEquals(startMonks[3], state.monksIn(DORMITORY, 3).size());
    }

    @Test
    public void vikingsTakeTreasure() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        List<Treasure> allTreasures = state.availableTreasures();
        Treasure cape = allTreasures.stream().filter(t -> t.getComponentName().equals("Cape"))
                .findFirst().orElseThrow( () -> new AssertionError("Cape not found"));
        Treasure candlestick = allTreasures.stream().filter(t -> t.getComponentName().equals("Candlestick"))
                .findFirst().orElseThrow( () -> new AssertionError("Candlestick not found"));
        Treasure altarCross = allTreasures.stream().filter(t -> t.getComponentName().equals("Altar Cross"))
                .findFirst().orElseThrow( () -> new AssertionError("Altar Cross not found"));

        advanceToSummerAndRemoveTreasure();
        emptyAllStores();

        state.addTreasure(cape);
        state.addTreasure(candlestick);

        state.acquireTreasure(cape, 1);
        state.acquireTreasure(candlestick, 1);
        state.acquireTreasure(altarCross, 3);
        state.addVP(10, 1);
        // get rid of any monks on PILGRIMAGE
        state.monksIn(PILGRIMAGE, 1).forEach(m -> state.moveMonk(m.getComponentID(), PILGRIMAGE, DORMITORY));

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

        assertEquals(SUMMER, state.getSeason());
        assertEquals(SACRIFICE, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());

        assertEquals(2, state.getTreasures(1).size());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.stream().filter(a -> a instanceof PayTreasure).count());
        assertTrue(actions.contains(new PayTreasure(candlestick)));
        fm.next(state, new PayTreasure(candlestick));

        assertEquals(AUTUMN, state.getSeason());

        assertEquals(startMonks[0], state.monksIn(DORMITORY, 0).size());
        assertEquals(startMonks[1], state.monksIn(DORMITORY, 1).size());
        assertEquals(startMonks[2], state.monksIn(DORMITORY, 2).size());
        assertEquals(startMonks[3], state.monksIn(DORMITORY, 3).size());

        assertEquals(startVP[1] - 3, state.getVictoryPoints(1));
        assertEquals(1, state.getTreasures(1).size());
        assertFalse(state.getTreasures(1).contains(candlestick));
        assertEquals(1, state.getTreasures(3).size());
    }

    @Test
    public void retiringLastMonkAllowsYouToUseActionPoints() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        state.createMonk(6, 0); // create a 6-er for player 0
        state.putToken(MEADOW, DiceMonasteryConstants.BONUS_TOKEN.PROMOTION, 0);
        state.putToken(MEADOW, DiceMonasteryConstants.BONUS_TOKEN.PROMOTION, 1);
        // and ensure that the MEADOW only has promotion options

        // then we move monks to ensure that only player 0 and 1 are in the MEADOW, and that
        // player 1 just has their level 6 monk
        for (Monk monk : state.monksIn(DORMITORY, 0)) {
            if (monk.getPiety() == 6)
                state.moveMonk(monk.getComponentID(), DORMITORY, MEADOW);
            else
                state.moveMonk(monk.getComponentID(), DORMITORY, GATEHOUSE);
        }
        for (Monk monk : state.monksIn(DORMITORY, -1)) {
            state.moveMonk(monk.getComponentID(), DORMITORY, GATEHOUSE);
        }

        fm.next(state, new DoNothing()); // this will move on phase and current player
        assertEquals(USE_MONKS, state.getGamePhase());
        assertEquals(MEADOW, state.getCurrentArea());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.nextPlayer());

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // take token
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new PromoteMonk(6, MEADOW), fm.computeAvailableActions(state).get(0));
        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // promote monk

        assertEquals(MEADOW, state.getCurrentArea());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.nextPlayer());
        assertEquals(0, state.monksIn(MEADOW, 0).size());
        assertEquals(6, state.getActionPointsLeft());
    }

    @Test
    public void devotionTokenCanBeUsedImmediately() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        state.putToken(MEADOW, DiceMonasteryConstants.BONUS_TOKEN.DEVOTION, 0);
        state.putToken(MEADOW, DiceMonasteryConstants.BONUS_TOKEN.DEVOTION, 1);
        // and ensure that the MEADOW only has devotion options


        // place monks randomly
        while (state.getGamePhase() == PLACE_MONKS) {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }

        assertEquals(USE_MONKS, state.getGamePhase());
        assertEquals(MEADOW, state.getCurrentArea());
        int currentPlayer = state.getCurrentPlayer();
        assertEquals(1, state.getResource(currentPlayer, PRAYER, STOREROOM));

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // take token
        assertEquals(2, state.getResource(currentPlayer, PRAYER, STOREROOM));

        assertEquals(3, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pray(0)));
        assertTrue(fm.computeAvailableActions(state).contains(new Pray(1)));
        assertTrue(fm.computeAvailableActions(state).contains(new Pray(2)));
    }

    @Test
    public void devotionTokenCanBeUsedImmediately2() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        state.putToken(MEADOW, DiceMonasteryConstants.BONUS_TOKEN.DEVOTION, 0);
        state.putToken(MEADOW, DiceMonasteryConstants.BONUS_TOKEN.DEVOTION, 1);
        // and ensure that the MEADOW only has devotion options

        for (int i = 0; i < 4; i++)
            state.addResource(i, PRAYER, -1);


        // place monks randomly
        while (state.getGamePhase() == PLACE_MONKS) {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }

        assertEquals(USE_MONKS, state.getGamePhase());
        assertEquals(MEADOW, state.getCurrentArea());
        int currentPlayer = state.getCurrentPlayer();
        assertEquals(0, state.getResource(currentPlayer, PRAYER, STOREROOM));

        fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state))); // take token
        assertEquals(1, state.getResource(currentPlayer, PRAYER, STOREROOM));

        assertEquals(2, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pray(0)));
        assertTrue(fm.computeAvailableActions(state).contains(new Pray(1)));
    }

}
