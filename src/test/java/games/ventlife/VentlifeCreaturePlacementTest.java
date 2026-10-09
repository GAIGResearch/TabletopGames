package games.ventlife;

import core.actions.AbstractAction;
import games.ventlife.components.Creature;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * The creature step after the tile: who places, which placements of Tube Worms and Vent Sponges are offered (with the
 * Low-Vent Bonus), what a placement takes from the supply, and when the step is skipped.
 * First tile in most tests: B/D at ((0,0),1) -> (0,0) Black Smoker, (-1,1) Basalt, (0,1) Diffuse, all level 1.
 */
public class VentlifeCreaturePlacementTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 42);
        fm = new VentlifeForwardModel();
    }

    private VentlifeGameState newStateWith(VentlifeParameters params) {
        state = newState(2, 42, params);
        return state;
    }

    /** Player 0 places the first tile (B/D at ((0,0),1)), without the creature step that follows. */
    private void placeFirstTile() {
        giveTile(state, 0, BASALT, DIFFUSE);
        fm.next(state, placeTile(state, hex(0, 0), 1));
    }

    @Test
    public void afterTheTileTheSamePlayerChoosesAmongEveryLegalCreaturePlacement() {
        useSpecies(state, WORM, SPONGE);
        placeFirstTile();
        assertEquals(VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        // the draw comes at the end of the turn: not yet (36 tiles - 2 in hands)
        assertEquals(0, state.getHand(0).getSize());
        assertEquals(34, state.getDrawDeck().getSize());
        // worms on the empty level-1 Smoker, 1 or 1 + lowVentBonus (1); sponges on the two empty non-Smoker hexes
        Set<AbstractAction> expected = Set.of(worm(hex(0, 0), 1), worm(hex(0, 0), 2),
                sponge(hex(-1, 1)), sponge(hex(0, 1)));
        assertEquals(expected, legalSet(state, fm));
    }

    @Test
    public void placingACreatureTakesItFromTheSupplyThenTheTurnEndsWithTheDraw() {
        useSpecies(state, WORM, SPONGE);
        placeFirstTile();
        fm.next(state, worm(hex(0, 0), 1));
        assertEquals(Map.of(hex(0, 0), new Creature(WORM, 0, 1)), state.getCreatures());
        assertFalse(state.isEmptyHex(hex(0, 0)));
        // 6 - 1 worms; sponges untouched
        assertEquals(5, state.getSupply(0, WORM));
        assertEquals(6, state.getSupply(0, SPONGE));
        assertEquals(6, state.getSupply(1, WORM));
        assertEquals(1, state.getHand(0).getSize());
        assertEquals(33, state.getDrawDeck().getSize());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());

        // player 1: tile 1 D/M at ((1,0),0) -> (1,0) S, (1,1) D, (2,0) M; a sponge on (1,1)
        place(state, fm, DIFFUSE, MAT, hex(1, 0), 0, sponge(hex(1, 1)));
        assertEquals(new Creature(SPONGE, 1, 1), state.getCreature(hex(1, 1)));
        assertEquals(5, state.getSupply(1, SPONGE));
        assertEquals(6, state.getSupply(1, WORM));
        assertEquals(0, state.getCurrentPlayer());
        assertTokensConserved(state, "after two turns");
    }

    @Test
    public void lowVentBonusPutsTwoWormsOnOneHexFromTheSupply() {
        useSpecies(state, WORM, SPONGE);
        placeFirstTile();
        fm.next(state, worm(hex(0, 0), 2));
        assertEquals(Map.of(hex(0, 0), new Creature(WORM, 0, 2)), state.getCreatures());
        // 6 - 2
        assertEquals(4, state.getSupply(0, WORM));
        assertEquals(1, state.getCurrentPlayer());
        assertTokensConserved(state, "after the bonus");
    }

    @Test
    public void occupiedHexesAreNotOfferedToEitherSpecies() {
        useSpecies(state, WORM, SPONGE);
        place(state, fm, BASALT, DIFFUSE, hex(0, 0), 1, worm(hex(0, 0), 1));
        place(state, fm, DIFFUSE, MAT, hex(1, 0), 0, sponge(hex(0, 1)));
        // player 0: tile B/M at ((-1,0),2) -> (-1,0) S, (-2,0) B, (-2,1) M, all level 1
        giveTile(state, 0, BASALT, MAT);
        fm.next(state, placeTile(state, hex(-1, 0), 2));
        // occupied: (0,0) worm, (0,1) sponge. Empty Smokers (1,0), (-1,0) (level 1: count 1 or 2);
        // empty non-Smokers (-1,1), (1,1), (2,0), (-2,0), (-2,1)
        Set<AbstractAction> expected = Set.of(worm(hex(1, 0), 1), worm(hex(1, 0), 2),
                worm(hex(-1, 0), 1), worm(hex(-1, 0), 2),
                sponge(hex(-1, 1)), sponge(hex(1, 1)), sponge(hex(2, 0)), sponge(hex(-2, 0)), sponge(hex(-2, 1)));
        assertEquals(expected, legalSet(state, fm));
    }

    @Test
    public void wormBonusIsOfferedOnlyOnSeafloorSmokers() {
        useSpecies(state);
        buildStandardField(state, fm, 3);
        // tiles 0-2: (0,0) is a level-2 Smoker; (1,0) is now covered by Basalt
        useSpecies(state, WORM);
        assertEquals(1, state.getCurrentPlayer());
        giveTile(state, 1, BASALT, MAT);
        fm.next(state, placeTile(state, hex(-1, 0), 2));
        // (-1,0) level-1 Smoker: 1 or 2; (0,0) level-2 Smoker: 1 only
        Set<AbstractAction> expected = Set.of(worm(hex(0, 0), 1), worm(hex(-1, 0), 1), worm(hex(-1, 0), 2));
        assertEquals(expected, legalSet(state, fm));
    }

    @Test
    public void bonusParameterSetsHowManyMoreWormsASeafloorSmokerTakes() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("lowVentBonus", 2);
        newStateWith(params);
        useSpecies(state, WORM);
        placeFirstTile();
        // counts 1 .. 1 + 2
        assertEquals(Set.of(worm(hex(0, 0), 1), worm(hex(0, 0), 2), worm(hex(0, 0), 3)), legalSet(state, fm));

        params = new VentlifeParameters();
        params.setParameterValue("lowVentBonus", 0);
        newStateWith(params);
        useSpecies(state, WORM);
        placeFirstTile();
        assertEquals(Set.of(worm(hex(0, 0), 1)), legalSet(state, fm));
    }

    @Test
    public void bonusIsLimitedByTheWormsLeftInTheSupply() {
        // supplies set directly (as if the other worms were on the field elsewhere)
        useSpecies(state, WORM);
        state.supply[0][WORM.ordinal()] = 1;
        placeFirstTile();
        assertEquals(Set.of(worm(hex(0, 0), 1)), legalSet(state, fm));

        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("lowVentBonus", 2);
        newStateWith(params);
        useSpecies(state, WORM);
        state.supply[0][WORM.ordinal()] = 2;
        placeFirstTile();
        // counts 1 .. min(1 + 2, 2)
        assertEquals(Set.of(worm(hex(0, 0), 1), worm(hex(0, 0), 2)), legalSet(state, fm));
    }

    @Test
    public void spongesGoOnBlackSmokersOnlyWhenTheHabitatRestrictionIsOff() {
        useSpecies(state, SPONGE);
        placeFirstTile();
        assertEquals(Set.of(sponge(hex(-1, 1)), sponge(hex(0, 1))), legalSet(state, fm));

        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("smokersOnlyForWormsAndShrimp", false);
        newStateWith(params);
        useSpecies(state, SPONGE);
        placeFirstTile();
        assertEquals(Set.of(sponge(hex(0, 0)), sponge(hex(-1, 1)), sponge(hex(0, 1))), legalSet(state, fm));
    }

    @Test
    public void onlySpeciesInPlayWithTokensLeftAreOffered() {
        // sponges not in play
        useSpecies(state, WORM);
        placeFirstTile();
        assertEquals(Set.of(worm(hex(0, 0), 1), worm(hex(0, 0), 2)), legalSet(state, fm));

        // sponges in play, but player 0 has none left (set directly)
        setup();
        useSpecies(state, WORM, SPONGE);
        state.supply[0][SPONGE.ordinal()] = 0;
        placeFirstTile();
        assertEquals(Set.of(worm(hex(0, 0), 1), worm(hex(0, 0), 2)), legalSet(state, fm));
    }

    @Test
    public void creatureStepIsSkippedWhenThePlayerHasNoLegalPlacement() {
        // only worms in play, and player 0 has none left (set directly)
        useSpecies(state, WORM);
        state.supply[0][WORM.ordinal()] = 0;
        placeFirstTile();
        assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getHand(0).getSize());
        assertEquals(33, state.getDrawDeck().getSize());
        assertTrue(state.getCreatures().isEmpty());
    }

    @Test
    public void placeCreatureDescriptionNamesTheSpeciesTheHexAndTheCount() {
        String s = worm(hex(0, 0), 2).getString(state);
        assertTrue(s, s.contains("Giant Tube Worms"));
        assertTrue(s, s.contains("(0,0)"));
        assertTrue(s, s.contains("2"));
        s = sponge(hex(-1, 1)).getString(state);
        assertTrue(s, s.contains("Vent Sponges"));
        assertTrue(s, s.contains("(-1,1)"));
    }
}
