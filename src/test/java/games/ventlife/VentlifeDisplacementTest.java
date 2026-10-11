package games.ventlife;

import core.actions.AbstractAction;
import games.ventlife.actions.Displace;
import games.ventlife.components.Creature;
import games.ventlife.components.Species;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Displacement of covered Shrimp, Fish and Crabs: the destinations of each species, the owner's choice among several,
 * the order in which the owners decide, and the placing player's creature step after the last one.
 * Two layouts (2 players):
 * - seafloor pair: standard tiles 0 and 1, level 1: (0,0) S, (-1,1) B, (0,1) D [tile 0]; (1,0) S, (1,1) D, (2,0) M
 *   [tile 1]; then player 0 places M/B at ((0,0),0): (0,0) S2, left (0,1) M2, right (1,0) B2.
 * - standard field after tiles 0-4: level 2: (0,0) S, (0,1) M, (1,0) B, (-1,0) S, (-2,1) D, (-1,1) B; level 1:
 *   (1,1) D, (2,0) M, (-2,0) B; then player 1 places tile 5 M/D at ((0,0),1): (0,0) S3, left (-1,1) M3, right (0,1) D3.
 */
public class VentlifeDisplacementTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 42);
        fm = new VentlifeForwardModel();
    }

    /** Tiles 0 and 1 with no species in play, then only the given species; player 0 to place a tile. */
    private void seafloorPair(Species... species) {
        useSpecies(state);
        buildStandardField(state, fm, 2);
        useSpecies(state, species);
        assertEquals(0, state.getCurrentPlayer());
    }

    /** Player 0 places the plateau M/B at ((0,0),0) over the seafloor pair. */
    private void coverSeafloorPair() {
        giveTile(state, 0, MAT, BASALT);
        fm.next(state, placeTile(state, hex(0, 0), 0));
    }

    /** Tiles 0-4 of the standard field with no species in play, then only the given species; player 1 to move. */
    private void standardFieldBeforeTile5(Species... species) {
        useSpecies(state);
        buildStandardField(state, fm, 5);
        useSpecies(state, species);
        assertEquals(1, state.getCurrentPlayer());
    }

    /** Player 1 places tile 5, M/D at ((0,0),1). */
    private void placeTile5() {
        giveTile(state, 1, MAT, DIFFUSE);
        fm.next(state, placeTile(state, hex(0, 0), 1));
    }

    private void assertCreatureStepOf(int player) {
        assertEquals("creature step expected", VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(player, state.getCurrentPlayer());
    }

    @Test
    public void ownerOfACoveredShrimpChoosesAmongNonBasaltHexesNextToItOffTheNewTile() {
        seafloorPair(SHRIMP);
        putCreature(state, hex(1, 0), SHRIMP, 1, 1);
        coverSeafloorPair();
        // (1,0), tile 1's Smoker, is under the new right hex; its neighbours: (2,0) M1, (1,1) D1, (0,0) S2 and
        // (0,1) M2 (the new tile, which no covered creature may move onto), (1,-1) and (2,-1) uncovered.
        // Player 1 decides on player 0's turn
        assertEquals(1, state.getCurrentPlayer());
        Set<AbstractAction> expected = Set.of(displace(SHRIMP, 1, hex(1, 0), hex(2, 0)),
                displace(SHRIMP, 1, hex(1, 0), hex(1, 1)));
        assertEquals(expected, legalSet(state, fm));
        assertTrue(state.getCreatures().isEmpty());

        Displace choice = displace(SHRIMP, 1, hex(1, 0), hex(1, 1));
        String text = choice.getString(state);
        assertTrue(text, text.contains(SHRIMP.label) && text.contains("(1,0)") && text.contains("(1,1)"));
        fm.next(state, choice);
        assertEquals(Map.of(hex(1, 1), new Creature(SHRIMP, 1, 1)), state.getCreatures());
        assertEquals(5, state.getSupply(1, SHRIMP));
        assertTokensConserved(state, "after the displacement");

        // then player 0's creature step: empty non-Basalt hexes (0,0) S2, (0,1) M2, (2,0) M1
        assertCreatureStepOf(0);
        assertEquals(Set.of(shrimp(hex(0, 0)), shrimp(hex(0, 1)), shrimp(hex(2, 0))), legalSet(state, fm));
    }

    @Test
    public void aCoveredCreatureWithOneDestinationMovesThereWithoutADecisionAndTheMoveIsLogged() {
        seafloorPair(SHRIMP);
        putCreature(state, hex(0, 1), SHRIMP, 1, 1);
        coverSeafloorPair();
        // (0,1)'s neighbours off the new tile: (1,1) D1 and (-1,1) B1; only (1,1) is not Basalt
        assertEquals(Map.of(hex(1, 1), new Creature(SHRIMP, 1, 1)), state.getCreatures());
        assertNoCoveringDecision(state);
        assertCreatureStepOf(0);
        assertEquals(Set.of(shrimp(hex(0, 0)), shrimp(hex(0, 1)), shrimp(hex(2, 0))), legalSet(state, fm));
        assertTokensConserved(state, "after the displacement");
        assertLogged("Player 1's " + SHRIMP + " covered at (0,1) moves to (1,1)");
    }

    @Test
    public void aCoveredShrimpWithNoHexOffTheNewTileReturnsToSupplyAndTheReturnIsLogged() {
        seafloorPair(SHRIMP);
        putCreature(state, hex(1, 1), SHRIMP, 0, 1);
        putCreature(state, hex(0, 1), SHRIMP, 1, 1);
        coverSeafloorPair();
        // (1,1) is occupied, and the new Smoker (0,0) is on the tile that covered the shrimp: it returns
        assertEquals(Map.of(hex(1, 1), new Creature(SHRIMP, 0, 1)), state.getCreatures());
        assertEquals(6, state.getSupply(1, SHRIMP));
        assertNoCoveringDecision(state);
        assertCreatureStepOf(0);
        assertEquals(Set.of(shrimp(hex(0, 0)), shrimp(hex(0, 1)), shrimp(hex(2, 0))), legalSet(state, fm));
        assertTokensConserved(state, "after the displacement");
        assertLogged("Player 1's " + SHRIMP + " covered at (0,1) has nowhere to go: returned to supply");
    }

    @Test
    public void thePlacerChoosesFirstAndMayTakeTheOnlyHexLeftForAnotherPlayersCreature() {
        seafloorPair(SHRIMP);
        putCreature(state, hex(1, 0), SHRIMP, 0, 1);
        putCreature(state, hex(0, 1), SHRIMP, 1, 1);
        coverSeafloorPair();
        // player 0's shrimp from (1,0) is resolved first: (2,0) or (1,1); player 1's from (0,1) has only (1,1)
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(displace(SHRIMP, 0, hex(1, 0), hex(2, 0)), displace(SHRIMP, 0, hex(1, 0), hex(1, 1))),
                legalSet(state, fm));
        assertTrue("nothing moves before the placer's choice", state.getCreatures().isEmpty());
        fm.next(state, displace(SHRIMP, 0, hex(1, 0), hex(1, 1)));

        assertEquals(Map.of(hex(1, 1), new Creature(SHRIMP, 0, 1)), state.getCreatures());
        assertEquals(6, state.getSupply(1, SHRIMP));
        assertCreatureStepOf(0);
        assertTokensConserved(state, "after the displacements");
        assertLogged("Player 1's " + SHRIMP + " covered at (0,1) has nowhere to go: returned to supply");
    }

    private void assertLogged(String line) {
        List<String> history = state.getHistoryAsText();
        assertTrue(line + " not in " + history, history.stream().anyMatch(h -> h.startsWith(line)));
    }

    @Test
    public void thePlacersClimbingWormTakesTheNewSmokerBeforeAnotherPlayersShrimpMoves() {
        seafloorPair(WORM, SHRIMP);
        putCreature(state, hex(0, 0), WORM, 0, 1);
        putCreature(state, hex(0, 1), SHRIMP, 1, 1);
        coverSeafloorPair();
        // player 0's worm is resolved first and climbs onto (0,0); player 1's shrimp then has only (1,1)
        assertEquals(Map.of(hex(0, 0), new Creature(WORM, 0, 1), hex(1, 1), new Creature(SHRIMP, 1, 1)),
                state.getCreatures());
        assertCreatureStepOf(0);
        // no empty Smoker for worms; shrimp on (0,1) M2 or (2,0) M1
        assertEquals(Set.of(shrimp(hex(0, 1)), shrimp(hex(2, 0))), legalSet(state, fm));
    }

    @Test
    public void ownerOfACoveredSeafloorFishChoosesAmongLevelOneNonSmokerHexes() {
        seafloorPair(FISH);
        putCreature(state, hex(0, 1), FISH, 1, 1);
        coverSeafloorPair();
        // neighbours of (0,1): (1,1) D1, (-1,1) B1 allowed; (0,0) S2 a Smoker, (1,0) B2 higher than the fish's level 1
        assertEquals(1, state.getCurrentPlayer());
        Set<AbstractAction> expected = Set.of(displace(FISH, 1, hex(0, 1), hex(1, 1)),
                displace(FISH, 1, hex(0, 1), hex(-1, 1)));
        assertEquals(expected, legalSet(state, fm));
        fm.next(state, displace(FISH, 1, hex(0, 1), hex(-1, 1)));
        assertEquals(Map.of(hex(-1, 1), new Creature(FISH, 1, 1)), state.getCreatures());

        // player 0's step: level-1 non-Smoker (1,1) and (2,0); (0,1) M2 is next to player 1's fish, not player 0's
        assertCreatureStepOf(0);
        assertEquals(Set.of(fish(hex(1, 1)), fish(hex(2, 0))), legalSet(state, fm));
    }

    @Test
    public void ownerOfACoveredFishChoosesAmongHexesOfTheSameOrALowerLevel() {
        standardFieldBeforeTile5(FISH);
        putCreature(state, hex(0, 1), FISH, 0, 1);
        placeTile5();
        // (0,1) M2 is under the new right hex; neighbours: (1,1) D1 (lower), (1,0) B2 (same), (-1,1) M3 (new, higher),
        // (0,0) S3 (Smoker); (0,2), (-1,2) uncovered. Player 0 decides on player 1's turn
        assertEquals(0, state.getCurrentPlayer());
        Set<AbstractAction> expected = Set.of(displace(FISH, 0, hex(0, 1), hex(1, 1)),
                displace(FISH, 0, hex(0, 1), hex(1, 0)));
        assertEquals(expected, legalSet(state, fm));
        fm.next(state, displace(FISH, 0, hex(0, 1), hex(1, 1)));
        assertEquals(Map.of(hex(1, 1), new Creature(FISH, 0, 1)), state.getCreatures());

        // player 1's step: level-1 non-Smoker (2,0) M1, (-2,0) B1; player 1 has no fish
        assertCreatureStepOf(1);
        assertEquals(Set.of(fish(hex(2, 0)), fish(hex(-2, 0))), legalSet(state, fm));
    }

    @Test
    public void aCoveredFishStaysOffSmokers() {
        standardFieldBeforeTile5(FISH);
        putCreature(state, hex(-1, 1), FISH, 0, 1);
        placeTile5();
        // (-1,1) B2 is under the new left hex; neighbours: (-2,1) D2 (same level), (-1,0) S2 (same level, but a
        // Smoker, and Fish never go on Smokers), (0,1) D3 and (0,0) S3 (new, higher),
        // (-1,2) and (-2,2) uncovered -> the single destination (-2,1), with no decision
        assertEquals(Map.of(hex(-2, 1), new Creature(FISH, 0, 1)), state.getCreatures());
        assertNoCoveringDecision(state);
        assertTokensConserved(state, "after the displacement");
        // player 1's step (no fish of their own): the level-1 non-Smoker hexes (1,1) D1, (2,0) M1, (-2,0) B1
        assertCreatureStepOf(1);
        assertEquals(Set.of(fish(hex(1, 1)), fish(hex(2, 0)), fish(hex(-2, 0))), legalSet(state, fm));
    }

    @Test
    public void aCoveredCrabMayChooseASmokerOfItsLevel() {
        standardFieldBeforeTile5(CRAB);
        putCreature(state, hex(0, 1), CRAB, 0, 1);
        putCreature(state, hex(-1, 1), CRAB, 1, 1);
        placeTile5();
        // the placer's crab first: (-2,1) D2 or the Smoker (-1,0) S2; player 1 decides
        assertEquals(1, state.getCurrentPlayer());
        Set<AbstractAction> expected = Set.of(displace(CRAB, 1, hex(-1, 1), hex(-2, 1)),
                displace(CRAB, 1, hex(-1, 1), hex(-1, 0)));
        assertEquals(expected, legalSet(state, fm));
        fm.next(state, displace(CRAB, 1, hex(-1, 1), hex(-1, 0)));
        // then player 0's crab, automatically to (1,0)
        assertEquals(Map.of(hex(-1, 0), new Creature(CRAB, 1, 1), hex(1, 0), new Creature(CRAB, 0, 1)),
                state.getCreatures());
        assertCreatureStepOf(1);
        // empty, level >= 2: (0,0) S3, (-1,1) M3, (0,1) D3, (-2,1) D2
        assertEquals(Set.of(crab(hex(0, 0)), crab(hex(-1, 1)), crab(hex(0, 1)), crab(hex(-2, 1))),
                legalSet(state, fm));
    }

    @Test
    public void thePlacersCreatureTakesTheOnlyFreeHexAndAnEarlierSeatedPlayersCreatureReturns() {
        standardFieldBeforeTile5(CRAB);
        putCreature(state, hex(0, 0), CRAB, 0, 1);
        putCreature(state, hex(-1, 0), CRAB, 0, 1);
        putCreature(state, hex(0, 1), CRAB, 1, 1);
        placeTile5();
        // player 1 (placer) first: crab under the right hex (0,1) M2 -> (1,0) B2, its only level-2 neighbour.
        // Then player 0's crab under the Smoker (0,0) S2: (1,0) now occupied, (-1,0) occupied, (0,1), (-1,1) level 3
        // -> no destination: returned (player 0: 6 - 2 + 1 = 5 in supply)
        assertEquals(Map.of(hex(1, 0), new Creature(CRAB, 1, 1), hex(-1, 0), new Creature(CRAB, 0, 1)),
                state.getCreatures());
        assertEquals(5, state.getSupply(0, CRAB));
        assertEquals(5, state.getSupply(1, CRAB));
        assertTokensConserved(state, "after the displacement");
        assertCreatureStepOf(1);
        // empty, level >= 2: (0,0) S3, (-1,1) M3, (0,1) D3, (-2,1) D2
        assertEquals(Set.of(crab(hex(0, 0)), crab(hex(-1, 1)), crab(hex(0, 1)), crab(hex(-2, 1))),
                legalSet(state, fm));
    }
}
