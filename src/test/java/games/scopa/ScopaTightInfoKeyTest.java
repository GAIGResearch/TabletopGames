package games.scopa;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.TarotCard;
import games.GameType;
import games.scopa.metrics.ScopaTightInfoKey;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.*;

public class ScopaTightInfoKeyTest {

    final ScopaTightInfoKey key = new ScopaTightInfoKey();
    final ScopaForwardModel fm = new ScopaForwardModel();

    /**
     * A state reached by random play, not over and with cards left in the draw deck.
     */
    ScopaGameState midGameState(long seed) {
        ScopaGameState state = ScopaTestUtils.newState(seed, fm);
        Random rnd = new Random(seed);
        int steps = 1 + rnd.nextInt(24);
        for (int i = 0; i < steps; i++) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            ScopaGameState next = (ScopaGameState) state.copy();
            fm.next(next, actions.get(rnd.nextInt(actions.size())));
            if (!next.isNotTerminal() || next.getDrawDeck().getSize() == 0)
                break;
            state = next;
        }
        return state;
    }

    @Test
    public void keyIsTheSameForEveryRedeterminisation() {
        for (int s = 0; s < 20; s++) {
            ScopaGameState state = midGameState(s);
            for (int p = 0; p < 2; p++) {
                Object expected = key.getKey(state, p);
                for (int i = 0; i < 100; i++)
                    assertEquals("seed " + s + " player " + p, expected, key.getKey(state.copy(p), p));
            }
        }
    }

    @Test
    public void aCaptureChangesTheOpponentsKey() {
        int captures = 0;
        for (int s = 0; s < 40 && captures < 20; s++) {
            ScopaGameState state = midGameState(s);
            int p = state.getCurrentPlayer();
            Optional<PlayCard> capture = fm.computeAvailableActions(state).stream()
                    .map(a -> (PlayCard) a).filter(PlayCard::isCapture).findFirst();
            if (capture.isEmpty()) continue;
            ScopaTightInfoKey.Key before = key.getKey(state, 1 - p);
            fm.next(state, capture.get());
            ScopaTightInfoKey.Key after = key.getKey(state, 1 - p);
            assertEquals(before.captured().get(p).size() + capture.get().captured.size() + 1,
                    after.captured().get(p).size());
            assertNotEquals(before.table(), after.table());
            captures++;
        }
        assertEquals(20, captures);
    }

    @Test
    public void swappingTheOpponentsCardWithTheDrawDeckDoesNotChangeTheKey() {
        int swaps = 0;
        for (int s = 0; s < 20; s++) {
            ScopaGameState state = midGameState(s);
            int p = state.getCurrentPlayer();
            Deck<TarotCard> hand = state.getPlayerHand(1 - p);
            Deck<TarotCard> drawDeck = state.getDrawDeck();
            if (hand.getSize() == 0) continue;  // the opponent has played their last card of the hand
            Object before = key.getKey(state, p);
            Object ownerBefore = key.getKey(state, 1 - p);
            TarotCard fromHand = hand.get(0);
            hand.setComponent(0, drawDeck.get(0));
            drawDeck.setComponent(0, fromHand);
            assertEquals("seed " + s, before, key.getKey(state, p));
            assertNotEquals("seed " + s, ownerBefore, key.getKey(state, 1 - p));
            swaps++;
        }
        assertTrue(swaps >= 10);
    }

    @Test
    public void mcgsAgentPlaysCompleteGames() {
        MCTSParams params = new MCTSParams();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", 50);
        params.setParameterValue("opponentTreePolicy", MCTSEnums.OpponentTreePolicy.MCGS);
        params.setParameterValue("MCGSStateKey", key);
        for (int g = 0; g < 20; g++) {
            Game game = GameType.Scopa.createGameInstance(2, g, ScopaTestUtils.valetParams());
            List<AbstractPlayer> players = List.of(new MCTSPlayer(params), new RandomPlayer());
            game.reset(players);
            game.run();
            assertFalse(game.getGameState().isNotTerminal());
        }
    }
}
