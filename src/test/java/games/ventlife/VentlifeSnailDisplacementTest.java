package games.ventlife;

import games.ventlife.components.Creature;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Volcano Snail displacement: to an adjacent empty hex whose level is strictly lower than the Snail's own hex before
 * it was covered; any terrain except a Black Smoker while smokersOnlyForWormsAndShrimp is on. None -> returned, one
 * -> moved, several -> the owner chooses.
 * Layout "downhill" (2 players): standard tiles 0-4 (players 0, 1, 0, 1, 0); player 1 places D/M at ((0,2),2):
 * (0,2) S1, (-1,2) D1, (-1,3) M1; then player 0 places tile 5 M/D at ((0,0),1), covering (0,0), (-1,1), (0,1) to
 * level 3. Neighbours of (0,1) M2 then: (1,1) D1, (0,2) S1, (-1,2) D1, (1,0) B2, (-1,1) M3, (0,0) S3. Neighbours of
 * (-1,1) B2: (-1,2) D1, (-2,1) D2, (-1,0) S2, (0,1) D3, (0,0) S3, (-2,2) uncovered.
 */
public class VentlifeSnailDisplacementTest {

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

    /** The downhill layout up to tile 5, with no species, then only Snails; player 0 to move. */
    private void downhillBeforeTile5() {
        useSpecies(state);
        buildStandardField(state, fm, 5);
        place(state, fm, DIFFUSE, MAT, hex(0, 2), 2);
        useSpecies(state, SNAIL);
        assertEquals(0, state.getCurrentPlayer());
    }

    /** The current player places tile 5, M/D at ((0,0),1). */
    private void placeTile5() {
        giveTile(state, state.getCurrentPlayer(), MAT, DIFFUSE);
        fm.next(state, placeTile(state, hex(0, 0), 1));
    }

    @Test
    public void aCoveredSnailMovesToItsOnlyLowerNeighbourWhateverItsTerrain() {
        downhillBeforeTile5();
        putCreature(state, hex(-1, 1), SNAIL, 0, 1);
        placeTile5();
        // from B2: only (-1,2) D1 is lower; (-2,1) D2 is the same level
        assertNoCoveringDecision(state);
        assertEquals(Map.of(hex(-1, 2), new Creature(SNAIL, 0, 1)), state.getCreatures());
        assertEquals(5, state.getSupply(0, SNAIL));
    }

    @Test
    public void theOwnerOfACoveredSnailChoosesAmongLowerNeighboursThatAreNotSmokers() {
        downhillBeforeTile5();
        putCreature(state, hex(0, 1), SNAIL, 1, 1);
        placeTile5();
        // from M2: (1,1) D1 and (-1,2) D1; not (0,2) S1 (a Smoker), (1,0) B2 (same level), the level-3 tile hexes.
        // Player 1 decides on player 0's turn
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(displace(SNAIL, 1, hex(0, 1), hex(1, 1)), displace(SNAIL, 1, hex(0, 1), hex(-1, 2))),
                legalSet(state, fm));
        fm.next(state, displace(SNAIL, 1, hex(0, 1), hex(1, 1)));
        assertEquals(Map.of(hex(1, 1), new Creature(SNAIL, 1, 1)), state.getCreatures());
        assertTokensConserved(state, "after the displacement");
        // then player 0's creature step: empty Basalt (1,0) B2, (-2,0) B1
        assertEquals(VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(snail(hex(1, 0)), snail(hex(-2, 0))), legalSet(state, fm));
    }

    @Test
    public void aCoveredSnailMayChooseALowerSmokerWhenSmokersAreOpenToEverySpecies() {
        state = newState(2, 42, smokersForAll());
        downhillBeforeTile5();
        putCreature(state, hex(0, 1), SNAIL, 1, 1);
        placeTile5();
        assertEquals(Set.of(displace(SNAIL, 1, hex(0, 1), hex(1, 1)), displace(SNAIL, 1, hex(0, 1), hex(-1, 2)),
                displace(SNAIL, 1, hex(0, 1), hex(0, 2))), legalSet(state, fm));
    }

    @Test
    public void aCoveredSnailWithNoLowerNeighbourReturnsEvenWithAFreeHexOfItsOwnLevel() {
        // standard tiles 0-4 only: (-1,2) is uncovered, so (-1,1) B2 has no lower neighbour; (-2,1) D2 is free
        useSpecies(state);
        buildStandardField(state, fm, 5);
        useSpecies(state, SNAIL);
        putCreature(state, hex(-1, 1), SNAIL, 0, 1);
        placeTile5();
        assertNoCoveringDecision(state);
        assertTrue(state.getCreatures().isEmpty());
        assertEquals(6, state.getSupply(0, SNAIL));
    }
}
