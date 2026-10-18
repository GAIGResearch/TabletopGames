package games.euchre;

import core.CoreConstants.GameResult;
import core.Game;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static core.components.FrenchCard.Suite.Diamonds;
import static games.euchre.EuchreTestUtils.*;
import static org.junit.Assert.*;

/**
 * The last trick of a one-deal game (the default targetScore 1), arranged and played through fm.next: scoring the
 * deal and ending the game. Team 0 is players 0 and 2, team 1 players 1 and 3. Diamonds are trumps (a round-2
 * call on the standard deal); the last trick is AC led by its winner, followed by KC, QC, 10C.
 */
public class EuchreScoringTest {

    static final GameResult[] TEAM_0_WINS = {WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME};
    static final GameResult[] TEAM_1_WINS = {LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME};

    EuchreGameState state;
    EuchreForwardModel fm;

    @Before
    public void setup() {
        Game game = newGame(11);
        state = (EuchreGameState) game.getGameState();
        fm = (EuchreForwardModel) game.getForwardModel();
        standardDeal(state);
    }

    private void assertPointsAndResults(int team0, int team1, GameResult[] results) {
        assertEquals("team 0 points", team0, state.getTeamPoints(0));
        assertEquals("team 1 points", team1, state.getTeamPoints(1));
        assertFalse("one deal to targetScore 1 ends the game", state.isNotTerminal());
        for (int p = 0; p < 4; p++)
            assertEquals("score of player " + p, p % 2 == 0 ? team0 : team1, state.getGameScore(p), 0.0);
        // both partners of the winning team WIN_GAME: the default endGame would call their shared first place a draw
        assertArrayEquals(results, state.getPlayerResults());
    }

    @Test
    public void makersTakingThreeTricksScoreOne() {
        startPlay(state, Diamonds, 2, null);
        playLastTrick(state, fm, new int[]{1, 1, 1, 1}, 0);
        // makers 0+2: 1 + 1 + the last trick = 3 -> pointsMade 1
        assertArrayEquals(new int[]{2, 1, 1, 1}, state.tricksTaken);
        assertEquals(3, state.getTeamTricks(0));
        assertEquals(20, state.getDiscardPile().getSize());   // 5 tricks of 4 cards
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(4, state.getKitty().getSize());
        assertAllCardsPresent(state);
        assertPointsAndResults(1, 0, TEAM_0_WINS);
    }

    @Test
    public void makersTakingFourTricksScoreOne() {
        startPlay(state, Diamonds, 0, null);
        playLastTrick(state, fm, new int[]{2, 1, 1, 0}, 2);
        // makers 0+2: 2 + 1 + the last trick = 4 -> still pointsMade 1
        assertEquals(4, state.getTeamTricks(0));
        assertPointsAndResults(1, 0, TEAM_0_WINS);
    }

    @Test
    public void aMarchScoresTwo() {
        startPlay(state, Diamonds, 3, null);                  // the stuck dealer called
        playLastTrick(state, fm, new int[]{0, 2, 0, 2}, 3);
        // makers 1+3: 2 + 2 + the last trick = all 5 -> pointsMarch 2
        assertEquals(5, state.getTeamTricks(1));
        assertPointsAndResults(0, 2, TEAM_1_WINS);
    }

    @Test
    public void makersTakingTwoTricksAreEuchredAndTheDefendersScoreTwo() {
        startPlay(state, Diamonds, 1, null);
        playLastTrick(state, fm, new int[]{1, 1, 1, 1}, 0);
        // makers 1+3: 1 + 1 = 2 < 3 -> euchred: pointsEuchred 2 to the defenders 0+2, nothing to the makers
        assertEquals(2, state.getTeamTricks(1));
        assertPointsAndResults(2, 0, TEAM_0_WINS);
    }

    @Test
    public void makersTakingNoTricksAreEuchred() {
        startPlay(state, Diamonds, 0, null);
        playLastTrick(state, fm, new int[]{0, 2, 0, 2}, 1);
        // makers 0+2: 0 tricks -> euchred: 2 to the defenders 1+3 (not a march for the defenders)
        assertEquals(0, state.getTeamTricks(0));
        assertPointsAndResults(0, 2, TEAM_1_WINS);
    }

    @Test
    public void theDealIsNotScoredBeforeTheFifthTrick() {
        startPlay(state, Diamonds, 0, null);
        // the fourth trick, 3 played (makers 0+2 already have 3): each player still holds 2 cards
        giveHand(state, 0, "AC", "9S");
        giveHand(state, 1, "KC", "10S");
        giveHand(state, 2, "QC", "QH");
        giveHand(state, 3, "10C", "KH");
        arrangeTrick(state, 0);
        state.tricksTaken = new int[]{2, 0, 1, 0};
        assertEquals(12, state.getDiscardPile().getSize());   // 3 tricks of 4 cards
        playCards(state, fm, "AC", "KC", "QC", "10C");
        // player 0 wins the fourth trick (4 tricks to the makers), but one remains
        assertEquals(4, state.getTeamTricks(0));
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getTeamPoints(0));
        assertEquals(0, state.getTeamPoints(1));
        assertEquals(0, state.getCurrentPlayer());
    }

    // Going alone: player 2 calls Diamonds alone in round 2, player 0 sits out (keeping their 5 cards) and
    // the last trick has 3 cards.

    @Test
    public void aLoneMarchScoresFour() {
        startPlayAlone(state, Diamonds, 2, null);
        playLastTrickAlone(state, fm, new int[]{0, 0, 4, 0}, 2);
        // lone maker 2: 4 + the last trick = all 5 -> pointsAloneMarch 4
        assertArrayEquals(new int[]{0, 0, 5, 0}, state.tricksTaken);
        assertEquals(5, state.getPlayerHand(0).getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertAllCardsPresent(state);
        assertPointsAndResults(4, 0, TEAM_0_WINS);
    }

    @Test
    public void aLoneMakerTakingFourTricksScoresOne() {
        startPlayAlone(state, Diamonds, 2, null);
        playLastTrickAlone(state, fm, new int[]{0, 1, 3, 0}, 2);
        // 3 + the last trick = 4 -> pointsMade 1, as for a partnership
        assertEquals(4, state.getTeamTricks(0));
        assertPointsAndResults(1, 0, TEAM_0_WINS);
    }

    @Test
    public void aLoneMakerTakingThreeTricksScoresOne() {
        startPlayAlone(state, Diamonds, 2, null);
        playLastTrickAlone(state, fm, new int[]{0, 1, 2, 1}, 2);
        // 2 + the last trick = 3 -> pointsMade 1
        assertEquals(3, state.getTeamTricks(0));
        assertPointsAndResults(1, 0, TEAM_0_WINS);
    }

    @Test
    public void aEuchredLoneMakerGivesTheDefendersTwo() {
        startPlayAlone(state, Diamonds, 2, null);
        playLastTrickAlone(state, fm, new int[]{0, 1, 2, 1}, 1);
        // the defenders win the last trick: the lone maker has 2 < 3 -> pointsEuchred 2 to players 1+3
        assertEquals(2, state.getTeamTricks(0));
        assertEquals(5, state.getPlayerHand(0).getSize());
        assertPointsAndResults(0, 2, TEAM_1_WINS);
    }

    @Test
    public void theLastTrickOfALoneDealIsCompleteWithThreeCards() {
        startPlayAlone(state, Diamonds, 3, null);   // the stuck dealer alone: player 1 sits out
        playLastTrickAlone(state, fm, new int[]{1, 0, 1, 2}, 0);
        // order 0, 2, 3 (1 skipped); the AC wins for player 0: makers 1+3 took 2 -> euchred, 2 to players 0+2
        assertArrayEquals(new int[]{2, 0, 1, 2}, state.tricksTaken);
        assertEquals(5, state.getPlayerHand(1).getSize());
        assertPointsAndResults(2, 0, TEAM_0_WINS);
    }
}
