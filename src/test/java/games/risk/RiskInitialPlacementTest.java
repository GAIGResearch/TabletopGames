package games.risk;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import games.risk.actions.ClaimTerritory;
import games.risk.actions.PlaceArmy;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.risk.WorldMap.*;
import static games.risk.RiskTestUtils.*;
import static org.junit.Assert.*;

public class RiskInitialPlacementTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;
    int f; // the first player

    @Before
    public void setup() {
        state = newState(3, 7, claimParams());
        f = state.getFirstPlayer();
    }

    /** The territories are claimed one at a time, not dealt. */
    private static RiskParameters claimParams() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("randomTerritoryDeal", false);
        return params;
    }

    private static Set<AbstractAction> claims(Collection<RiskTerritory> territories) {
        Set<AbstractAction> set = new HashSet<>();
        for (RiskTerritory t : territories) set.add(new ClaimTerritory(t));
        return set;
    }

    private static Set<AbstractAction> placements(Collection<RiskTerritory> territories, int n) {
        Set<AbstractAction> set = new HashSet<>();
        for (RiskTerritory t : territories) set.add(new PlaceArmy(t, n));
        return set;
    }

    @Test
    public void claimOffersExactlyTheUnclaimedTerritories() {
        assertEquals(claims(WorldMap.ALL), actionSet(fm, state));

        fm.next(state, new ClaimTerritory(ALASKA));
        Set<RiskTerritory> unclaimed = new HashSet<>(WorldMap.ALL);
        unclaimed.remove(ALASKA);
        assertEquals(claims(unclaimed), actionSet(fm, state));
    }

    @Test
    public void claimingPutsOneArmyThereAndPassesTheTurn() {
        fm.next(state, new ClaimTerritory(ALASKA));
        assertEquals(f, state.getOwner(ALASKA));
        assertEquals(1, state.getArmies(ALASKA));
        assertEquals(34, state.getArmiesToPlace(f)); // 35 - 1
        assertEquals(35, state.getArmiesToPlace((f + 1) % 3));
        assertEquals((f + 1) % 3, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.CLAIM, state.getGamePhase());
    }

    private RiskGameState randomDealState() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("randomTerritoryDeal", true);
        RiskGameState s = newState(3, 7, params);
        f = s.getFirstPlayer();
        return s;
    }

    @Test
    public void placeInitialOffersOnlyTheTerritoriesHeld() {
        state = randomDealState();
        List<RiskTerritory> held = state.getTerritories(f);
        assertEquals(14, held.size());
        // 35 - 14 dealt = 21 left: initialPlacementBatch (5) at a time
        assertEquals(placements(held, 5), actionSet(fm, state));
    }

    @Test
    public void placingAnInitialBatchAddsItAndPassesTheTurn() {
        state = randomDealState();
        RiskTerritory t = state.getTerritories(f).get(0);
        fm.next(state, new PlaceArmy(t, 5));
        assertEquals(6, state.getArmies(t));
        assertEquals(16, state.getArmiesToPlace(f)); // 35 - 14 dealt - 5
        assertEquals((f + 1) % 3, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.PLACE_INITIAL, state.getGamePhase());
    }

    @Test
    public void armiesArePlacedInBatchesWhileMoreThanABatchIsLeftThenOneAtATime() {
        state = randomDealState();
        List<RiskTerritory> held = state.getTerritories(f);
        state.setArmiesToPlace(f, 6);
        assertEquals(placements(held, 5), actionSet(fm, state));
        state.setArmiesToPlace(f, 5);
        assertEquals(placements(held, 1), actionSet(fm, state));
        state.setArmiesToPlace(f, 1);
        assertEquals(placements(held, 1), actionSet(fm, state));
    }

    @Test
    public void withABatchOfOneArmiesArePlacedOneAtATime() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("placementBatch", 1);
        state = newState(3, 7, params);
        f = state.getFirstPlayer();
        assertEquals(placements(state.getTerritories(f), 1), actionSet(fm, state));
    }

    @Test
    public void aBatchIsCutToTheRoomLeftUnderMaxArmiesPerTerritory() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("maxArmiesPerTerritory", 8);
        state = newState(3, 7, params);
        f = state.getFirstPlayer();
        List<RiskTerritory> held = state.getTerritories(f);
        RiskTerritory crowded = held.get(0), full = held.get(1);
        state.addArmies(crowded, 5); // 6: room for 2
        state.addArmies(full, 7);    // 8: no room
        Set<AbstractAction> expected = placements(held.subList(2, held.size()), 5);
        expected.add(new PlaceArmy(crowded, 2));
        assertEquals(expected, actionSet(fm, state));
    }

    @Test
    public void playersWithNoArmiesLeftToPlaceAreSkipped() {
        state = randomDealState();
        state.setArmiesToPlace((f + 1) % 3, 0);
        fm.next(state, new PlaceArmy(state.getTerritories(f).get(0), 5));
        assertEquals((f + 2) % 3, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.PLACE_INITIAL, state.getGamePhase());
    }

    @Test
    public void lastInitialArmyStartsPlayWithTheFirstPlayerReinforcing() {
        // f: Australia + 11 of Asia (not Siam) = 15 territories; f+1 the rest but Argentina; f+2 Argentina
        int second = (f + 1) % 3, third = (f + 2) % 3;
        fillBoard(state, second);
        give(state, f, 1, AUSTRALIA);
        List<RiskTerritory> asia = new ArrayList<>(continent(WorldContinents.ASIA));
        asia.remove(SIAM);
        give(state, f, 1, asia);
        give(state, third, 1, ARGENTINA);
        state.setGamePhase(RiskGamePhase.PLACE_INITIAL);
        for (int p = 0; p < 3; p++) state.setArmiesToPlace(p, 0);
        state.setArmiesToPlace(second, 1);
        state.setTurnOwner(second);
        // the next player round the table (third) has none left; play starts with the first player, not "the next"

        fm.next(state, new PlaceArmy(ALASKA));
        assertEquals(2, state.getArmies(ALASKA));
        assertEquals(0, state.getArmiesToPlace(second));
        assertEquals(f, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(7, state.getArmiesToPlace(f)); // 15 / 3 = 5, + Australia 2
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void claimingContinuesTheRotationIntoPlaceInitial() {
        // 4 players: 42 claims in territory order; claim i is made by (first + i) % 4
        Game game = newGame(4, 3, claimParams());
        RiskGameState s = (RiskGameState) game.getGameState();
        AbstractForwardModel gfm = game.getForwardModel();
        int first = s.getFirstPlayer();
        List<RiskTerritory> all = WorldMap.ALL;
        for (int i = 0; i < 42; i++) {
            assertEquals("claim " + i, (first + i) % 4, s.getCurrentPlayer());
            assertEquals(RiskGamePhase.CLAIM, s.getGamePhase());
            List<AbstractAction> actions = gfm.computeAvailableActions(s);
            assertEquals(42 - i, actions.size());
            assertTrue(actions.contains(new ClaimTerritory(all.get(i))));
            gfm.next(s, new ClaimTerritory(all.get(i)));
        }
        assertEquals(RiskGamePhase.PLACE_INITIAL, s.getGamePhase());
        // the 42nd claim (i = 41) was by first + 1, so first + 2 places next
        assertEquals((first + 2) % 4, s.getCurrentPlayer());
        int[] held = {11, 11, 10, 10}; // player first + k made the claims i with i mod 4 = k
        for (int k = 0; k < 4; k++) {
            int p = (first + k) % 4;
            assertEquals(held[k], s.getNTerritories(p));
            assertEquals(30 - held[k], s.getArmiesToPlace(p));
        }
        assertEquals(0, s.getRoundCounter());
    }

    @Test
    public void wholeInitialPlacementLeadsToTheFirstPlayersFirstTurn() {
        Game game = newGame(3, 9, claimParams());
        RiskGameState s = (RiskGameState) game.getGameState();
        AbstractForwardModel gfm = game.getForwardModel();
        int first = s.getFirstPlayer();
        List<RiskTerritory> all = WorldMap.ALL;
        for (int i = 0; i < 42; i++)
            gfm.next(s, new ClaimTerritory(all.get(i)));
        // each holds 14 and has 35 - 14 = 21 to place; 42 claims is a whole number of rotations, so first goes next.
        // 21 -> 16 -> 11 -> 6 -> 1 in batches of 5, then the last one alone: 5 placements each
        int[] batches = {5, 5, 5, 5, 1};
        for (int j = 0; j < 15; j++) {
            int p = (first + j) % 3;
            assertEquals("placement " + j, p, s.getCurrentPlayer());
            assertEquals(RiskGamePhase.PLACE_INITIAL, s.getGamePhase());
            RiskTerritory t = s.getTerritories(p).get(0);
            PlaceArmy place = new PlaceArmy(t, batches[j / 3]);
            assertTrue(gfm.computeAvailableActions(s).contains(place));
            gfm.next(s, place);
        }
        assertEquals(RiskGamePhase.REINFORCE, s.getGamePhase());
        assertEquals(first, s.getCurrentPlayer());
        assertEquals(0, s.getRoundCounter());
        for (int p = 0; p < 3; p++)
            assertEquals(35, s.getTotalArmies(p));
        // first holds RiskTerritory indices 0, 3, ..., 39: 14 territories, no whole continent -> 14 / 3 = 4
        assertEquals(4, s.getArmiesToPlace(first));
        assertEquals(22, s.getArmies(ALASKA)); // index 0: 1 claimed + 21 placed
    }
}
