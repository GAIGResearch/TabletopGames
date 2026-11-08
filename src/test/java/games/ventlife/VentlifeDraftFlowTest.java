package games.ventlife;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.ventlife.actions.DraftSpecies;
import games.ventlife.actions.PlaceCreature;
import games.ventlife.actions.PlaceTile;
import games.ventlife.components.Species;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/** The species draft (Advanced Variant) through the real game: the draft, then play with the species chosen. */
public class VentlifeDraftFlowTest {

    @Test
    public void draftedSpeciesAreTheOnesPlacedInTheFirstTurn() {
        Game game = newGame(2, 41, draftParams(4));
        VentlifeGameState state = (VentlifeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        draft(state, fm, SHRIMP, CRAB, WORM, SPONGE);          // by 0, 1, 0, 1
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, placeTile(state, hex(0, 0), 1));
        // the first tile's Black Smoker takes a Tube Worm; nothing of the 3 undrafted species is offered
        assertEquals(VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        Set<Species> drafted = EnumSet.of(SHRIMP, CRAB, WORM, SPONGE);
        boolean worm = false;
        for (AbstractAction a : fm.computeAvailableActions(state))
            if (a instanceof PlaceCreature pc) {
                assertTrue(pc + " is not a drafted species", drafted.contains(pc.species));
                worm |= pc.species == WORM;
            }
        assertTrue("no Tube Worm offered on the first Smoker", worm);
        stopPlacing(state, fm);
        assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void aRandomGameWithTheDraftReachesTheEndWithTheChosenSpecies() {
        int[] tiles = {36, 48, 60};
        int[] picks = {4, 3, 4};                               // nSpecies 4: 4 picks at 2/4 players, 7 - 4 at 3
        for (int n = 2; n <= 4; n++)
            for (long seed = 1; seed <= 2; seed++) {
                String which = n + " players, seed " + seed;
                Game game = newGame(n, 400 + 10 * n + seed, draftParams(4));
                VentlifeGameState state = (VentlifeGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                int draftPicks = 0, placements = 0, steps = 0;
                while (state.isNotTerminal() && steps++ < 5000) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    assertFalse(which + ": no legal action at step " + steps, actions.isEmpty());
                    AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));
                    if (state.getGamePhase() == VentlifeGameState.Phase.DRAFT) {
                        assertTrue(which + ": " + chosen + " in the draft", chosen instanceof DraftSpecies);
                        assertEquals(which + ": picker " + draftPicks, draftPicks % n, state.getCurrentPlayer());
                        draftPicks++;
                    } else
                        assertFalse(which + ": " + chosen + " after the draft", chosen instanceof DraftSpecies);
                    if (chosen instanceof PlaceTile)
                        placements++;
                    fm.next(state, chosen);
                }
                assertFalse(which + ": did not end within 5000 actions", state.isNotTerminal());
                assertEquals(which, CoreConstants.GameResult.GAME_END, state.getGameStatus());
                assertEquals(which, picks[n - 2], draftPicks);
                assertEquals(which, tiles[n - 2], placements);
                assertEquals(which, 0, unplacedCount(state));
                // in play: the drafted species at 2/4 players, the others at 3
                Set<Species> expected = EnumSet.noneOf(Species.class);
                for (Species s : Species.values())
                    if (state.getDrafted().contains(s) != (n == 3))
                        expected.add(s);
                assertEquals(which, picks[n - 2], state.getDrafted().size());
                assertEquals(which, expected, new HashSet<>(state.getSpeciesInPlay()));
                assertEquals(which, 4, state.getSpeciesInPlay().size());
                assertTokensConserved(state, which + " at the end");
            }
    }
}
