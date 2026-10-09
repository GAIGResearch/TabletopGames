package games.ventlife;

import games.ventlife.actions.PlaceTile;
import games.ventlife.actions.StopPlacing;
import games.ventlife.components.Creature;
import games.ventlife.components.HexCell;
import games.ventlife.components.VentTile;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

public class VentlifeCopyTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 3);
        fm = new VentlifeForwardModel();
    }

    /** Writes a field of one tile at ((0,0), 1) straight into the state (no rules involved), for the copy checks. */
    private void arrangeField() {
        state.field.put(hex(0, 0), new HexCell(SMOKER, 1, 0));
        state.field.put(hex(-1, 1), new HexCell(BASALT, 1, 0));
        state.field.put(hex(0, 1), new HexCell(DIFFUSE, 1, 0));
        state.tilesPlaced = 1;
    }

    @Test
    public void copyEqualsTheOriginalAndIsIndependentOfIt() {
        arrangeField();
        useSpecies(state, WORM, SPONGE);
        putCreature(state, hex(0, 0), WORM, 0, 1);
        VentlifeGameState copy = (VentlifeGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        copy.field.put(hex(1, 0), new HexCell(SMOKER, 1, 1));
        copy.getDrawDeck().draw();
        copy.setCreature(hex(0, 0), new Creature(WORM, 0, 2));
        copy.changeSupply(0, WORM, -1);
        assertEquals(3, state.getField().size());
        assertEquals(34, state.getDrawDeck().getSize());
        assertEquals(new Creature(WORM, 0, 1), state.getCreature(hex(0, 0)));
        assertEquals(5, state.getSupply(0, WORM));
        assertNotEquals(state, copy);
    }

    @Test
    public void copiesDifferingOnlyInTheirCreaturesAreNotEqual() {
        arrangeField();
        useSpecies(state, WORM, SPONGE);
        putCreature(state, hex(-1, 1), SPONGE, 1, 1);
        VentlifeGameState copy = (VentlifeGameState) state.copy();
        // the same sponge, but player 0's (supplies kept equal so only the creatures differ)
        copy.setCreature(hex(-1, 1), new Creature(SPONGE, 0, 1));
        assertNotEquals(state, copy);
    }

    @Test
    public void placingACreatureInACopyLeavesTheOriginalUnchanged() {
        useSpecies(state, WORM, SPONGE);
        // player 0: tile B/D at ((0,0),1), then the creature step
        giveTile(state, 0, BASALT, DIFFUSE);
        fm.next(state, placeTile(state, hex(0, 0), 1));
        VentlifeGameState before = (VentlifeGameState) state.copy();
        VentlifeGameState copy = (VentlifeGameState) state.copy();
        assertEquals(state.hashCode(), copy.hashCode());
        fm.next(copy, worm(hex(0, 0), 2));
        assertEquals(new Creature(WORM, 0, 2), copy.getCreature(hex(0, 0)));
        assertEquals(4, copy.getSupply(0, WORM));
        assertTrue(state.getCreatures().isEmpty());
        assertEquals(6, state.getSupply(0, WORM));
        assertEquals(VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(before, state);
        assertNotEquals(state, copy);
    }

    @Test
    public void placingATileInACopyLeavesTheOriginalUnchanged() {
        VentlifeGameState before = (VentlifeGameState) state.copy();
        VentlifeGameState copy = (VentlifeGameState) state.copy();
        takeTurn(copy, fm, placeTile(copy, hex(0, 0), 1));
        assertEquals(3, copy.getField().size());
        assertTrue(state.getField().isEmpty());
        assertEquals(0, state.getTilesPlaced());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(before, state);
        assertNotEquals(state, copy);
    }

    @Test
    public void copyForAPlayerKeepsTheirTileAndTheFieldAndRedeterminisesTheHiddenTiles() {
        arrangeField();
        VentTile own = state.getHand(0).get(0);
        Map<VentTile, Integer> allTiles = unplacedTiles(state);
        List<VentTile> deck = new ArrayList<>(state.getDrawDeck().getComponents());
        VentTile other = state.getHand(1).get(0);
        int changed = 0;
        // player 1's tile and the 34 tiles of the draw deck are hidden from player 0; an unchanged order of all 35
        // by chance is negligible, so one changed copy in 20 is (almost) certain if they are reshuffled
        for (int i = 0; i < 20; i++) {
            VentlifeGameState copy = (VentlifeGameState) state.copy(0);
            assertEquals(List.of(own), copy.getHand(0).getComponents());
            assertEquals(state.getField(), copy.getField());
            assertEquals(1, copy.getTilesPlaced());
            assertEquals(1, copy.getHand(1).getSize());
            assertEquals(34, copy.getDrawDeck().getSize());
            assertEquals(allTiles, unplacedTiles(copy));
            if (!copy.getDrawDeck().getComponents().equals(deck) || !copy.getHand(1).get(0).equals(other))
                changed++;
        }
        assertTrue("hidden tiles never redeterminised", changed > 0);
        // the original is untouched
        assertEquals(deck, state.getDrawDeck().getComponents());
        assertEquals(other, state.getHand(1).get(0));
    }

    @Test
    public void aCopyTakenDuringADisplacementDecisionAwaitsTheSameDecisionIndependently() {
        // standard tiles 0 and 1, player 1's shrimp on (0,1) D1; player 0 covers it with M/B at ((0,0),0), and
        // player 1 is to choose (1,1) or (0,0) (as in VentlifeDisplacementTest)
        useSpecies(state);
        buildStandardField(state, fm, 2);
        useSpecies(state, SHRIMP);
        putCreature(state, hex(0, 1), SHRIMP, 1, 1);
        giveTile(state, 0, MAT, BASALT);
        fm.next(state, placeTile(state, hex(0, 0), 0));
        Set<Object> decision = Set.of(displace(SHRIMP, 1, hex(0, 1), hex(1, 1)),
                displace(SHRIMP, 1, hex(0, 1), hex(0, 0)));
        assertEquals(decision, new HashSet<>(legalSet(state, fm)));

        VentlifeGameState before = (VentlifeGameState) state.copy();
        VentlifeGameState copy = (VentlifeGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(1, copy.getCurrentPlayer());
        assertEquals(decision, new HashSet<>(legalSet(copy, fm)));

        fm.next(copy, displace(SHRIMP, 1, hex(0, 1), hex(0, 0)));
        assertEquals(new Creature(SHRIMP, 1, 1), copy.getCreature(hex(0, 0)));
        assertEquals(0, copy.getCurrentPlayer());
        // the original still awaits player 1's choice
        assertEquals(before, state);
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.getCreatures().isEmpty());
        assertEquals(decision, new HashSet<>(legalSet(state, fm)));
        assertNotEquals(state, copy);
    }

    @Test
    public void aCopyTakenDuringTheSnailFollowOnOffersTheSameChoicesIndependently() {
        // the Basalt chain field (as in VentlifeSnailPlacementTest): player 1's first Snail on (1,0), then (0,1) or
        // (2,0) or stop
        state = newState(2, 42);
        useSpecies(state);
        buildBasaltChain(state, fm);
        useSpecies(state, SNAIL);
        placeChainTurnTile(state, fm);
        fm.next(state, snail(hex(1, 0)));
        Set<Object> choices = Set.of(snail(hex(0, 1)), snail(hex(2, 0)), new StopPlacing());
        assertEquals(choices, new HashSet<>(legalSet(state, fm)));

        VentlifeGameState before = (VentlifeGameState) state.copy();
        VentlifeGameState copy = (VentlifeGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(choices, new HashSet<>(legalSet(copy, fm)));

        // in the copy, a second Snail on (2,0): the follow-on now offers (0,1) and (3,0)
        fm.next(copy, snail(hex(2, 0)));
        assertEquals(Set.of(snail(hex(0, 1)), snail(hex(3, 0)), new StopPlacing()), new HashSet<>(legalSet(copy, fm)));
        // the original is untouched
        assertEquals(before, state);
        assertEquals(choices, new HashSet<>(legalSet(state, fm)));
        assertNotEquals(state, copy);
    }
}
