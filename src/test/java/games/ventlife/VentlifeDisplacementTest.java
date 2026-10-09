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
 * Displacement of covered Shrimp, Fish and Crabs: destinations are the empty hexes next to the covered position
 * (the new tile's own hexes included), by species; none -> returned to the supply; one -> moved there; several -> the
 * creature's owner chooses with Displace, whoever placed the tile. Resolution order: the placing player's creatures
 * first, then the other players clockwise. After the last one, the placing player's creature step.
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

    private static VentlifeParameters smokersForAll() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("smokersOnlyForWormsAndShrimp", false);
        return params;
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
    public void ownerOfACoveredShrimpChoosesAmongNonBasaltHexesNextToItSmokersIncluded() {
        seafloorPair(SHRIMP);
        putCreature(state, hex(0, 1), SHRIMP, 1, 1);
        coverSeafloorPair();
        // (0,1) is under the new left hex; its neighbours: (1,1) D1, (-1,1) B1, (0,0) S2 and (1,0) B2 (new tile),
        // (0,2) and (-1,2) uncovered. Not Basalt: (1,1) and the Smoker (0,0). Player 1 decides on player 0's turn
        assertEquals(1, state.getCurrentPlayer());
        Set<AbstractAction> expected = Set.of(displace(SHRIMP, 1, hex(0, 1), hex(1, 1)),
                displace(SHRIMP, 1, hex(0, 1), hex(0, 0)));
        assertEquals(expected, legalSet(state, fm));
        assertTrue(state.getCreatures().isEmpty());

        Displace choice = displace(SHRIMP, 1, hex(0, 1), hex(0, 0));
        String text = choice.getString(state);
        assertTrue(text, text.contains(SHRIMP.label) && text.contains("(0,1)") && text.contains("(0,0)"));
        fm.next(state, choice);
        assertEquals(Map.of(hex(0, 0), new Creature(SHRIMP, 1, 1)), state.getCreatures());
        assertEquals(5, state.getSupply(1, SHRIMP));
        assertTokensConserved(state, "after the displacement");

        // then player 0's creature step: empty non-Basalt hexes (0,1) M2, (1,1) D1, (2,0) M1
        assertCreatureStepOf(0);
        assertEquals(Set.of(shrimp(hex(0, 1)), shrimp(hex(1, 1)), shrimp(hex(2, 0))), legalSet(state, fm));
    }

    @Test
    public void aCoveredCreatureWithOneDestinationMovesThereWithoutADecision() {
        seafloorPair(SHRIMP);
        putCreature(state, hex(1, 1), SHRIMP, 0, 1);
        putCreature(state, hex(0, 1), SHRIMP, 1, 1);
        coverSeafloorPair();
        // (1,1) is occupied, so player 1's shrimp has only the new Smoker (0,0)
        assertEquals(Map.of(hex(1, 1), new Creature(SHRIMP, 0, 1), hex(0, 0), new Creature(SHRIMP, 1, 1)),
                state.getCreatures());
        assertNoCoveringDecision(state);
        assertCreatureStepOf(0);
        assertEquals(Set.of(shrimp(hex(0, 1)), shrimp(hex(2, 0))), legalSet(state, fm));
        assertTokensConserved(state, "after the displacement");
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
    public void coveredCrabsMoveToTheirOnlyHexOfTheSameLevelThatIsNotASmoker() {
        standardFieldBeforeTile5(CRAB);
        putCreature(state, hex(0, 1), CRAB, 0, 1);
        putCreature(state, hex(-1, 1), CRAB, 1, 1);
        placeTile5();
        // player 1's crab (placer) under the left hex (-1,1) B2: neighbours (-2,1) D2, (-1,0) S2 (Smoker),
        // (0,1) and (0,0) now level 3 -> (-2,1).
        // player 0's crab under the right hex (0,1) M2: (1,0) B2, (1,1) D1, (-1,1) and (0,0) level 3 -> (1,0)
        assertEquals(Map.of(hex(-2, 1), new Creature(CRAB, 1, 1), hex(1, 0), new Creature(CRAB, 0, 1)),
                state.getCreatures());
        assertCreatureStepOf(1);
        // empty, level >= 2, not a Smoker: (-1,1) M3, (0,1) D3
        assertEquals(Set.of(crab(hex(-1, 1)), crab(hex(0, 1))), legalSet(state, fm));
        assertTokensConserved(state, "after the displacement");
    }

    @Test
    public void aCoveredCrabMayChooseASmokerOfItsLevelWhenSmokersAreOpenToEverySpecies() {
        state = newState(2, 42, smokersForAll());
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
        state = newState(2, 42, smokersForAll());
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
