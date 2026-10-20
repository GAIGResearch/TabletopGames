package games.ventlife;

import core.Game;
import core.actions.AbstractAction;
import games.ventlife.actions.*;
import games.ventlife.components.HexCell;
import games.ventlife.components.Terrain;
import games.ventlife.features.VentlifeActionFeatures;
import games.ventlife.features.VentlifeStateFeatures;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

public class VentlifeFeaturesTest {

    VentlifeStateFeatures features = new VentlifeStateFeatures();
    VentlifeActionFeatures actionFeatures = new VentlifeActionFeatures();
    VentlifeForwardModel fm = new VentlifeForwardModel();

    private double feature(VentlifeGameState state, int player, String name) {
        int i = Arrays.asList(features.names()).indexOf(name);
        assertTrue("no feature " + name, i >= 0);
        return features.doubleVector(state, player)[i];
    }

    private static void putCell(VentlifeGameState state, int q, int r, Terrain terrain, int level, int tileId) {
        state.setCell(hex(q, r), new HexCell(terrain, level, tileId));
    }

    @Test
    public void fieldFeaturesOfTheStandardField() {
        VentlifeGameState state = newState(2, 42);
        useSpecies(state);
        buildStandardField(state, fm, 6);
        // (0,0) S3, (-1,1) M3, (0,1) D3, (1,0) B2, (1,1) D1, (2,0) M1, (-1,0) S2, (-2,0) B1, (-2,1) D2
        assertEquals(9, feature(state, 0, "fieldSize"), 0);
        assertEquals(9, feature(state, 0, "emptyHexes"), 0);
        assertEquals(3, feature(state, 0, "maxLevel"), 0);
        assertEquals(6, feature(state, 0, "raisedHexes"), 0);
        assertEquals(6.0 / (6 + unplacedCount(state)), feature(state, 0, "progress"), 1e-9);
    }

    @Test
    public void creaturesAPlateauTileCouldCoverAreExposedButNotTubeWormsUnderItsSmoker() {
        VentlifeGameState state = newState(2, 42);
        useSpecies(state, WORM, SPONGE);
        // a plateau tile fits at ((0,0),0), covering (0,0), (0,1) and (1,0), which are on two tiles
        putCell(state, 0, 0, SMOKER, 1, 0);
        putCell(state, 1, 0, BASALT, 1, 1);
        putCell(state, 0, 1, DIFFUSE, 1, 1);
        putCell(state, 2, 0, MAT, 1, 1);
        putCreature(state, hex(0, 0), WORM, 0, 2);
        putCreature(state, hex(1, 0), SPONGE, 0, 1);
        putCreature(state, hex(2, 0), SPONGE, 0, 1);
        putCreature(state, hex(0, 1), SPONGE, 1, 1);
        assertEquals(1, feature(state, 0, "exposedCreatures"), 0);
        assertEquals(1, feature(state, 1, "exposedCreatures"), 0);
        assertEquals(4, feature(state, 0, "creaturesOnField"), 0);
        assertEquals(1, feature(state, 0, "oppCreaturesOnField"), 0);
        assertEquals(4, feature(state, 1, "oppCreaturesOnField"), 0);
        assertEquals(2, feature(state, 0, "speciesPresent"), 0);
        assertEquals(0, feature(state, 1, "emptyHexes"), 0);
    }

    @Test
    public void noCreatureIsExposedWhenTheThreeHexesAreOneTile() {
        VentlifeGameState state = newState(2, 42);
        useSpecies(state, SPONGE);
        putCell(state, 0, 0, SMOKER, 1, 0);
        putCell(state, 1, 0, BASALT, 1, 0);
        putCell(state, 0, 1, DIFFUSE, 1, 0);
        putCreature(state, hex(1, 0), SPONGE, 0, 1);
        assertEquals(0, feature(state, 0, "exposedCreatures"), 0);
    }

    @Test
    public void heightScoringLevelCrabPlateausAndShoals() {
        VentlifeGameState state = newState(2, 42);
        useSpecies(state, WORM, CRAB, FISH);
        putCell(state, 0, 0, SMOKER, 1, 0);
        putCell(state, 5, 5, BASALT, 3, 1);
        putCell(state, 6, 5, BASALT, 3, 2);
        putCreature(state, hex(0, 0), WORM, 0, 2);
        putCreature(state, hex(5, 5), CRAB, 0, 1);
        putCreature(state, hex(6, 5), CRAB, 1, 1);
        for (int q : List.of(10, 11, 12, 14)) {
            putCell(state, q, 0, MAT, 1, 3);
            putCreature(state, hex(q, 0), FISH, 0, 1);
        }
        // Worms 2 at level 1 and a Crab at level 3; Fish do not count
        assertEquals(5.0 / 3, feature(state, 0, "meanHeightScoringLevel"), 1e-9);
        assertEquals(3, feature(state, 1, "meanHeightScoringLevel"), 0);
        // tied for the most Crabs on the plateau: both lead it
        assertEquals(1, feature(state, 0, "crabPlateausLed"), 0);
        assertEquals(1, feature(state, 1, "crabPlateausLed"), 0);
        assertEquals(3, feature(state, 0, "largestShoal"), 0);
        assertEquals(0, feature(state, 1, "largestShoal"), 0);
    }

    @Test
    public void speciesExhaustedCountsSpeciesInPlayWithNoTokensLeft() {
        VentlifeGameState state = newState(2, 42);
        useSpecies(state, SPONGE, SHRIMP);
        putCell(state, 0, 0, MAT, 1, 0);
        state.changeSupply(0, SPONGE, -state.getSupply(0, SPONGE));
        assertEquals(1, feature(state, 0, "speciesExhausted"), 0);
        assertEquals(0, feature(state, 1, "speciesExhausted"), 0);
    }

    private Object action(VentlifeGameState state, AbstractAction action, int player, String name) {
        int i = Arrays.asList(actionFeatures.names()).indexOf(name);
        assertTrue("no action feature " + name, i >= 0);
        return actionFeatures.featureVector(action, state, player)[i];
    }

    /** The exposure field: a plateau tile fits at ((0,0),0) over (0,0) S1, (0,1) D1 and (1,0) B1, on two tiles. */
    private VentlifeGameState plateauField() {
        VentlifeGameState state = newState(2, 42);
        useSpecies(state, WORM, SPONGE, SHRIMP);
        putCell(state, 0, 0, SMOKER, 1, 0);
        putCell(state, 1, 0, BASALT, 1, 1);
        putCell(state, 0, 1, DIFFUSE, 1, 1);
        putCell(state, 2, 0, MAT, 1, 1);
        return state;
    }

    @Test
    public void tileFeaturesGiveItsKindLevelAndTheCreaturesItCovers() {
        VentlifeGameState state = plateauField();
        putCreature(state, hex(0, 0), WORM, 0, 2);
        putCreature(state, hex(1, 0), SPONGE, 0, 1);
        putCreature(state, hex(0, 1), SPONGE, 1, 1);
        PlaceTile plateau = new PlaceTile(tile(BASALT, MAT), hex(0, 0), 0);
        assertEquals(VentlifeActionFeatures.ActionType.TILE_PLATEAU, action(state, plateau, 0, "actionType"));
        assertEquals(2, action(state, plateau, 0, "targetLevel"));
        // the Worms climb, so only the Sponges are covered
        assertEquals(1, action(state, plateau, 0, "ownCovered"));
        assertEquals(1, action(state, plateau, 0, "oppCovered"));
        assertEquals(1, action(state, plateau, 1, "ownCovered"));
        PlaceTile seafloor = new PlaceTile(tile(BASALT, MAT), hex(5, 5), 0);
        assertEquals(VentlifeActionFeatures.ActionType.TILE_SEAFLOOR, action(state, seafloor, 0, "actionType"));
        assertEquals(1, action(state, seafloor, 0, "targetLevel"));
        assertEquals(0, action(state, seafloor, 0, "ownCovered"));
        assertNull(action(state, seafloor, 0, "targetTerrain"));
    }

    @Test
    public void placementFeaturesDescribeTheTargetHexAndItsNeighbours() {
        VentlifeGameState state = plateauField();
        putCreature(state, hex(1, 0), SPONGE, 0, 1);
        putCreature(state, hex(0, 0), SPONGE, 1, 1);
        // (2,0) M1: next to (1,0) with player 0's Sponge; its other neighbours are uncovered
        PlaceCreature atMat = sponge(hex(2, 0));
        assertEquals(VentlifeActionFeatures.ActionType.PLACE, action(state, atMat, 0, "actionType"));
        assertEquals(1, action(state, atMat, 0, "targetLevel"));
        assertEquals(true, action(state, atMat, 0, "targetIsEdge"));
        assertEquals(MAT, action(state, atMat, 0, "targetTerrain"));
        assertEquals(1, action(state, atMat, 0, "ownAdjacent"));
        assertEquals(0, action(state, atMat, 0, "oppAdjacent"));
        assertEquals(0, action(state, atMat, 0, "emptyAdjacent"));
        assertEquals(false, action(state, atMat, 0, "exposedAfter"));
        // (0,1) D1: next to (0,0) (player 1), (1,0) (player 0); the plateau tile would cover it
        PlaceCreature atVents = shrimp(hex(0, 1));
        assertEquals(1, action(state, atVents, 0, "ownAdjacent"));
        assertEquals(1, action(state, atVents, 0, "oppAdjacent"));
        assertEquals(true, action(state, atVents, 0, "exposedAfter"));
    }

    @Test
    public void tubeWormsOnThePlateauSmokerAreNotExposedButOtherSpeciesAre() {
        VentlifeGameState state = plateauField();
        assertEquals(false, action(state, worm(hex(0, 0), 1), 0, "exposedAfter"));
        assertEquals(true, action(state, sponge(hex(0, 0)), 0, "exposedAfter"));
    }

    @Test
    public void displacementTargetsTheHexMovedTo() {
        VentlifeGameState state = plateauField();
        Displace d = displace(SHRIMP, 0, hex(0, 1), hex(2, 0));
        assertEquals(VentlifeActionFeatures.ActionType.DISPLACE, action(state, d, 0, "actionType"));
        assertEquals(MAT, action(state, d, 0, "targetTerrain"));
        assertEquals(VentlifeActionFeatures.ActionType.DRAFT, action(state, new DraftSpecies(SHRIMP), 0, "actionType"));
        assertEquals(VentlifeActionFeatures.ActionType.STOP, action(state, new StopPlacing(), 0, "actionType"));
    }

    @Test
    public void everyLegalActionHasFeaturesThroughWholeGames() {
        for (int nPlayers = 2; nPlayers <= 4; nPlayers++) {
            VentlifeGameState state = newState(nPlayers, 7 + nPlayers, draftParams(4));
            Random rnd = new Random(nPlayers);
            while (state.isNotTerminal()) {
                List<AbstractAction> legal = fm.computeAvailableActions(state);
                for (AbstractAction a : legal) {
                    Object[] v = actionFeatures.featureVector(a, state, state.getCurrentPlayer());
                    assertEquals(actionFeatures.names().length, v.length);
                    assertNotNull(v[0]);
                }
                fm.next(state, legal.get(rnd.nextInt(legal.size())));
            }
        }
    }

    @Test
    public void scoreLeadAndBestSpeciesMatchTheState() {
        for (int nPlayers = 2; nPlayers <= 4; nPlayers++) {
            Game game = newGame(nPlayers, 17 + nPlayers);
            game.run();
            VentlifeGameState state = (VentlifeGameState) game.getGameState();
            for (int p = 0; p < nPlayers; p++) {
                double[] v = features.doubleVector(state, p);
                assertEquals(features.names().length, v.length);
                for (double d : v)
                    assertFalse(Double.isNaN(d) || Double.isInfinite(d));
                assertEquals(state.getGameScore(p), feature(state, p, "score"), 0);
                assertEquals(state.getLead(p), feature(state, p, "lead"), 0);
                assertEquals(state.getTiebreak(p, 1), feature(state, p, "bestSpeciesScore"), 0);
                assertEquals(1, feature(state, p, "progress"), 0);
            }
        }
    }
}
