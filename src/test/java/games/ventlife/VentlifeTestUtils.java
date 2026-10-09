package games.ventlife;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import games.GameType;
import games.ventlife.actions.CreatureStep;
import games.ventlife.actions.Displace;
import games.ventlife.actions.PlaceCreature;
import games.ventlife.actions.PlaceTile;
import games.ventlife.actions.StopPlacing;
import games.ventlife.components.*;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

class VentlifeTestUtils {

    static final Terrain SMOKER = Terrain.BLACK_SMOKER;
    static final Terrain BASALT = Terrain.BASALT_RIDGE;
    static final Terrain DIFFUSE = Terrain.DIFFUSE_VENTS;
    static final Terrain MAT = Terrain.MICROBIAL_MAT;
    static final Species WORM = Species.TUBE_WORM;
    static final Species SPONGE = Species.VENT_SPONGE;
    static final Species SHRIMP = Species.VENT_SHRIMP;
    static final Species FISH = Species.EELPOUT_FISH;
    static final Species CRAB = Species.YETI_CRAB;
    static final Species SNAIL = Species.VOLCANO_SNAIL;
    static final Species OCTOPUS = Species.OCTOPUS;

    private VentlifeTestUtils() {
    }

    /** A game with random players, and default parameters when params is null. */
    static Game newGame(int nPlayers, long seed, VentlifeParameters params) {
        Game game = GameType.Ventlife.createGameInstance(nPlayers, seed, params == null ? new VentlifeParameters() : params);
        List<AbstractPlayer> players = IntStream.range(0, nPlayers)
                .mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList();
        game.reset(players);
        return game;
    }

    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, null);
    }

    /** A directly instantiated state, set up with the given seed. */
    static VentlifeGameState newState(int nPlayers, long seed, VentlifeParameters params) {
        VentlifeParameters p = params == null ? new VentlifeParameters() : params;
        p.setRandomSeed(seed);
        VentlifeGameState state = new VentlifeGameState(p, nPlayers);
        new VentlifeForwardModel().setup(state);
        return state;
    }

    static VentlifeGameState newState(int nPlayers, long seed) {
        return newState(nPlayers, seed, null);
    }

    static Hex hex(int q, int r) {
        return new Hex(q, r);
    }

    static VentTile tile(Terrain left, Terrain right) {
        return new VentTile(left, right);
    }

    /** The tile the current player holds. */
    static VentTile heldTile(VentlifeGameState state) {
        return state.getHand(state.getCurrentPlayer()).peek();
    }

    /** PlaceTile of the tile the current player holds. */
    static PlaceTile placeTile(VentlifeGameState state, Hex smoker, int orientation) {
        return new PlaceTile(heldTile(state), smoker, orientation);
    }

    /**
     * Plays a whole turn from its tile placement: the action, then the first legal Displace for every displacement
     * decision it raises (whichever player is asked), then, if the player is asked to place creatures, the first legal
     * placement, and StopPlacing if that was a Volcano Snail with more on offer (fixed choices for tests that do not
     * care about them).
     */
    static void takeTurn(VentlifeGameState state, AbstractForwardModel fm, AbstractAction action) {
        fm.next(state, action);
        resolveDisplacements(state, fm);
        if (state.isNotTerminal() && state.getGamePhase() == VentlifeGameState.Phase.PLACE_CREATURES) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
            stopPlacing(state, fm);
        }
    }

    /**
     * While the current player is still in the creature step after a placement (the Volcano Snail follow-on), takes
     * StopPlacing if offered, otherwise the first legal action. At most maxSnailsPerTurn steps.
     */
    static void stopPlacing(VentlifeGameState state, AbstractForwardModel fm) {
        for (int guard = 0; guard < 5 && state.isNotTerminal()
                && state.getGamePhase() == VentlifeGameState.Phase.PLACE_CREATURES; guard++) {
            List<AbstractAction> legal = fm.computeAvailableActions(state);
            fm.next(state, legal.contains(new StopPlacing()) ? new StopPlacing() : legal.get(0));
        }
        if (state.isNotTerminal() && state.getGamePhase() == VentlifeGameState.Phase.PLACE_CREATURES)
            fail("the creature step did not end");
    }

    /**
     * Plays a whole turn: the tile, then the given creature placement, which must be offered to the same player in
     * the creature step. A Volcano Snail placement may leave the player in the follow-on (the test continues it).
     */
    static void takeTurn(VentlifeGameState state, AbstractForwardModel fm, PlaceTile tileAction, PlaceCreature creature) {
        int player = state.getCurrentPlayer();
        fm.next(state, tileAction);
        assertEquals("creature step after " + tileAction, VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals("creature step player", player, state.getCurrentPlayer());
        List<AbstractAction> legal = fm.computeAvailableActions(state);
        assertTrue(creature + " is not among " + legal, legal.contains(creature));
        fm.next(state, creature);
    }

    /**
     * While the game waits for a displacement decision (the legal actions are Displace), takes the first legal one.
     * At most 10 decisions (a tile covers 3 creatures).
     */
    static void resolveDisplacements(VentlifeGameState state, AbstractForwardModel fm) {
        for (int guard = 0; guard < 10 && state.isNotTerminal(); guard++) {
            List<AbstractAction> legal = fm.computeAvailableActions(state);
            if (legal.isEmpty() || !(legal.get(0) instanceof Displace))
                return;
            fm.next(state, legal.get(0));
        }
        if (state.isNotTerminal())
            fail("displacement decisions did not end");
    }

    /**
     * No covering decision is waiting after a tile: either nothing is in progress, or only the current player's
     * creature step, not yet begun.
     */
    static void assertNoCoveringDecision(VentlifeGameState state) {
        if (state.getGamePhase() == VentlifeGameState.Phase.PLACE_CREATURES)
            assertEquals(new CreatureStep(state.getCurrentPlayer()), state.currentActionInProgress());
        else
            assertFalse(state.isActionInProgress());
    }

    /**
     * Starts the creature step of the current player (arranging a field without playing a tile), as the forward
     * model does after a tile: the phase, and the CreatureStep sequence that offers the placements.
     */
    static void startCreatureStep(VentlifeGameState state) {
        state.setGamePhase(VentlifeGameState.Phase.PLACE_CREATURES);
        state.setActionInProgress(new CreatureStep(state.getCurrentPlayer()));
    }

    /**
     * Writes the top hex of a position straight into the field (no tile, no rules): for scoring tests only, never
     * under a tile conservation check.
     */
    static void putCell(VentlifeGameState state, Hex hex, Terrain terrain, int level) {
        state.setCell(hex, new HexCell(terrain, level, 0));
    }

    static PlaceCreature shrimp(Hex hex) {
        return new PlaceCreature(SHRIMP, hex, 1);
    }

    static PlaceCreature fish(Hex hex) {
        return new PlaceCreature(FISH, hex, 1);
    }

    static PlaceCreature crab(Hex hex) {
        return new PlaceCreature(CRAB, hex, 1);
    }

    /** The decision to move the owner's single covered creature of the species from the covered position to another. */
    static Displace displace(Species species, int owner, Hex from, Hex to) {
        return new Displace(new Creature(species, owner, 1), from, to);
    }

    /** The legal actions as a set, checking the list has no duplicates. */
    static Set<AbstractAction> legalSet(VentlifeGameState state, AbstractForwardModel fm) {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        Set<AbstractAction> retValue = new HashSet<>(actions);
        assertEquals("duplicate actions in " + actions, actions.size(), retValue.size());
        return retValue;
    }

    static PlaceCreature worm(Hex hex, int count) {
        return new PlaceCreature(WORM, hex, count);
    }

    static PlaceCreature sponge(Hex hex) {
        return new PlaceCreature(SPONGE, hex, 1);
    }

    static PlaceCreature snail(Hex hex) {
        return new PlaceCreature(SNAIL, hex, 1);
    }

    static PlaceCreature octopus(Hex hex) {
        return new PlaceCreature(OCTOPUS, hex, 1);
    }

    /**
     * The "Basalt chain" field (2 players, three seafloor tiles, all level 1), after which player 1 is to place a tile:
     * tile 0 B/B at ((0,0),0): (0,0) S, (0,1) B, (1,0) B [player 0];
     * tile 1 B/B at ((3,-1),1): (3,-1) S, (2,0) B, (3,0) B [player 1];
     * tile 2 D/B at ((0,-1),2): (0,-1) S, (-1,-1) D, (-1,0) B [player 0].
     * Basalt: the chain (0,1)-(1,0)-(2,0)-(3,0), and (-1,0) on its own. Player 1's own tile is then D/M at ((1,-2),0):
     * (1,-2) S, (1,-1) D, (2,-2) M - no Basalt (see placeChainTurnTile).
     */
    static void buildBasaltChain(VentlifeGameState state, AbstractForwardModel fm) {
        place(state, fm, BASALT, BASALT, hex(0, 0), 0);
        place(state, fm, BASALT, BASALT, hex(3, -1), 1);
        place(state, fm, DIFFUSE, BASALT, hex(0, -1), 2);
        assertEquals(1, state.getCurrentPlayer());
    }

    /** Player 1's tile on the Basalt chain field, D/M at ((1,-2),0), which brings the creature step. */
    static void placeChainTurnTile(VentlifeGameState state, AbstractForwardModel fm) {
        giveTile(state, 1, DIFFUSE, MAT);
        fm.next(state, placeTile(state, hex(1, -2), 0));
    }

    /**
     * Puts exactly these species in play, each player holding tokensPerSpecies of each and none of the others (no
     * species: the creature step never arises, for building a field). Only before any creature is on the field.
     */
    static void useSpecies(VentlifeGameState state, Species... species) {
        assertTrue("creatures already on the field", state.getCreatures().isEmpty());
        state.speciesInPlay = new ArrayList<>(List.of(species));
        int tokens = state.getParams().tokensPerSpecies;
        for (int p = 0; p < state.getNPlayers(); p++) {
            Arrays.fill(state.supply[p], 0);
            for (Species s : species)
                state.supply[p][s.ordinal()] = tokens;
        }
    }

    /** Puts count creatures of the player's on the hex, taken from their supply (conserves tokens; no rules checked). */
    static void putCreature(VentlifeGameState state, Hex hex, Species species, int owner, int count) {
        state.setCreature(hex, new Creature(species, owner, count));
        state.changeSupply(owner, species, -count);
    }

    /**
     * Token conservation: for every player and species in play, supply + tokens on the field = tokensPerSpecies; no
     * tokens of species not in play anywhere; every creature on a covered position with a count of at least 1.
     */
    static void assertTokensConserved(VentlifeGameState state, String when) {
        int tokens = state.getParams().tokensPerSpecies;
        for (Map.Entry<Hex, Creature> e : state.getCreatures().entrySet()) {
            assertTrue(when + ": creature on uncovered " + e.getKey(), state.getField().containsKey(e.getKey()));
            assertTrue(when + ": empty creature at " + e.getKey(), e.getValue().count() >= 1);
        }
        for (int p = 0; p < state.getNPlayers(); p++)
            for (Species s : Species.values()) {
                int onField = 0;
                for (Creature c : state.getCreatures().values())
                    if (c.owner() == p && c.species() == s)
                        onField += c.count();
                int expected = state.getSpeciesInPlay().contains(s) ? tokens : 0;
                assertEquals(when + ": " + s + " of player " + p, expected, state.getSupply(p, s) + onField);
            }
    }

    /**
     * Places the first nTiles (1 to 6) of the standard field used across the tests, a whole turn each:
     * tile 0 B/D at ((0,0),1): (0,0) S, (-1,1) B, (0,1) D - level 1;
     * tile 1 D/M at ((1,0),0): (1,0) S, (1,1) D, (2,0) M - level 1;
     * tile 2 M/B at ((0,0),0): (0,0) S, (0,1) M, (1,0) B - level 2;
     * tile 3 B/M at ((-1,0),2): (-1,0) S, (-2,0) B, (-2,1) M - level 1;
     * tile 4 D/B at ((-1,0),1): (-1,0) S, (-2,1) D, (-1,1) B - level 2;
     * tile 5 M/D at ((0,0),1): (0,0) S, (-1,1) M, (0,1) D - level 3.
     * After all six the top hexes are (0,0) S3, (-1,1) M3, (0,1) D3, (1,0) B2, (1,1) D1, (2,0) M1, (-1,0) S2,
     * (-2,0) B1, (-2,1) D2.
     */
    static void buildStandardField(VentlifeGameState state, AbstractForwardModel fm, int nTiles) {
        Object[][] tiles = {
                {BASALT, DIFFUSE, hex(0, 0), 1}, {DIFFUSE, MAT, hex(1, 0), 0}, {MAT, BASALT, hex(0, 0), 0},
                {BASALT, MAT, hex(-1, 0), 2}, {DIFFUSE, BASALT, hex(-1, 0), 1}, {MAT, DIFFUSE, hex(0, 0), 1}};
        for (int i = 0; i < nTiles; i++)
            place(state, fm, (Terrain) tiles[i][0], (Terrain) tiles[i][1], (Hex) tiles[i][2], (Integer) tiles[i][3]);
    }

    /**
     * Gives the player the tile (left, right) by swapping: a matching tile is taken from the draw deck and the
     * player's current tile goes into the draw deck at the same index. Conserves the tiles; fails if the player does
     * not hold it and the draw deck has none.
     */
    static void giveTile(VentlifeGameState state, int player, Terrain left, Terrain right) {
        Deck<VentTile> hand = state.getHand(player);
        VentTile wanted = tile(left, right);
        if (hand.getSize() == 1 && hand.get(0).equals(wanted))
            return;
        Deck<VentTile> deck = state.getDrawDeck();
        for (int i = 0; i < deck.getSize(); i++) {
            if (deck.get(i).equals(wanted)) {
                VentTile found = deck.get(i);
                deck.remove(i);
                if (hand.getSize() > 0) {
                    VentTile old = hand.draw();
                    deck.add(old, i);
                }
                hand.add(found);
                return;
            }
        }
        fail("no " + wanted + " left in the draw deck to give player " + player);
    }

    /**
     * Moves tiles of the given kinds (pairs left, right) within the draw deck so that they are its top tiles, the
     * first pair on top. Conserves the tiles; fails if the draw deck lacks one.
     */
    static void stackDrawDeck(VentlifeGameState state, VentTile... topFirst) {
        Deck<VentTile> deck = state.getDrawDeck();
        for (int k = topFirst.length - 1; k >= 0; k--) {
            // the tiles already stacked are at the top (indices below alreadyStacked): search below them
            int alreadyStacked = topFirst.length - 1 - k;
            int idx = -1;
            for (int i = alreadyStacked; i < deck.getSize(); i++)
                if (deck.get(i).equals(topFirst[k])) {
                    idx = i;
                    break;
                }
            if (idx < 0)
                fail("no " + topFirst[k] + " in the draw deck below the stacked tiles");
            VentTile t = deck.get(idx);
            deck.remove(idx);
            deck.add(t);
        }
        for (int k = 0; k < topFirst.length; k++)
            assertTrue("draw deck stacking failed", deck.get(k).equals(topFirst[k]));
    }

    /** Gives the current player the tile (left, right) and plays PlaceTile(smoker, orientation) as their turn. */
    static void place(VentlifeGameState state, AbstractForwardModel fm, Terrain left, Terrain right,
                      Hex smoker, int orientation) {
        giveTile(state, state.getCurrentPlayer(), left, right);
        takeTurn(state, fm, placeTile(state, smoker, orientation));
    }

    /** As place, with the given creature placement as the turn's creature step. */
    static void place(VentlifeGameState state, AbstractForwardModel fm, Terrain left, Terrain right,
                      Hex smoker, int orientation, PlaceCreature creature) {
        giveTile(state, state.getCurrentPlayer(), left, right);
        takeTurn(state, fm, placeTile(state, smoker, orientation), creature);
    }

    /** The tiles in the draw deck and every hand, counted by kind. */
    static Map<VentTile, Integer> unplacedTiles(VentlifeGameState state) {
        Map<VentTile, Integer> retValue = new HashMap<>();
        for (VentTile t : state.getDrawDeck().getComponents())
            retValue.merge(t, 1, Integer::sum);
        for (int p = 0; p < state.getNPlayers(); p++)
            for (VentTile t : state.getHand(p).getComponents())
                retValue.merge(t, 1, Integer::sum);
        return retValue;
    }

    /** The number of tiles in the draw deck and every hand. */
    static int unplacedCount(VentlifeGameState state) {
        int n = state.getDrawDeck().getSize();
        for (int p = 0; p < state.getNPlayers(); p++)
            n += state.getHand(p).getSize();
        return n;
    }

    /**
     * Every seafloor placement of the tile on a field covering the given positions, enumerated from the rules: all three
     * positions uncovered and at least one of them sharing an edge with a covered position. Searches smoker positions
     * with |q|, |r| <= 8, enough for the small fields of the unit tests.
     */
    static Set<PlaceTile> seafloorPlacements(VentTile tile, Set<Hex> covered) {
        Set<PlaceTile> retValue = new HashSet<>();
        for (int q = -8; q <= 8; q++)
            for (int r = -8; r <= 8; r++)
                for (int o = 0; o < 6; o++) {
                    Hex s = hex(q, r);
                    List<Hex> three = List.of(s, s.neighbour((o + 1) % 6), s.neighbour(o));
                    boolean free = three.stream().noneMatch(covered::contains);
                    boolean touching = three.stream().anyMatch(h -> covered.stream().anyMatch(c -> distance(h, c) == 1));
                    if (free && touching)
                        retValue.add(new PlaceTile(tile, s, o));
                }
        return retValue;
    }

    /** The hex distance (number of steps) between two positions. */
    static int distance(Hex a, Hex b) {
        int dq = a.q() - b.q(), dr = a.r() - b.r();
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(dq + dr)) / 2;
    }
}
