package games.golfsix;

import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.golfsix.actions.DiscardCard;
import games.golfsix.actions.DrawCard;
import games.golfsix.actions.ReplaceCard;
import games.golfsix.actions.TurnUp;
import org.junit.Test;

import java.util.*;

import static core.CoreConstants.GameResult.*;
import static games.golfsix.GolfSixTestUtils.*;
import static org.junit.Assert.*;

/**
 * Real games from the factory, driven by fm.next: scripted turns, a scripted whole deal, and seeded random games
 * played to the end.
 */
public class GolfSixGameFlowTest {

    static final Set<AbstractAction> DRAWS = Set.of(new DrawCard(false), new DrawCard(true));

    @Test
    public void revealThenTurnsGoRoundTheTableWithEachKindOfDrawAndPlacement() {
        Game game = newGame(3, 9);
        GolfSixGameState state = (GolfSixGameState) game.getGameState();
        GolfSixForwardModel fm = (GolfSixForwardModel) game.getForwardModel();

        step(fm, state, new TurnUp(0));
        step(fm, state, new TurnUp(4));
        assertEquals(1, state.getCurrentPlayer());
        step(fm, state, new TurnUp(1));
        step(fm, state, new TurnUp(2));
        step(fm, state, new TurnUp(3));
        step(fm, state, new TurnUp(5));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DRAWS, actions(fm, state));

        // player 0 takes the top discard and puts it on their face-up position 4
        FrenchCard firstDiscard = state.getDiscardPile().peek();
        FrenchCard p0Card4 = state.getGrid(0).get(4);
        step(fm, state, new DrawCard(true));
        assertEquals(firstDiscard, state.getDrawnCard());
        step(fm, state, new ReplaceCard(4));
        assertEquals(firstDiscard, state.getGrid(0).get(4));
        assertEquals(List.of(p0Card4), state.getDiscardPile().getComponents());
        assertEquals("UDDDUD", faceUpPattern(state, 0));
        assertEquals(1, state.getCurrentPlayer());

        // player 1 takes the card player 0 gave up, and puts it on their face-down position 0
        FrenchCard p1Card0 = state.getGrid(1).get(0);
        step(fm, state, new DrawCard(true));
        assertEquals(p0Card4, state.getDrawnCard());
        assertEquals(Set.of(new ReplaceCard(0), new ReplaceCard(1), new ReplaceCard(2), new ReplaceCard(3),
                new ReplaceCard(4), new ReplaceCard(5)), actions(fm, state));
        step(fm, state, new ReplaceCard(0));
        assertEquals(p0Card4, state.getGrid(1).get(0));
        assertEquals(List.of(p1Card0), state.getDiscardPile().getComponents());
        assertEquals("UUUDDD", faceUpPattern(state, 1));
        assertEquals(2, state.getCurrentPlayer());

        // player 2 draws from the draw deck and discards it; then it is player 0's turn again
        FrenchCard drawTop = state.getDrawDeck().peek();
        int drawDeckSize = state.getDrawDeck().getSize();
        step(fm, state, new DrawCard(false));
        assertEquals(drawTop, state.getDrawnCard());
        step(fm, state, new DiscardCard());
        assertEquals(List.of(drawTop, p1Card0), state.getDiscardPile().getComponents());
        assertEquals(drawDeckSize - 1, state.getDrawDeck().getSize());
        assertEquals("DDDUDU", faceUpPattern(state, 2));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DRAWS, actions(fm, state));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void aWholeDealIsPlayedAndScored() {
        Game game = newGame(2, 5);
        GolfSixGameState state = (GolfSixGameState) game.getGameState();
        GolfSixForwardModel fm = (GolfSixForwardModel) game.getForwardModel();
        setGrid(state, 0, "7H", "8H", "9H", "10H", "JH", "QH");
        setGrid(state, 1, "AH", "4C", "6D", "3H", "QD", "6S");
        // player 0 draws 2S, 7C, 8C, KS; player 1 draws 9S, 10S, JS in between
        stackDrawDeck(state, "2S", "9S", "7C", "10S", "8C", "JS", "KS");
        setTopDiscard(state, "5C");

        step(fm, state, new TurnUp(0));
        step(fm, state, new TurnUp(1));
        step(fm, state, new TurnUp(0));
        step(fm, state, new TurnUp(1));
        for (int pos = 2; pos < 6; pos++) {
            assertEquals(0, state.getCurrentPlayer());
            step(fm, state, new DrawCard(false));
            step(fm, state, new ReplaceCard(pos));
            if (pos == 5) break;
            assertTrue("player 0 still has a card face-down", state.isNotTerminal());
            assertEquals(1, state.getCurrentPlayer());
            step(fm, state, new DrawCard(false));
            step(fm, state, new DiscardCard());
        }

        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(cards("7H", "8H", "2S", "7C", "8C", "KS"), state.getGrid(0).getComponents());
        // the replaced cards and player 1's discards, the last on top, above the first discard
        assertEquals(cards("QH", "JS", "JH", "10S", "10H", "9S", "9H", "5C"), state.getDiscardPile().getComponents());
        assertEquals(52 - 12 - 1 - 7, state.getDrawDeck().getSize());
        assertEquals("UUUUUU", faceUpPattern(state, 1));
        // player 0: 7/7 pair + 8/8 pair + 2/K = 0 + 0 + (-2 + 0) = -2
        assertEquals(-2, state.getScore(0));
        // player 1: A/3 + 4/Q + 6/6 pair = (1 + 3) + (4 + 10) + 0 = 18
        assertEquals(18, state.getScore(1));
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void seededRandomGamesPlayToTheEndWithCorrectScoresAndResults() {
        int games = 0;
        for (int nPlayers = 2; nPlayers <= 4; nPlayers++) {
            for (long seed = 1; seed <= 5; seed++) {
                playRandomGame(nPlayers, seed);
                games++;
            }
        }
        assertEquals(15, games);
    }

    private void playRandomGame(int nPlayers, long seed) {
        String label = nPlayers + " players, seed " + seed + ": ";
        Game game = newGame(nPlayers, seed);
        GolfSixGameState state = (GolfSixGameState) game.getGameState();
        GolfSixForwardModel fm = (GolfSixForwardModel) game.getForwardModel();
        Random rnd = new Random(seed);
        int steps = 0;
        while (state.isNotTerminal() && steps++ < 3000) {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            assertFalse(label + "no actions available", available.isEmpty());
            fm.next(state, available.get(rnd.nextInt(available.size())));
            assertAllCardsPresent(state);
        }
        assertFalse(label + "game did not end within 3000 actions", state.isNotTerminal());
        // the safeguard ends the deal after nPlayers * 50 turns at most
        assertTrue(label + "turns " + state.getTurnCounter(), state.getTurnCounter() <= nPlayers * 50);

        int best = Integer.MAX_VALUE;
        int[] expected = new int[nPlayers];
        for (int p = 0; p < nPlayers; p++) {
            assertEquals(label + "every card face-up", "UUUUUU", faceUpPattern(state, p));
            expected[p] = oracleGridScore(state.getGrid(p).getComponents());
            assertEquals(label + "score of player " + p, expected[p], state.getScore(p));
            best = Math.min(best, expected[p]);
        }
        int nBest = 0;
        for (int s : expected) if (s == best) nBest++;
        for (int p = 0; p < nPlayers; p++) {
            GameResult result = expected[p] != best ? LOSE_GAME : nBest == 1 ? WIN_GAME : DRAW_GAME;
            assertEquals(label + "result of player " + p, result, state.getPlayerResults()[p]);
        }
    }
}
