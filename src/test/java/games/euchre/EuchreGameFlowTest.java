package games.euchre;

import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.euchre.actions.CallTrump;
import games.euchre.actions.Discard;
import games.euchre.actions.Pass;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static core.components.FrenchCard.Suite.*;
import static games.euchre.EuchreTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Whole deals and games in real games driven by fm.next: a scripted deal from the first bid to the game's end, and
 * seeded random games checked at every step against oracles written from the rules.
 */
public class EuchreGameFlowTest {

    @Test
    public void aScriptedDealFromTheBiddingToTheEndOfTheGame() {
        Game game = newGame(5);
        EuchreGameState state = (EuchreGameState) game.getGameState();
        EuchreForwardModel fm = (EuchreForwardModel) game.getForwardModel();
        standardDeal(state);   // P0 9S 10S JS AH 9D / P1 QS KS AS 10H 10D / P2 QD AD QH QC 10C / P3 KD KH AC KC JC

        // round 1: everyone passes on the 9H; round 2: player 0 passes, player 1 calls Diamonds
        for (int p = 0; p < 4; p++) {
            assertEquals(p, state.getCurrentPlayer());
            pass(state, fm, 1);
        }
        assertEquals(0, state.getCurrentPlayer());
        pass(state, fm, 1);
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new CallTrump(Diamonds, false));
        assertEquals(1, state.getMaker());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);

        // trick 1: spades led; players 2 and 3 are void and trump, the KD beats the QD: player 3 wins
        playCards(state, fm, "9S");
        assertEquals(plays("QS", "KS", "AS"), available(state, fm));
        playCards(state, fm, "AS", "QD", "KD");
        assertArrayEquals(new int[]{0, 0, 0, 1}, state.tricksTaken);
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(Spades), state.getKnownVoids().get(2));
        assertEquals(Set.of(Spades), state.getKnownVoids().get(3));

        // trick 2: hearts led by player 3, all follow: player 0's AH wins
        playCards(state, fm, "KH", "AH", "10H", "QH");
        assertArrayEquals(new int[]{1, 0, 0, 1}, state.tricksTaken);
        assertEquals(0, state.getCurrentPlayer());

        // trick 3: spades led; 2 and 3 discard clubs (no trumps played), the AC cannot win: player 1's QS wins
        playCards(state, fm, "10S", "QS", "QC", "AC");
        assertArrayEquals(new int[]{1, 1, 0, 1}, state.tricksTaken);
        assertEquals(1, state.getCurrentPlayer());

        // trick 4: spades led; player 2 trumps with the AD; player 0 must follow with the JS
        playCards(state, fm, "KS", "AD", "KC");
        assertEquals(plays("JS"), available(state, fm));
        playCards(state, fm, "JS");
        assertArrayEquals(new int[]{1, 1, 1, 1}, state.tricksTaken);
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(16, state.getDiscardPile().getSize());
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getTeamPoints(0));
        assertEquals(0, state.getTeamPoints(1));

        // trick 5: clubs led; 0 and 1 are void and trump, the 10D beats the 9D: player 1 wins
        playCards(state, fm, "10C", "JC", "9D", "10D");
        assertArrayEquals(new int[]{1, 2, 1, 1}, state.tricksTaken);
        // makers 1+3 took 2 + 1 = 3 -> pointsMade 1; one deal to targetScore 1 ends the game
        assertEquals(0, state.getTeamPoints(0));
        assertEquals(1, state.getTeamPoints(1));
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
        assertEquals(20, state.getDiscardPile().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void aScriptedLoneDealWithThePartnerSittingOut() {
        Game game = newGame(21);
        EuchreGameState state = (EuchreGameState) game.getGameState();
        EuchreForwardModel fm = (EuchreForwardModel) game.getForwardModel();
        standardDeal(state);   // P0 9S 10S JS AH 9D / P1 QS KS AS 10H 10D / P2 QD AD QH QC 10C / P3 KD KH AC KC JC

        // round 1: players 0 and 1 pass, player 2 orders up the 9H alone; player 0 sits out
        pass(state, fm, 2);
        fm.next(state, new CallTrump(Hearts, true));
        assertEquals(0, state.getSittingOut());
        // dealer 3 takes the 9H and discards the KD: P3 KH AC KC JC 9H
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new Discard(card("KD")));
        List<FrenchCard> p0Hand = cardsOf(state.getPlayerHand(0));

        // trick 1: led by 3 (the lone maker's left), then 1, 2. P1 is void in clubs and trumps: 10H wins
        assertEquals(3, state.getCurrentPlayer());
        playCards(state, fm, "AC", "10H", "QC");
        assertArrayEquals(new int[]{0, 1, 0, 0}, state.tricksTaken);

        // trick 2: 1, 2, 3. Both void in spades and trump: the KH beats the QH
        assertEquals(1, state.getCurrentPlayer());
        playCards(state, fm, "AS", "QH", "KH");
        assertArrayEquals(new int[]{0, 1, 0, 1}, state.tricksTaken);

        // trick 3: 3, 1, 2. The 9H is the only trump (the bowers JH, JD are in the kitty)
        assertEquals(3, state.getCurrentPlayer());
        playCards(state, fm, "9H", "KS", "AD");
        assertArrayEquals(new int[]{0, 1, 0, 2}, state.tricksTaken);

        // trick 4: 3, 1, 2. P2 must follow with the 10C: the KC wins
        assertEquals(3, state.getCurrentPlayer());
        playCards(state, fm, "KC", "QS");
        assertEquals(plays("10C"), available(state, fm));
        playCards(state, fm, "10C");
        assertArrayEquals(new int[]{0, 1, 0, 3}, state.tricksTaken);
        assertEquals(12, state.getDiscardPile().getSize());   // 4 tricks of 3 cards
        assertTrue(state.isNotTerminal());

        // trick 5: 3, 1, 2. The JC is a plain club (Hearts trumps), neither can follow nor trump: player 3 wins
        assertEquals(3, state.getCurrentPlayer());
        playCards(state, fm, "JC", "10D", "QD");
        assertArrayEquals(new int[]{0, 1, 0, 4}, state.tricksTaken);
        // lone maker 2 took 0 < 3: euchred, pointsEuchred 2 to players 1+3; the game (targetScore 1) is over
        assertEquals(0, state.getTeamPoints(0));
        assertEquals(2, state.getTeamPoints(1));
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
        // player 0 never played: still the same 5 cards; 5 tricks x 3 = 15 cards discarded
        assertEquals(p0Hand, cardsOf(state.getPlayerHand(0)));
        assertEquals(15, state.getDiscardPile().getSize());
        assertEquals(4, state.getKitty().getSize());
        assertAllCardsPresent(state);
    }

    /**
     * The points for a deal in which the makers took the given number of tricks, going alone or not: {team 0, team 1}.
     */
    private static int[] dealPoints(int makersTeam, int makersTricks, boolean alone) {
        int[] points = new int[2];
        if (makersTricks == 5) points[makersTeam] = alone ? 4 : 2;   // pointsAloneMarch, pointsMarch
        else if (makersTricks >= 3) points[makersTeam] = 1;      // pointsMade
        else points[1 - makersTeam] = 2;                         // pointsEuchred, to the defenders
        return points;
    }

    /**
     * Plays a random game to the target score, checking each step: whose turn it is (never the partner of a player
     * going alone), the kind of actions offered (the exact bidding actions, going alone included), 3-card tricks when
     * someone sits out, card conservation, the start of each deal, each deal's points and the final results.
     *
     * @return the number of deals in which the maker went alone
     */
    private int playRandomGame(long seed, int targetScore, boolean sittingOutDealerPicksUp) {
        Game game = newGame(seed, params(targetScore, sittingOutDealerPicksUp));
        EuchreGameState state = (EuchreGameState) game.getGameState();
        EuchreForwardModel fm = (EuchreForwardModel) game.getForwardModel();
        Random rnd = new Random(seed);
        int[] expectedPoints = new int[2];
        int deals = 0, steps = 0, aloneDeals = 0;
        boolean dealStart = true;

        while (state.isNotTerminal() && steps < 2000) {
            int round = state.getRoundCounter();
            int dealer = (round + 3) % 4;
            String d = "seed " + seed + " deal " + round + " step " + steps + ": ";
            if (dealStart) {
                assertEquals(d + "deals so far", deals, round);
                assertEquals(d + "dealer", dealer, state.getDealer());
                assertEquals(d + "first bidder", (dealer + 1) % 4, state.getCurrentPlayer());
                for (int p = 0; p < 4; p++) {
                    assertEquals(d + "hand", 5, state.getPlayerHand(p).getSize());
                    assertEquals(d + "tricks", 0, state.getTricksTaken(p));
                    assertTrue(d + "voids", state.getKnownVoids().get(p).isEmpty());
                }
                assertEquals(d + "kitty", 4, state.getKitty().getSize());
                assertEquals(d + "up-card", state.getKitty().peek(), state.getUpCard());
                assertEquals(d + "discard pile", 0, state.getDiscardPile().getSize());
                assertNull(d + "trumps", state.getTrumpSuit());
                assertNull(d + "dealer's discard", state.getDealerDiscard());
                assertEquals(d + "passes", 0, state.getPasses());
                assertArrayEquals(d + "points carried over", expectedPoints, state.teamPoints);
                dealStart = false;
            }

            int player = state.getCurrentPlayer();
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse(d + "no actions", actions.isEmpty());
            FrenchCard.Suite up = state.getUpCard().suite;
            if (state.getTrumpSuit() == null) {
                // bidding: in turn from the dealer's left; round 1 the up-card's suit, round 2 any other suit and
                // the dealer may not pass
                int passes = state.getPasses();
                assertEquals(d + "bidder", (dealer + 1 + passes) % 4, player);
                Set<AbstractAction> expected = new HashSet<>();
                for (FrenchCard.Suite s : FrenchCard.Suite.values())
                    if (passes < 4 ? s == up : s != up) {
                        expected.add(new CallTrump(s, false));
                        expected.add(new CallTrump(s, true));
                    }
                if (passes < 7)
                    expected.add(new Pass());
                assertEquals(d + "bidding actions", expected, new HashSet<>(actions));
            } else if (state.getPlayerHand(dealer).getSize() == 6) {
                assertEquals(d + "the dealer discards", dealer, player);
                Set<AbstractAction> expected = new HashSet<>();
                for (FrenchCard c : state.getPlayerHand(dealer).getComponents())
                    expected.add(new Discard(c));
                assertEquals(d + "discards", expected, new HashSet<>(actions));
            } else {
                // play: the partner of a lone maker never plays and keeps 5 cards; tricks have 3 cards then
                int out = state.getSittingOut();
                assertEquals(d + "kitty in play", 4, state.getKitty().getSize());
                assertEquals(d + "trick's player sitting out", out, state.getCurrentTrick().getSittingOut());
                assertTrue(d + "trick not yet taken", state.getCurrentTrick().getSize() < (out < 0 ? 4 : 3));
                if (out >= 0) {
                    assertNotEquals(d + "the partner sitting out is to play", out, player);
                    assertEquals(d + "hand of the partner sitting out", 5, state.getPlayerHand(out).getSize());
                }
                for (AbstractAction a : actions)
                    assertTrue(d + a + " is a card in hand",
                            a instanceof PlayCard pc && state.getPlayerHand(player).contains(pc.card));
            }

            AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));
            boolean alone = state.isAlone();
            int makersTeam = state.getMaker() < 0 ? -1 : state.getMaker() % 2;
            int makersTricks = makersTeam < 0 ? 0 : state.getTeamTricks(makersTeam);
            int tricksSoFar = makersTeam < 0 ? 0 : state.getTeamTricks(0) + state.getTeamTricks(1);
            fm.next(state, chosen);
            steps++;
            assertAllCardsPresent(state);

            if (!state.isNotTerminal() || state.getRoundCounter() != round) {
                // the deal is over: this was the last card of the 5th trick, which the makers won or lost
                deals++;
                dealStart = true;
                assertEquals(d + "the deal ended on the 5th trick's last card", 4, tricksSoFar);
                assertTrue(d, chosen instanceof PlayCard);
                if (alone) aloneDeals++;
                int[] ifLost = dealPoints(makersTeam, makersTricks, alone);
                int[] ifWon = dealPoints(makersTeam, makersTricks + 1, alone);
                int[] delta = {state.getTeamPoints(0) - expectedPoints[0], state.getTeamPoints(1) - expectedPoints[1]};
                if (!state.isNotTerminal()) {
                    // the final deal's tricks are still there: the exact outcome
                    assertEquals(d + "tricks", 5, state.getTeamTricks(0) + state.getTeamTricks(1));
                    assertArrayEquals(d + "final deal's points",
                            dealPoints(makersTeam, state.getTeamTricks(makersTeam), alone), delta);
                } else {
                    assertTrue(d + "deal points " + delta[0] + ", " + delta[1],
                            Arrays.equals(delta, ifLost) || Arrays.equals(delta, ifWon));
                }
                expectedPoints[0] += delta[0];
                expectedPoints[1] += delta[1];
                assertEquals(d + "the game ends when a team reaches " + targetScore,
                        expectedPoints[0] >= targetScore || expectedPoints[1] >= targetScore, !state.isNotTerminal());
            }
        }
        assertFalse("seed " + seed + ": game did not end within 2000 actions", state.isNotTerminal());
        assertTrue("at most 2 x targetScore - 1 deals", deals <= 2 * targetScore - 1);
        int winner = expectedPoints[0] >= targetScore ? 0 : 1;
        for (int p = 0; p < 4; p++)
            assertEquals("seed " + seed + ": result of player " + p, p % 2 == winner ? WIN_GAME : LOSE_GAME,
                    state.getPlayerResults()[p]);
        return aloneDeals;
    }

    @Test
    public void randomSingleDealGamesFollowTheRules() {
        int aloneDeals = 0;
        for (long seed = 1; seed <= 10; seed++)
            aloneDeals += playRandomGame(seed, 1, true);
        assertTrue("no maker went alone", aloneDeals > 0);
    }

    @Test
    public void randomGamesToTenCarryThePointsFromDealToDeal() {
        int aloneDeals = 0;
        for (long seed = 1; seed <= 3; seed++)
            aloneDeals += playRandomGame(seed, 10, true);
        assertTrue("no maker went alone", aloneDeals > 0);
    }

    @Test
    public void randomGamesWithoutTheSittingOutDealerPickingUpFollowTheRules() {
        int aloneDeals = 0;
        for (long seed = 1; seed <= 3; seed++)
            aloneDeals += playRandomGame(seed, 10, false);
        assertTrue("no maker went alone", aloneDeals > 0);
    }
}
