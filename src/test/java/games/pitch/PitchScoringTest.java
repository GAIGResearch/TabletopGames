package games.pitch;

import core.CoreConstants;
import core.components.Deck;
import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.Suite.*;
import static games.pitch.PitchTestUtils.*;
import static org.junit.Assert.*;

/**
 * Scoring a deal, with the default RECYCLE counting unless a test sets countHighLowSeparately. The default
 * targetScore of 1 ends the game after the deal. Every arranged last trick has Hearts as trumps.
 */
public class PitchScoringTest {

    PitchParameters params;
    PitchForwardModel fm;
    PitchGameState state;

    @Before
    public void setup() {
        fm = new PitchForwardModel();
        state = newState(31, fm);
        params = (PitchParameters) state.getGameParameters();
    }

    private int[] points(FrenchCard.Suite trumps, Deck<FrenchCard> team0, Deck<FrenchCard> team1) {
        return PitchUtils.teamPoints(List.of(team0, team1), trumps, params);
    }

    // ---------------------------------------------------------------- PitchUtils.teamPoints

    @Test
    public void highLowAndJackGoToTheTeamsThatWonThem() {
        // trumps Hearts. Trumps played: AH (team 0), 2H, JH, 5H (team 1).
        // High AH -> team 0; Low 2H -> team 1; Jack JH -> team 1.
        // Game: team 0 AH 4 + 7C 0 + 10D 10 = 14; team 1 2H 0 + JH 1 + 5H 0 + KC 3 = 4 -> team 0.
        // team 0: High + Game = 2; team 1: Low + Jack = 2
        assertArrayEquals(new int[]{2, 2}, points(Hearts,
                deckOf("AH", "7C", "10D"),
                deckOf("2H", "JH", "5H", "KC")));
    }

    @Test
    public void highAndLowAreTheExtremesOfTheTrumpsPlayedNotOfTheSuit() {
        // trumps Hearts; the trumps played are only 9H (team 0) and 5H, 7H (team 1): High 9H, Low 5H.
        // Game: team 0 9H 0 + QC 2 = 2; team 1 0 -> team 0. team 0: High + Game = 2; team 1: Low = 1
        assertArrayEquals(new int[]{2, 1}, points(Hearts,
                deckOf("9H", "QC"),
                deckOf("5H", "7H", "2C")));
    }

    @Test
    public void noJackPointWhenTheJackOfTrumpsWasNotPlayed() {
        // trumps Hearts; trumps played AH, 3H (team 0), KH (team 1); JH never played. JC is not a trump Jack.
        // High AH and Low 3H -> team 0. Game: team 0 AH 4 + QC 2 = 6; team 1 KH 3 + 10C 10 + JC 1 = 14 -> team 1.
        // team 0: 2; team 1: 1
        assertArrayEquals(new int[]{2, 1}, points(Hearts,
                deckOf("AH", "3H", "QC"),
                deckOf("KH", "10C", "JC")));
    }

    @Test
    public void aLoneTrumpThatIsHighAndLowScoresOnce() {
        // trumps Diamonds; 7D is the only trump played: High and Low, one card -> 1 (RECYCLE).
        // Game: team 0 7D 0 + AC 4 + KS 3 = 7; team 1 QH 2 + 10H 10 = 12 -> team 1.
        // team 0: 1; team 1: 1
        assertArrayEquals(new int[]{1, 1}, points(Diamonds,
                deckOf("7D", "AC", "KS"),
                deckOf("QH", "10H", "2C")));
    }

    @Test
    public void aJackOfTrumpsThatIsAlsoLowScoresOnce() {
        // trumps Diamonds; trumps played JD, QD (team 0), AD (team 1). High AD -> team 1; Low JD and Jack JD ->
        // team 0, one card -> 1. Game: team 0 JD 1 + QD 2 = 3; team 1 AD 4 + 9C 0 = 4 -> team 1.
        // team 0: 1; team 1: High + Game = 2
        assertArrayEquals(new int[]{1, 2}, points(Diamonds,
                deckOf("JD", "QD"),
                deckOf("AD", "9C")));
    }

    @Test
    public void countedSeparatelyALoneTrumpThatIsHighAndLowScoresTwice() {
        // the deal of aLoneTrumpThatIsHighAndLowScoresOnce: 7D the only trump (team 0); Game -> team 1 (12 v 7).
        // default: team 0 1 (one card); team 1 Game 1
        assertArrayEquals(new int[]{1, 1}, points(Diamonds,
                deckOf("7D", "AC", "KS"),
                deckOf("QH", "10H", "2C")));
        params.setParameterValue("countHighLowSeparately", true);
        // separately: team 0 High 1 + Low 1 = 2; team 1 Game 1
        assertArrayEquals(new int[]{2, 1}, points(Diamonds,
                deckOf("7D", "AC", "KS"),
                deckOf("QH", "10H", "2C")));
    }

    @Test
    public void countedSeparatelyALoneTrumpJackScoresThree() {
        // trumps Diamonds; JD the only trump played (team 0): High, Low and Jack.
        // Game: team 0 JD 1 + 2C 0 = 1; team 1 QH 2 + 10H 10 = 12 -> team 1.
        // default: team 0 1 (one card); team 1 Game 1
        assertArrayEquals(new int[]{1, 1}, points(Diamonds,
                deckOf("JD", "2C"),
                deckOf("QH", "10H")));
        params.setParameterValue("countHighLowSeparately", true);
        // separately: team 0 High 1 + Low 1 + Jack 1 = 3; team 1 Game 1
        assertArrayEquals(new int[]{3, 1}, points(Diamonds,
                deckOf("JD", "2C"),
                deckOf("QH", "10H")));
    }

    @Test
    public void countedSeparatelyAJackOfTrumpsThatIsAlsoLowScoresTwice() {
        // the deal of aJackOfTrumpsThatIsAlsoLowScoresOnce: JD, QD (team 0), AD (team 1); Game -> team 1 (4 v 3).
        params.setParameterValue("countHighLowSeparately", true);
        // separately: team 0 Low 1 + Jack 1 = 2 (default 1); team 1 High 1 + Game 1 = 2
        assertArrayEquals(new int[]{2, 2}, points(Diamonds,
                deckOf("JD", "QD"),
                deckOf("AD", "9C")));
    }

    @Test
    public void equalGameTotalsGiveTheGamePointToNobody() {
        // trumps Spades. High AS -> team 0, Low 2S -> team 1.
        // Game: team 0 AS 4 + 10H 10 = 14; team 1 2S 0 + 10C 10 + AH 4 = 14 -> tie, nobody. [1, 1]
        assertArrayEquals(new int[]{1, 1}, points(Spades,
                deckOf("AS", "10H"),
                deckOf("2S", "10C", "AH")));
        // one more point of card value (JC 1) for team 1 gives it the Game point: [1, 2]
        assertArrayEquals(new int[]{1, 2}, points(Spades,
                deckOf("AS", "10H"),
                deckOf("2S", "10C", "AH", "JC")));
    }

    // ---------------------------------------------------------------- the last trick ends and scores the deal (fm.next)

    @Test
    public void thePitchingTeamMakesItsBidExactly() {
        // player 0 bid 3, the others passed.
        arrangeLastTrick(state,0, new int[]{3, 0, 0, 0}, 0,
                new String[]{"AH", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C",
                        "2D", "3D", "4D", "5D", "6D", "7D", "8D"},
                new String[]{"3H", "9D", "2S", "3S"},
                "10S", "4S", "2H", "5S");
        // last trick: 10S, 4S (follows), 2H (no spades: trumps it), 5S -> player 2 wins with the only trump
        play(state, fm, 0, "10S");
        play(state, fm, 1, "4S");
        play(state, fm, 2, "2H");
        play(state, fm, 3, "5S");

        // trumps played: AH, 2H (team 0), 3H (team 1). High AH, Low 2H -> team 0; JH not played.
        // Game: team 0 AH 4 + 10S 10 = 14 (the rest 0); team 1 0 -> team 0.
        // team 0: 3 points, bid 3: 3 >= 3 made -> +3. team 1: 0.
        assertEquals(20, state.getTeamTricks(0).getSize());
        assertArrayEquals(new int[]{3, 0},
                PitchUtils.teamPoints(state.teamTricks, Hearts, params));
        assertEquals(3, state.getTeamScore(0));
        assertEquals(0, state.getTeamScore(1));
        assertEquals(3.0, state.getGameScore(2), 0.0);
        assertEquals(0.0, state.getGameScore(3), 0.0);

        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        // both partners win
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME},
                state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aSetPitchingTeamLosesItsBidWhileTheOtherTeamScores() {
        // player 0 bid 2, player 1 bid 3, players 2 and 3 passed: player 1 is the pitcher (team 1, bid 3).
        arrangeLastTrick(state,1, new int[]{2, 3, 0, 0}, 3,
                new String[]{"AH", "10C", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "2D", "3D"},
                new String[]{"2H", "4D", "5D", "6D", "7D", "8D", "9D", "2S"},
                "3S", "4S", "5S", "JH");
        // last trick: player 3 leads JH (trump), 3S, 4S, 5S (no hearts) -> player 3 wins
        play(state, fm, 3, "JH");
        play(state, fm, 0, "3S");
        play(state, fm, 1, "4S");
        play(state, fm, 2, "5S");

        // trumps played: AH (team 0), 2H, JH (team 1). High AH -> team 0; Low 2H, Jack JH -> team 1.
        // Game: team 0 AH 4 + 10C 10 = 14; team 1 JH 1 -> team 0.
        // team 0: 2 points (not pitching) -> +2. team 1: 2 points < bid 3 -> set, -3.
        assertArrayEquals(new int[]{2, 2},
                PitchUtils.teamPoints(state.teamTricks, Hearts, params));
        assertEquals(2, state.getTeamScore(0));
        assertEquals(-3, state.getTeamScore(1));
        assertEquals(-3.0, state.getGameScore(1), 0.0);

        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME},
                state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void equalTeamScoresAreADrawForAll() {
        // player 0 bid 2, the others passed.
        arrangeLastTrick(state,0, new int[]{2, 0, 0, 0}, 0,
                new String[]{"AH", "10C", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "2D", "3D"},
                new String[]{"2H", "JH", "4D", "5D", "6D", "7D", "8D", "9D"},
                "9S", "8S", "7S", "6S");
        // last trick: 9S, 8S, 7S, 6S, no trumps -> player 0 wins with the highest spade (all worth 0)
        play(state, fm, 0, "9S");
        play(state, fm, 1, "8S");
        play(state, fm, 2, "7S");
        play(state, fm, 3, "6S");

        // trumps played: AH (team 0), 2H, JH (team 1). High -> team 0; Low and Jack -> team 1.
        // Game: team 0 AH 4 + 10C 10 = 14; team 1 JH 1 -> team 0.
        // team 0: 2 >= bid 2 -> +2; team 1: +2. 2 v 2: all draw.
        assertEquals(16, state.getTeamTricks(0).getSize());
        assertEquals(2, state.getTeamScore(0));
        assertEquals(2, state.getTeamScore(1));
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertArrayEquals(new CoreConstants.GameResult[]{DRAW_GAME, DRAW_GAME, DRAW_GAME, DRAW_GAME},
                state.getPlayerResults());
    }

    // ---------------------------------------------------------------- smudge (a bid of 5)

    /** Team 0 has won the first five tricks, including AH, 2H and JH; player 0 leads KH to the last trick. */
    private static final String[] ALL_FOUR_POINTS_BEFORE_THE_LAST_TRICK = {"AH", "2H", "JH", "10C",
            "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D"};

    /** Player 0 leads KH and wins the last trick: the others have no hearts. */
    private void playKingOfHeartsToWinTheLastTrick() {
        play(state, fm, 0, "KH");
        play(state, fm, 1, "3S");
        play(state, fm, 2, "4S");
        play(state, fm, 3, "5S");
    }

    @Test
    public void aSmudgeMadeScoresFive() {
        // player 0 bid 5 (smudge), the others passed.
        arrangeLastTrick(state,0, new int[]{5, 0, 0, 0}, 0, ALL_FOUR_POINTS_BEFORE_THE_LAST_TRICK, new String[]{},
                "KH", "3S", "4S", "5S");
        playKingOfHeartsToWinTheLastTrick();

        // team 0 won all 6 tricks (24 cards). Trumps AH, 2H, JH, KH: High AH, Low 2H, Jack JH, three different cards.
        // Game: team 0 AH 4 + JH 1 + KH 3 + 10C 10 = 18; team 1 0 -> team 0. team 0: 4 points, team 1: 0.
        // smudge: all tricks and exactly 4 points -> +5 (not +4). team 1: 0 points -> 0.
        assertEquals(24, state.getTeamTricks(0).getSize());
        assertEquals(6, state.getTricksWon(0));
        assertArrayEquals(new int[]{4, 0}, PitchUtils.teamPoints(state.teamTricks, Hearts, params));
        assertEquals(5, state.getTeamScore(0));
        assertEquals(0, state.getTeamScore(1));
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        // 5 v 0: team 0 wins
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME},
                state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aBidOfFourWinningAllTricksScoresOnlyItsPoints() {
        // the deal of aSmudgeMadeScoresFive, but player 0 bid 4.
        arrangeLastTrick(state,0, new int[]{4, 0, 0, 0}, 0, ALL_FOUR_POINTS_BEFORE_THE_LAST_TRICK, new String[]{},
                "KH", "3S", "4S", "5S");
        playKingOfHeartsToWinTheLastTrick();

        // team 0: all tricks, 4 points >= bid 4 -> +4 (only a bid of 5 scores 5). team 1: 0.
        assertEquals(24, state.getTeamTricks(0).getSize());
        assertEquals(4, state.getTeamScore(0));
        assertEquals(0, state.getTeamScore(1));
    }

    @Test
    public void aSmudgeWithAllTricksButOnlyThreePointsLosesFive() {
        // player 0 bid 5. The only trumps are AH (won before) and JH (player 0 leads it).
        arrangeLastTrick(state,0, new int[]{5, 0, 0, 0}, 0,
                new String[]{"AH", "10C", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C",
                        "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "10D", "QD"},
                new String[]{},
                "JH", "3S", "4S", "5S");
        play(state, fm, 0, "JH");
        play(state, fm, 1, "3S");
        play(state, fm, 2, "4S");
        play(state, fm, 3, "5S");

        // team 0 won all 24 cards. High AH; JH is both Low and Jack -> one card, 1 (RECYCLE).
        // Game: team 0 AH 4 + JH 1 + 10C 10 + 10D 10 + QD 2 = 27 v 0 -> team 0. points 1 + 1 + 1 = 3.
        // smudge needs exactly 4: -5. team 1: 0. 0 v -5: team 1 wins.
        assertEquals(24, state.getTeamTricks(0).getSize());
        assertArrayEquals(new int[]{3, 0}, PitchUtils.teamPoints(state.teamTricks, Hearts, params));
        assertEquals(-5, state.getTeamScore(0));
        assertEquals(0, state.getTeamScore(1));
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME},
                state.getPlayerResults());
    }

    @Test
    public void aSmudgeWithFourPointsButATrickLostLosesFive() {
        // player 0 bid 5. Team 1 won one trick of worthless diamonds (6D 7D 8D 9D); team 0 the other four so far.
        arrangeLastTrick(state,0, new int[]{5, 0, 0, 0}, 0,
                new String[]{"AH", "2H", "JH", "10C", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C",
                        "2D", "3D", "4D", "5D"},
                new String[]{"6D", "7D", "8D", "9D"},
                "KH", "3S", "4S", "5S");
        playKingOfHeartsToWinTheLastTrick();

        // team 0: 5 tricks (20 cards), team 1: 1 trick. team 0: High AH, Low 2H, Jack JH, Game 18 v 0 -> 4 points.
        // team 1: 0 points (with all four points gone to team 0 it cannot score).
        // not all six tricks -> smudge failed: -5. team 1: +0. 0 v -5: team 1 wins.
        assertEquals(5, state.getTricksWon(0));
        assertArrayEquals(new int[]{4, 0}, PitchUtils.teamPoints(state.teamTricks, Hearts, params));
        assertEquals(-5, state.getTeamScore(0));
        assertEquals(0, state.getTeamScore(1));
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME},
                state.getPlayerResults());
    }

    @Test
    public void countedSeparatelyASmudgeWithTheJackAsLowIsMade() {
        // the deal of aSmudgeWithAllTricksButOnlyThreePointsLosesFive, with countHighLowSeparately.
        params.setParameterValue("countHighLowSeparately", true);
        arrangeLastTrick(state,0, new int[]{5, 0, 0, 0}, 0,
                new String[]{"AH", "10C", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C",
                        "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "10D", "QD"},
                new String[]{},
                "JH", "3S", "4S", "5S");
        play(state, fm, 0, "JH");
        play(state, fm, 1, "3S");
        play(state, fm, 2, "4S");
        play(state, fm, 3, "5S");

        // team 0 won all 24 cards. High AH 1; JH Low 1 + Jack 1 (separately); Game 27 v 0 -> 1. 4 points.
        // smudge made: +5 (the default counting gives 3 and -5). team 1: 0.
        assertArrayEquals(new int[]{4, 0}, PitchUtils.teamPoints(state.teamTricks, Hearts, params));
        assertEquals(5, state.getTeamScore(0));
        assertEquals(0, state.getTeamScore(1));
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME},
                state.getPlayerResults());
    }

    @Test
    public void countedSeparatelyALoneTrumpJackScoresThreeInAWholeDeal() {
        // player 1 bid 2 (team 1), the others passed. JH is the only trump: player 3 leads it to the last trick.
        params.setParameterValue("countHighLowSeparately", true);
        arrangeLastTrick(state,1, new int[]{0, 2, 0, 0}, 3,
                new String[]{"10C", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "2D", "3D", "4D"},
                new String[]{"5D", "6D", "7D", "8D", "9D", "2S", "3S", "4S"},
                "5S", "6S", "7S", "JH");
        play(state, fm, 3, "JH");
        play(state, fm, 0, "5S");
        play(state, fm, 1, "6S");
        play(state, fm, 2, "7S");

        // player 3 wins with the only trump. team 1: JH is High, Low and Jack -> 3 (default: 1).
        // Game: team 0 10C 10; team 1 JH 1 -> team 0: 1.
        // team 1 (pitching, bid 2): 3 >= 2 -> +3 (with the default count, 1 < 2 would be set, -2). team 0: +1.
        assertEquals(1, state.getTeamScore(0));
        assertEquals(3, state.getTeamScore(1));
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME},
                state.getPlayerResults());
    }
}
