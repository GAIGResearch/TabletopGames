package games.sueca;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.sueca.SuecaTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Pagat's rubber (playRubber): the games scored for a deal, including the extra games from tied deals. targetGames is
 * 8 in these tests, so no single deal ends the rubber.
 */
public class SuecaRubberScoringTest {

    // team 0 piles for the arranged last trick (5S 2S 3S 4S, worth 0, won by team 0): card points in the comments
    // A 11 x 4 = 44 + 7S 10 + JS 3 + KS 4 = 61
    static final List<FrenchCard> PILE_61 = cards("AS", "AH", "AD", "AC", "7S", "JS", "KS", "6S");
    // A 11 x 4 = 44 + K 4 x 4 = 16: 60
    static final List<FrenchCard> PILE_60 = cards("AS", "AH", "AD", "AC", "KS", "KH", "KD", "KC");
    // A 11 x 4 = 44 + 7S 10 + QS 2 + JS 3 = 59 (team 1: 120 - 59 = 61)
    static final List<FrenchCard> PILE_59 = cards("AS", "AH", "AD", "AC", "7S", "QS", "JS", "6S");
    // A 11 x 4 = 44 + 7 10 x 4 = 40 + JS JH 3 x 2 = 6: 90
    static final List<FrenchCard> PILE_90 = cards("AS", "AH", "AD", "AC", "7S", "7H", "7D", "7C", "JS", "JH", "6S", "6H");
    // A 11 x 4 = 44 + 7 10 x 4 = 40 + KS 4 + JS 3: 91
    static final List<FrenchCard> PILE_91 = cards("AS", "AH", "AD", "AC", "7S", "7H", "7D", "7C", "KS", "JS", "6S", "6H");

    /**
     * Every card but the last trick's 2S 3S 4S 5S: all 120 card points in 36 cards (9 tricks).
     */
    static List<FrenchCard> allButLastTrick() {
        return FULL_PACK.stream().filter(c -> !(c.suite == FrenchCard.Suite.Spades && c.number >= 2 && c.number <= 5)).toList();
    }

    /**
     * All 120 card points but one trick of four Sixes (0 points) left to team 1: 32 cards.
     */
    static List<FrenchCard> allButLastTrickAndTheSixes() {
        return allButLastTrick().stream().filter(c -> c.number != 6).toList();
    }

    SuecaGameState state;
    SuecaForwardModel fm;

    @Before
    public void setUp() {
        state = newRubberState(11, 8);
        fm = new SuecaForwardModel();
    }

    // ---- SuecaUtils.gamesForDeal ----

    @Test
    public void sixtyOnePointsScoreOneGame() {
        assertEquals(1, SuecaUtils.gamesForDeal(61, false));
    }

    @Test
    public void ninetyPointsStillScoreOneGame() {
        assertEquals(1, SuecaUtils.gamesForDeal(90, false));
    }

    @Test
    public void ninetyOnePointsScoreTwoGames() {
        assertEquals(2, SuecaUtils.gamesForDeal(91, false));
    }

    @Test
    public void oneHundredAndNineteenPointsScoreTwoGames() {
        assertEquals(2, SuecaUtils.gamesForDeal(119, false));
    }

    @Test
    public void allOneHundredAndTwentyPointsWithoutEveryTrickScoreTwoGames() {
        assertEquals(2, SuecaUtils.gamesForDeal(120, false));
    }

    @Test
    public void allTenTricksScoreFourGames() {
        assertEquals(4, SuecaUtils.gamesForDeal(120, true));
    }

    // ---- scoreDeal, after the 10th trick through fm.next ----

    private void assertGames(int team0, int team1, int extra) {
        assertEquals("team 0 games", team0, state.getTeamGames(0));
        assertEquals("team 1 games", team1, state.getTeamGames(1));
        assertEquals("extra games", extra, state.getExtraGames());
    }

    @Test
    public void aSixtyOnePointDealScoresOneGameForTheWinnersOnly() {
        playZeroLastTrickWonByTeam0(state, fm, PILE_61);
        // team 0 61 + 0 = 61 (team 1 59): 1 game
        assertGames(1, 0, 0);
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void aNinetyPointDealScoresOneGame() {
        playZeroLastTrickWonByTeam0(state, fm, PILE_90);
        // team 0 90 (team 1 30): 1 game
        assertGames(1, 0, 0);
    }

    @Test
    public void aNinetyOnePointDealScoresTwoGames() {
        playZeroLastTrickWonByTeam0(state, fm, PILE_91);
        // team 0 91 (team 1 29): 2 games
        assertGames(2, 0, 0);
    }

    @Test
    public void allTheCardPointsWithATrickLostScoreTwoGamesNotFour() {
        playZeroLastTrickWonByTeam0(state, fm, allButLastTrickAndTheSixes());
        // team 0 120 points in 8 + 1 = 9 tricks; team 1 took the four Sixes (0 points): 2 games
        assertGames(2, 0, 0);
    }

    @Test
    public void allTenTricksScoreFourGamesInTheDeal() {
        playZeroLastTrickWonByTeam0(state, fm, allButLastTrick());
        // team 0 took 9 + 1 = 10 tricks: 4 games
        assertGames(4, 0, 0);
        assertTrue("4 < targetGames 8", state.isNotTerminal());
    }

    @Test
    public void teamOneScoresWhenItHasSixtyOneEvenIfTeamZeroTookTheLastTrick() {
        playZeroLastTrickWonByTeam0(state, fm, PILE_59);
        // team 0 59, team 1 120 - 59 = 61: 1 game to team 1, none to team 0
        assertGames(0, 1, 0);
    }

    @Test
    public void earlierGamesAreKeptAndAddedTo() {
        state.teamGames[0] = 2;
        state.teamGames[1] = 3;
        playZeroLastTrickWonByTeam0(state, fm, PILE_91);
        // team 0 2 + 2 = 4, team 1 stays 3
        assertGames(4, 3, 0);
    }

    @Test
    public void extraGamesAreAddedToAOneGameWinAndReset() {
        state.extraGames = 1;
        playZeroLastTrickWonByTeam0(state, fm, PILE_61);
        // 1 game for 61 + 1 extra = 2; the extra games go back to 0
        assertGames(2, 0, 0);
    }

    @Test
    public void extraGamesAreAddedToATwoGameWin() {
        state.extraGames = 1;
        playZeroLastTrickWonByTeam0(state, fm, PILE_91);
        // 2 games for 91 + 1 extra = 3
        assertGames(3, 0, 0);
    }

    @Test
    public void extraGamesGoToTheWinnersOfTheDealWhicheverTeam() {
        state.extraGames = 2;
        playZeroLastTrickWonByTeam0(state, fm, PILE_59);
        // team 1 61: 1 game + 2 extra = 3
        assertGames(0, 3, 0);
    }

    @Test
    public void aSixtyAllDealScoresNothingAndMakesTheNextDealWorthOneMore() {
        playZeroLastTrickWonByTeam0(state, fm, PILE_60);
        // 60 + 0 = 60 each
        assertGames(0, 0, 1);
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void twoTiedDealsInARowAddTwoExtraGamesWhichTheNextWinnersScore() {
        playZeroLastTrickWonByTeam0(state, fm, PILE_60);
        assertGames(0, 0, 1);
        // the second deal (dealt by the forward model) is arranged the same way
        playZeroLastTrickWonByTeam0(state, fm, PILE_60);
        assertGames(0, 0, 2);
        playZeroLastTrickWonByTeam0(state, fm, PILE_61);
        // 1 game for 61 + 2 extra = 3
        assertGames(3, 0, 0);
        assertEquals("three deals played", 3, state.getRoundCounter());
        assertAllCardsPresent(state);
    }

    @Test
    public void theSingleDealGameScoresNoGames() {
        // default parameters (no rubber): the deal ends the game, and teamGames / extraGames are untouched
        state = newState(11);
        playZeroLastTrickWonByTeam0(state, fm, PILE_91);
        assertFalse(state.isNotTerminal());
        assertGames(0, 0, 0);
        assertEquals(91, state.getGameScore(0), 0.0);
    }
}
