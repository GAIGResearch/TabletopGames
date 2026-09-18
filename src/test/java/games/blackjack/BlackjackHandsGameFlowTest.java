package games.blackjack;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.blackjack.actions.Bet;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Insurance;
import games.blackjack.actions.Stand;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Betting;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration: games of several hands from the factory, driven only by fm.next.
 */
public class BlackjackHandsGameFlowTest {

    private static BlackjackParameters hands(int nHands) {
        BlackjackParameters params = new BlackjackParameters();
        params.setParameterValue("nHands", nHands);
        return params;
    }

    @Test
    public void aThreeHandGameInWhichOnePlayerGoesBrokeAndSitsOut() {
        Game game = newGame(3, 3, hands(3));
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();

        // Hand 1. Dealer 7 + 10 = 17. Player 0 bets 10 on 16, hits the K: bust, 0 chips. Player 1 bets 10 on 14,
        // hits the 3: 17 pushes. Player 2 bets 2 on 18 and wins: 12.
        stackDeal(state, new String[]{"10H 6C", "9S 5D", "10C 8H"}, "7S", "10D", "KS 3D");
        fm.next(state, new Bet(10));
        fm.next(state, new Bet(10));
        fm.next(state, new Bet(2));
        assertEquals(Play, state.getGamePhase());
        fm.next(state, new Hit());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Hit());
        fm.next(state, new Stand());
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Stand());

        assertNewHandStarted(state, 1, 1);
        assertArrayEquals(new int[]{0, 10, 12}, state.chips);

        // Hand 2: player 0 sits out. Player 1 bets 4 on 19, player 2 bets 6 on 17; dealer Ace up, 6 in the hole.
        stackDeal(state, new String[]{"10H 9C", "10S 7D"}, "AS", "6D", "4C");
        fm.next(state, new Bet(4));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Bet(6));
        assertEquals(0, state.getPlayerHand(0, 0).getSize());
        assertEquals(52 - 4 - 2, state.getDrawDeck().getSize());
        // insurance to player 1 (6 chips cover 2) then player 2 (6 chips cover 3), who buys it: 3 chips left
        assertEquals(1, state.getCurrentPlayer());
        insure(state, fm, false, true);
        // no dealer Blackjack: the insurance is lost
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertArrayEquals(new int[]{0, 6, 3}, state.chips);
        fm.next(state, new Stand());
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Hit());
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Stand());
        // dealer soft 17 stands. Player 1: 19 wins 1:1 -> 6 + 8 = 14. Player 2: a 3-card 21 wins payout21 2.0 ->
        // 3 + 6 + 12 = 21.
        assertNewHandStarted(state, 2, 1);
        assertArrayEquals(new int[]{0, 14, 21}, state.chips);

        // Hand 3: player 1 bets 10 on 17, player 2 bets 2 on 9 and hits the 8 (17); dealer 9 + 10 = 19 beats both.
        stackDeal(state, new String[]{"10C 7H", "5S 4H"}, "9D", "10S", "8C");
        fm.next(state, new Bet(10));
        fm.next(state, new Bet(2));
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Stand());
        fm.next(state, new Hit());
        assertTrue(state.isNotTerminal());
        fm.next(state, new Stand());

        // three hands played: the game is over. Results are against the 10 starting chips, not the last hand:
        // player 2 lost the last hand but is up overall.
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new int[]{0, 4, 19}, state.chips);
        assertEquals(0, state.getPlayerHand(0, 0).getSize());
        assertArrayEquals(new GameResult[]{LOSE_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void randomGamesOfSeveralHandsInBothModesRunToTheEndWithEveryCardAccountedFor() {
        int fullLength = 0, endedEarly = 0, sitOuts = 0, reshuffles = 0;
        for (boolean naturalOnly : new boolean[]{false, true}) {
            double payout21 = naturalOnly ? 1.5 : 2.0;
            for (int nHands : new int[]{3, 5, 10}) {
                for (int nPlayers : new int[]{1, 3, 7}) {
                    for (long seed = 0; seed < 10; seed++) {
                        String game0 = "mode " + naturalOnly + ", " + nHands + " hands, " + nPlayers + " players, seed " + seed;
                        BlackjackParameters params = hands(nHands);
                        if (naturalOnly)
                            pagatNaturals(params);
                        Game game = newGame(nPlayers, seed, params);
                        BlackjackGameState state = (BlackjackGameState) game.getGameState();
                        AbstractForwardModel fm = game.getForwardModel();
                        Random rnd = new Random(seed);

                        int[] start = state.chips.clone();      // chips at the start of the current hand
                        int[] bet = new int[nPlayers];          // bet placed in the current hand
                        int[] bought = new int[nPlayers];       // insurance bought in the current hand
                        int handsPlayed = 0, steps = 0;
                        while (state.isNotTerminal() && steps++ < 3000) {
                            String where = game0 + ", hand " + (handsPlayed + 1);
                            int player = state.getCurrentPlayer();
                            assertTrue(where + ": player " + player + " sits out but is asked to act", start[player] >= 2);
                            List<AbstractAction> actions = fm.computeAvailableActions(state);
                            if (state.getGamePhase() == Betting)
                                assertEquals(where, betsUpTo(state.getChips(player)), new HashSet<>(actions));
                            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
                            if (action instanceof Bet b)
                                bet[player] = b.amount;
                            if (action instanceof Insurance ins && ins.buy)
                                bought[player] = bet[player] / 2;
                            List<FrenchCard> drawDeckBefore = new ArrayList<>(state.getDrawDeck().getComponents());

                            fm.next(state, action);

                            assertAllCardsPresent(state);
                            for (int p = 0; p < nPlayers; p++)
                                assertTrue(where + ": negative chips", state.getChips(p) >= 0);
                            boolean handOver = !state.isNotTerminal() || state.getRoundCounter() > handsPlayed;
                            if (!handOver) {
                                for (int p = 0; p < nPlayers; p++) {
                                    int chips = state.getChips(p), stake = state.getBet(p, 0);
                                    if (start[p] < 2) {
                                        assertEquals(where + ": sitting out, bet", 0, stake);
                                        assertEquals(where + ": sitting out, cards", 0, state.getPlayerHand(p, 0).getSize());
                                        assertEquals(where + ": sitting out, chips", start[p], chips);
                                    } else if (state.getGamePhase() == Play && stake == 0 && bet[p] > 0) {
                                        // only a natural paid at once leaves the hand before settlement
                                        assertTrue(where + ": bet gone before settlement", naturalOnly);
                                        assertEquals(where, start[p] - bought[p] + (int) Math.floor(bet[p] * payout21), chips);
                                    } else {
                                        // lost insurance is gone once play starts
                                        int lost = state.getGamePhase() == Play ? bought[p] : 0;
                                        assertEquals(where + ": chips + bet + insurance",
                                                start[p] - lost, chips + stake + state.getInsurance(p));
                                    }
                                }
                                continue;
                            }

                            handsPlayed++;
                            for (int p = 0; p < nPlayers; p++) {
                                // the hand's result for p: the bet lost, pushed, won 1:1 or paid payout21; insurance
                                // lost or paid 2:1 on top
                                Set<Integer> allowed = new HashSet<>();
                                int b = bet[p];
                                for (int x : new int[]{-b, 0, b, (int) Math.floor(b * payout21)}) {
                                    if (bought[p] == 0) allowed.add(x);
                                    else { allowed.add(x - bought[p]); allowed.add(x + 2 * bought[p]); }
                                }
                                int net = state.getChips(p) - start[p];
                                assertTrue(where + ": player " + p + " bet " + b + " insured " + bought[p] + " net " + net,
                                        allowed.contains(net));
                            }
                            if (state.isNotTerminal()) {
                                int first = 0;
                                while (state.getChips(first) < 2) first++;
                                assertNewHandStarted(state, handsPlayed, first);
                                assertFalse(where + ": not reshuffled",
                                        keepsRelativeOrder(drawDeckBefore, state.getDrawDeck().getComponents()));
                                reshuffles++;
                                for (int p = 0; p < nPlayers; p++)
                                    if (state.getChips(p) < 2) sitOuts++;
                            }
                            start = state.chips.clone();
                            bet = new int[nPlayers];
                            bought = new int[nPlayers];
                        }
                        assertFalse(game0 + ": did not end within 3000 actions", state.isNotTerminal());

                        boolean anyoneCanBet = false;
                        for (int p = 0; p < nPlayers; p++)
                            anyoneCanBet |= state.getChips(p) >= 2;
                        assertTrue(game0 + ": " + handsPlayed + " hands played",
                                handsPlayed == nHands || (handsPlayed < nHands && !anyoneCanBet));
                        if (handsPlayed == nHands) fullLength++;
                        else endedEarly++;
                        for (int p = 0; p < nPlayers; p++) {
                            int chips = state.getChips(p);
                            GameResult expected = chips > 10 ? WIN_GAME : chips == 10 ? DRAW_GAME : LOSE_GAME;
                            assertEquals(game0 + ", player " + p, expected, state.getPlayerResults()[p]);
                        }
                    }
                }
            }
        }
        assertTrue("no game played all its hands", fullLength > 0);
        assertTrue("no game ended early", endedEarly > 0);
        assertTrue("no player sat out a hand", sitOuts > 0);
        assertTrue("no new hand was started", reshuffles > 0);
    }
}
