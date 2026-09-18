package games.blackjack;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.blackjack.actions.Bet;
import games.blackjack.actions.DoubleDown;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Insurance;
import games.blackjack.actions.Split;
import games.blackjack.actions.Stand;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.FrenchCardType.Ace;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Betting;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackGameState.handValue;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration: real games from the factory with splitting, driven only by fm.next.
 */
public class BlackjackSplitGameFlowTest {

    static final Set<AbstractAction> HIT_OR_STAND = Set.of(new Hit(), new Stand());
    static final Set<AbstractAction> HIT_STAND_OR_SPLIT = Set.of(new Hit(), new Stand(), new Split());
    static final Set<AbstractAction> ALL_FOUR = Set.of(new Hit(), new Stand(), new DoubleDown(), new Split());

    private static Set<AbstractAction> legalActions(AbstractForwardModel fm, BlackjackGameState state) {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    @Test
    public void aScriptedTwoHandGameWithAResplitAndSplitAces() {
        BlackjackParameters params = new BlackjackParameters();
        params.setParameterValue("splitting", true);
        params.setParameterValue("doubleDown", true);
        params.setParameterValue("nHands", 2);
        Game game = newGame(2, 13, params);
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();

        // hand 1: dealer 6 + 10. Player 0 bets 2 on 8 + 8 (8 left), player 1 bets 4 on A + A (6 left)
        betAndDeal(state, fm, new int[]{2, 4}, new String[]{"8H 8D", "AH AD"}, "6S", "10D",
                "8S 3C 5D 2H 10S 9D 5C KD KH");
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(ALL_FOUR, legalActions(fm, state));
        fm.next(state, new Split());                // 8 + 8S, 8 + 3C
        assertEquals(6, state.getChips(0));
        assertEquals(HIT_STAND_OR_SPLIT, legalActions(fm, state));     // no double after a split
        fm.next(state, new Split());                // 8 + 5D, 8S + 2H, 8 + 3C
        assertEquals(3, state.getPlayerHands(0).size());
        assertEquals(setOf("8S 2H"), setOf(state.getPlayerHand(0, 1)));
        assertEquals(setOf("8D 3C"), setOf(state.getPlayerHand(0, 2)));
        assertEquals(4, state.getChips(0));
        assertEquals(HIT_OR_STAND, legalActions(fm, state));
        fm.next(state, new Hit());                  // 8 + 5 + 10 = 23, bust
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getActiveHand());
        fm.next(state, new Hit());                  // 8 + 2 + 9 = 19
        fm.next(state, new Stand());
        assertEquals(2, state.getActiveHand());
        fm.next(state, new Stand());                // 11
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
        assertEquals(ALL_FOUR, legalActions(fm, state));
        fm.next(state, new Split());                // A + 5C, A + KD: finished at once

        // the dealer's 16 draws the K: bust. Player 0: bust hand lost, 19 and 11 win 2 each: 4 + 4 + 4 = 12.
        // Player 1: soft 16 wins 4, and A + K (21, not a natural) wins floor(4 x 2.0) = 8: 2 + 8 + 12 = 22
        assertNewHandStarted(state, 1, 0);
        assertArrayEquals(new int[]{12, 22}, state.chips);

        // hand 2: dealer 9 + 8. Bets 2 each leave 10 and 20
        betAndDeal(state, fm, new int[]{2, 2}, new String[]{"7H 7D", "10H 9C"}, "9S", "8D", "7C 4S 2D");
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(ALL_FOUR, legalActions(fm, state));
        fm.next(state, new Split());                // 7 + 7C (a pair again), 7 + 4S
        assertEquals(8, state.getChips(0));
        assertEquals(HIT_STAND_OR_SPLIT, legalActions(fm, state));
        fm.next(state, new Stand());                // 14
        assertEquals(1, state.getActiveHand());
        assertEquals(HIT_OR_STAND, legalActions(fm, state));   // 7 + 4 = 11, but no double after a split
        fm.next(state, new Hit());                  // 13
        fm.next(state, new Stand());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Stand());                // 19

        // the dealer stands on 17: 14 and 13 lose: 8. 19 wins 2: 20 + 4 = 24
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("9S 8D"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{8, 24}, state.chips);
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    // ---------------------------------------------------------------- random games

    /**
     * The 13 ranks, each with its 4 cards.
     */
    private static List<List<FrenchCard>> byRank() {
        Map<String, List<FrenchCard>> ranks = new LinkedHashMap<>();
        for (FrenchCard c : FULL_DECK)
            ranks.computeIfAbsent(c.type + ":" + c.number, k -> new ArrayList<>()).add(c);
        return new ArrayList<>(ranks.values());
    }

    /**
     * At the start of a hand, stack the draw deck so that every player dealt in (chips for the minimum bet) gets a pair
     * of a different random rank, and the other two cards of each pair's rank follow the deal in seat order - so
     * that splits and resplits are frequent. The up card and hole card are random cards of the other ranks, and the
     * rest of the draw deck follows, low cards first, so that hands take many cards and a 7-player hand sometimes runs
     * through the draw deck (Hit, Split and the dealer's draws all do, over these seeds). Moves cards only (stackDrawDeck).
     */
    private static void stackPairs(BlackjackGameState state, Random rnd) {
        List<Integer> dealtIn = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++)
            if (state.getChips(p) >= 2) dealtIn.add(p);
        List<List<FrenchCard>> ranks = byRank();
        Collections.shuffle(ranks, rnd);
        List<FrenchCard> firsts = new ArrayList<>(), seconds = new ArrayList<>(), extras = new ArrayList<>();
        for (int i = 0; i < dealtIn.size(); i++) {
            List<FrenchCard> suits = new ArrayList<>(ranks.get(i));
            Collections.shuffle(suits, rnd);
            firsts.add(suits.get(0));
            seconds.add(suits.get(1));
            extras.add(suits.get(2));
            extras.add(suits.get(3));
        }
        List<FrenchCard> order = new ArrayList<>(firsts);
        order.add(ranks.get(dealtIn.size()).get(rnd.nextInt(4)));          // up card
        order.addAll(seconds);
        order.add(ranks.get(dealtIn.size() + 1).get(rnd.nextInt(4)));      // hole card
        order.addAll(extras);
        // then the rest of the draw deck, low cards first, so that hands take many cards
        List<FrenchCard> rest = new ArrayList<>(state.getDrawDeck().getComponents());
        for (FrenchCard c : order) rest.remove(c);
        Collections.shuffle(rest, rnd);
        rest.sort(Comparator.comparingInt(BlackjackGameState::cardValue));
        order.addAll(rest);
        stackDrawDeck(state, order);
    }

    /**
     * Every sum of one outcome per hand: lost, pushed, won 1:1 or paid payout21, on that hand's bet.
     */
    private static Set<Integer> allowedNets(List<Integer> handBets, double payout21) {
        Set<Integer> sums = new HashSet<>(Set.of(0));
        for (int b : handBets) {
            Set<Integer> next = new HashSet<>();
            for (int s : sums)
                for (int x : new int[]{-b, 0, b, (int) Math.floor(b * payout21)})
                    next.add(s + x);
            sums = next;
        }
        return sums;
    }

    private static int cardsInPlay(BlackjackGameState state) {
        int n = 0;
        for (Deck<FrenchCard> d : allDecks(state)) n += d.getSize();
        return n;
    }

    private static boolean sameRank(FrenchCard a, FrenchCard b) {
        return a.type == b.type && a.number == b.number;
    }

    @Test
    public void randomGamesWithSplittingKeepEveryCardAndChipAccountedFor() {
        int splits = 0, fourHands = 0, aceSplits = 0, packsAdded = 0, newHandsWithExtraPacks = 0;
        for (boolean naturalOnly : new boolean[]{false, true}) {
            double payout21 = naturalOnly ? 1.5 : 2.0;
            for (long seed = 0; seed < 25; seed++) {
                int nPlayers = 7;
                String game0 = "mode " + naturalOnly + ", seed " + seed;
                BlackjackParameters params = new BlackjackParameters();
                params.setParameterValue("splitting", true);
                params.setParameterValue("doubleDown", true);
                params.setParameterValue("dealerHitsSoft17", true);
                params.setParameterValue("nHands", 3);
                if (naturalOnly)
                    pagatNaturals(params);
                Game game = newGame(nPlayers, seed, params);
                BlackjackGameState state = (BlackjackGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);

                int packs = 1;
                boolean stacked = false;                    // the draw deck has been stacked for the current hand
                int[] start = state.chips.clone();          // chips at the start of the current hand
                List<List<Integer>> handBets = new ArrayList<>();   // the bet on each hand, as the actions made them
                for (int p = 0; p < nPlayers; p++) handBets.add(new ArrayList<>(List.of(0)));
                int[] bought = new int[nPlayers];           // insurance bought in the current hand
                Set<Integer> splitAces = new HashSet<>();   // players who split Aces in the current hand
                int handsPlayed = 0, steps = 0;
                while (state.isNotTerminal() && steps++ < 5000) {
                    String where = game0 + ", hand " + (handsPlayed + 1) + ", step " + steps;
                    if (state.getGamePhase() == Betting && !stacked) {
                        stackPairs(state, rnd);
                        stacked = true;
                    }
                    int player = state.getCurrentPlayer();
                    boolean inPlay = state.getGamePhase() == Play;
                    int active = state.getActiveHand();
                    int nHandsBefore = state.getPlayerHands(player).size();
                    int chipsBefore = state.getChips(player);
                    int stakeBefore = state.getBet(player, active);
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    boolean acePair = false;
                    if (inPlay) {
                        assertFalse(where + ": player " + player + " plays on after splitting Aces", splitAces.contains(player));
                        // the oracle, from the rules: DoubleDown on an unsplit two-card non-natural, Split on a pair
                        // under the hand limit; each needs chips for the hand's bet
                        List<FrenchCard> cards = state.getPlayerHand(player, active).getComponents();
                        Set<AbstractAction> expected = new HashSet<>(HIT_OR_STAND);
                        if (nHandsBefore == 1 && cards.size() == 2 && !isNaturalHand(cards) && chipsBefore >= stakeBefore)
                            expected.add(new DoubleDown());
                        if (cards.size() == 2 && sameRank(cards.get(0), cards.get(1)) && nHandsBefore < 4 && chipsBefore >= stakeBefore)
                            expected.add(new Split());
                        assertEquals(where + ", player " + player + " hand " + active + " of " + nHandsBefore + " " + cards
                                + " bet " + stakeBefore + " chips " + chipsBefore, expected, new HashSet<>(actions));
                        acePair = cards.size() == 2 && cards.get(0).type == Ace && cards.get(1).type == Ace;
                    }

                    // always split when offered; bet the minimum more often than not, so that splits are affordable; mostly hit
                    AbstractAction action;
                    if (actions.contains(new Split()))
                        action = new Split();
                    else if (actions.contains(new Bet(2)) && rnd.nextInt(10) < 6)
                        action = new Bet(2);
                    else if (actions.contains(new Hit()) && rnd.nextInt(4) > 0)
                        action = new Hit();
                    else
                        action = actions.get(rnd.nextInt(actions.size()));

                    if (action instanceof Bet b)
                        handBets.get(player).set(0, b.amount);
                    if (action instanceof Insurance ins && ins.buy)
                        bought[player] = handBets.get(player).get(0) / 2;
                    if (action instanceof DoubleDown)
                        handBets.get(player).set(active, 2 * handBets.get(player).get(active));
                    if (action instanceof Split) {
                        handBets.get(player).add(active + 1, handBets.get(player).get(active));
                        splits++;
                        if (acePair) {
                            aceSplits++;
                            splitAces.add(player);
                        }
                    }
                    int drawDeckBefore = state.getDrawDeck().getSize();
                    int tableBefore = cardsInPlay(state) - drawDeckBefore;

                    fm.next(state, action);

                    // cards: whole packs, and a new one exactly when the draw deck ran out
                    int packsNow = assertCardsConserved(state);
                    assertTrue(where + ": packs " + packs + " -> " + packsNow, packsNow == packs || packsNow == packs + 1);
                    boolean newHand = state.isNotTerminal() && state.getRoundCounter() > handsPlayed;
                    if (!newHand) {
                        int dealt = cardsInPlay(state) - state.getDrawDeck().getSize() - tableBefore;
                        assertEquals(where + ": " + action + " dealt " + dealt + " from a draw deck of " + drawDeckBefore
                                + " - a new pack exactly when the draw deck runs out", drawDeckBefore < dealt, packsNow > packs);
                    }
                    if (packsNow > packs) packsAdded++;
                    packs = packsNow;
                    for (int p = 0; p < nPlayers; p++)
                        assertTrue(where + ": negative chips", state.getChips(p) >= 0);
                    for (int p = 0; p < nPlayers; p++)
                        for (int h = 0; h < state.getPlayerHands(p).size() && state.getPlayerHands(p).size() > 1; h++)
                            assertFalse(where + ": a natural after a split", state.isNatural(p, h));

                    boolean handOver = !state.isNotTerminal() || newHand;
                    if (!handOver) {
                        if (action instanceof Split) {
                            assertEquals(where + ": hands after a split", nHandsBefore + 1, state.getPlayerHands(player).size());
                            assertEquals(where + ": the new hand's bet", stakeBefore, state.getBet(player, active + 1));
                            assertEquals(where + ": chips after a split", chipsBefore - stakeBefore, state.getChips(player));
                            assertEquals(where + ": cards of the split hand", 2, state.getPlayerHand(player, active).getSize());
                            assertEquals(where + ": cards of the new hand", 2, state.getPlayerHand(player, active + 1).getSize());
                            if (state.getPlayerHands(player).size() == 4) fourHands++;
                        }
                        if (inPlay) {
                            // hands in order: how far this player has got after the action
                            int doneUpTo = active + 1;
                            if (action instanceof Hit && handValue(state.getPlayerHand(player, active).getComponents()) <= 21)
                                doneUpTo = active;
                            if (action instanceof Split)
                                doneUpTo = acePair ? active + 2 : active;
                            if (state.getGamePhase() == Play && state.getCurrentPlayer() == player) {
                                assertEquals(where + ": active hand after " + action, doneUpTo, state.getActiveHand());
                            } else {
                                assertEquals(where + ": the turn passed before every hand was played",
                                        state.getPlayerHands(player).size(), doneUpTo);
                                assertEquals(where + ": the next player starts on hand 0", 0, state.getActiveHand());
                            }
                        }
                        for (int p = 0; p < nPlayers; p++) {
                            List<Integer> stateBets = state.bets.get(p);
                            int staked = 0;
                            for (int b : stateBets) staked += b;
                            int b0 = handBets.get(p).get(0);
                            if (state.getGamePhase() == Play && handBets.get(p).size() == 1 && b0 > 0 && staked == 0) {
                                // only a natural paid at once leaves the hand before settlement
                                assertTrue(where + ": bet gone before settlement", naturalOnly);
                                assertEquals(where, start[p] - bought[p] + (int) Math.floor(b0 * payout21), state.getChips(p));
                            } else {
                                int lost = state.getGamePhase() == Play ? bought[p] : 0;
                                assertEquals(where + ": bets of player " + p, handBets.get(p), stateBets);
                                assertEquals(where + ": chips + bets + insurance of player " + p,
                                        start[p] - lost, state.getChips(p) + staked + state.getInsurance(p));
                            }
                        }
                        continue;
                    }

                    handsPlayed++;
                    for (int p = 0; p < nPlayers; p++) {
                        Set<Integer> allowed = new HashSet<>();
                        for (int x : allowedNets(handBets.get(p), payout21)) {
                            if (bought[p] == 0) allowed.add(x);
                            else { allowed.add(x - bought[p]); allowed.add(x + 2 * bought[p]); }
                        }
                        int net = state.getChips(p) - start[p];
                        assertTrue(where + ": player " + p + " bets " + handBets.get(p) + " insured " + bought[p] + " net " + net,
                                allowed.contains(net));
                    }
                    if (newHand) {
                        int first = 0;
                        while (state.getChips(first) < 2) first++;
                        assertNewHandStarted(state, handsPlayed, first, packs);
                        if (packs > 1) newHandsWithExtraPacks++;
                    }
                    start = state.chips.clone();
                    for (int p = 0; p < nPlayers; p++) handBets.set(p, new ArrayList<>(List.of(0)));
                    bought = new int[nPlayers];
                    splitAces.clear();
                    stacked = false;
                }
                assertFalse(game0 + ": did not end within 5000 actions", state.isNotTerminal());
                for (int p = 0; p < nPlayers; p++) {
                    int chips = state.getChips(p);
                    GameResult expected = chips > 10 ? WIN_GAME : chips == 10 ? DRAW_GAME : LOSE_GAME;
                    assertEquals(game0 + ", player " + p, expected, state.getPlayerResults()[p]);
                }
            }
        }
        assertTrue("nobody split", splits > 0);
        assertTrue("nobody reached four hands", fourHands > 0);
        assertTrue("nobody split Aces", aceSplits > 0);
        assertTrue("the draw deck never ran out", packsAdded > 0);
        assertTrue("no hand started with the extra packs gathered", newHandsWithExtraPacks > 0);
    }
}
