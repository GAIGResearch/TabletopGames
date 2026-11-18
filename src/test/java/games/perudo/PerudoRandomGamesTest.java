package games.perudo;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.perudo.actions.Bid;
import games.perudo.actions.Calza;
import games.perudo.actions.Dudo;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static games.perudo.PerudoParameters.PACO;
import static games.perudo.PerudoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Random legal play on factory games for 2 to 6 players, with Palifico and Calza off and on. After every action an
 * oracle from the rules checks each player's dice, which challenges are offered and whether the next round is a
 * Palifico round; every game ends with one winner and the others ranked by elimination order.
 */
public class PerudoRandomGamesTest {

    private static final int STEP_CAP = 20000;

    /** Bids on Pacos, and face bids made over a Paco bid, across all the games: random play must reach both. */
    private int pacoBids, faceBidsOverPacos;
    /** Palifico rounds started, and Palifico rounds opened on Pacos, across all the games. */
    private int palificoRounds, pacoOpeningsInPalifico;
    /** Calzas that gained a die, lost one, or were exact at the maximum, across all the games. */
    private int calzaGains, calzaLosses, calzaAtMaximum;

    @Test
    public void randomGamesConserveDiceAndEndWithOneWinner() {
        int dudos = 0;
        for (int nPlayers = 2; nPlayers <= 6; nPlayers++) {
            for (long seed = 1; seed <= 4; seed++) {
                dudos += playRandomGame(nPlayers, seed * 100 + nPlayers, new PerudoParameters());
            }
        }
        assertTrue(dudos > 0);
        assertTrue("no Paco bid in any game", pacoBids > 0);
        assertTrue("no face bid over a Paco bid in any game", faceBidsOverPacos > 0);
    }

    @Test
    public void randomGamesWithPalificoEndWithOneWinnerAndHavePalificoRounds() {
        for (int nPlayers = 2; nPlayers <= 6; nPlayers++) {
            for (long seed = 1; seed <= 4; seed++) {
                playRandomGame(nPlayers, seed * 100 + nPlayers, palificoParams());
            }
        }
        assertTrue("no Palifico round in any game", palificoRounds > 0);
        assertTrue("no Palifico round opened on Pacos", pacoOpeningsInPalifico > 0);
    }

    @Test
    public void randomGamesWithCalzaEndWithOneWinnerAndCalzasBothGainAndLose() {
        for (int nPlayers = 2; nPlayers <= 6; nPlayers++) {
            for (long seed = 1; seed <= 4; seed++) {
                playRandomGame(nPlayers, seed * 100 + nPlayers, calzaParams());
            }
        }
        assertTrue("no Calza gained a die in any game", calzaGains > 0);
        assertTrue("no Calza lost a die in any game", calzaLosses > 0);
    }

    @Test
    public void randomGamesWithCalzaAndPalificoEndWithOneWinner() {
        for (int nPlayers = 2; nPlayers <= 6; nPlayers++) {
            for (long seed = 1; seed <= 4; seed++) {
                playRandomGame(nPlayers, seed * 100 + nPlayers, calzaPalificoParams());
            }
        }
        assertTrue("no Calza in any game", calzaGains + calzaLosses + calzaAtMaximum > 0);
        assertTrue("no Palifico round in any game", palificoRounds > 0);
    }

    /** @return the number of Dudo calls played */
    private int playRandomGame(int nPlayers, long seed, PerudoParameters params) {
        boolean palifico = (boolean) params.getParameterValue("palifico");
        boolean calza = (boolean) params.getParameterValue("calza");
        int maxDice = (int) params.getParameterValue("nDicePerPlayer");
        String game = nPlayers + " players, seed " + seed + (palifico ? ", palifico" : "") + (calza ? ", calza" : "");
        Game g = newGame(nPlayers, seed, params);
        AbstractForwardModel fm = g.getForwardModel();
        PerudoGameState state = (PerudoGameState) g.getGameState();
        Random rnd = new Random(seed);
        int dudos = 0;
        int diceLost = 0, diceGained = 0;
        int steps = 0;
        while (state.isNotTerminal() && steps++ < STEP_CAP) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse(game + ": no legal actions", actions.isEmpty());
            boolean palificoRound = state.isPalificoRound();
            assertEquals(game + ", step " + steps + ": Dudo offered", state.hasBid(), actions.contains(new Dudo()));
            assertEquals(game + ", step " + steps + ": Calza offered",
                    calza && state.hasBid() && !palificoRound, actions.contains(new Calza()));
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            int[] before = new int[nPlayers];
            boolean[] hadPalifico = new boolean[nPlayers];
            for (int p = 0; p < nPlayers; p++) {
                before[p] = state.getNDice(p);
                hadPalifico[p] = state.hasHadPalifico(p);
            }
            if (palificoRound && state.hasBid())
                for (AbstractAction a : actions)
                    if (a instanceof Bid b)
                        assertTrue(game + ": Palifico raise " + b + " over " + state.getBidQuantity() + " x " + state.getBidFace(),
                                b.face == state.getBidFace() && b.quantity > state.getBidQuantity());
            int caller = state.getCurrentPlayer();
            int bidder = state.getBidder();
            int[] change = new int[nPlayers];
            int starter = -1;       // who starts the next round (before skipping a player who is out)
            if (action instanceof Dudo || action instanceof Calza) {
                // the bid face plus Pacos count; only the Pacos count for a Paco bid, and Pacos are not wild in a
                // Palifico round
                int face = state.getBidFace();
                boolean wild = face != PACO && !palificoRound;
                int count = 0;
                for (int p = 0; p < nPlayers; p++)
                    count += state.getDiceShowing(p, face) + (wild ? state.getDiceShowing(p, PACO) : 0);
                if (action instanceof Dudo) {
                    starter = count < state.getBidQuantity() ? bidder : caller;
                    change[starter] = -1;
                    dudos++;
                } else {
                    starter = caller;
                    if (count != state.getBidQuantity()) {
                        change[caller] = -1;
                        calzaLosses++;
                    } else if (before[caller] < maxDice) {
                        change[caller] = +1;
                        calzaGains++;
                    } else {
                        calzaAtMaximum++;
                    }
                }
            } else {
                assertTrue(game + ": unexpected action " + action, action instanceof Bid);
                Bid bid = (Bid) action;
                if (bid.face == PACO) pacoBids++;
                else if (state.hasBid() && state.getBidFace() == PACO) faceBidsOverPacos++;
                if (palificoRound && !state.hasBid() && bid.face == PACO) pacoOpeningsInPalifico++;
            }

            fm.next(state, action);

            for (int p = 0; p < nPlayers; p++) {
                assertEquals(game + ", step " + steps + ", player " + p + " after " + action,
                        before[p] + change[p], state.getNDice(p));
                if (change[p] < 0) diceLost++;
                if (change[p] > 0) diceGained++;
            }
            if (!state.isNotTerminal()) break;
            if (starter >= 0) {
                // a new round: Palifico if the starter (Dudo loser or Calza caller) now has exactly one die for the
                // first time; a Calza gain leaves the caller with at least two
                boolean expectPalifico = palifico && state.getNDice(starter) == 1 && !hadPalifico[starter];
                assertEquals(game + ", step " + steps + ": Palifico round", expectPalifico, state.isPalificoRound());
                for (int p = 0; p < nPlayers; p++)
                    assertEquals(game + ", step " + steps + ": player " + p + " has had Palifico",
                            hadPalifico[p] || (expectPalifico && p == starter), state.hasHadPalifico(p));
                if (state.getNDice(starter) > 0)
                    assertEquals(game + ", step " + steps + ": next round starter", starter, state.getCurrentPlayer());
                if (expectPalifico) palificoRounds++;
            } else {
                assertEquals(game + ": a bid changed the round type", palificoRound, state.isPalificoRound());
            }
            if (!palifico) assertFalse(game + ": Palifico round with palifico off", state.isPalificoRound());
        }
        assertFalse(game + ": did not end within " + STEP_CAP + " actions", state.isNotTerminal());

        int winner = -1;
        for (int p = 0; p < nPlayers; p++) {
            CoreConstants.GameResult r = state.getPlayerResults()[p];
            if (r == WIN_GAME) {
                assertEquals(game + ": two winners", -1, winner);
                winner = p;
            } else {
                assertEquals(game + ": player " + p, LOSE_GAME, r);
                assertEquals(0, state.getNDice(p));
                assertTrue(state.getEliminationRound(p) >= 0);
            }
        }
        assertTrue(game + ": no winner", winner >= 0);
        assertTrue(state.getNDice(winner) > 0);
        // every die but the winner's is accounted for: lost one at a time to a Dudo or wrong Calza, less those
        // regained by an exact Calza
        assertEquals(game, nPlayers * maxDice - state.getNDice(winner), diceLost - diceGained);
        if (!calza) assertEquals(game, diceLost, dudos);
        assertEquals(1, state.getOrdinalPosition(winner));
        // the losers rank by elimination round, later is better; at most one player is out per round, so the
        // rounds differ
        for (int p = 0; p < nPlayers; p++) {
            if (p == winner) continue;
            int laterOut = 0;
            for (int o = 0; o < nPlayers; o++)
                if (o != winner && o != p && state.getEliminationRound(o) > state.getEliminationRound(p)) laterOut++;
            assertEquals(game + ": player " + p, 2 + laterOut, state.getOrdinalPosition(p));
        }
        return dudos;
    }
}
