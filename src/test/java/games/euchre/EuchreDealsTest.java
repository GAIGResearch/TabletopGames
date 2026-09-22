package games.euchre;

import core.CoreConstants.GameResult;
import core.Game;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static core.components.FrenchCard.Suite.*;
import static games.euchre.EuchreTestUtils.*;
import static org.junit.Assert.*;

/**
 * A game to targetScore 10: the deal passing to the left with the points kept, and the game ending after the first
 * deal in which a team reaches 10. The last trick of the first deal is arranged and played through fm.next.
 */
public class EuchreDealsTest {

    EuchreGameState state;
    EuchreForwardModel fm;

    @Before
    public void setup() {
        Game game = newGame(21, params(10));
        state = (EuchreGameState) game.getGameState();
        fm = (EuchreForwardModel) game.getForwardModel();
        standardDeal(state);
        assertEquals(3, state.getDealer());
    }

    @Test
    public void theNextDealIsDealtByThePlayerOnTheLeftAndThePointsAreKept() {
        // player 0 called Hearts in round 1; dealer 3 took the 9H and discarded the KD; player 2 showed a spade void
        startPlay(state, Hearts, 0, "KD");
        state.knownVoids.get(2).add(Spades);
        playLastTrick(state, fm, new int[]{1, 1, 1, 1}, 0);
        // makers 0+2 took 1 + 1 + 1 = 3 -> 1 point; 1 < 10, so the game goes on

        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals(1, state.getTeamPoints(0));
        assertEquals(0, state.getTeamPoints(1));
        assertEquals("dealer 3 + 1", 0, state.getDealer());
        assertEquals("the new dealer's left bids first", 1, state.getCurrentPlayer());
        assertEquals(1, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentTrick().getSize());
        for (int p = 0; p < 4; p++) {
            assertEquals("hand " + p, 5, state.getPlayerHand(p).getSize());
            assertEquals("tricks " + p, 0, state.getTricksTaken(p));
            assertTrue("voids " + p, state.getKnownVoids().get(p).isEmpty());
        }
        assertEquals(4, state.getKitty().getSize());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(state.getKitty().peek(), state.getUpCard());
        assertNull(state.getTrumpSuit());
        assertEquals(-1, state.getMaker());
        assertEquals(0, state.getPasses());
        assertNull(state.getDealerDiscard());
        assertFalse(state.isAlone());
        assertTrue(state.isFirstBiddingRound());
        assertAllCardsPresent(state);
        assertEquals(passAndCalls(state.getUpCard().suite), available(state, fm));
    }

    @Test
    public void theGameGoesOnWhileNeitherTeamHasReachedTheTarget() {
        state.teamPoints = new int[]{8, 9};
        startPlay(state, Diamonds, 2, null);
        playLastTrick(state, fm, new int[]{1, 1, 1, 1}, 2);
        // makers 0+2: 3 tricks -> 8 + 1 = 9; team 1 stays at 9: both below 10
        assertTrue(state.isNotTerminal());
        assertEquals(9, state.getTeamPoints(0));
        assertEquals(9, state.getTeamPoints(1));
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getDealer());
    }

    @Test
    public void aMarchReachingExactlyTheTargetEndsTheGame() {
        state.teamPoints = new int[]{8, 9};
        startPlay(state, Diamonds, 0, null);
        playLastTrick(state, fm, new int[]{2, 0, 2, 0}, 0);
        // makers 0+2 march: 8 + 2 = 10 >= 10 -> game over; team 1 had 9
        assertFalse(state.isNotTerminal());
        assertEquals(10, state.getTeamPoints(0));
        assertEquals(9, state.getTeamPoints(1));
        assertEquals(0, state.getRoundCounter());
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void euchringTheMakersCanWinTheGameForTheDefenders() {
        state.teamPoints = new int[]{3, 8};
        startPlay(state, Hearts, 2, "KD");
        playLastTrick(state, fm, new int[]{1, 1, 0, 2}, 1);
        // makers 0+2 took 1 + 0 = 1 < 3: euchred, defenders 1+3 score 2 -> 8 + 2 = 10 -> game over
        assertFalse(state.isNotTerminal());
        assertEquals(3, state.getTeamPoints(0));
        assertEquals(10, state.getTeamPoints(1));
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }
}
