package games.ventlife;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.ventlife.actions.Displace;
import games.ventlife.actions.PlaceCreature;
import games.ventlife.actions.PlaceTile;
import games.ventlife.actions.StopPlacing;
import games.ventlife.components.*;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * The turn sequence through the real game: draw after placing, passing the turn, the end when every tile is placed,
 * and conservation of the tiles.
 */
public class VentlifeGameFlowTest {

    @Test
    public void placingATileDrawsTheTopTileAndPassesTheTurn() {
        Game game = newGame(3, 5);
        VentlifeGameState state = (VentlifeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        giveTile(state, 0, BASALT, DIFFUSE);
        giveTile(state, 1, DIFFUSE, MAT);
        giveTile(state, 2, MAT, BASALT);
        stackDrawDeck(state, tile(MAT, MAT), tile(BASALT, BASALT), tile(DIFFUSE, DIFFUSE));
        // 48 tiles, 3 in hands
        assertEquals(45, state.getDrawDeck().getSize());

        takeTurn(state, fm, placeTile(state, hex(0, 0), 1));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        assertEquals(List.of(tile(MAT, MAT)), state.getHand(0).getComponents());
        assertEquals(List.of(tile(DIFFUSE, MAT)), state.getHand(1).getComponents());
        assertEquals(44, state.getDrawDeck().getSize());

        // (1,0), (1,1), (2,0) touch the Smoker at (0,0)
        takeTurn(state, fm, placeTile(state, hex(1, 0), 0));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(List.of(tile(BASALT, BASALT)), state.getHand(1).getComponents());

        // plateau on (0,0) [tile 0], (0,1) [tile 0], (1,0) [tile 1]
        takeTurn(state, fm, placeTile(state, hex(0, 0), 0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(tile(DIFFUSE, DIFFUSE)), state.getHand(2).getComponents());
        assertEquals(new HexCell(SMOKER, 2, 2), state.getCell(hex(0, 0)));
        assertEquals(new HexCell(MAT, 2, 2), state.getCell(hex(0, 1)));
        assertEquals(42, state.getDrawDeck().getSize());
        assertEquals(3, state.getTilesPlaced());
        assertTrue(state.isNotTerminal());
    }

    /** Plays the tile without the creature step, asserting the step is skipped (the turn passes straight on). */
    private static void placeSkippingCreatures(VentlifeGameState state, AbstractForwardModel fm, Terrain left,
                                               Terrain right, Hex smoker, int orientation) {
        int player = state.getCurrentPlayer();
        giveTile(state, player, left, right);
        fm.next(state, placeTile(state, smoker, orientation));
        assertEquals("creature step not skipped", VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        assertEquals((player + 1) % state.getNPlayers(), state.getCurrentPlayer());
        assertEquals("placer drew", 1, state.getHand(player).getSize());
    }

    /**
     * The rulebook's Tube Worm example (section 7.4) in a real game with only Tube Worms in play: a worm placed on a seafloor Smoker scores 1, 2 once
     * a plateau tile's Smoker lifts it to level 2, and 3 at level 3; worms under the other hexes of a plateau tile
     * return; the creature step is skipped while no Smoker is empty. Tiles as in buildStandardField.
     */
    @Test
    public void tubeWormsClimbWithTheirSmokerStackAndScoreItsLevel() {
        Game game = newGame(2, 11);
        VentlifeGameState state = (VentlifeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        useSpecies(state, WORM);

        // P0, tile 0 B/D at ((0,0),1): one worm on the level-1 Smoker (0,0) (the bonus is also offered)
        giveTile(state, 0, BASALT, DIFFUSE);
        fm.next(state, placeTile(state, hex(0, 0), 1));
        assertEquals(Set.of(worm(hex(0, 0), 1), worm(hex(0, 0), 2)), legalSet(state, fm));
        fm.next(state, worm(hex(0, 0), 1));
        assertEquals(1, state.getGameScore(0), 0);

        // P1, tile 1 D/M at ((1,0),0): (0,0) is occupied, so only (1,0)
        giveTile(state, 1, DIFFUSE, MAT);
        fm.next(state, placeTile(state, hex(1, 0), 0));
        assertEquals(Set.of(worm(hex(1, 0), 1), worm(hex(1, 0), 2)), legalSet(state, fm));
        fm.next(state, worm(hex(1, 0), 1));
        assertEquals(1, state.getGameScore(1), 0);
        assertEquals(5, state.getSupply(1, WORM));

        // P0, tile 2 M/B at ((0,0),0): P0's worm climbs to level 2; P1's worm under the right hex (1,0) returns.
        // No empty Smoker is left, so P0's creature step is skipped
        placeSkippingCreatures(state, fm, MAT, BASALT, hex(0, 0), 0);
        assertEquals(Map.of(hex(0, 0), new Creature(WORM, 0, 1)), state.getCreatures());
        assertEquals(2, state.getGameScore(0), 0);
        assertEquals(0, state.getGameScore(1), 0);
        assertEquals(6, state.getSupply(1, WORM));

        // P1, tile 3 B/M at ((-1,0),2): two worms (bonus) on the new level-1 Smoker (-1,0)
        giveTile(state, 1, BASALT, MAT);
        fm.next(state, placeTile(state, hex(-1, 0), 2));
        assertEquals(Set.of(worm(hex(-1, 0), 1), worm(hex(-1, 0), 2)), legalSet(state, fm));
        fm.next(state, worm(hex(-1, 0), 2));
        assertEquals(2, state.getGameScore(1), 0);
        assertEquals(4, state.getSupply(1, WORM));

        // P0, tile 4 D/B at ((-1,0),1): its Smoker lifts P1's two worms to level 2 (2 x 2); skipped again
        placeSkippingCreatures(state, fm, DIFFUSE, BASALT, hex(-1, 0), 1);
        assertEquals(new Creature(WORM, 1, 2), state.getCreature(hex(-1, 0)));
        assertEquals(4, state.getGameScore(1), 0);

        // P1, tile 5 M/D at ((0,0),1): P0's worm is lifted to level 3; skipped again
        placeSkippingCreatures(state, fm, MAT, DIFFUSE, hex(0, 0), 1);
        assertEquals(Map.of(hex(0, 0), new Creature(WORM, 0, 1), hex(-1, 0), new Creature(WORM, 1, 2)),
                state.getCreatures());
        assertEquals(3, state.getLevel(hex(0, 0)));
        assertEquals(3, state.getGameScore(0), 0);
        assertEquals(4, state.getGameScore(1), 0);
        assertEquals(5, state.getSupply(0, WORM));
        assertEquals(4, state.getSupply(1, WORM));
        assertTokensConserved(state, "end of walk-through");
        assertEquals(2, state.getOrdinalPosition(0));
        assertEquals(1, state.getOrdinalPosition(1));
    }

    /**
     * Displacement decisions in a real 3-player game: player 1 places a plateau over three Shrimp, its own and players
     * 2's and 0's; the owners decide in the order placer, placer + 1, placer + 2, each choosing among the hexes still
     * empty at that moment; then player 1's creature step. Only Shrimp in play.
     * Tiles (level 1): P0 B/D at ((0,0),1): (0,0) S, (-1,1) B, (0,1) D; P1 D/M at ((1,0),0): (1,0) S, (1,1) D, (2,0) M;
     * P2 B/M at ((-1,0),2): (-1,0) S, (-2,0) B, (-2,1) M; P0 B/B at ((-2,-1),3): (-2,-1) S, (-2,-2) B, (-3,-1) B.
     * Then P1 places M/B at ((0,0),0): (0,0) S2 [P1's shrimp], left (0,1) M2 [P2's], right (1,0) B2 [P0's].
     */
    @Test
    public void coveredCreaturesAreDecidedByTheirOwnersFromThePlacerClockwise() {
        Game game = newGame(3, 21);
        VentlifeGameState state = (VentlifeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        useSpecies(state);
        place(state, fm, BASALT, DIFFUSE, hex(0, 0), 1);
        place(state, fm, DIFFUSE, MAT, hex(1, 0), 0);
        place(state, fm, BASALT, MAT, hex(-1, 0), 2);
        place(state, fm, BASALT, BASALT, hex(-2, -1), 3);
        useSpecies(state, SHRIMP);
        putCreature(state, hex(0, 0), SHRIMP, 1, 1);
        putCreature(state, hex(0, 1), SHRIMP, 2, 1);
        putCreature(state, hex(1, 0), SHRIMP, 0, 1);
        assertEquals(1, state.getCurrentPlayer());
        giveTile(state, 1, MAT, BASALT);
        fm.next(state, placeTile(state, hex(0, 0), 0));

        // player 1's shrimp from (0,0): (0,1) M2, (-1,0) S1; not (1,0) B2 or (-1,1) B1
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(displace(SHRIMP, 1, hex(0, 0), hex(0, 1)), displace(SHRIMP, 1, hex(0, 0), hex(-1, 0))),
                legalSet(state, fm));
        fm.next(state, displace(SHRIMP, 1, hex(0, 0), hex(-1, 0)));

        // then player 2's (placer + 1) from (0,1): (1,1) D1, (0,0) S2; not (-1,1) B1 or (1,0) B2
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(Set.of(displace(SHRIMP, 2, hex(0, 1), hex(1, 1)), displace(SHRIMP, 2, hex(0, 1), hex(0, 0))),
                legalSet(state, fm));
        fm.next(state, displace(SHRIMP, 2, hex(0, 1), hex(1, 1)));

        // then player 0's from (1,0): (2,0) M1, (0,1) M2, (0,0) S2 - not (1,1), just taken by player 2's shrimp
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(displace(SHRIMP, 0, hex(1, 0), hex(2, 0)), displace(SHRIMP, 0, hex(1, 0), hex(0, 1)),
                displace(SHRIMP, 0, hex(1, 0), hex(0, 0))), legalSet(state, fm));
        fm.next(state, displace(SHRIMP, 0, hex(1, 0), hex(0, 1)));

        assertEquals(Map.of(hex(-1, 0), new Creature(SHRIMP, 1, 1), hex(1, 1), new Creature(SHRIMP, 2, 1),
                hex(0, 1), new Creature(SHRIMP, 0, 1)), state.getCreatures());
        assertTokensConserved(state, "after the displacements");
        // player 1's creature step: empty non-Basalt (0,0) S2, (2,0) M1, (-2,1) M1, (-2,-1) S1
        assertEquals(VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(shrimp(hex(0, 0)), shrimp(hex(2, 0)), shrimp(hex(-2, 1)), shrimp(hex(-2, -1))),
                legalSet(state, fm));
        fm.next(state, shrimp(hex(2, 0)));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        assertEquals(1, state.getHand(1).getSize());
        // 6 - 1 for players 0 and 2; 6 - 2 for player 1
        assertEquals(5, state.getSupply(0, SHRIMP));
        assertEquals(4, state.getSupply(1, SHRIMP));
        assertEquals(5, state.getSupply(2, SHRIMP));
    }

    @Test
    public void snailGroupThenCoveringAndAnOctopusWalkThrough() {
        Game game = newGame(2, 5);
        VentlifeGameState state = (VentlifeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        useSpecies(state);
        // standard tiles 0-4 (players 0, 1, 0, 1, 0), then player 1's B/B at ((0,2),2): (0,2) S1, (-1,2) B1, (-1,3) B1
        buildStandardField(state, fm, 5);
        place(state, fm, BASALT, BASALT, hex(0, 2), 2);
        useSpecies(state, SNAIL, OCTOPUS);

        // player 0: D/M at ((3,-1),4), away from the Basalt, then a group of three Snails from (-1,1) B2
        place(state, fm, DIFFUSE, MAT, hex(3, -1), 4, snail(hex(-1, 1)));
        assertEquals(VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        // only Snails in the follow-on, though Octopus is in play
        assertEquals(Set.of(snail(hex(-1, 2)), new StopPlacing()), legalSet(state, fm));
        fm.next(state, snail(hex(-1, 2)));
        assertEquals(Set.of(snail(hex(-1, 3)), new StopPlacing()), legalSet(state, fm));
        fm.next(state, snail(hex(-1, 3)));
        assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(3, state.getSupply(0, SNAIL));

        // player 1: tile 5 M/D at ((0,0),1) covers the Snail on (-1,1) B2: its only lower neighbour (-1,2) is taken
        // (the others are level 2 or 3, or uncovered), so it returns
        giveTile(state, 1, MAT, DIFFUSE);
        fm.next(state, placeTile(state, hex(0, 0), 1));
        Creature s = new Creature(SNAIL, 0, 1);
        assertEquals(Map.of(hex(-1, 2), s, hex(-1, 3), s), state.getCreatures());
        assertEquals(4, state.getSupply(0, SNAIL));
        assertEquals(VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        // (-1,1) M3 is an edge ((-2,2) uncovered); (0,1) D3 is not (all six neighbours covered)
        Set<AbstractAction> legal = legalSet(state, fm);
        assertTrue(legal.contains(octopus(hex(-1, 1))));
        assertFalse(legal.contains(octopus(hex(0, 1))));
        fm.next(state, octopus(hex(-1, 1)));
        assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());

        // player 0's Snails: (-1,2) B1 next to (-1,1) M3 and (0,1) D3 -> 2 ((0,2) S1, (-1,3) B1 the same level);
        // (-1,3) B1 -> 0. Player 1's Octopus on (-1,1): one species (Snail) around it -> 2
        assertEquals(2.0, state.getGameScore(0), 0.0);
        assertEquals(2.0, state.getGameScore(1), 0.0);
        assertTokensConserved(state, "end of the walk-through");
    }

    /** A turn ends when the next player is to place a tile (no decision of this turn still in progress). */
    private static boolean turnOver(VentlifeGameState state) {
        return !state.isNotTerminal()
                || (state.getGamePhase() == VentlifeGameState.Phase.PLACE_TILE && !state.isActionInProgress());
    }

    @Test
    public void gameEndsWhenEveryTileHasBeenPlacedWithEqualTurns() {
        int[] tiles = {36, 48, 60};
        for (int n = 2; n <= 4; n++) {
            int total = tiles[n - 2];
            Game game = newGame(n, 100 + n);
            VentlifeGameState state = (VentlifeGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            // every species, so that creature steps are certain to arise
            useSpecies(state, Species.values());
            Random rnd = new Random(n);
            int placements = 0, steps = 0, creatureSteps = 0;
            while (state.isNotTerminal() && steps++ < 5000) {
                // one whole turn: the tile, then the creature step (if any) until the next player is to place a tile
                int placer = state.getCurrentPlayer();
                // placement k (from 0) is by player k mod n, who always holds a tile
                assertEquals("placer of tile " + placements, placements % n, placer);
                assertEquals("hand of placer of tile " + placements, 1, state.getHand(placer).getSize());
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                assertFalse("no legal action at step " + steps, actions.isEmpty());
                assertTrue("not all tile placements: " + actions, actions.stream().allMatch(a -> a instanceof PlaceTile));
                fm.next(state, actions.get(rnd.nextInt(actions.size())));
                while (!turnOver(state) && steps++ < 5000) {
                    if (state.getGamePhase() == VentlifeGameState.Phase.PLACE_CREATURES) {
                        assertEquals("creature step player", placer, state.getCurrentPlayer());
                        creatureSteps++;
                    }
                    actions = fm.computeAvailableActions(state);
                    assertFalse("no legal action at step " + steps, actions.isEmpty());
                    assertDecisionByOwner(state, actions);
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                }
                placements++;
                // the draw deck started with total - n tiles; the placer draws at the end of the turn while any are left
                int deckBefore = Math.max(0, total - n - (placements - 1));
                assertEquals(Math.max(0, deckBefore - 1), state.getDrawDeck().getSize());
                assertEquals("hand after tile " + placements, deckBefore > 0 ? 1 : 0, state.getHand(placer).getSize());
                if (placements < total)
                    assertTrue("game ended after only " + placements + " tiles", state.isNotTerminal());
            }
            assertTrue(n + "-player game had no creature step", creatureSteps > 0);
            assertFalse(n + "-player game did not end within 5000 actions", state.isNotTerminal());
            assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
            assertEquals(total, placements);
            assertEquals(total, state.getTilesPlaced());
            assertEquals(0, unplacedCount(state));
        }
    }

    /** The number of species in play of which the player has no tokens left in their supply. */
    private static int speciesRunOut(VentlifeGameState state, int player) {
        int n = 0;
        for (Species s : state.getSpeciesInPlay())
            if (state.getSupply(player, s) == 0)
                n++;
        return n;
    }

    /**
     * At the default parameters (4 random species, 6 tokens each) the game ends only when every tile has been placed,
     * even after a player has used up two or more species: every player has the same number of turns, and the tiles
     * and tokens are all accounted for at the end.
     */
    @Test
    public void aRandomGameAtTheDefaultParametersEndsOnlyWhenEveryTileIsPlaced() {
        int[] tiles = {36, 48, 60};
        // turns taken while some player had run out of at least two species: random play must reach that position,
        // or the test would not show that running out does not end the game
        int turnsAfterRunningOut = 0;
        for (int n = 2; n <= 4; n++)
            for (long seed = 1; seed <= 3; seed++) {
                int total = tiles[n - 2];
                Game game = newGame(n, 300 + 10 * n + seed);
                VentlifeGameState state = (VentlifeGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                int[] turns = new int[n];
                int placements = 0, steps = 0;
                while (state.isNotTerminal() && steps++ < 5000) {
                    int placer = state.getCurrentPlayer();
                    // placement k (from 0) is by player k mod n
                    assertEquals("placer of tile " + placements, placements % n, placer);
                    boolean someoneRunOut = false;
                    for (int p = 0; p < n; p++)
                        someoneRunOut |= speciesRunOut(state, p) >= 2;
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    assertFalse("no legal action at step " + steps, actions.isEmpty());
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                    while (!turnOver(state) && steps++ < 5000) {
                        actions = fm.computeAvailableActions(state);
                        assertFalse("no legal action at step " + steps, actions.isEmpty());
                        fm.next(state, actions.get(rnd.nextInt(actions.size())));
                    }
                    turns[placer]++;
                    placements++;
                    if (someoneRunOut)
                        turnsAfterRunningOut++;
                    if (placements < total)
                        assertTrue(n + " players, seed " + seed + ": game ended after only " + placements + " of "
                                + total + " tiles", state.isNotTerminal());
                }
                String which = n + " players, seed " + seed;
                assertFalse(which + ": did not end within 5000 actions", state.isNotTerminal());
                assertEquals(which, CoreConstants.GameResult.GAME_END, state.getGameStatus());
                assertEquals(which, total, placements);
                assertEquals(which, total, state.getTilesPlaced());
                assertEquals(which, 0, unplacedCount(state));
                for (int p = 0; p < n; p++) {
                    assertEquals(which + ": turns of player " + p, total / n, turns[p]);
                    assertNotNull(which + ": result of player " + p, state.getPlayerResults()[p]);
                }
                assertTokensConserved(state, which + " at the end");
            }
        assertTrue("no player ran out of two species in any game", turnsAfterRunningOut > 0);
    }

    @Test
    public void tilesAndCreatureTokensAreConservedThroughARandomGame() {
        // seed 26 gives displacement decisions and turns with several Snails
        Game game = newGame(3, 26);
        VentlifeGameState state = (VentlifeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // every species in play, so every placement and covering rule is exercised
        useSpecies(state, Species.values());
        Random rnd = new Random(26);
        int steps = 0, plateaus = 0, creaturesPlaced = 0, returns = 0, decisions = 0, multiSnailTurns = 0;
        // the Volcano Snails placed in the current turn, in order
        List<Hex> snailsThisTurn = new ArrayList<>();
        while (state.isNotTerminal() && steps++ < 5000) {
            int supplyBefore = totalSupply(state);
            if (state.getGamePhase() == VentlifeGameState.Phase.PLACE_CREATURES)
                creaturesPlaced++;
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse("no legal action at step " + steps, actions.isEmpty());
            if (assertDecisionByOwner(state, actions))
                decisions++;
            assertSnailFollowOn(state, actions, snailsThisTurn);
            // half the time, prefer a plateau (Smoker on a covered position) when one is offered
            List<AbstractAction> onTop = actions.stream()
                    .filter(a -> a instanceof PlaceTile pt && state.getCell(pt.smoker) != null).toList();
            AbstractAction action = !onTop.isEmpty() && rnd.nextBoolean()
                    ? onTop.get(rnd.nextInt(onTop.size())) : actions.get(rnd.nextInt(actions.size()));
            if (onTop.contains(action))
                plateaus++;
            if (action instanceof PlaceCreature pc && pc.species == SNAIL) {
                // Snails are placed on Basalt, each after the first next to one placed this turn, at most
                // maxSnailsPerTurn a turn
                assertEquals("snail placed off Basalt at step " + steps, BASALT, state.getCell(pc.hex).terrain());
                assertTrue("snail not next to this turn's snails at step " + steps, snailsThisTurn.isEmpty()
                        || snailsThisTurn.stream().anyMatch(h -> distance(h, pc.hex) == 1));
                snailsThisTurn.add(pc.hex);
                assertTrue("too many snails in one turn", snailsThisTurn.size() <= state.getParams().maxSnailsPerTurn);
            }
            if (action instanceof PlaceCreature pc && pc.species == OCTOPUS)
                assertTrue("octopus placed off an edge at step " + steps, state.isEdge(pc.hex));
            fm.next(state, action);
            if (turnOver(state)) {
                if (snailsThisTurn.size() > 1)
                    multiSnailTurns++;
                snailsThisTurn.clear();
            }
            int placed = state.getTilesPlaced();
            assertEquals("tiles at step " + steps, 48, unplacedCount(state) + placed);
            assertTrue("field positions at step " + steps, state.getField().size() <= 3 * placed);
            for (HexCell cell : state.getField().values()) {
                assertTrue("tile id " + cell.tileId() + " with " + placed + " placed", cell.tileId() < placed);
                assertTrue(cell.level() >= 1);
            }
            // while an owner decides where a covered creature goes, it is held by the decision, not on the field
            if (!state.isActionInProgress())
                assertTokensConserved(state, "step " + steps);
            for (Map.Entry<Hex, Creature> e : state.getCreatures().entrySet()) {
                Creature c = e.getValue();
                HexCell cell = state.getCell(e.getKey());
                String where = c.species() + " at " + e.getKey() + " on " + cell;
                // worms only ever on a Black Smoker (covered ones climb or return); only worms stack
                if (c.species() == WORM)
                    assertEquals("worm off a Smoker at " + e.getKey(), SMOKER, cell.terrain());
                else
                    assertEquals("stacked " + where, 1, c.count());
                // placement and displacement keep each species on its terrain (default: Smokers for worms and
                // shrimp only); crabs only ever move between hexes of one level >= 2
                switch (c.species()) {
                    case VENT_SHRIMP -> assertNotEquals(where, BASALT, cell.terrain());
                    case EELPOUT_FISH, VENT_SPONGE, VOLCANO_SNAIL, OCTOPUS ->
                            assertNotEquals(where, SMOKER, cell.terrain());
                    case YETI_CRAB -> {
                        assertNotEquals(where, SMOKER, cell.terrain());
                        assertTrue(where, cell.level() >= 2);
                    }
                    default -> {
                    }
                }
            }
            if ((action instanceof PlaceTile || action instanceof Displace) && totalSupply(state) > supplyBefore)
                returns++;
        }
        assertTrue("no displacement decision arose", decisions > 0);
        assertTrue("no turn placed more than one snail", multiSnailTurns > 0);
        assertFalse("game did not end", state.isNotTerminal());
        assertEquals(48, state.getTilesPlaced());
        // plateaus are preferred whenever offered, so the stacking and covering are exercised as well
        assertTrue("no plateau was placed", plateaus > 0);
        assertTrue("no creature was placed", creaturesPlaced > 0);
        assertTrue("no covered creature returned to a supply", returns > 0);
    }

    /**
     * If the legal actions are a displacement decision: they are all Displace of one covered creature, at least two
     * (one destination is automatic), asked of the creature's owner. Returns whether they were a decision.
     */
    private static boolean assertDecisionByOwner(VentlifeGameState state, List<AbstractAction> actions) {
        if (!(actions.get(0) instanceof Displace first))
            return false;
        assertTrue("a single destination offered as a decision: " + actions, actions.size() >= 2);
        for (AbstractAction a : actions) {
            assertTrue("mixed decision " + actions, a instanceof Displace d && d.creature.equals(first.creature)
                    && d.from.equals(first.from));
        }
        assertEquals("decision not asked of the owner", first.creature.owner(), state.getCurrentPlayer());
        return true;
    }

    /**
     * If StopPlacing is offered (the Snail follow-on): at least one Snail was placed this turn, and every other action
     * is a Snail on an empty Basalt hex next to one of them.
     */
    private static void assertSnailFollowOn(VentlifeGameState state, List<AbstractAction> actions, List<Hex> snailsThisTurn) {
        if (!actions.contains(new StopPlacing()))
            return;
        assertFalse("StopPlacing before any snail: " + actions, snailsThisTurn.isEmpty());
        for (AbstractAction a : actions) {
            if (a instanceof StopPlacing)
                continue;
            assertTrue("not a snail in the follow-on: " + a, a instanceof PlaceCreature pc && pc.species == SNAIL);
            Hex h = ((PlaceCreature) a).hex;
            assertTrue("follow-on snail off empty Basalt: " + a,
                    state.isEmptyHex(h) && state.getCell(h).terrain() == BASALT);
            assertTrue("follow-on snail not next to this turn's: " + a,
                    snailsThisTurn.stream().anyMatch(s -> distance(s, h) == 1));
        }
    }

    private static int totalSupply(VentlifeGameState state) {
        int total = 0;
        for (int p = 0; p < state.getNPlayers(); p++)
            for (Species s : Species.values())
                total += state.getSupply(p, s);
        return total;
    }
}
