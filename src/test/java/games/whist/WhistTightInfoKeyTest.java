package games.whist;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.tricktaking.PlayCard;
import games.whist.metrics.WhistTightInfoKey;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.*;

import static org.junit.Assert.*;

public class WhistTightInfoKeyTest {

    final WhistTightInfoKey key = new WhistTightInfoKey();
    final WhistForwardModel fm = new WhistForwardModel();

    /**
     * A state reached by random play from a new deal: some cards into the deal, and not over.
     */
    WhistGameState midGameState(long seed) {
        WhistGameState state = WhistTestUtils.newState(seed);
        Random rnd = new Random(seed);
        int steps = 1 + rnd.nextInt(50);
        for (int i = 0; i < steps && state.isNotTerminal(); i++) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
        }
        assertTrue(state.isNotTerminal());
        return state;
    }

    static int code(FrenchCard card) {
        return card.suite.ordinal() * 100 + card.number;
    }

    @Test
    public void keyIsTheSameForEveryRedeterminisation() {
        for (int s = 0; s < 20; s++) {
            WhistGameState state = midGameState(s);
            for (int p = 0; p < 4; p++) {
                Object expected = key.getKey(state, p);
                for (int i = 0; i < 100; i++)
                    assertEquals("seed " + s + " player " + p, expected, key.getKey(state.copy(p), p));
            }
        }
    }

    @Test
    public void cardsPlayedToCompletedTricksAreCreditedToTheirPlayers() {
        for (int s = 0; s < 20; s++) {
            WhistGameState state = midGameState(s);
            List<FrenchCard> inTrick = state.getCurrentTrick().getComponents();
            List<List<Integer>> expected = new ArrayList<>();
            for (int p = 0; p < 4; p++)
                expected.add(new ArrayList<>());
            for (Pair<Integer, AbstractAction> entry : state.getHistory()) {
                FrenchCard card = (FrenchCard) ((PlayCard<?>) entry.b).card;
                if (!inTrick.contains(card))
                    expected.get(entry.a).add(code(card));
            }
            expected.forEach(Collections::sort);
            assertEquals("seed " + s, expected, key.getKey(state, 0).playedCards());
        }
    }

    @Test
    public void cardsAreCreditedCorrectlyInLaterDeals() {
        for (int s = 0; s < 20; s++) {
            WhistGameState state = WhistTestUtils.newState(s, new WhistParameters());
            Random rnd = new Random(s);
            int steps = 52 + rnd.nextInt(3 * 52 - 1);
            for (int i = 0; i < steps; i++) {
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                fm.next(state, actions.get(rnd.nextInt(actions.size())));
            }
            List<FrenchCard> inTrick = state.getCurrentTrick().getComponents();
            List<List<Integer>> expected = new ArrayList<>();
            for (int p = 0; p < 4; p++)
                expected.add(new ArrayList<>());
            List<Pair<Integer, AbstractAction>> history = state.getHistory();
            // each deal is 52 cards
            for (Pair<Integer, AbstractAction> entry : history.subList(52 * state.getRoundCounter(), history.size())) {
                FrenchCard card = (FrenchCard) ((PlayCard<?>) entry.b).card;
                if (!inTrick.contains(card))
                    expected.get(entry.a).add(code(card));
            }
            expected.forEach(Collections::sort);
            assertEquals("seed " + s, expected, key.getKey(state, 0).playedCards());
        }
    }

    @Test
    public void playingACardChangesEveryKey() {
        for (int s = 0; s < 20; s++) {
            WhistGameState state = midGameState(s);
            List<WhistTightInfoKey.Key> before = new ArrayList<>();
            for (int p = 0; p < 4; p++)
                before.add(key.getKey(state, p));
            AbstractAction action = fm.computeAvailableActions(state).get(0);
            int played = code((FrenchCard) ((PlayCard<?>) action).card);
            fm.next(state, action);
            for (int p = 0; p < 4; p++) {
                WhistTightInfoKey.Key after = key.getKey(state, p);
                assertNotEquals(before.get(p), after);
                boolean seen = after.currentTrick().stream().anyMatch(c -> c.get(1) == played)
                        || after.playedCards().stream().anyMatch(cards -> cards.contains(played));
                assertTrue("seed " + s + " player " + p, seen);
            }
        }
    }

    @Test
    public void swappingCardsBetweenOpponentsDoesNotChangeTheKey() {
        for (int s = 0; s < 20; s++) {
            WhistGameState state = midGameState(s);
            int p = state.getCurrentPlayer();
            Deck<FrenchCard> handA = state.getPlayerHand((p + 1) % 4);
            Deck<FrenchCard> handB = state.getPlayerHand((p + 2) % 4);
            Object before = key.getKey(state, p);
            Object ownerBefore = key.getKey(state, (p + 1) % 4);
            FrenchCard a = handA.stream().filter(c -> !c.equals(state.getTrumpCard())).findFirst().orElseThrow();
            FrenchCard b = handB.stream().filter(c -> !c.equals(state.getTrumpCard())).findFirst().orElseThrow();
            handA.remove(a);
            handB.remove(b);
            handA.add(b);
            handB.add(a);
            assertEquals("seed " + s, before, key.getKey(state, p));
            assertNotEquals("seed " + s, ownerBefore, key.getKey(state, (p + 1) % 4));
        }
    }

    @Test
    public void mcgsAgentPlaysCompleteGames() {
        MCTSParams params = new MCTSParams();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", 50);
        params.setParameterValue("opponentTreePolicy", MCTSEnums.OpponentTreePolicy.MCGS);
        params.setParameterValue("MCGSStateKey", key);
        for (int g = 0; g < 20; g++) {
            Game game = GameType.Whist.createGameInstance(4, g, WhistTestUtils.valetParams());
            List<AbstractPlayer> players = List.of(new MCTSPlayer((MCTSParams) params.copy()), new RandomPlayer(),
                    new MCTSPlayer((MCTSParams) params.copy()), new RandomPlayer());
            game.reset(players);
            game.run();
            assertFalse(game.getGameState().isNotTerminal());
        }
    }
}
