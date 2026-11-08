package games.risk;

import games.risk.components.RiskMission;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMissions.*;
import static org.junit.Assert.*;

/**
 * Copies with secretMission: copy() keeps every mission, eliminatedBy and the mission winner (and they are part of
 * equals/hashCode); copy(p) keeps p's own mission and redeterminises the others' with the unused cards.
 */
public class RiskMissionCopyTest {

    RiskGameState state;

    @Before
    public void setup() {
        // 4 players, secretMission, as dealt by setup (seed 7)
        state = newState(4, 7, missionParams());
    }

    @Test
    public void aFullCopyKeepsTheMissionsAndIsEqual() {
        RiskGameState copy = (RiskGameState) state.copy();
        for (int p = 0; p < 4; p++)
            assertEquals(state.getMission(p), copy.getMission(p));
        assertEquals(state.getUnusedMissions().getComponents(), copy.getUnusedMissions().getComponents());
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void eliminatedByAndTheMissionWinnerAreCopiedAndPartOfEquals() {
        state.eliminatedBy[2] = 0;
        state.missionWinner = 1;
        RiskGameState copy = (RiskGameState) state.copy();
        assertEquals(0, copy.getEliminatedBy(2));
        assertEquals(1, copy.getMissionWinner());
        assertEquals(state, copy);

        copy.eliminatedBy[2] = 3;
        assertEquals("the original is unchanged", 0, state.getEliminatedBy(2));
        assertNotEquals(state, copy);

        copy = (RiskGameState) state.copy();
        copy.missionWinner = -1;
        assertNotEquals(state, copy);
    }

    @Test
    public void aDifferentMissionMakesTheStatesUnequal() {
        RiskGameState copy = (RiskGameState) state.copy();
        RiskMission other = copy.getUnusedMissions().peek();
        giveMission(copy, 0, other); // player 0's card swapped for an unused one
        assertNotEquals(state.getMission(0), copy.getMission(0));
        assertNotEquals("the original keeps its card", other, state.getMission(0));
        assertNotEquals(state, copy);
        assertNotEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void twoPlayersSwappingMissionsMakesTheStatesUnequal() {
        // the same 14 cards in the same places but for players 0 and 1: only the missions differ
        RiskGameState copy = (RiskGameState) state.copy();
        RiskMission m0 = copy.getMission(0), m1 = copy.getMission(1);
        assertNotEquals(m0, m1);
        copy.missions.get(0).clear();
        copy.missions.get(0).add(m1);
        copy.missions.get(1).clear();
        copy.missions.get(1).add(m0);
        assertEquals(state.getUnusedMissions(), copy.getUnusedMissions());
        assertNotEquals(state, copy);
        assertNotEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void aPlayersCopyKeepsTheirOwnMissionAndOneEachForTheOthers() {
        for (int observer = 0; observer < 4; observer++) {
            RiskGameState copy = (RiskGameState) state.copy(observer);
            assertEquals(state.getMission(observer), copy.getMission(observer));
            for (int p = 0; p < 4; p++)
                assertEquals(1, copy.missions.get(p).getSize());
            assertEquals(10, copy.getUnusedMissions().getSize()); // 14 - 4
            assertTrue(sameCards(ALL_MISSIONS, allMissionCards(copy)));
            // the observer's card is never handed to anyone else, nor put among the unused
            assertFalse(copy.getUnusedMissions().contains(state.getMission(observer)));
        }
    }

    @Test
    public void aPlayersCopyRedeterminisesTheOtherPlayersMissions() {
        // over 50 copies for player 0, player 1's mission is not always the true one, and the other players'
        // missions come from the 13 cards player 0 has not seen (never player 0's own)
        Set<RiskMission> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            RiskGameState copy = (RiskGameState) state.copy(0);
            seen.add(copy.getMission(1));
            for (int p = 1; p < 4; p++)
                assertNotEquals(state.getMission(0), copy.getMission(p));
        }
        assertTrue("player 1 always had " + seen, seen.size() > 1);
        // the original is untouched
        RiskGameState fresh = newState(4, 7, missionParams());
        for (int p = 0; p < 4; p++)
            assertEquals(fresh.getMission(p), state.getMission(p));
    }

    @Test
    public void aPlayersCopyKeepsAnEliminatedPlayersEmptyMissionDeck() {
        // once an eliminated player's card has been handed on they hold none, and copies keep the deck sizes
        RiskMission card2 = state.getMission(2);
        state.missions.get(2).clear();
        state.getUnusedMissions().add(card2);
        eliminate(state, 2, 4, 0);
        RiskGameState copy = (RiskGameState) state.copy(0);
        assertNull(copy.getMission(2));
        for (int p : new int[]{0, 1, 3})
            assertNotNull(copy.getMission(p));
        assertEquals(11, copy.getUnusedMissions().getSize()); // 14 - 3
        assertTrue(sameCards(ALL_MISSIONS, allMissionCards(copy)));
    }

    @Test
    public void withoutSecretMissionCopiesHaveNoMissions() {
        RiskGameState plain = newState(3, 7, null);
        RiskGameState copy = (RiskGameState) plain.copy(1);
        for (int p = 0; p < 3; p++)
            assertNull(copy.getMission(p));
        assertEquals(0, copy.getUnusedMissions().getSize());
        assertEquals(-1, copy.getMissionWinner());
        assertEquals(-1, copy.getEliminatedBy(0));
    }
}
