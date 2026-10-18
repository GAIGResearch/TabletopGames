package games.pitch;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.pitch.actions.Bid;
import games.pitch.actions.Pass;
import games.pitch.actions.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.Suite.*;
import static games.pitch.PitchGameState.Phase.BIDDING;
import static games.pitch.PitchGameState.Phase.PLAYING;
import static games.pitch.PitchTestUtils.*;
import static org.junit.Assert.*;

/**
 * A whole deal through fm.next: a scripted deal from arranged hands, and seeded random games checked against an
 * oracle written from the rules.
 */
public class PitchGameFlowTest {

    @Test
    public void aScriptedDealFromBiddingToTheResult() {
        Game game = newGame(5);
        PitchGameState state = (PitchGameState) game.getGameState();
        PitchForwardModel fm = (PitchForwardModel) game.getForwardModel();
        giveHand(state, 0, "3S", "2H", "3H", "4H", "QH", "AH");
        giveHand(state, 1, "4S", "2D", "3D", "4D", "5D", "6D");
        giveHand(state, 2, "AS", "KS", "7D", "8C", "9H", "10H");
        giveHand(state, 3, "2S", "QD", "KD", "3C", "4C", "5C");
        assertAllCardsPresent(state);

        // bidding: 0 pass, 1 bids 2, 2 bids 4, the dealer (3) may pass or bid 4 or 5, and passes
        fm.next(state, new Pass());
        fm.next(state, new Bid(2));
        fm.next(state, new Bid(4));
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new Pass(), new Bid(4), new Bid(5)), new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, new Pass());
        assertEquals(PLAYING, state.getGamePhase());
        assertEquals(2, state.getPitcher());

        // trick 1: player 2 leads AS - trumps Spades; everyone else must play their one spade
        play(state, fm, 2, "AS");
        assertEquals(Spades, state.getTrumpSuit());
        assertEquals(Set.of(new PlayCard(card("2S"))), new HashSet<>(fm.computeAvailableActions(state)));
        play(state, fm, 3, "2S");
        play(state, fm, 0, "3S");
        play(state, fm, 1, "4S");
        // AS wins for team 0
        assertEquals(4, state.getTeamTricks(0).getSize());

        // trick 2: player 2 leads 7D; player 3 must follow with a diamond (no trumps left)
        play(state, fm, 2, "7D");
        assertEquals(Set.of(new PlayCard(card("QD")), new PlayCard(card("KD"))),
                new HashSet<>(fm.computeAvailableActions(state)));
        play(state, fm, 3, "KD");
        play(state, fm, 0, "2H");     // no diamonds: any card
        play(state, fm, 1, "2D");
        // KD is the highest diamond: player 3 wins for team 1 and leads
        assertEquals(Set.of(card("7D"), card("KD"), card("2H"), card("2D")), setOf(state.getTeamTricks(1)));
        // trick 1 (trumps led, all followed) showed nothing; 0's 2H on a diamond lead shows 0 void in Diamonds
        assertVoids(state, Set.of(Diamonds), Set.of(), Set.of(), Set.of());

        // trick 3: player 3 leads 3C; player 2 holds 8C and trumps KS: either
        play(state, fm, 3, "3C");
        play(state, fm, 0, "3H");
        play(state, fm, 1, "3D");
        assertEquals(Set.of(new PlayCard(card("8C")), new PlayCard(card("KS"))),
                new HashSet<>(fm.computeAvailableActions(state)));
        play(state, fm, 2, "KS");     // trumps the club: player 2 wins
        // 0 (3H) and 1 (3D) did not follow clubs and did not trump: void in Clubs. 2 trumped: nothing.
        assertVoids(state, Set.of(Diamonds, Clubs), Set.of(Clubs), Set.of(), Set.of());

        // trick 4: 9H led; player 0 wins with AH
        play(state, fm, 2, "9H");
        play(state, fm, 3, "4C");
        play(state, fm, 0, "AH");
        play(state, fm, 1, "4D");
        // 3 (4C) and 1 (4D) did not follow hearts and did not trump: void in Hearts
        assertVoids(state, Set.of(Diamonds, Clubs), Set.of(Clubs, Hearts), Set.of(), Set.of(Hearts));

        // trick 5: player 0 leads QH; player 2 must follow with 10H; QH wins
        play(state, fm, 0, "QH");
        play(state, fm, 1, "5D");
        play(state, fm, 2, "10H");
        play(state, fm, 3, "5C");
        assertTrue("not over before the sixth trick", state.isNotTerminal());
        assertEquals(0, state.getTeamScore(0));

        // trick 6: player 0 leads 4H, the only heart and no trumps: player 0 wins
        play(state, fm, 0, "4H");
        play(state, fm, 1, "6D");
        play(state, fm, 2, "8C");
        // trick 5 added nothing new (1 and 3 already void in Hearts, 2 followed); now 2 (8C) is void in Hearts
        assertVoids(state, Set.of(Diamonds, Clubs), Set.of(Clubs, Hearts), Set.of(Hearts), Set.of(Hearts));
        play(state, fm, 3, "QD");

        // team 1 won only trick 2 (7D KD 2H 2D): Game value KD 3 = 3.
        // team 0 won the other 20 cards: trumps AS, 2S, 3S, 4S, KS - High AS, Low 2S; JS was never dealt.
        // Game: AS 4 + KS 3 + AH 4 + QH 2 + 10H 10 + QD 2 = 25 > 3 -> team 0.
        // team 0: 3 points < bid 4 -> set, -4. team 1: 0 points -> 0.
        assertEquals(20, state.getTeamTricks(0).getSize());
        assertEquals(4, state.getTeamTricks(1).getSize());
        assertEquals(-4, state.getTeamScore(0));
        assertEquals(0, state.getTeamScore(1));
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        // team 1 (0) beats team 0 (-4): both of team 1 win
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME},
                state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    // ---------------------------------------------------------------- random games against an oracle

    @Test
    public void randomGamesFollowTheRulesAndConserveTheCards() {
        int deals = 0, smudges = 0;
        for (long seed = 1; seed <= 10; seed++) {
            Game game = newGame(seed);
            PitchGameState state = (PitchGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            Random rnd = new Random(seed);
            FrenchCard.Suite trumps = null;
            List<Integer> trickPlayers = new ArrayList<>();
            List<FrenchCard> trickCards = new ArrayList<>();
            List<List<FrenchCard>> won = List.of(new ArrayList<>(), new ArrayList<>());
            int[] bids = {-1, -1, -1, -1};
            int pitcher = -1;
            int steps = 0;
            while (state.isNotTerminal() && steps++ < 100) {
                int p = state.getCurrentPlayer();
                List<AbstractAction> actions = new ArrayList<>(fm.computeAvailableActions(state));
                assertFalse("no legal action", actions.isEmpty());
                AbstractAction a = actions.get(rnd.nextInt(actions.size()));
                if (state.getGamePhase() == BIDDING) {
                    assertEquals("bidding order", steps - 1, p);
                    if (a instanceof Bid bid) {
                        bids[p] = bid.amount;
                        pitcher = p;
                    } else bids[p] = 0;
                } else {
                    if (trickCards.isEmpty() && trumps == null) {
                        assertEquals("the pitcher leads", pitcher, p);
                        trumps = ((PlayCard) a).card.suite;
                    } else if (!trickCards.isEmpty()) {
                        assertEquals("clockwise", (trickPlayers.get(trickPlayers.size() - 1) + 1) % 4, p);
                    }
                    trickPlayers.add(p);
                    trickCards.add(((PlayCard) a).card);
                }
                fm.next(state, a);
                assertAllCardsPresent(state);
                if (trickCards.size() == 4) {
                    int winner = oracleTrickWinner(trickPlayers, trickCards, trumps);
                    won.get(winner % 2).addAll(trickCards);
                    assertEquals("team 0 cards won", new HashSet<>(won.get(0)), setOf(state.getTeamTricks(0)));
                    assertEquals("team 1 cards won", new HashSet<>(won.get(1)), setOf(state.getTeamTricks(1)));
                    if (state.isNotTerminal())
                        assertEquals("the winner leads", winner, state.getCurrentPlayer());
                    trickPlayers.clear();
                    trickCards.clear();
                }
            }
            assertFalse("the deal did not end within 100 actions", state.isNotTerminal());
            // 4 bids + 24 cards
            assertEquals(28, steps);
            assertEquals(trumps, state.getTrumpSuit());
            assertEquals(pitcher, state.getPitcher());

            int[] points = oraclePoints(won, trumps);
            int[] expected = points.clone();
            int pt = pitcher % 2;
            if (bids[pitcher] == 5) {
                // smudge: +5 for all six tricks (all 24 cards) and exactly 4 points, otherwise -5
                smudges++;
                boolean made = won.get(pt).size() == 24 && points[pt] == 4;
                expected[pt] = made ? 5 : -5;
            } else if (points[pt] < bids[pitcher]) expected[pt] = -bids[pitcher];
            assertEquals("team 0 score, seed " + seed, expected[0], state.getTeamScore(0));
            assertEquals("team 1 score, seed " + seed, expected[1], state.getTeamScore(1));
            for (int p = 0; p < 4; p++) {
                int mine = expected[p % 2], theirs = expected[1 - p % 2];
                CoreConstants.GameResult r = mine > theirs ? WIN_GAME : mine < theirs ? LOSE_GAME : DRAW_GAME;
                assertEquals("result of player " + p + ", seed " + seed, r, state.getPlayerResults()[p]);
            }
            deals++;
        }
        assertEquals(10, deals);
        assertTrue("no random game had a bid of 5", smudges > 0);
    }
}
