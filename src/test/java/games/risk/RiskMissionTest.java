package games.risk;

import games.risk.components.RiskMission;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldContinents.*;
import static games.risk.WorldMap.*;
import static games.risk.WorldMissions.*;
import static org.junit.Assert.*;

/**
 * The Secret Mission cards (pdf p.13-14): the missions read from the map file, and when each is complete
 * (RiskMission.isComplete). Base: 4 players, player 1 holds every territory with 1 army; the holder is player 0.
 */
public class RiskMissionTest {

    static final String MISSION_MAP = "src/test/resources/risk_missionMap.json";
    static final String NO_BACKUP_MAP = "src/test/resources/risk_noBackupMap.json";

    RiskGameState state;

    @Before
    public void setup() {
        state = newState(4, 7, null);
        fillBoard(state, 1);
    }

    /** The first n territories of the map (index order) to the player, each with the given armies. */
    private void occupyFirst(int player, int n, int armies) {
        give(state, player, armies, ALL.subList(0, n));
    }

    // ---- the map's missions ----

    @Test
    public void theWorldMapHasItsEightMissionsInFileOrder() {
        // worldMap.json "missions": NA+Africa; NA+Australia; Asia+Australia; Asia+Africa; Europe+Australia+3rd;
        // Europe+South America+3rd; 24 territories (armies 1 by default); 18 territories with 2 armies each
        assertEquals(List.of(
                RiskMission.conquer(List.of(NORTH_AMERICA, AFRICA), false),
                RiskMission.conquer(List.of(NORTH_AMERICA, WorldContinents.AUSTRALIA), false),
                RiskMission.conquer(List.of(ASIA, WorldContinents.AUSTRALIA), false),
                RiskMission.conquer(List.of(ASIA, AFRICA), false),
                RiskMission.conquer(List.of(EUROPE, WorldContinents.AUSTRALIA), true),
                RiskMission.conquer(List.of(EUROPE, SOUTH_AMERICA), true),
                RiskMission.occupy(24, 1),
                RiskMission.occupy(18, 2)), MAP.missions());
    }

    @Test
    public void worldMapBackupMissionIs24Territories() {
        assertEquals(24, MAP.backupTerritories());
    }

    @Test
    public void theBackupMissionUsesTheMapsBackupTerritories() {
        // the mission test map has 6 territories, no missions of its own, and backupTerritories 3
        RiskParameters params = new RiskParameters();
        params.setParameterValue("mapFile", MISSION_MAP);
        params.setParameterValue("secretMission", true);
        RiskGameState small = new RiskGameState(params, 3);
        new RiskForwardModel().setup(small);
        List<RiskTerritory> all = small.getMap().territories();
        give(small, 1, 1, all);
        give(small, 0, 1, all.subList(0, 2));
        assertFalse("2 < 3", RiskMission.destroy(0).isComplete(small, 0));
        give(small, 0, 1, all.subList(2, 3));
        assertTrue("3 >= 3", RiskMission.destroy(0).isComplete(small, 0));
    }

    @Test
    public void aMapWithMissionsButNoBackupTerritoriesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RiskMap(NO_BACKUP_MAP));
    }

    @Test
    public void secretMissionOnAMapWithoutBackupTerritoriesIsRejected() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("mapFile", RiskMapTest.TEST_MAP);
        params.setParameterValue("secretMission", true);
        RiskGameState small = new RiskGameState(params, 3);
        assertThrows(IllegalArgumentException.class, () -> new RiskForwardModel().setup(small));
    }

    @Test
    public void aMapWithoutAMissionsListHasNone() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("mapFile", RiskMapTest.TEST_MAP);
        assertEquals(List.of(), params.getMap().missions());
    }

    // ---- conquer ----

    @Test
    public void conquerTwoContinentsIsCompleteWhenBothAreHeld() {
        // North America (9) + Africa (6) = 15 territories
        give(state, 0, 1, continent(NORTH_AMERICA));
        give(state, 0, 1, continent(AFRICA));
        assertTrue(NA_AFRICA.isComplete(state, 0));
        assertFalse("player 1 holds neither", NA_AFRICA.isComplete(state, 1));
    }

    @Test
    public void conquerTwoContinentsIsNotCompleteWithOneTerritoryMissing() {
        give(state, 0, 1, continent(NORTH_AMERICA));
        give(state, 0, 1, continent(AFRICA));
        give(state, 1, 1, MADAGASCAR); // Africa now 5 of 6
        assertFalse(NA_AFRICA.isComplete(state, 0));
        // but North America and Australia is a different card: Australia not held
        assertFalse(NA_AUSTRALIA.isComplete(state, 0));
    }

    @Test
    public void conquerWithAThirdContinentNeedsAnotherWholeContinent() {
        give(state, 0, 1, continent(EUROPE));
        give(state, 0, 1, continent(WorldContinents.AUSTRALIA));
        // Europe + Australia only: no third continent
        assertFalse(EUROPE_AUSTRALIA_3RD.isComplete(state, 0));
        // + all of Asia but Siam: still no third
        give(state, 0, 1, continent(ASIA));
        give(state, 1, 1, SIAM);
        assertFalse(EUROPE_AUSTRALIA_3RD.isComplete(state, 0));
        // + South America (any other continent will do)
        give(state, 0, 1, continent(SOUTH_AMERICA));
        assertTrue(EUROPE_AUSTRALIA_3RD.isComplete(state, 0));
    }

    @Test
    public void conquerWithAThirdContinentIsCompleteWithAnyThird() {
        give(state, 0, 1, continent(EUROPE));
        give(state, 0, 1, continent(SOUTH_AMERICA));
        give(state, 0, 1, continent(WorldContinents.AUSTRALIA)); // Australia as the third
        assertTrue(EUROPE_SOUTH_AMERICA_3RD.isComplete(state, 0));
        give(state, 1, 1, ICELAND); // Europe no longer whole
        assertFalse(EUROPE_SOUTH_AMERICA_3RD.isComplete(state, 0));
    }

    // ---- occupy ----

    @Test
    public void occupy24TerritoriesNeeds24() {
        occupyFirst(0, 23, 1);
        assertFalse("23 < 24", OCCUPY_24.isComplete(state, 0));
        occupyFirst(0, 24, 1);
        assertTrue("24 >= 24, 1 army each is enough", OCCUPY_24.isComplete(state, 0));
        occupyFirst(0, 30, 1);
        assertTrue("30 >= 24", OCCUPY_24.isComplete(state, 0));
    }

    @Test
    public void occupy18WithTwoArmiesNeeds18TerritoriesOfAtLeastTwo() {
        occupyFirst(0, 17, 2);
        assertFalse("17 with 2 < 18", OCCUPY_18_TWO_ARMIES.isComplete(state, 0));
        occupyFirst(0, 18, 2);
        assertTrue("18 with 2", OCCUPY_18_TWO_ARMIES.isComplete(state, 0));
        occupyFirst(0, 18, 3);
        assertTrue("18 with 3 >= 2", OCCUPY_18_TWO_ARMIES.isComplete(state, 0));
    }

    @Test
    public void occupy18WithTwoArmiesFailsWithOneOfThe18OnOneArmy() {
        occupyFirst(0, 18, 2);
        give(state, 0, 1, ALL.get(17)); // 17 territories with 2, one with 1
        assertFalse(OCCUPY_18_TWO_ARMIES.isComplete(state, 0));
        // more territories with 1 army do not help: 17 with 2 + 13 with 1 = 30 territories, still only 17 with 2
        give(state, 0, 1, ALL.subList(18, 30));
        assertFalse(OCCUPY_18_TWO_ARMIES.isComplete(state, 0));
        // a 2nd army on any of them makes 18
        give(state, 0, 2, ALL.get(25));
        assertTrue(OCCUPY_18_TWO_ARMIES.isComplete(state, 0));
    }

    // ---- destroy ----

    @Test
    public void destroyIsCompleteWhenTheHolderEliminatedTheTarget() {
        give(state, 2, 1, SIAM);
        assertFalse("player 2 still in", destroy(2).isComplete(state, 0));
        give(state, 1, 1, SIAM);
        eliminate(state, 2, 4, 0);
        assertTrue("player 0 eliminated player 2", destroy(2).isComplete(state, 0));
    }

    @Test
    public void destroyIsNotCompleteWhileTheTargetIsInEvenWith24Territories() {
        give(state, 2, 1, SIAM);
        occupyFirst(0, 30, 1);
        // the target is in the game, so the backup does not apply
        assertFalse(destroy(2).isComplete(state, 0));
    }

    @Test
    public void destroyYourselfIsOccupy24Instead() {
        occupyFirst(0, 23, 1);
        assertFalse("23 < 24", destroy(0).isComplete(state, 0));
        occupyFirst(0, 24, 1);
        assertTrue("24", destroy(0).isComplete(state, 0));
    }

    @Test
    public void destroyAnUnplayedColourIsOccupy24Instead() {
        // 4 players: colours 4 and 5 are not in the game
        occupyFirst(0, 23, 1);
        assertFalse(destroy(4).isComplete(state, 0));
        assertFalse(destroy(5).isComplete(state, 0));
        occupyFirst(0, 24, 1);
        assertTrue(destroy(4).isComplete(state, 0));
        assertTrue(destroy(5).isComplete(state, 0));
        // colour 3 is in the game (player 3, holding nothing here but not eliminated): not the backup
        give(state, 3, 1, ARGENTINA);
        assertFalse(destroy(3).isComplete(state, 0));
    }

    @Test
    public void destroyATargetEliminatedBySomeoneElseIsOccupy24Instead() {
        eliminate(state, 2, 4, 3); // player 3 eliminated player 2
        occupyFirst(0, 23, 1);
        assertFalse("23 < 24", destroy(2).isComplete(state, 0));
        occupyFirst(0, 24, 1);
        assertTrue("24", destroy(2).isComplete(state, 0));
        // for player 3, who eliminated player 2, the card is complete with no territories counted
        assertTrue(destroy(2).isComplete(state, 3));
    }
}
