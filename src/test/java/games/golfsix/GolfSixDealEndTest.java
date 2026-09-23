package games.golfsix;

import core.CoreConstants.GameResult;
import games.golfsix.actions.DiscardCard;
import games.golfsix.actions.DrawCard;
import games.golfsix.actions.ReplaceCard;
import games.golfsix.actions.TurnUp;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.golfsix.GolfSixTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of the deal, and with the default nDeals 1 and finalTurns false the end of the game: a grid all face-up
 * after a placement, or the safeguard; every card turned up, scores, results.
 */
public class GolfSixDealEndTest {

    final GolfSixForwardModel fm = new GolfSixForwardModel();

    /**
     * Player 0 has only position 4 (a 9H) face-down, and the 3S on top of the draw deck; every other player is past
     * the reveal with positions 0 and 1 face-up.
     */
    private GolfSixGameState oneCardFromTheEnd(int nPlayers, long seed) {
        GolfSixGameState state = newState(nPlayers, seed);
        setGrid(state, 0, "5H", "KS", "2C", "5D", "9H", "2D");
        // player 1 scores A/4 + J/Q + 10/10 = (1 + 4) + (10 + 10) + 0 = 25 (a Jack and a Queen are not a pair)
        setGrid(state, 1, "AH", "JC", "10S", "4D", "QC", "10H");
        skipReveal(state);
        faceUp(state, 0, 2, 3, 5);
        stackDrawDeck(state, "3S");
        assertEquals("UUUUDU", faceUpPattern(state, 0));
        return state;
    }

    @Test
    public void turningUpTheLastFaceDownCardEndsTheGameAndScoresEveryGrid() {
        GolfSixGameState state = oneCardFromTheEnd(2, 41);
        fm.next(state, new DrawCard(false));
        assertTrue(state.isNotTerminal());
        fm.next(state, new ReplaceCard(4));

        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(cards("5H", "KS", "2C", "5D", "3S", "2D"), state.getGrid(0).getComponents());
        assertEquals(card("9H"), state.getDiscardPile().peek());
        assertEquals("every card turned up", "UUUUUU", faceUpPattern(state, 0));
        assertEquals("every card turned up", "UUUUUU", faceUpPattern(state, 1));
        // player 0: 5/5 pair + K/3 + 2/2 pair = 0 + (0 + 3) + 0 = 3
        assertEquals(3, state.getScore(0));
        assertEquals(25, state.getScore(1));
        assertEquals(-3, state.getGameScore(0), 0.0);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void playersTiedOnTheLowestScoreDrawAndTheOthersLose() {
        GolfSixGameState state = oneCardFromTheEnd(3, 42);
        // player 2: A/2 + K/4 + 6/6 pair = (1 - 2) + (0 + 4) + 0 = 3, the same as player 0 will have
        setGrid(state, 2, "AS", "KH", "6C", "2H", "4C", "6S");
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));

        assertEquals(GAME_END, state.getGameStatus());
        assertEquals("UUUUUU", faceUpPattern(state, 2));
        assertEquals(3, state.getScore(0));
        assertEquals(25, state.getScore(1));
        assertEquals(3, state.getScore(2));
        assertArrayEquals(new GameResult[]{DRAW_GAME, LOSE_GAME, DRAW_GAME}, state.getPlayerResults());
    }

    @Test
    public void discardingWithOneCardFaceDownDoesNotEndTheDeal() {
        GolfSixGameState state = oneCardFromTheEnd(2, 41);
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());
        assertTrue(state.isNotTerminal());
        assertEquals("UUUUDU", faceUpPattern(state, 0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getScore(0));
    }

    /**
     * A 2-player state before the reveal. Player 0 would score 25 and player 1 3 (see above).
     */
    private GolfSixGameState safeguardState(int maxTurnsPerPlayer) {
        GolfSixParameters params = new GolfSixParameters();
        params.setParameterValue("maxTurnsPerPlayer", maxTurnsPerPlayer);
        params.setRandomSeed(43);
        GolfSixGameState state = newState(params, 2);
        setGrid(state, 0, "AH", "JC", "10S", "4D", "QC", "10H");
        // player 1: 5/5 pair + K/3 + 2/2 pair = 3
        setGrid(state, 1, "5H", "KS", "2C", "5D", "3S", "2D");
        return state;
    }

    @Test
    public void theDealIsScoredOnceEachPlayerHasHadMaxTurnsPerPlayerTurns() {
        // 2 players x 1 turn each = 2 turns: the reveal turns
        GolfSixGameState state = safeguardState(1);
        fm.next(state, new TurnUp(0));
        fm.next(state, new TurnUp(1));
        assertEquals(1, state.getTurnCounter());
        assertTrue("one turn so far", state.isNotTerminal());
        fm.next(state, new TurnUp(0));
        fm.next(state, new TurnUp(1));

        assertEquals(GAME_END, state.getGameStatus());
        assertEquals("UUUUUU", faceUpPattern(state, 0));
        assertEquals("UUUUUU", faceUpPattern(state, 1));
        assertEquals(25, state.getScore(0));
        assertEquals(3, state.getScore(1));
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void withTwoTurnsPerPlayerTheDealGoesOnAfterTheReveal() {
        // 2 players x 2 turns each = 4 turns: the reveal turns and one normal turn each
        GolfSixGameState state = safeguardState(2);
        fm.next(state, new TurnUp(0));
        fm.next(state, new TurnUp(1));
        fm.next(state, new TurnUp(0));
        fm.next(state, new TurnUp(1));
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());
        assertTrue("three turns so far", state.isNotTerminal());
        fm.next(state, new DrawCard(false));
        fm.next(state, new DiscardCard());

        assertEquals(GAME_END, state.getGameStatus());
        assertEquals("UUUUUU", faceUpPattern(state, 0));
        // the grids are unchanged by the discards
        assertEquals(25, state.getScore(0));
        assertEquals(3, state.getScore(1));
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }
}
