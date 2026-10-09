package games.sueca;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static games.sueca.SuecaRubberScoringTest.*;
import static games.sueca.SuecaTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Pagat's rubber (playRubber): the new deal after a deal that does not decide the rubber, the end of the rubber at
 * targetGames or maxRounds, getGameScore and the heuristic, and random rubbers played to the end.
 */
public class SuecaRubberFlowTest {

    private final SuecaForwardModel fm = new SuecaForwardModel();

    // ---- the new deal ----

    /**
     * A 61-point deal for team 0 (1 game, targetGames 4), whose last trick 5S 2H 3S 4S (led by player 0, Diamonds
     * trumps) records player 1 as void in Spades before the last card.
     */
    private SuecaGameState playFirstDealWithAVoid(long seed) {
        SuecaGameState state = newRubberState(seed, 4);
        arrangeLastTrick(state, 0, PILE_61, "5S", "2H", "3S", "4S");
        setTrumpCard(state, "2D");
        playCards(state, fm, "5S", "2H", "3S");
        // arrangement guard: player 1 failed to follow spades
        assertTrue(state.getKnownVoids().get(1).contains(FrenchCard.Suite.Spades));
        playCards(state, fm, "4S");
        return state;
    }

    @Test
    public void aDealThatDoesNotDecideTheRubberIsFollowedByANewDealFromTheNextDealer() {
        SuecaGameState state = playFirstDealWithAVoid(21);
        // 5S wins (the 2H is a discard): team 0 61 -> 1 game of 4
        assertEquals(1, state.getTeamGames(0));
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals("the deal passes from player 3 to player 0", 0, state.getDealer());
        assertEquals(1, state.getFirstLeader());
        assertEquals("the player after the dealer leads", 1, state.getCurrentPlayer());
        for (int p = 0; p < 4; p++)
            assertEquals("hand " + p, 10, state.getPlayerHand(p).getSize());
        assertEquals(0, state.getTeamPile(0).getSize());
        assertEquals(0, state.getTeamPile(1).getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(1, state.getCurrentTrick().getLeader());
        assertAllCardsPresent(state);
        // the new trump card is the new dealer's last card dealt
        assertTrue("trump card " + state.getTrumpCard() + " with the new dealer",
                state.getPlayerHand(0).contains(state.getTrumpCard()));
        assertTrue(state.isTrumpCardHeld());
        for (int p = 0; p < 4; p++)
            assertTrue("known voids of " + p + " cleared", state.getKnownVoids().get(p).isEmpty());
        assertEquals(0, state.getCardPoints(0));
        // the new first leader may lead any card
        assertEquals(10, fm.computeAvailableActions(state).size());
    }

    @Test
    public void theNewDealIsShuffledWithTheGameRandomNumbers() {
        // two identical arrangements that differ only in the state's random number generator (the setup seed)
        SuecaGameState a = playFirstDealWithAVoid(21);
        SuecaGameState b = playFirstDealWithAVoid(22);
        assertNotEquals(cardsOf(a.getPlayerHand(0)), cardsOf(b.getPlayerHand(0)));
    }

    @Test
    public void theTrumpCardStaysWithTheNewDealerInRedeterminisation() {
        SuecaGameState state = playFirstDealWithAVoid(21);
        FrenchCard trumpCard = state.getTrumpCard();
        // unconstrained, the trump card stays among the dealer's 10 of the 30 hidden cards with chance 1/3 per copy:
        // a false pass needs (1/3)^(3 x 40)
        for (int observer = 1; observer < 4; observer++) {
            for (int i = 0; i < 40; i++) {
                SuecaGameState copy = (SuecaGameState) state.copy(observer);
                assertTrue("observer " + observer + ": dealer 0 lost the trump card",
                        copy.getPlayerHand(0).contains(trumpCard));
                assertAllCardsPresent(copy);
            }
        }
    }

    @Test
    public void aTiedDealAlsoPassesTheDeal() {
        SuecaGameState state = newRubberState(23, 4);
        playZeroLastTrickWonByTeam0(state, fm, PILE_60);
        assertEquals(1, state.getExtraGames());
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getDealer());
        assertEquals(1, state.getCurrentPlayer());
        for (int p = 0; p < 4; p++)
            assertEquals(10, state.getPlayerHand(p).getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void theDealKeepsPassingRoundTheTable() {
        SuecaGameState state = newRubberState(24, 8);
        // deals of 61 points for team 0, 1 game each: the first deal is dealt by 3, the next by 0, 1, 2 and 3
        int[] dealers = {0, 1, 2, 3};
        for (int d = 0; d < 4; d++) {
            playZeroLastTrickWonByTeam0(state, fm, PILE_61);
            assertEquals(d + 1, state.getTeamGames(0));
            assertEquals(dealers[d], state.getDealer());
            assertEquals((dealers[d] + 1) % 4, state.getCurrentPlayer());
            assertTrue(state.getPlayerHand(dealers[d]).contains(state.getTrumpCard()));
        }
    }

    // ---- the end of the rubber ----

    @Test
    public void reachingTheTargetEndsTheRubberWithBothPartnersWinning() {
        SuecaGameState state = newRubberState(25, 4);
        state.teamGames[0] = 3;
        state.teamGames[1] = 2;
        playZeroLastTrickWonByTeam0(state, fm, PILE_61);
        // 3 + 1 = 4 = targetGames
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertEquals(4, state.getGameScore(0), 0.0);
        assertEquals(2, state.getGameScore(1), 0.0);
        assertEquals(4, state.getGameScore(2), 0.0);
        assertEquals(2, state.getGameScore(3), 0.0);
        assertAllCardsPresent(state);
    }

    @Test
    public void oneGameShortOfTheTargetTheRubberGoesOn() {
        SuecaGameState state = newRubberState(25, 4);
        state.teamGames[0] = 2;
        playZeroLastTrickWonByTeam0(state, fm, PILE_61);
        // 2 + 1 = 3 < 4
        assertTrue(state.isNotTerminal());
        assertEquals(3, state.getGameScore(0), 0.0);
        assertEquals(0, state.getGameScore(1), 0.0);
    }

    @Test
    public void passingTheTargetAlsoEndsTheRubberForTeamOne() {
        SuecaGameState state = newRubberState(26, 4);
        state.teamGames[0] = 3;
        state.teamGames[1] = 3;
        playZeroLastTrickWonByTeam0(state, fm, PILE_59);
        // team 1 61 points: 3 + 1 = 4
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
        assertEquals(3, state.getGameScore(0), 0.0);
        assertEquals(4, state.getGameScore(1), 0.0);
    }

    @Test
    public void aTargetOfOneEndsTheRubberAtTheFirstDecidedDealWithTheExtraGame() {
        SuecaGameState state = newRubberState(27, 1);
        playZeroLastTrickWonByTeam0(state, fm, PILE_60);
        // a tie decides nothing: the rubber goes on
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getExtraGames());
        playZeroLastTrickWonByTeam0(state, fm, PILE_59);
        // team 1 61: 1 + 1 extra = 2 >= 1
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
        assertEquals(0, state.getGameScore(0), 0.0);
        assertEquals(2, state.getGameScore(1), 0.0);
    }

    @Test
    public void aTargetOfOneEndsTheRubberAfterTheFirstDeal() {
        SuecaGameState state = newRubberState(27, 1);
        playZeroLastTrickWonByTeam0(state, fm, PILE_61);
        // 61 points: 1 game = targetGames 1
        assertFalse(state.isNotTerminal());
        assertEquals(1, state.getTeamGames(0));
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertEquals(1, state.getGameScore(0), 0.0);
        assertEquals(0, state.getGameScore(1), 0.0);
    }

    @Test
    public void atMaxRoundsTheTeamWithMoreGamesWins() {
        SuecaParameters params = rubberParams(4);
        params.setMaxRounds(1);
        SuecaGameState state = newState(28, params);
        // team 1 61 points: 1 game to 0; maxRounds 1 ends the rubber after this deal
        playZeroLastTrickWonByTeam0(state, fm, PILE_59);
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
        assertEquals(1, state.getGameScore(1), 0.0);
    }

    @Test
    public void atMaxRoundsEqualGamesAreADraw() {
        SuecaParameters params = rubberParams(4);
        params.setMaxRounds(1);
        SuecaGameState state = newState(28, params);
        state.teamGames[0] = 2;
        state.teamGames[1] = 1;
        // team 1 61 points: 1 + 1 = 2 games each (team 0 59 card points)
        playZeroLastTrickWonByTeam0(state, fm, PILE_59);
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{DRAW_GAME, DRAW_GAME, DRAW_GAME, DRAW_GAME}, state.getPlayerResults());
    }

    // ---- heuristic ----

    @Test
    public void theRubberHeuristicIsTheShareOfTheTargetGamesNotCardPoints() {
        SuecaGameState state = newRubberState(29, 4);
        arrangeLastTrick(state, 0, PILE_61, "5S", "2S", "3S", "4S");
        setTrumpCard(state, "2D");
        // team 0 has 61 card points but no games yet
        assertEquals(0.0, state.getHeuristicScore(0), 1e-9);
        assertEquals(0.0, state.getHeuristicScore(1), 1e-9);
        playCards(state, fm, "5S", "2S", "3S", "4S");
        // 1 game of 4
        assertEquals(0.25, state.getHeuristicScore(0), 1e-9);
        assertEquals(0.25, state.getHeuristicScore(2), 1e-9);
        assertEquals(0.0, state.getHeuristicScore(1), 1e-9);
    }

    @Test
    public void theRubberHeuristicIsCappedAtOne() {
        SuecaGameState state = newRubberState(29, 2);
        state.teamGames[1] = 3;      // not reachable in play (the rubber would be over); checks the cap
        assertEquals(1.0, state.getHeuristicScore(1), 1e-9);
        assertEquals(0.0, state.getHeuristicScore(0), 1e-9);
    }

    // ---- integration ----

    /**
     * Oracle from the rules: the games for a won deal, before extra games.
     */
    private static int expectedGames(int points, int tricks) {
        if (tricks == 10) return 4;          // all ten tricks
        return points >= 91 ? 2 : 1;
    }

    @Test
    public void randomRubbersPlayToTheEndScoringEveryDealByTheRules() {
        for (long seed = 1; seed <= 5; seed++) {
            Game game = newGame(seed, rubberParams(4));
            SuecaGameState state = (SuecaGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            Random rnd = new Random(seed);
            int[] games = new int[2];
            int extra = 0;
            int deals = 0;
            int actions = 0;
            while (state.isNotTerminal() && actions < 2000) {
                // one deal
                assertEquals("seed " + seed, deals, state.getRoundCounter());
                int dealer = (deals + 3) % 4;
                assertEquals(dealer, state.getDealer());
                assertEquals((dealer + 1) % 4, state.getCurrentPlayer());
                assertTrue(state.getPlayerHand(dealer).contains(state.getTrumpCard()));
                for (int p = 0; p < 4; p++)
                    assertEquals(10, state.getPlayerHand(p).getSize());
                FrenchCard.Suite trumps = state.getTrumpSuit();
                int[] points = new int[2];
                int[] tricks = new int[2];
                for (int i = 0; i < 40; i++) {
                    assertTrue("seed " + seed + ": the deal ended early", state.isNotTerminal());
                    int player = state.getCurrentPlayer();
                    List<FrenchCard> trick = new ArrayList<>(cardsOf(state.getCurrentTrick()));
                    int leader = state.getCurrentTrick().getLeader();
                    assertEquals((leader + trick.size()) % 4, player);
                    List<AbstractAction> available = fm.computeAvailableActions(state);
                    assertEquals(new HashSet<>(expectedLegalPlays(cardsOf(state.getPlayerHand(player)), trick)),
                            new HashSet<>(available.stream().map(a -> ((PlayCard<?>) a).card).toList()));
                    double h = state.getHeuristicScore(player);
                    assertTrue("heuristic " + h, h >= 0 && h <= 1);
                    @SuppressWarnings("unchecked")
                    PlayCard<FrenchCard> action = (PlayCard<FrenchCard>) available.get(rnd.nextInt(available.size()));
                    fm.next(state, action);
                    actions++;
                    assertAllCardsPresent(state);
                    if (trick.size() == 3) {
                        trick.add(action.card);
                        int winner = (leader + expectedWinningIndex(trick, trumps)) % 4;
                        points[winner % 2] += expectedCardPoints(trick);
                        tricks[winner % 2]++;
                    }
                }
                deals++;
                assertEquals(120, points[0] + points[1]);
                if (points[0] == 60) {
                    extra++;
                } else {
                    int team = points[0] > 60 ? 0 : 1;
                    games[team] += expectedGames(points[team], tricks[team]) + extra;
                    extra = 0;
                }
                assertEquals("seed " + seed + " deal " + deals, games[0], state.getTeamGames(0));
                assertEquals("seed " + seed + " deal " + deals, games[1], state.getTeamGames(1));
                assertEquals("seed " + seed + " deal " + deals, extra, state.getExtraGames());
                boolean over = games[0] >= 4 || games[1] >= 4;
                assertEquals("seed " + seed + " deal " + deals + ": games " + games[0] + "-" + games[1],
                        over, !state.isNotTerminal());
            }
            assertFalse("seed " + seed + ": the rubber did not end", state.isNotTerminal());
            assertTrue("seed " + seed + ": deals " + deals, deals >= 1);
            GameResult team0 = games[0] >= 4 ? WIN_GAME : LOSE_GAME;
            GameResult team1 = games[1] >= 4 ? WIN_GAME : LOSE_GAME;
            assertArrayEquals("seed " + seed, new GameResult[]{team0, team1, team0, team1}, state.getPlayerResults());
            for (int p = 0; p < 4; p++)
                assertEquals(games[p % 2], state.getGameScore(p), 0.0);
        }
    }
}
