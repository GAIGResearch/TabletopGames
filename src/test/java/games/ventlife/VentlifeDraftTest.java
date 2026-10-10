package games.ventlife;

import core.actions.AbstractAction;
import games.ventlife.actions.DraftSpecies;
import games.ventlife.actions.PlaceTile;
import games.ventlife.components.Species;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * The species draft (Advanced Variant): the order of the picks, the species they put in play at each player count,
 * and the supplies and first turn that follow.
 */
public class VentlifeDraftTest {

    private final VentlifeForwardModel fm = new VentlifeForwardModel();

    private static Set<AbstractAction> draftActions(Species... species) {
        Set<AbstractAction> retValue = new HashSet<>();
        for (Species s : species)
            retValue.add(new DraftSpecies(s));
        return retValue;
    }

    /**
     * The state just after the draft: exactly these species in play with full supplies (none of the others), phase
     * PLACE_TILE, and player 0 offered the 6 orientations of their tile at the origin.
     */
    private void assertPlayStarts(VentlifeGameState state, Set<Species> inPlay) {
        assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        assertEquals("player 0 places the first tile, whoever picked last", 0, state.getCurrentPlayer());
        assertEquals(inPlay, new HashSet<>(state.getSpeciesInPlay()));
        assertEquals("species in play listed once each", inPlay.size(), state.getSpeciesInPlay().size());
        int tokens = state.getParams().tokensPerSpecies;
        for (int p = 0; p < state.getNPlayers(); p++)
            for (Species s : Species.values())
                assertEquals("supply of " + s + " for player " + p, inPlay.contains(s) ? tokens : 0,
                        state.getSupply(p, s));
        Set<AbstractAction> first = new HashSet<>();
        for (int o = 0; o < 6; o++)
            first.add(new PlaceTile(heldTile(state), hex(0, 0), o));
        assertEquals(first, legalSet(state, fm));
    }

    @Test
    public void withTheDraftTheGameStartsInTheDraftWithNoSpeciesAndEmptySupplies() {
        int[] tiles = {36, 48, 60};
        for (int n = 2; n <= 4; n++) {
            VentlifeGameState state = newState(n, 21, draftParams(4));
            assertEquals(VentlifeGameState.Phase.DRAFT, state.getGamePhase());
            assertEquals(0, state.getCurrentPlayer());
            assertTrue(state.getSpeciesInPlay().isEmpty());
            assertTrue(state.getDrafted().isEmpty());
            for (int p = 0; p < n; p++) {
                for (Species s : Species.values())
                    assertEquals(0, state.getSupply(p, s));
                // tiles dealt as usual before the draft: one hidden tile each, the rest in the draw deck
                assertEquals(1, state.getHand(p).getSize());
            }
            assertEquals(tiles[n - 2] - n, state.getDrawDeck().getSize());
        }
    }

    @Test
    public void draftPicksAreTheSpeciesUsedAt2And4PlayersAndTheSpeciesNotUsedAt3() {
        // 2 and 4 players draft the nSpecies used; 3 players draft the 7 - nSpecies not used
        assertEquals(4, VentlifeUtils.draftPicks(4, 2));
        assertEquals(3, VentlifeUtils.draftPicks(4, 3));
        assertEquals(4, VentlifeUtils.draftPicks(4, 4));
        assertEquals(3, VentlifeUtils.draftPicks(3, 2));
        assertEquals(4, VentlifeUtils.draftPicks(3, 3));
        assertEquals(3, VentlifeUtils.draftPicks(3, 4));
        assertEquals(5, VentlifeUtils.draftPicks(5, 2));
        assertEquals(2, VentlifeUtils.draftPicks(5, 3));
        assertEquals(5, VentlifeUtils.draftPicks(5, 4));
    }

    @Test
    public void theDraftOffersExactlyTheSpeciesNotYetDrafted() {
        VentlifeGameState state = newState(2, 21, draftParams(4));
        assertEquals("all 7 species and no tile action", draftActions(Species.values()), legalSet(state, fm));
        fm.next(state, new DraftSpecies(SNAIL));
        assertEquals(draftActions(WORM, SHRIMP, FISH, CRAB, OCTOPUS, SPONGE), legalSet(state, fm));
        fm.next(state, new DraftSpecies(SPONGE));
        assertEquals(draftActions(WORM, SHRIMP, FISH, CRAB, OCTOPUS), legalSet(state, fm));
    }

    @Test
    public void picksAreRecordedInPickOrder() {
        VentlifeGameState state = newState(2, 21, draftParams(4));
        draft(state, fm, OCTOPUS, WORM, CRAB);
        assertEquals(List.of(OCTOPUS, WORM, CRAB), state.getDrafted());
    }

    @Test
    public void picksAreMadeInTurnOrderFromPlayer0WrappingRound() {
        // 2 players: 0,1,0,1 (4 picks); 3 players: 0,1,2 (3 picks); 4 players: 0,1,2,3 (4 picks)
        int[][] pickers = {{0, 1, 0, 1}, {0, 1, 2}, {0, 1, 2, 3}};
        Species[] order = {CRAB, FISH, WORM, SHRIMP};
        for (int n = 2; n <= 4; n++) {
            VentlifeGameState state = newState(n, 22, draftParams(4));
            int[] expected = pickers[n - 2];
            for (int k = 0; k < expected.length; k++) {
                assertEquals(n + " players: phase before pick " + k, VentlifeGameState.Phase.DRAFT, state.getGamePhase());
                assertEquals(n + " players: picker " + k, expected[k], state.getCurrentPlayer());
                fm.next(state, new DraftSpecies(order[k]));
            }
            assertEquals(n + " players: draft over after " + expected.length + " picks",
                    VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        }
    }

    @Test
    public void at2PlayersTheFourDraftedSpeciesAreInPlay() {
        VentlifeGameState state = newState(2, 23, draftParams(4));
        draft(state, fm, SHRIMP, OCTOPUS, WORM, SPONGE);       // by 0, 1, 0, 1
        assertPlayStarts(state, EnumSet.of(SHRIMP, OCTOPUS, WORM, SPONGE));
        assertEquals("the drafted list stays", List.of(SHRIMP, OCTOPUS, WORM, SPONGE), state.getDrafted());
    }

    @Test
    public void at3PlayersTheFourSpeciesNotDraftedAreInPlay() {
        VentlifeGameState state = newState(3, 23, draftParams(4));
        draft(state, fm, FISH, SNAIL, OCTOPUS);                // 7 - 4 = 3 picks by 0, 1, 2: the species NOT used
        assertPlayStarts(state, EnumSet.of(WORM, SHRIMP, CRAB, SPONGE));
        assertEquals("the drafted list stays", List.of(FISH, SNAIL, OCTOPUS), state.getDrafted());
    }

    @Test
    public void at4PlayersTheFourDraftedSpeciesAreInPlay() {
        VentlifeGameState state = newState(4, 23, draftParams(4));
        draft(state, fm, CRAB, SPONGE, SNAIL, FISH);           // one pick each, players 0..3
        assertPlayStarts(state, EnumSet.of(CRAB, SPONGE, SNAIL, FISH));
    }

    @Test
    public void withThreeSpeciesAt2PlayersPlayer0PicksLastAndStillPlacesTheFirstTile() {
        // nSpecies 3 at 2 players: 3 picks by 0, 1, 0; player 0 starts play (not "the player after the last picker")
        VentlifeGameState state = newState(2, 24, draftParams(3));
        draft(state, fm, WORM, CRAB);
        assertEquals(0, state.getCurrentPlayer());
        draft(state, fm, SNAIL);
        assertPlayStarts(state, EnumSet.of(WORM, CRAB, SNAIL));
    }

    @Test
    public void withFiveSpeciesAt3PlayersTwoPicksNameTheTwoSpeciesNotUsed() {
        // nSpecies 5 at 3 players: 7 - 5 = 2 picks by 0, 1; player 0 (not player 2) then places the first tile
        VentlifeGameState state = newState(3, 24, draftParams(5));
        draft(state, fm, SPONGE, SHRIMP);
        assertPlayStarts(state, EnumSet.of(WORM, SNAIL, FISH, CRAB, OCTOPUS));
    }

    @Test
    public void withThreeSpeciesAt3PlayersFourPicksNameTheSpeciesNotUsed() {
        // nSpecies 3 at 3 players: 7 - 3 = 4 picks by 0, 1, 2, 0
        VentlifeGameState state = newState(3, 25, draftParams(3));
        draft(state, fm, WORM, SNAIL, SHRIMP);
        assertEquals("the fourth pick wraps round to player 0", 0, state.getCurrentPlayer());
        assertEquals(VentlifeGameState.Phase.DRAFT, state.getGamePhase());
        draft(state, fm, FISH);
        assertPlayStarts(state, EnumSet.of(CRAB, OCTOPUS, SPONGE));
    }

    @Test
    public void draftSpeciesNamesTheSpecies() {
        VentlifeGameState state = newState(2, 21, draftParams(4));
        for (Species s : Species.values()) {
            assertTrue(new DraftSpecies(s).getString(state).contains(s.label));
            assertTrue(new DraftSpecies(s).toString().contains(s.label));
        }
    }
}
