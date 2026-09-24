package games.klaverjassen;

import core.CoreConstants.GameResult;
import core.Game;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.Suite.*;
import static games.klaverjassen.KlaverjassenTestUtils.*;
import static org.junit.Assert.*;

/**
 * The last trick of a one-hand game, arranged and then played through fm.next: the last trick bonus, roem and pit,
 * the nat and the end of the game. Player 0 chooses trumps, so team 0 is the trump team. The card points before
 * the last trick are 152 less the last trick's card points, so the hand totals 162.
 */
public class KlaverjassenEndOfHandTest {

    KlaverjassenGameState state;
    KlaverjassenForwardModel fm;

    @Before
    public void setup() {
        Game game = newGame(11);
        state = (KlaverjassenGameState) game.getGameState();
        fm = (KlaverjassenForwardModel) game.getForwardModel();
    }

    private void assertHandOver() {
        assertEquals(162, state.getHandPoints(0) + state.getHandPoints(1));
        assertEquals(8, state.getTricksWon(0) + state.getTricksWon(1));
        assertEquals(32, state.getDiscardPile().getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        for (int p = 0; p < 4; p++)
            assertEquals(0, state.getPlayerHand(p).getSize());
        assertAllCardsPresent(state);
        assertFalse("one hand: the game is over", state.isNotTerminal());
    }

    @Test
    public void theLastTrickEarnsTenAndATrumpTeamWithFewerPointsScoresNothing() {
        setTrumps(state, Diamonds);
        // before: 60 + 78 = 138 = 152 - 14
        arrangeLastTrick(state, 0, new int[]{60, 78}, new int[]{3, 4}, "8S", "AS", "7S", "QS");
        playCards(state, fm, "8S", "AS", "7S", "QS");
        // the Ace of Spades (player 1, team 1) wins: 8S 0 + AS 11 + 7S 0 + QS 3 = 14, + 10 last trick = 24
        assertArrayEquals(new int[]{60, 78 + 24}, state.handPoints);
        assertArrayEquals(new int[]{3, 5}, state.tricksWon);
        assertHandOver();

        // trump team 0 has 60 < 102: nat, team 1 scores 60 + 102 = 162
        assertArrayEquals(new int[]{0, 162}, state.teamScores);
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(162.0, state.getGameScore(1), 0.0);
        assertEquals(0.0, state.getGameScore(2), 0.0);
        assertEquals(162.0, state.getGameScore(3), 0.0);
        // both partners of team 1 win - the default endGame would call their shared first place a draw
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void aTrumpTeamWithMorePointsKeepsItsOwnAndBothPartnersWin() {
        setTrumps(state, Spades);
        // before: 80 + 52 = 132 = 152 - 20. Player 2 leads; play goes 2, 3, 0, 1
        arrangeLastTrick(state, 2, new int[]{80, 52}, new int[]{4, 3}, "8C", "9H", "JS", "7D");
        playCards(state, fm, "JS", "7D", "8C", "9H");
        // the Jack of Spades (trump, player 2) wins: 20 + 0 + 0 + 0 = 20, + 10 last trick = 30
        assertArrayEquals(new int[]{80 + 30, 52}, state.handPoints);
        assertArrayEquals(new int[]{5, 3}, state.tricksWon);
        assertHandOver();

        // 110 > 52: each team scores its own
        assertArrayEquals(new int[]{110, 52}, state.teamScores);
        assertEquals(110.0, state.getGameScore(0), 0.0);
        assertEquals(110.0, state.getGameScore(2), 0.0);
        assertEquals(52.0, state.getGameScore(1), 0.0);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void equalTotalsKeepTheirOwnAndTheGameIsDrawn() {
        setTrumps(state, Clubs);
        // before: 81 + 50 = 131 = 152 - 21. Player 1 leads; play goes 1, 2, 3, 0
        arrangeLastTrick(state, 1, new int[]{81, 50}, new int[]{4, 3}, "10D", "AH", "7H", "8H");
        playCards(state, fm, "AH", "7H", "8H", "10D");
        // the Ace of Hearts (player 1) wins: 11 + 0 + 0 + 10 = 21, + 10 last trick = 31
        assertArrayEquals(new int[]{81, 50 + 31}, state.handPoints);
        assertArrayEquals(new int[]{4, 4}, state.tricksWon);
        assertHandOver();

        // 81 = 81: not nat (strict), each keeps its own
        assertArrayEquals(new int[]{81, 81}, state.teamScores);
        assertArrayEquals(new GameResult[]{DRAW_GAME, DRAW_GAME, DRAW_GAME, DRAW_GAME}, state.getPlayerResults());
    }

    @Test
    public void equalTotalsAreNatWhenTieIsFailure() {
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("tieIsFailure", true);
        Game game = newGame(11, params);
        state = (KlaverjassenGameState) game.getGameState();
        fm = (KlaverjassenForwardModel) game.getForwardModel();
        // the same last trick as equalTotalsKeepTheirOwnAndTheGameIsDrawn
        setTrumps(state, Clubs);
        arrangeLastTrick(state, 1, new int[]{81, 50}, new int[]{4, 3}, "10D", "AH", "7H", "8H");
        playCards(state, fm, "AH", "7H", "8H", "10D");
        assertArrayEquals(new int[]{81, 50 + 31}, state.handPoints);
        assertHandOver();

        // 81 = 81 fails with tieIsFailure: team 1 scores 81 + 81 = 162, trump team 0 nothing; team 1 wins
        assertArrayEquals(new int[]{0, 162}, state.teamScores);
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void roemInTheLastTrickDecidesTheNatAgainstMoreCardPoints() {
        setTrumps(state, Hearts);
        // before: 100 + 26 = 126 = 152 - 26. Player 1 leads; play goes 1, 2, 3, 0
        arrangeLastTrick(state, 1, new int[]{100, 26}, new int[]{5, 2}, "JC", "JS", "JD", "JH");
        state.handRoem = new int[]{20, 0};
        playCards(state, fm, "JS", "JD", "JH", "JC");
        // the Jack of Hearts (trump, player 3, team 1) wins: JS 2 + JD 2 + JH 20 + JC 2 = 26, + 10 last trick = 36
        assertArrayEquals(new int[]{100, 26 + 36}, state.handPoints);
        assertArrayEquals(new int[]{5, 3}, state.tricksWon);
        // four Jacks: 200 roem to team 1
        assertArrayEquals(new int[]{20, 200}, state.handRoem);
        assertHandOver();

        // card points alone 100 > 62 would keep the trump team's points; with roem 100 + 20 = 120 v 62 + 200 = 262:
        // nat, team 1 scores 120 + 262 = 382
        assertArrayEquals(new int[]{0, 382}, state.teamScores);
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void takingTheLastTrickForAllEightScoresItsRoemAndThePit() {
        setTrumps(state, Diamonds);
        // before: 140 + 0 = 140 = 152 - 12; team 0 has taken all 7 tricks. Player 0 leads; play goes 0, 1, 2, 3
        arrangeLastTrick(state, 0, new int[]{140, 0}, new int[]{7, 0}, "10S", "9S", "8S", "JS");
        state.handRoem = new int[]{40, 0};
        playCards(state, fm, "10S", "9S", "8S", "JS");
        // the 10 of Spades (player 0) wins - it ranks above the Jack in a plain suit: 10 + 0 + 0 + 2 = 12, + 10 = 22
        assertArrayEquals(new int[]{140 + 22, 0}, state.handPoints);
        assertArrayEquals(new int[]{8, 0}, state.tricksWon);
        // 8-9-10-J of Spades is a run of four (50), and all 8 tricks is pit (100): 40 + 50 + 100 = 190
        assertArrayEquals(new int[]{190, 0}, state.handRoem);
        assertHandOver();

        // 162 + 190 = 352 v 0
        assertArrayEquals(new int[]{352, 0}, state.teamScores);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }
}
