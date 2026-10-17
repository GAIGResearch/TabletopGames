package games.skitgubbe;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.skitgubbe.actions.PickUp;
import games.skitgubbe.actions.PlayCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

/** Phase two and whole games in real games from the factory, driven only by fm.next. */
public class SkitgubbeFullGameTest {

    @Test
    public void scriptedPhaseTwoPlayedToTheEndOfTheGame() {
        Game game = newGame(3, 21);
        SkitgubbeGameState state = (SkitgubbeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // trumps Hearts; A = 0 to lead
        arrangePhaseTwo(state, "H", 0, "5S 2C 4D", "9S", "JS 3C");

        fm.next(state, new PlayCard(card("5S")));
        fm.next(state, new PlayCard(card("9S")));     // B's last card: out with exitScore = trickSize 3
        assertEquals(3, state.getExitScore(1));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("JS")));     // 3 cards: complete, discarded
        assertEquals(0, state.trick.getSize());
        assertEquals(2, state.trickSize);             // A {2C 4D}, C {3C}
        assertEquals(0, state.getCurrentPlayer());    // after C: A

        fm.next(state, new PlayCard(card("2C")));
        assertEquals(2, state.getCurrentPlayer());    // after A: B is out, so C
        // on 2C: 3C beats (higher Club)
        assertEquals(Set.of(new PlayCard(card("3C")), new PickUp()), new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, new PlayCard(card("3C")));     // C's last card: exitScore = trickSize 2; 2 cards complete

        // only A holds cards (4D): game over
        assertFalse(state.isNotTerminal());
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(3.0, state.getGameScore(1), 0.0);
        assertEquals(2.0, state.getGameScore(2), 0.0);
        assertEquals(3, state.getOrdinalPosition(0));
        assertEquals(1, state.getOrdinalPosition(1));
        assertEquals(2, state.getOrdinalPosition(2));
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertEquals(5, state.phaseTwoActions);
        assertAllCardsPresent(state);
    }

    /** Plays a whole game with random legal actions, checking conservation and the phase-two invariants each step. */
    private void randomFullGame(int nPlayers, int handSize, long seed) {
        SkitgubbeParameters params = SkitgubbeTestUtils.valetParams();
        params.setParameterValue("handSize", handSize);
        Game game = newGame(nPlayers, seed, params);
        SkitgubbeGameState state = (SkitgubbeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        int cap = params.maxPhaseTwoActions;
        Random rnd = new Random(seed);
        int steps = 0;
        // phase one takes at most 52 plays (plus bounces, each still one play), phase two at most about cap + a trick
        while (state.isNotTerminal() && steps++ < 2 * cap) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse("no actions at step " + steps + " in " + state.getGamePhase(), actions.isEmpty());
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
            assertAllCardsPresent(state);
            if (state.getGamePhase() == SkitgubbeGameState.Phase.PHASE_TWO) {
                assertTrue("a trick of trickSize cards was left on the table",
                        state.trick.getSize() < state.trickSize || state.trickSize == 0);
                for (int p = 0; p < nPlayers; p++) {
                    boolean holds = state.collectedCards.get(p).getSize() > 0;
                    assertEquals("player " + p + " has an exit score iff out", !holds, state.getExitScore(p) > 0);
                }
                if (state.isNotTerminal())
                    assertTrue("player to act holds no cards",
                            state.collectedCards.get(state.getCurrentPlayer()).getSize() > 0);
            }
        }
        assertFalse("seed " + seed + ": the game did not end within " + (2 * cap) + " actions", state.isNotTerminal());
        int holders = 0;
        for (int p = 0; p < nPlayers; p++)
            if (state.collectedCards.get(p).getSize() > 0) holders++;
        assertTrue("seed " + seed + ": ended with " + holders + " holders and " + state.phaseTwoActions + " phase-two actions",
                holders == 1 || state.phaseTwoActions >= cap);
        // the game ends as soon as the second-last player goes out, so a trick may be left unfinished
    }

    @Test
    public void randomFullGamesWithThreePlayersRunToTheEnd() {
        for (long seed = 1; seed <= 5; seed++) randomFullGame(3, 3, seed);
    }

    @Test
    public void randomFullGamesWithFourPlayersRunToTheEnd() {
        for (long seed = 1; seed <= 5; seed++) randomFullGame(4, 3, seed);
    }

    @Test
    public void randomFullGamesWithHandSizeTwoRunToTheEnd() {
        for (long seed = 1; seed <= 5; seed++) randomFullGame(3, 2, seed);
    }

    /** Plays a whole game with random legal actions, with completerLeads and exitOrderTiebreak on. */
    private void randomFullGameWithPagatOptions(int nPlayers, int handSize, long seed) {
        SkitgubbeParameters params = SkitgubbeTestUtils.valetParams();
        params.setParameterValue("handSize", handSize);
        params.setParameterValue("completerLeads", true);
        params.setParameterValue("exitOrderTiebreak", true);
        Game game = newGame(nPlayers, seed, params);
        SkitgubbeGameState state = (SkitgubbeGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        int cap = params.maxPhaseTwoActions;
        Random rnd = new Random(seed);
        int steps = 0, completions = 0;
        while (state.isNotTerminal() && steps++ < 2 * cap) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            boolean phaseTwo = state.getGamePhase() == SkitgubbeGameState.Phase.PHASE_TWO;
            int actor = state.getCurrentPlayer();
            // a card played on a trick of trickSize - 1 cards completes it
            boolean completing = phaseTwo && action instanceof PlayCard && state.trick.getSize() == state.trickSize - 1;
            boolean[] heldBefore = new boolean[nPlayers];
            for (int p = 0; p < nPlayers; p++) heldBefore[p] = state.collectedCards.get(p).getSize() > 0;
            fm.next(state, action);
            if (!phaseTwo) continue;
            // a player who has just gone out records the phase-two action count
            for (int p = 0; p < nPlayers; p++)
                if (heldBefore[p] && state.collectedCards.get(p).getSize() == 0)
                    assertEquals("seed " + seed + ": exitActions of " + p, state.phaseTwoActions, state.getExitActions(p));
            // after a completing play the trick is empty, and the completer leads if still holding cards
            if (completing && state.isNotTerminal()) {
                assertEquals(0, state.trick.getSize());
                if (state.collectedCards.get(actor).getSize() > 0) {
                    completions++;
                    assertEquals("seed " + seed + ": the completer leads", actor, state.getCurrentPlayer());
                }
            }
        }
        assertFalse(state.isNotTerminal());
        assertTrue("seed " + seed + ": no completer kept the lead", completions > 0);
        // players on equal non-zero scores are ranked by who went out first
        for (int p = 0; p < nPlayers; p++)
            for (int q = 0; q < nPlayers; q++)
                if (p != q && state.getExitScore(p) > 0 && state.getExitScore(p) == state.getExitScore(q)
                        && state.getExitActions(p) < state.getExitActions(q))
                    assertTrue("seed " + seed + ": " + p + " went out before " + q,
                            state.getOrdinalPosition(p) < state.getOrdinalPosition(q));
    }

    @Test
    public void randomFullGamesWithThePagatOptionsRunToTheEnd() {
        for (long seed = 1; seed <= 5; seed++) {
            randomFullGameWithPagatOptions(3, 3, seed);
            randomFullGameWithPagatOptions(4, 2, seed);
        }
    }
}
