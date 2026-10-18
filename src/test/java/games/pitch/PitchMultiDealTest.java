package games.pitch;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.pitch.actions.Bid;
import games.pitch.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static games.pitch.PitchGameState.Phase.BIDDING;
import static games.pitch.PitchTestUtils.*;
import static org.junit.Assert.*;

/**
 * The multi-deal game, with targetScore 21 unless a test sets another.
 * <p>
 * The next deal starts in the same fm.next as the last card of a deal, so the deal's cards are gone from the team
 * decks by the time a test can look: these tests check score changes instead.
 */
public class PitchMultiDealTest {

    PitchParameters params;
    PitchForwardModel fm;
    PitchGameState state;

    @Before
    public void setup() {
        fm = new PitchForwardModel();
        state = newState(51, fm);
        params = (PitchParameters) state.getGameParameters();
        params.setParameterValue("targetScore", 21);
        assertEquals(21, params.targetScore);
        assertEquals(0, state.getRoundCounter());
        assertEquals(3, state.getDealer());
    }

    // ---------------------------------------------------------------- arranged last tricks (from PitchScoringTest)

    /** Player 0 (team 0) bid 3 and makes it exactly: team 0 +3, team 1 +0. */
    private void playDealTeam0Bids3AndMakesIt() {
        arrangeLastTrick(state, 0, new int[]{3, 0, 0, 0}, 0,
                new String[]{"AH", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C",
                        "2D", "3D", "4D", "5D", "6D", "7D", "8D"},
                new String[]{"3H", "9D", "2S", "3S"},
                "10S", "4S", "2H", "5S");
        // 10S, 4S, 2H (trumps), 5S -> player 2 wins.
        // trumps AH, 2H (team 0), 3H (team 1): High AH, Low 2H -> team 0; no JH.
        // Game: team 0 AH 4 + 10S 10 = 14 v 0 -> team 0. team 0: 3 >= bid 3 -> +3. team 1: 0.
        play(state, fm, 0, "10S");
        play(state, fm, 1, "4S");
        play(state, fm, 2, "2H");
        play(state, fm, 3, "5S");
    }

    /** Player 1 (team 1) bid 3 and is set: team 0 +2, team 1 -3. */
    private void playDealTeam1Bids3AndIsSet() {
        arrangeLastTrick(state, 1, new int[]{2, 3, 0, 0}, 3,
                new String[]{"AH", "10C", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "2D", "3D"},
                new String[]{"2H", "4D", "5D", "6D", "7D", "8D", "9D", "2S"},
                "3S", "4S", "5S", "JH");
        // player 3 leads JH (trump) and wins. trumps AH (team 0), 2H, JH (team 1).
        // High AH -> team 0; Low 2H, Jack JH -> team 1. Game: team 0 AH 4 + 10C 10 = 14 v JH 1 -> team 0.
        // team 0: 2 (not pitching) -> +2. team 1: 2 < bid 3 -> -3.
        play(state, fm, 3, "JH");
        play(state, fm, 0, "3S");
        play(state, fm, 1, "4S");
        play(state, fm, 2, "5S");
    }

    /** Player 0 (team 0) bid 2 and makes it; team 1 also scores 2: team 0 +2, team 1 +2. */
    private void playDealBothTeamsScore2Team0Pitching() {
        arrangeLastTrick(state, 0, new int[]{2, 0, 0, 0}, 0,
                new String[]{"AH", "10C", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "2D", "3D"},
                new String[]{"2H", "JH", "4D", "5D", "6D", "7D", "8D", "9D"},
                "9S", "8S", "7S", "6S");
        // no trumps in the last trick: player 0 wins with 9S. trumps AH (team 0), 2H, JH (team 1).
        // High AH -> team 0; Low 2H, Jack JH -> team 1. Game: 14 v 1 -> team 0.
        // team 0: 2 >= bid 2 -> +2. team 1: +2.
        play(state, fm, 0, "9S");
        play(state, fm, 1, "8S");
        play(state, fm, 2, "7S");
        play(state, fm, 3, "6S");
    }

    /** Player 0 (team 0) bid 5 (smudge) and makes it: team 0 +5, team 1 +0. */
    private void playDealTeam0SmudgesAndMakesIt() {
        arrangeLastTrick(state, 0, new int[]{5, 0, 0, 0}, 0,
                new String[]{"AH", "2H", "JH", "10C", "2C", "3C", "4C", "5C", "6C", "7C",
                        "8C", "9C", "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D"},
                new String[]{},
                "KH", "3S", "4S", "5S");
        // KH wins: team 0 all six tricks. High AH, Low 2H, Jack JH; Game 18 v 0 -> 4 points: smudge made, +5.
        play(state, fm, 0, "KH");
        play(state, fm, 1, "3S");
        play(state, fm, 2, "4S");
        play(state, fm, 3, "5S");
    }

    /** Asserts a new deal has started for round `round` with the given scores carried over. */
    private void assertNewDeal(int round, int score0, int score1) {
        assertTrue("the game goes on", state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_ONGOING, state.getGameStatus());
        assertEquals(round, state.getRoundCounter());
        // dealer = (round + 3) % 4; the player to the dealer's left bids first
        int dealer = (round + 3) % 4;
        assertEquals(dealer, state.getDealer());
        assertEquals((dealer + 1) % 4, state.getCurrentPlayer());
        assertEquals(BIDDING, state.getGamePhase());
        for (int p = 0; p < 4; p++) {
            assertEquals(6, state.getPlayerHand(p).getSize());
            assertEquals(-1, state.getPlayerBid(p));
        }
        assertEquals(-1, state.getPitcher());
        assertNull(state.getTrumpSuit());
        assertEquals(0, state.getTeamTricks(0).getSize());
        assertEquals(0, state.getTeamTricks(1).getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        // 52 - 4 * 6 = 28
        assertEquals(28, state.getUndealtDeck().getSize());
        assertEquals(score0, state.getTeamScore(0));
        assertEquals(score1, state.getTeamScore(1));
        assertAllCardsPresent(state);
    }

    // ---------------------------------------------------------------- unit / scripted flows

    @Test
    public void afterADealBelowTheTargetTheNextDealStartsWithTheDealerMovedLeft() {
        playDealTeam0Bids3AndMakesIt();
        // [0, 0] + [3, 0] = [3, 0]; 3 < 21. Round 1: dealer 0, player 1 bids first.
        assertNewDeal(1, 3, 0);

        // and again: [3, 0] + [2, -3] = [5, -3]. Round 2: dealer 1, player 2 bids first.
        playDealTeam1Bids3AndIsSet();
        assertNewDeal(2, 5, -3);
    }

    @Test
    public void thePitchingTeamReachingTheTargetOnAMadeBidWins() {
        state.teamScores[0] = 18;
        state.teamScores[1] = 5;
        playDealTeam0Bids3AndMakesIt();
        // [18, 5] + [3, 0] = [21, 5]: team 0 pitched, made its bid, 21 >= 21 -> team 0 wins
        assertEquals(21, state.getTeamScore(0));
        assertEquals(5, state.getTeamScore(1));
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME},
                state.getPlayerResults());
        // no new deal
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void aMadeSmudgeTakesThePitchingTeamOut() {
        state.teamScores[0] = 16;
        state.teamScores[1] = 0;
        playDealTeam0SmudgesAndMakesIt();
        // [16, 0] + [5, 0] = [21, 0]: smudge made, 21 >= 21 -> team 0 wins
        assertEquals(21, state.getTeamScore(0));
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME},
                state.getPlayerResults());
    }

    @Test
    public void theNonPitchingTeamOverTheTargetDoesNotWinWhenThePitchersAreSet() {
        state.teamScores[0] = 20;
        state.teamScores[1] = 10;
        playDealTeam1Bids3AndIsSet();
        // [20, 10] + [2, -3] = [22, 7]: team 0 is at 22 >= 21 but did not pitch -> the game goes on
        assertNewDeal(1, 22, 7);
    }

    @Test
    public void theNonPitchingTeamOverTheTargetDoesNotWinWhenThePitchersMakeTheirBidBelowIt() {
        state.teamScores[0] = 10;
        state.teamScores[1] = 19;
        playDealBothTeamsScore2Team0Pitching();
        // [10, 19] + [2, 2] = [12, 21]: team 0 made its bid but 12 < 21; team 1 at 21 did not pitch -> goes on
        assertNewDeal(1, 12, 21);
    }

    @Test
    public void aSetPitchingTeamStillOverTheTargetDoesNotWin() {
        state.teamScores[0] = 5;
        state.teamScores[1] = 25;
        playDealTeam1Bids3AndIsSet();
        // [5, 25] + [2, -3] = [7, 22]: team 1 pitched and is at 22 >= 21, but it was set -> the game goes on
        assertNewDeal(1, 7, 22);
    }

    @Test
    public void thePitchingTeamGoesOutEvenWhenTheOtherTeamHasMore() {
        state.teamScores[0] = 19;
        state.teamScores[1] = 25;
        playDealBothTeamsScore2Team0Pitching();
        // [19, 25] + [2, 2] = [21, 27]: team 0 pitched, made its bid and has 21 >= 21 -> team 0 wins,
        // although team 1 has more (27). Only the pitching team can go out.
        assertEquals(21, state.getTeamScore(0));
        assertEquals(27, state.getTeamScore(1));
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME},
                state.getPlayerResults());
    }

    @Test
    public void afterMaxRoundsDealsTheGameEndsByScore() {
        params.setMaxRounds(2);
        playDealBothTeamsScore2Team0Pitching();
        // [0, 0] + [2, 2] = [2, 2]; deal 1 of 2 -> goes on
        assertNewDeal(1, 2, 2);
        playDealBothTeamsScore2Team0Pitching();
        // [2, 2] + [2, 2] = [4, 4]; round counter reaches maxRounds 2 -> ends by score: equal, all draw
        assertEquals(4, state.getTeamScore(0));
        assertEquals(4, state.getTeamScore(1));
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertArrayEquals(new CoreConstants.GameResult[]{DRAW_GAME, DRAW_GAME, DRAW_GAME, DRAW_GAME},
                state.getPlayerResults());
    }

    // ---------------------------------------------------------------- random games against an oracle

    @Test
    public void randomGamesToATargetEndWhenTheLastPitchingTeamGoesOut() {
        int maxDeals = PitchTestUtils.valetParams().getMaxRounds();
        assertEquals(100, maxDeals);
        for (int target : new int[]{7, 21}) {
            int wentOut = 0, totalDeals = 0;
            for (long seed = 1; seed <= 5; seed++) {
                PitchParameters p = PitchTestUtils.valetParams();
                p.setParameterValue("targetScore", target);
                Game game = newGame(seed, p);
                PitchGameState state = (PitchGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                assertEquals(target, ((PitchParameters) state.getGameParameters()).targetScore);
                Random rnd = new Random(seed);
                int[] scores = {0, 0};
                int deals = 0;
                // per-deal oracle state
                FrenchCard.Suite trumps = null;
                List<Integer> trickPlayers = new ArrayList<>();
                List<FrenchCard> trickCards = new ArrayList<>();
                List<List<FrenchCard>> won = List.of(new ArrayList<>(), new ArrayList<>());
                int[] tricksWon = {0, 0};
                int[] bids = {-1, -1, -1, -1};
                int pitcher = -1, nBids = 0;
                int steps = 0;
                // a deal is 28 actions: 4 bids + 24 cards
                while (state.isNotTerminal() && steps++ < 28 * maxDeals + 10) {
                    int pl = state.getCurrentPlayer();
                    List<AbstractAction> actions = new ArrayList<>(fm.computeAvailableActions(state));
                    assertFalse("no legal action", actions.isEmpty());
                    AbstractAction a = actions.get(rnd.nextInt(actions.size()));
                    if (state.getGamePhase() == BIDDING) {
                        // random high bids are nearly always set, so scores drift down and no game reaches the
                        // target: bid only the lowest bid available (or pass), so that bids are often made
                        List<AbstractAction> low = actions.stream()
                                .filter(x -> !(x instanceof Bid b) || actions.stream().noneMatch(y -> y instanceof Bid c && c.amount < b.amount))
                                .toList();
                        a = low.get(rnd.nextInt(low.size()));
                        assertEquals("round", deals, state.getRoundCounter());
                        int dealer = (deals + 3) % 4;
                        assertEquals("bidding order", (dealer + 1 + nBids) % 4, pl);
                        nBids++;
                        if (a instanceof Bid bid) {
                            bids[pl] = bid.amount;
                            pitcher = pl;
                        } else bids[pl] = 0;
                    } else {
                        if (trickCards.isEmpty() && trumps == null) {
                            assertEquals("the pitcher leads", pitcher, pl);
                            trumps = ((PlayCard) a).card.suite;
                        }
                        trickPlayers.add(pl);
                        trickCards.add(((PlayCard) a).card);
                    }
                    fm.next(state, a);
                    assertAllCardsPresent(state);
                    if (trickCards.size() < 4) continue;

                    int winner = oracleTrickWinner(trickPlayers, trickCards, trumps);
                    won.get(winner % 2).addAll(trickCards);
                    tricksWon[winner % 2]++;
                    trickPlayers.clear();
                    trickCards.clear();
                    if (tricksWon[0] + tricksWon[1] < 6) {
                        assertEquals("the winner leads", winner, state.getCurrentPlayer());
                        continue;
                    }

                    // the deal is over: score it from the oracle and check the scores carried over
                    int pt = pitcher % 2;
                    int[] points = oraclePoints(won, trumps);
                    int[] change = oracleScoreChange(points, pitcher, bids[pitcher], tricksWon[pt]);
                    boolean made = bids[pitcher] == 5 ? change[pt] == 5 : points[pt] >= bids[pitcher];
                    scores[0] += change[0];
                    scores[1] += change[1];
                    deals++;
                    String msg = "target " + target + ", seed " + seed + ", deal " + deals;
                    assertEquals(msg, scores[0], state.getTeamScore(0));
                    assertEquals(msg, scores[1], state.getTeamScore(1));
                    if (made && scores[pt] >= target) {
                        // the pitching team goes out and wins, whatever the other team's score
                        assertFalse(msg, state.isNotTerminal());
                        for (int q = 0; q < 4; q++)
                            assertEquals(msg + ", player " + q, q % 2 == pt ? WIN_GAME : LOSE_GAME,
                                    state.getPlayerResults()[q]);
                        wentOut++;
                    } else if (deals == maxDeals) {
                        // safety: the higher score wins, equal draws
                        assertFalse(msg, state.isNotTerminal());
                        for (int q = 0; q < 4; q++) {
                            int mine = scores[q % 2], theirs = scores[1 - q % 2];
                            assertEquals(msg, mine > theirs ? WIN_GAME : mine < theirs ? LOSE_GAME : DRAW_GAME,
                                    state.getPlayerResults()[q]);
                        }
                    } else {
                        assertTrue(msg + ": the game should go on", state.isNotTerminal());
                        assertEquals(msg, deals, state.getRoundCounter());
                        int dealer = (deals + 3) % 4;
                        assertEquals(msg, dealer, state.getDealer());
                        assertEquals(msg, (dealer + 1) % 4, state.getCurrentPlayer());
                        assertEquals(msg, BIDDING, state.getGamePhase());
                        assertEquals(msg, 0, state.getTeamTricks(0).getSize() + state.getTeamTricks(1).getSize());
                        assertEquals(msg, 28, state.getUndealtDeck().getSize());
                        assertEquals(msg, -1, state.getPitcher());
                        assertNull(msg, state.getTrumpSuit());
                        for (int q = 0; q < 4; q++) {
                            assertEquals(msg, 6, state.getPlayerHand(q).getSize());
                            assertEquals(msg, -1, state.getPlayerBid(q));
                        }
                    }
                    // reset the per-deal oracle
                    trumps = null;
                    won.get(0).clear();
                    won.get(1).clear();
                    tricksWon[0] = tricksWon[1] = 0;
                    java.util.Arrays.fill(bids, -1);
                    pitcher = -1;
                    nBids = 0;
                }
                assertFalse("target " + target + ", seed " + seed + ": the game did not end", state.isNotTerminal());
                totalDeals += deals;
            }
            assertTrue("target " + target + ": no game ended by a pitching team going out", wentOut > 0);
            assertTrue("target " + target + ": every game lasted one deal", totalDeals > 5);
        }
    }
}
