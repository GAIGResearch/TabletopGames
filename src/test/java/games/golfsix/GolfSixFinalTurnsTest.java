package games.golfsix;

import core.CoreConstants.GameResult;
import games.golfsix.actions.DiscardCard;
import games.golfsix.actions.DrawCard;
import games.golfsix.actions.ReplaceCard;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.golfsix.GolfSixTestUtils.*;
import static org.junit.Assert.*;

/**
 * finalTurns: once a grid is all face-up, each other player has one more turn, in turn order, and the
 * deal ends when the turn would pass back to the finisher. Without finalTurns the deal ends at once
 * (GolfSixDealEndTest).
 */
public class GolfSixFinalTurnsTest {

    final GolfSixForwardModel fm = new GolfSixForwardModel();

    // grid scores (default values), once every card is up:
    // FINISHING with 3S at position 4: 5/5 pair + K/3 + 2/2 pair = 0 + (0 + 3) + 0 = 3
    static final String[] FINISHING = {"5H", "KS", "2C", "5D", "9H", "2D"};
    // A/4 + J/Q + 10/10 pair = (1 + 4) + (10 + 10) + 0 = 25 (a Jack and a Queen are not a pair)
    static final String[] SCORES_25 = {"AH", "JC", "10S", "4D", "QC", "10H"};
    // 7/8 + K/4 + 6/6 pair = (7 + 8) + (0 + 4) + 0 = 19
    static final String[] SCORES_19 = {"7C", "KH", "6C", "8S", "4C", "6S"};

    /**
     * A 3-player state past the reveal (positions 0 and 1 up everywhere). The finisher's grid is FINISHING with only
     * position 4 (the 9H) face-down; the others have SCORES_25 and SCORES_19 in turn order after the finisher.
     */
    private GolfSixGameState threePlayers(int finisher, int maxTurnsPerPlayer, long seed) {
        GolfSixParameters params = variant(true, 1);
        params.setParameterValue("maxTurnsPerPlayer", maxTurnsPerPlayer);
        params.setRandomSeed(seed);
        GolfSixGameState state = newState(params, 3);
        setGrid(state, finisher, FINISHING);
        setGrid(state, (finisher + 1) % 3, SCORES_25);
        setGrid(state, (finisher + 2) % 3, SCORES_19);
        skipReveal(state);
        faceUp(state, finisher, 2, 3, 5);
        return state;
    }

    @Test
    public void turningUpTheLastCardSetsTheFinisherAndTheGameGoesOn() {
        GolfSixGameState state = threePlayers(0, 50, 51);
        stackDrawDeck(state, "3S");
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));

        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getFinisher());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals("UUUUUU", faceUpPattern(state, 0));
        assertEquals("the other grids are not turned up yet", "UUDDDD", faceUpPattern(state, 1));
        assertEquals("UUDDDD", faceUpPattern(state, 2));
        assertEquals("nothing scored yet", 0, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertAllCardsPresent(state);
    }

    @Test
    public void eachOtherPlayerHasExactlyOneMoreTurnThenTheDealIsScored() {
        GolfSixGameState state = threePlayers(0, 50, 52);
        stackDrawDeck(state, "3S", "8D", "QH");
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));

        // player 1's final turn: the 8D replaces the face-down 10S at position 2
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(2));
        assertTrue("player 2 still has a final turn", state.isNotTerminal());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.getFinisher());

        // player 2's final turn: the QH is discarded; the turn would pass back to player 0
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());

        assertEquals(GAME_END, state.getGameStatus());
        for (int p = 0; p < 3; p++)
            assertEquals("every card turned up", "UUUUUU", faceUpPattern(state, p));
        assertEquals(cards("AH", "JC", "8D", "4D", "QC", "10H"), state.getGrid(1).getComponents());
        assertEquals(3, state.getScore(0));
        // player 1: A/4 + J/Q + 8/10 = (1 + 4) + (10 + 10) + (8 + 10) = 43
        assertEquals(43, state.getScore(1));
        assertEquals(19, state.getScore(2));
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aPlayerWhoFinishesInTheirFinalTurnDoesNotStartMoreFinalTurns() {
        GolfSixGameState state = threePlayers(0, 50, 53);
        // player 1 also has only position 4 (the QC) face-down
        faceUp(state, 1, 2, 3, 5);
        stackDrawDeck(state, "3S", "8D", "QH");
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));

        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));
        assertEquals("UUUUUU", faceUpPattern(state, 1));
        assertTrue("player 2 still has a final turn", state.isNotTerminal());
        assertEquals("the first finisher stays the finisher", 0, state.getFinisher());
        assertEquals(2, state.getCurrentPlayer());

        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());

        assertEquals("no final turns for players 2 and 0 after player 1", GAME_END, state.getGameStatus());
        assertEquals(3, state.getScore(0));
        // player 1: A/4 + J/8 + 10/10 pair = (1 + 4) + (10 + 8) + 0 = 23
        assertEquals(23, state.getScore(1));
        assertEquals(19, state.getScore(2));
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void finalTurnsGoRoundPastPlayerZeroAndStopBeforeTheFinisher() {
        // player 2 finishes; players 0 (SCORES_25) and 1 (SCORES_19) then have their final turns
        GolfSixGameState state = threePlayers(2, 50, 54);
        stackDrawDeck(state, "8D", "QH", "3S", "7D", "JD");
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));

        assertTrue(state.isNotTerminal());
        assertEquals(2, state.getFinisher());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());
        assertTrue("player 1 still has a final turn", state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());

        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(25, state.getScore(0));
        assertEquals(19, state.getScore(1));
        assertEquals(3, state.getScore(2));
        assertArrayEquals(new GameResult[]{LOSE_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void theSafeguardStillEndsTheDealDuringFinalTurns() {
        // maxTurnsPerPlayer 1 with 3 players: the deal is scored after 3 turns (none spent on the reveal here).
        // Player 1 finishes on turn 2; player 2's final turn is turn 3, so player 0 never gets a final turn.
        GolfSixGameState state = threePlayers(1, 1, 55);
        stackDrawDeck(state, "8D", "3S", "QH");
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getFinisher());
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());

        assertEquals(GAME_END, state.getGameStatus());
        // player 2 has SCORES_25, player 0 SCORES_19
        assertEquals(19, state.getScore(0));
        assertEquals(3, state.getScore(1));
        assertEquals(25, state.getScore(2));
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }
}
