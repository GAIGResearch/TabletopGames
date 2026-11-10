package games.risk;

import games.risk.components.RiskMission;
import org.junit.Test;

import java.util.*;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static games.risk.WorldMissions.*;
import static org.junit.Assert.*;

/**
 * Setup with and without secretMission (pdf p.13): the mission cards dealt, and the territories dealt at random.
 */
public class RiskMissionDealTest {

    @Test
    public void eachPlayerIsDealtOneMissionAndTheRestAreUnused() {
        for (int n = 3; n <= 6; n++) {
            RiskGameState state = newState(n, 7, missionParams());
            for (int p = 0; p < n; p++) {
                assertEquals("player " + p + " of " + n, 1, state.missions.get(p).getSize());
                assertNotNull(state.getMission(p));
            }
            // 14 cards = 8 from the map + 6 destroy cards: n dealt, 14 - n unused
            assertEquals(14 - n, state.getUnusedMissions().getSize());
            assertTrue(n + " players: " + allMissionCards(state), sameCards(ALL_MISSIONS, allMissionCards(state)));
        }
    }

    @Test
    public void theDestroyCardsAreOnePerColourWhateverTheNumberOfPlayers() {
        // 3 players: destroy 3, 4 and 5 are still among the 14 (dealt or unused)
        RiskGameState state = newState(3, 11, missionParams());
        List<RiskMission> all = allMissionCards(state);
        for (int k = 0; k < 6; k++)
            assertTrue("destroy " + k, all.contains(destroy(k)));
    }

    @Test
    public void theMissionsDealtDependOnTheRandomSeed() {
        // two setups that differ only in the seed; over 20 seeds player 0's card is not always the same
        Set<RiskMission> seen = new HashSet<>();
        Set<List<RiskMission>> deals = new HashSet<>();
        for (long seed = 1; seed <= 20; seed++) {
            RiskGameState state = newState(4, seed, missionParams());
            seen.add(state.getMission(0));
            List<RiskMission> deal = new ArrayList<>();
            for (int p = 0; p < 4; p++) deal.add(state.getMission(p));
            deals.add(deal);
        }
        seen.remove(null);
        assertTrue("player 0 always had " + seen, seen.size() > 1);
        assertTrue(deals.size() > 1);
        // the same seed deals the same cards
        RiskGameState a = newState(4, 3, missionParams());
        RiskGameState b = newState(4, 3, missionParams());
        for (int p = 0; p < 4; p++)
            assertEquals(a.getMission(p), b.getMission(p));
    }

    @Test
    public void secretMissionAlwaysDealsTheTerritoriesAtRandom() {
        RiskParameters params = missionParams();
        params.setParameterValue("randomTerritoryDeal", false);
        RiskGameState state = newState(4, 7, params);
        int first = state.getFirstPlayer();
        // as randomTerritoryDeal: every territory owned with 1 army; 4 players: 42 = 4 x 10 + 2, so the first two
        // from the first player hold 11 and have 30 - 11 = 19 armies left, the others 10 and 30 - 10 = 20
        for (RiskTerritory t : ALL) {
            assertTrue(t + " unowned", state.getOwner(t) >= 0);
            assertEquals(1, state.getArmies(t));
        }
        for (int i = 0; i < 4; i++) {
            int p = (first + i) % 4;
            int held = i < 2 ? 11 : 10;
            assertEquals("player " + p, held, state.getNTerritories(p));
            assertEquals("player " + p, 30 - held, state.getArmiesToPlace(p));
        }
        assertEquals(RiskGamePhase.PLACE_INITIAL, state.getGamePhase());
        for (int p = 0; p < 4; p++)
            assertNotNull(state.getMission(p));
    }

    @Test
    public void withoutSecretMissionNobodyHasAMission() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("randomTerritoryDeal", false);
        RiskGameState state = newState(4, 7, params);
        for (int p = 0; p < 4; p++)
            assertNull(state.getMission(p));
        assertEquals(RiskGamePhase.CLAIM, state.getGamePhase()); // the territories are claimed as usual
        assertEquals(-1, state.getMissionWinner());
        for (int p = 0; p < 4; p++)
            assertEquals(-1, state.getEliminatedBy(p));
    }

    @Test
    public void aMapWithoutMissionsDealsFromTheSixDestroyCards() {
        RiskParameters params = missionParams();
        params.setParameterValue("mapFile", RiskMissionTest.MISSION_MAP);
        RiskGameState state = newState(3, 7, params);
        // no map missions: 6 destroy cards, 3 dealt and 3 unused
        for (int p = 0; p < 3; p++)
            assertEquals(RiskMission.Kind.DESTROY, state.getMission(p).kind);
        assertEquals(3, state.getUnusedMissions().getSize());
        assertTrue(sameCards(DESTROY_MISSIONS, allMissionCards(state)));
    }

    @Test
    public void theMissionDecksAreSeenOnlyByTheirOwners() {
        RiskGameState state = newState(3, 7, missionParams());
        for (int p = 0; p < 3; p++) {
            assertEquals(core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER, state.missions.get(p).getVisibilityMode());
            assertEquals(p, state.missions.get(p).getOwnerId());
        }
        assertEquals(core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL, state.getUnusedMissions().getVisibilityMode());
    }
}
