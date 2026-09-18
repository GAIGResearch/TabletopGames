package games.blackjack;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.blackjack.actions.Bet;
import games.blackjack.actions.DoubleDown;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Insurance;
import games.blackjack.actions.Stand;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.FrenchCardType.Ace;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Betting;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackGameState.cardValue;
import static games.blackjack.BlackjackGameState.handValue;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration: real games from the factory with dealerHitsSoft17 and doubleDown, driven only by fm.next.
 */
public class BlackjackOptionsGameFlowTest {

    static final Set<AbstractAction> HIT_OR_STAND = Set.of(new Hit(), new Stand());
    static final Set<AbstractAction> HIT_STAND_OR_DOUBLE = Set.of(new Hit(), new Stand(), new DoubleDown());

    private static Set<AbstractAction> legalActions(AbstractForwardModel fm, BlackjackGameState state) {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    @Test
    public void aScriptedTwoHandGameWithDoubling() {
        BlackjackParameters params = new BlackjackParameters();
        params.setParameterValue("doubleDown", true);
        params.setParameterValue("nHands", 2);
        Game game = newGame(3, 11, params);
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();

        // hand 1: dealer 6 + 10. Bets 2, 4, 8 leave 8, 6, 2 chips
        betAndDeal(state, fm, new int[]{2, 4, 8}, new String[]{"5H 6C", "9S 2D", "8H 8D"}, "6S", "10D", "9D 3C 2H 2S");
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(HIT_STAND_OR_DOUBLE, legalActions(fm, state));
        fm.next(state, new DoubleDown());           // 5 + 6 + 9 = 20, bet 4
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(HIT_STAND_OR_DOUBLE, legalActions(fm, state));
        fm.next(state, new DoubleDown());           // 9 + 2 + 3 = 14, bet 8
        assertEquals(2, state.getCurrentPlayer());
        assertArrayEquals(new int[]{6, 2, 2}, state.chips);
        assertEquals(HIT_OR_STAND, legalActions(fm, state));   // bet 8, 2 chips
        fm.next(state, new Hit());                  // 8 + 8 + 2 = 18
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions(fm, state));
        fm.next(state, new Stand());

        // the dealer's 16 draws the 2: 18. 20 wins 4: 6 + 8 = 14; 14 loses 8: 2; 18 pushes: 2 + 8 = 10
        assertNewHandStarted(state, 1, 0);
        assertArrayEquals(new int[]{14, 2, 10}, state.chips);
        assertEquals(betsUpTo(14), legalActions(fm, state));

        // hand 2: dealer 7 + 10. Bets 2 each leave 12, 0, 8
        betAndDeal(state, fm, new int[]{2, 2, 2}, new String[]{"5D 5C", "10H 7C", "4S 3D"}, "7H", "10C", "AS 9C");
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new DoubleDown());           // 5 + 5 + A = 21, bet 4
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions(fm, state));   // no chips left
        fm.next(state, new Stand());                // 17
        assertEquals(HIT_STAND_OR_DOUBLE, legalActions(fm, state));
        fm.next(state, new DoubleDown());           // 4 + 3 + 9 = 16, bet 4

        // the dealer stands on 17. Player 0 has 12 - 2 = 10 left; the 21 wins floor(4 x 2.0) = 8: 10 + 4 + 8 = 22.
        // 17 pushes: 0 + 2 = 2. 16 loses the 4: 6
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("7H 10C"), setOf(state.getDealerHand()));
        assertEquals(setOf("5D 5C AS"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("4S 3D 9C"), setOf(state.getPlayerHand(2, 0)));
        assertArrayEquals(new int[]{22, 2, 6}, state.chips);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aScriptedGameAgainstASoftSeventeenWithEachSetting() {
        for (boolean hitsSoft17 : new boolean[]{false, true}) {
            BlackjackParameters params = new BlackjackParameters();
            params.setParameterValue("dealerHitsSoft17", hitsSoft17);
            Game game = newGame(2, 5, params);
            BlackjackGameState state = (BlackjackGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            // dealer 6 + A = soft 17; player 0 (bet 2) stands on 18, player 1 (bet 4) on 19
            betAndDeal(state, fm, new int[]{2, 4}, new String[]{"10H 8C", "10C 9D"}, "6S", "AD", "2H 4C");
            assertEquals(Play, state.getGamePhase());
            fm.next(state, new Stand());
            fm.next(state, new Stand());
            assertFalse(state.isNotTerminal());
            if (hitsSoft17) {
                // draws the 2: soft 19. 18 loses: 8; 19 pushes: 10
                assertEquals(setOf("6S AD 2H"), setOf(state.getDealerHand()));
                assertEquals(card("4C"), state.getDrawDeck().peek());
                assertArrayEquals(new int[]{8, 10}, state.chips);
                assertArrayEquals(new GameResult[]{LOSE_GAME, DRAW_GAME}, state.getPlayerResults());
            } else {
                // stands on 17: both win 1:1: 8 + 4 = 12, 6 + 8 = 14
                assertEquals(setOf("6S AD"), setOf(state.getDealerHand()));
                assertEquals(card("2H"), state.getDrawDeck().peek());
                assertArrayEquals(new int[]{12, 14}, state.chips);
                assertArrayEquals(new GameResult[]{WIN_GAME, WIN_GAME}, state.getPlayerResults());
            }
            assertAllCardsPresent(state);
        }
    }

    /**
     * The dealer's cards in the order they were dealt: Deck.add puts each later card on top (index 0), so the up
     * card is last and the latest draw first.
     */
    private static List<FrenchCard> inDealOrder(List<FrenchCard> dealerHand) {
        List<FrenchCard> cards = new ArrayList<>(dealerHand);
        Collections.reverse(cards);
        return cards;
    }

    /**
     * Soft: an Ace can count 11 without going over 21. Written out from the rules.
     */
    private static boolean soft(List<FrenchCard> cards) {
        int hard = 0;
        boolean ace = false;
        for (FrenchCard c : cards) {
            hard += cardValue(c);
            ace |= c.type == Ace;
        }
        return ace && hard + 10 <= 21;
    }

    @Test
    public void randomGamesWithBothOptionsRunToTheEndWithEveryCardAccountedFor() {
        int doubles = 0, doubledBusts = 0, notAffordable = 0, soft17Draws = 0, dealerChecks = 0;
        for (boolean naturalOnly : new boolean[]{false, true}) {
            double payout21 = naturalOnly ? 1.5 : 2.0;
            for (int nPlayers : new int[]{1, 3, 7}) {
                for (long seed = 0; seed < 30; seed++) {
                    String game0 = "mode " + naturalOnly + ", " + nPlayers + " players, seed " + seed;
                    BlackjackParameters params = new BlackjackParameters();
                    params.setParameterValue("dealerHitsSoft17", true);
                    params.setParameterValue("doubleDown", true);
                    params.setParameterValue("nHands", 3);
                    if (naturalOnly)
                        pagatNaturals(params);
                    Game game = newGame(nPlayers, seed, params);
                    BlackjackGameState state = (BlackjackGameState) game.getGameState();
                    AbstractForwardModel fm = game.getForwardModel();
                    Random rnd = new Random(seed);

                    int[] start = state.chips.clone();      // chips at the start of the current hand
                    int[] bet = new int[nPlayers];          // the bet on the current hand, doubled if doubled
                    int[] bought = new int[nPlayers];       // insurance bought in the current hand
                    int handsPlayed = 0, steps = 0;
                    while (state.isNotTerminal() && steps++ < 3000) {
                        String where = game0 + ", hand " + (handsPlayed + 1);
                        int player = state.getCurrentPlayer();
                        List<AbstractAction> actions = fm.computeAvailableActions(state);
                        if (state.getGamePhase() == Play) {
                            // DoubleDown exactly on two cards, not a natural, with chips for the bet
                            List<FrenchCard> cards = state.getPlayerHand(player, state.getActiveHand()).getComponents();
                            int stake = state.getBet(player, state.getActiveHand());
                            boolean canDouble = cards.size() == 2 && !isNaturalHand(cards) && state.getChips(player) >= stake;
                            assertEquals(where + ", player " + player + " " + cards + " bet " + stake + " chips " + state.getChips(player),
                                    canDouble ? HIT_STAND_OR_DOUBLE : HIT_OR_STAND, new HashSet<>(actions));
                            if (cards.size() == 2 && !isNaturalHand(cards) && !canDouble)
                                notAffordable++;
                        }
                        AbstractAction action = actions.get(rnd.nextInt(actions.size()));
                        if (action instanceof Bet b)
                            bet[player] = b.amount;
                        if (action instanceof Insurance ins && ins.buy)
                            bought[player] = bet[player] / 2;
                        int chipsBefore = state.getChips(player), stakeBefore = state.getBet(player, 0);
                        if (action instanceof DoubleDown) {
                            bet[player] *= 2;
                            doubles++;
                        }

                        fm.next(state, action);

                        assertAllCardsPresent(state);
                        for (int p = 0; p < nPlayers; p++)
                            assertTrue(where + ": negative chips", state.getChips(p) >= 0);
                        boolean handOver = !state.isNotTerminal() || state.getRoundCounter() > handsPlayed;
                        if (action instanceof DoubleDown && !handOver) {
                            List<FrenchCard> cards = state.getPlayerHand(player, 0).getComponents();
                            assertEquals(where + ": cards after a double", 3, cards.size());
                            assertEquals(where + ": doubled bet", 2 * stakeBefore, state.getBet(player, 0));
                            assertEquals(where + ": chips after a double", chipsBefore - stakeBefore, state.getChips(player));
                            assertTrue(where + ": the turn did not pass after a double",
                                    state.getCurrentPlayer() != player || state.getGamePhase() != Play);
                            if (handValue(cards) > 21) doubledBusts++;
                        }
                        if (!handOver) {
                            for (int p = 0; p < nPlayers; p++) {
                                int chips = state.getChips(p), stake = state.getBet(p, 0);
                                if (state.getGamePhase() == Play && stake == 0 && bet[p] > 0) {
                                    // only a natural paid at once leaves the hand before settlement
                                    assertTrue(where + ": bet gone before settlement", naturalOnly);
                                    assertEquals(where, start[p] - bought[p] + (int) Math.floor(bet[p] * payout21), chips);
                                } else {
                                    int lost = state.getGamePhase() == Play ? bought[p] : 0;
                                    assertEquals(where + ": chips + bet + insurance",
                                            start[p] - lost, chips + stake + state.getInsurance(p));
                                    assertEquals(where + ": bet", bet[p], stake);
                                }
                            }
                            continue;
                        }

                        handsPlayed++;
                        for (int p = 0; p < nPlayers; p++) {
                            // lost, pushed, won 1:1 or paid payout21 on the (doubled) bet; insurance lost or paid 2:1
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
                        }
                        start = state.chips.clone();
                        bet = new int[nPlayers];
                        bought = new int[nPlayers];
                    }
                    assertFalse(game0 + ": did not end within 3000 actions", state.isNotTerminal());

                    // the last hand's cards are still on the table: check the dealer's play with dealerHitsSoft17
                    List<FrenchCard> dealer = inDealOrder(state.getDealerHand().getComponents());
                    boolean anyLive = false;
                    for (int p = 0; p < nPlayers; p++) {
                        List<FrenchCard> hand = state.getPlayerHand(p, 0).getComponents();
                        anyLive |= !hand.isEmpty() && handValue(hand) <= 21 && !(naturalOnly && isNaturalHand(hand));
                    }
                    if (anyLive && !isNaturalHand(dealer)) {
                        dealerChecks++;
                        for (int k = 2; k <= dealer.size(); k++) {
                            List<FrenchCard> sofar = dealer.subList(0, k);
                            int total = handValue(sofar);
                            boolean draws = total < 17 || (total == 17 && soft(sofar));
                            assertEquals(game0 + ": dealer " + dealer + " after " + k + " cards", k < dealer.size(), draws);
                            if (k < dealer.size() && total == 17) soft17Draws++;
                        }
                    }
                    for (int p = 0; p < nPlayers; p++) {
                        int chips = state.getChips(p);
                        GameResult expected = chips > 10 ? WIN_GAME : chips == 10 ? DRAW_GAME : LOSE_GAME;
                        assertEquals(game0 + ", player " + p, expected, state.getPlayerResults()[p]);
                    }
                }
            }
        }
        assertTrue("nobody doubled", doubles > 0);
        assertTrue("no double went bust", doubledBusts > 0);
        assertTrue("never too few chips to double", notAffordable > 0);
        assertTrue("the dealer's play was never checked", dealerChecks > 0);
        assertTrue("the dealer never drew on a soft 17", soft17Draws > 0);
    }
}
