package games.ventlife;

import core.actions.AbstractAction;
import games.ventlife.components.Creature;
import games.ventlife.components.Hex;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Deep-Sea Octopus placement on edge hexes (covered positions with an uncovered neighbour), and its retreat along
 * the six straight lines from the covered position when covered: each to the end of the line, the last hex before an
 * uncovered one in the direction of travel, and only if that hex is free (designer, 2026-10-11).
 * Layout "ring" (2 players): standard tiles 0-4 (players 0, 1, 0, 1, 0); player 1 places D/M at ((-2,2),0):
 * (-2,2) S, (-2,3) D, (-1,2) M; player 0 places M/D at ((-1,4),4): (-1,4) S, (0,3) M, (-1,3) D; then player 1 places
 * tile 5 M/D at ((0,0),1), covering (0,0), (-1,1), (0,1) to level 3. Afterwards (-1,1) is the only position with no
 * uncovered neighbour (not an edge); (0,2) stays an uncovered gap with (0,3) covered beyond it.
 * Lines from (0,1) after tile 5: E (1,1), (2,1) uncovered; SE (0,2) uncovered; SW (-1,2), (-2,3), (-3,4) uncovered;
 * W (-1,1) [not an edge], (-2,1), (-3,1) uncovered; NW (0,0) S3, (0,-1) uncovered; NE (1,0), (2,-1) uncovered.
 */
public class VentlifeOctopusTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 42);
        fm = new VentlifeForwardModel();
    }

    /** The ring layout up to (not including) tile 5, with no species, then only Octopus; player 1 to move. */
    private void ringBeforeTile5() {
        useSpecies(state);
        buildStandardField(state, fm, 5);
        place(state, fm, DIFFUSE, MAT, hex(-2, 2), 0);
        place(state, fm, MAT, DIFFUSE, hex(-1, 4), 4);
        useSpecies(state, OCTOPUS);
        assertEquals(1, state.getCurrentPlayer());
    }

    /** Player 1 places tile 5, M/D at ((0,0),1). */
    private void placeTile5() {
        giveTile(state, 1, MAT, DIFFUSE);
        fm.next(state, placeTile(state, hex(0, 0), 1));
    }

    /** The whole ring layout (tile 5 included) with no species, then only Octopus, in player 0's creature step. */
    private void ringCreatureStep() {
        useSpecies(state);
        buildStandardField(state, fm, 5);
        place(state, fm, DIFFUSE, MAT, hex(-2, 2), 0);
        place(state, fm, MAT, DIFFUSE, hex(-1, 4), 4);
        place(state, fm, MAT, DIFFUSE, hex(0, 0), 1);
        useSpecies(state, OCTOPUS);
        assertEquals(0, state.getCurrentPlayer());
        startCreatureStep(state);
    }

    /** Player 1's Octopuses on the given hexes (blocking lines), and player 0's Octopus on (0,1). */
    private void octopusOn01With(Hex... blockers) {
        putCreature(state, hex(0, 1), OCTOPUS, 0, 1);
        for (Hex h : blockers)
            putCreature(state, h, OCTOPUS, 1, 1);
    }

    private Set<AbstractAction> retreats(Hex... to) {
        Set<AbstractAction> retValue = new HashSet<>();
        for (Hex h : to)
            retValue.add(displace(OCTOPUS, 0, hex(0, 1), h));
        return retValue;
    }

    @Test
    public void anOctopusMayBePlacedOnAnyEmptyEdgeHexSmokersIncludedButNotAnInteriorOne() {
        ringCreatureStep();
        putCreature(state, hex(2, 0), OCTOPUS, 1, 1);
        // every empty edge hex, the Smokers (0,0), (-1,0), (-2,2), (-1,4) included; (2,0) is taken, and (-1,1) M3 has
        // all six neighbours covered
        assertEquals(Set.of(octopus(hex(0, 1)), octopus(hex(1, 0)), octopus(hex(-2, 1)), octopus(hex(1, 1)),
                        octopus(hex(-2, 0)), octopus(hex(-2, 3)), octopus(hex(-1, 2)), octopus(hex(0, 3)),
                        octopus(hex(-1, 3)), octopus(hex(0, 0)), octopus(hex(-1, 0)), octopus(hex(-2, 2)),
                        octopus(hex(-1, 4))),
                legalSet(state, fm));
    }

    @Test
    public void aCoveredOctopusOwnerChoosesAmongTheEndsOfTheLines() {
        ringBeforeTile5();
        octopusOn01With(hex(-1, 2));
        placeTile5();
        // E (1,1); SW passes player 1's Octopus on (-1,2) to (-2,3); W passes the interior (-1,1) to (-2,1);
        // NE (1,0); NW ends on the new Smoker (0,0) and fails; SE fails at once (the covered (0,3) beyond the gap is
        // never reached). Player 0 decides on player 1's turn
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(retreats(hex(1, 1), hex(-2, 3), hex(-2, 1), hex(1, 0)), legalSet(state, fm));
        fm.next(state, displace(OCTOPUS, 0, hex(0, 1), hex(-2, 1)));
        assertEquals(Map.of(hex(-2, 1), new Creature(OCTOPUS, 0, 1), hex(-1, 2), new Creature(OCTOPUS, 1, 1)),
                state.getCreatures());
        assertEquals(5, state.getSupply(0, OCTOPUS));
        assertEquals(VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertTokensConserved(state, "after the retreat");
    }

    @Test
    public void aRetreatingOctopusNeverStopsOnTheTileThatCoveredIt() {
        ringBeforeTile5();
        octopusOn01With(hex(-1, 2));
        placeTile5();
        // NW ends at the Smoker (0,0) S3, an empty edge hex, but on tile 5: that direction fails
        assertEquals(retreats(hex(1, 1), hex(-2, 3), hex(-2, 1), hex(1, 0)), legalSet(state, fm));
    }

    @Test
    public void anEdgeHexPartWayAlongALineIsPassedOver() {
        ringBeforeTile5();
        octopusOn01With();
        placeTile5();
        // SW: (-1,2) is an empty edge hex (the gap (0,2) is beside it), but the line goes on over (-2,3) to the
        // uncovered (-3,4), so the Octopus goes to (-2,3), the end of the line
        assertEquals(retreats(hex(1, 1), hex(-2, 3), hex(-2, 1), hex(1, 0)), legalSet(state, fm));
    }

    @Test
    public void aTakenHexAtTheEndOfALineClosesThatDirection() {
        ringBeforeTile5();
        // SW ends at (-2,3), which is taken: the empty edge hex (-1,2) before it may not be used instead
        octopusOn01With(hex(-2, 3));
        placeTile5();
        assertEquals(retreats(hex(1, 1), hex(-2, 1), hex(1, 0)), legalSet(state, fm));
    }

    @Test
    public void anOctopusWithOneOpenDirectionRetreatsThereWithoutADecision() {
        ringBeforeTile5();
        // blocked at the ends: E (1,1), then uncovered (2,1); NE (1,0), then (2,-1); SW (-2,3), then (-3,4).
        // Only W is left: past the interior (-1,1) to (-2,1)
        octopusOn01With(hex(1, 1), hex(1, 0), hex(-2, 3));
        placeTile5();
        assertNoCoveringDecision(state);
        assertEquals(new Creature(OCTOPUS, 0, 1), state.getCreature(hex(-2, 1)));
        assertNull(state.getCreature(hex(-1, 1)));
        assertEquals(4, state.getCreatures().size());
        assertEquals(5, state.getSupply(0, OCTOPUS));
        assertTokensConserved(state, "after the retreat");
    }

    @Test
    public void anOctopusWithEveryDirectionFailingReturnsToItsOwner() {
        ringBeforeTile5();
        // as in the one-direction case, and W blocked at (-2,1) before the uncovered (-3,1)
        octopusOn01With(hex(1, 1), hex(1, 0), hex(-2, 3), hex(-2, 1));
        placeTile5();
        assertNoCoveringDecision(state);
        assertNull(state.getCreature(hex(0, 1)));
        assertEquals(4, state.getCreatures().size());
        assertEquals(6, state.getSupply(0, OCTOPUS));
        assertTokensConserved(state, "after the return");
    }
}
