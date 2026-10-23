package games.perudo;

import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.perudo.actions.Bid;
import players.simple.RandomPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static games.perudo.PerudoParameters.PACO;

/**
 * Dice are held as a count per face, so {@link #setAllDice} replaces each player's counts outright and may change
 * how many dice a player holds - keep the row totals as the test needs.
 */
class PerudoTestUtils {

    /** A game with seeded random players. */
    static Game newGame(int nPlayers, long seed) {
        Game g = GameType.Perudo.createGameInstance(nPlayers, seed);
        g.reset(IntStream.range(0, nPlayers).mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** A game with seeded random players and the given parameters. */
    static Game newGame(int nPlayers, long seed, PerudoParameters params) {
        Game g = GameType.Perudo.createGameInstance(nPlayers, seed, params);
        g.reset(IntStream.range(0, nPlayers).mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** Parameters that offer every legal bid, with no limit on the quantities. */
    static PerudoParameters allBidsParams() {
        PerudoParameters params = new PerudoParameters();
        params.setParameterValue("minBidPercent", 0);
        params.setParameterValue("maxRaise", 100);
        return params;
    }

    static PerudoParameters palificoParams() {
        PerudoParameters params = allBidsParams();
        params.setParameterValue("palifico", true);
        return params;
    }

    static PerudoParameters calzaParams() {
        PerudoParameters params = allBidsParams();
        params.setParameterValue("calza", true);
        return params;
    }

    static PerudoParameters calzaPalificoParams() {
        PerudoParameters params = calzaParams();
        params.setParameterValue("palifico", true);
        return params;
    }

    /** A state set up directly (no Game) with every legal bid offered and the given seed. */
    static PerudoGameState newState(int nPlayers, long seed) {
        PerudoParameters params = allBidsParams();
        params.setRandomSeed(seed);
        return newState(params, nPlayers);
    }

    /** A state set up directly (no Game) with the given parameters. */
    static PerudoGameState newState(PerudoParameters params, int nPlayers) {
        PerudoGameState state = new PerudoGameState(params, nPlayers);
        new PerudoForwardModel().setup(state);
        return state;
    }

    /** Count of dice per face, written face 1 (Paco) first: faces(2, 0, 1, 0, 2, 0) = two Pacos, a three, two fives. */
    static int[] faces(int... countsByFace) {
        return countsByFace;
    }

    /** Sets every player's dice, one row of {@link #faces} per player in seat order. */
    static void setAllDice(PerudoGameState state, int[]... rows) {
        if (rows.length != state.getNPlayers())
            throw new IllegalArgumentException("Expected " + state.getNPlayers() + " rows, got " + rows.length);
        for (int p = 0; p < rows.length; p++)
            state.setDice(p, rows[p]);
    }

    /** Arranges the player as already out of the game, eliminated in the given round. */
    static void eliminate(PerudoGameState state, int player, int round) {
        state.setDice(player, new int[state.getParams().nFaces]);
        state.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, player);
        state.setEliminationRound(player, round);
    }

    /**
     * The bids on faces 2..nFaces among the actions. Raises include Paco bids too, so tests of face raises compare
     * this set rather than the whole action list.
     */
    static Set<Bid> faceBids(List<AbstractAction> actions) {
        Set<Bid> bids = new HashSet<>();
        for (AbstractAction a : actions)
            if (a instanceof Bid b && b.face != PACO)
                bids.add(b);
        return bids;
    }

    /** The bids on Pacos among the actions. */
    static Set<Bid> pacoBids(List<AbstractAction> actions) {
        Set<Bid> bids = new HashSet<>();
        for (AbstractAction a : actions)
            if (a instanceof Bid b && b.face == PACO)
                bids.add(b);
        return bids;
    }

    /** Every Bid(q, f) with qFrom <= q <= qTo and fFrom <= f <= fTo. */
    static Set<Bid> bidRange(int qFrom, int qTo, int fFrom, int fTo) {
        Set<Bid> bids = new HashSet<>();
        for (int q = qFrom; q <= qTo; q++)
            for (int f = fFrom; f <= fTo; f++)
                bids.add(new Bid(q, f));
        return bids;
    }
}
