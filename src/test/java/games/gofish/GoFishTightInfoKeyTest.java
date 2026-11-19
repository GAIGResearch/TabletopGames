package games.gofish;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.gofish.actions.GoFishAsk;
import games.gofish.metrics.GoFishTightInfoKey;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.simple.RandomPlayer;

import java.util.*;

import static games.gofish.GoFishTestUtils.card;
import static games.gofish.GoFishTestUtils.giveHand;
import static org.junit.Assert.*;

public class GoFishTightInfoKeyTest {

    final GoFishTightInfoKey key = new GoFishTightInfoKey();
    final GoFishForwardModel fm = new GoFishForwardModel();

    static GoFishParameters valetParams() {
        GoFishParameters params = new GoFishParameters();
        TunableParameters.loadFromJSONFile(params, "data/gofish/GoFish_Valet.json");
        return params;
    }

    /**
     * A 3-player state reached by random play, not over and with cards left to draw.
     */
    GoFishGameState midGameState(long seed) {
        GoFishGameState state = GoFishTestUtils.newState(valetParams(), 3, seed);
        Random rnd = new Random(seed);
        int steps = 1 + rnd.nextInt(12);
        for (int i = 0; i < steps; i++) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            GoFishGameState next = (GoFishGameState) state.copy();
            fm.next(next, actions.get(rnd.nextInt(actions.size())));
            if (!next.isNotTerminal() || next.getDrawDeck().getSize() == 0)
                break;
            state = next;
        }
        return state;
    }

    static int code(FrenchCard card) {
        return card.suite.ordinal() * 100 + card.number;
    }

    @Test
    public void keyIsTheSameForEveryRedeterminisation() {
        for (int s = 0; s < 20; s++) {
            GoFishGameState state = midGameState(s);
            for (int p = 0; p < 3; p++) {
                Object expected = key.getKey(state, p);
                for (int i = 0; i < 100; i++)
                    assertEquals("seed " + s + " player " + p, expected, key.getKey(state.copy(p), p));
            }
        }
    }

    @Test
    public void withRestrictedKnowledgeTheKeyInTheSearchMatchesItsRedeterminisations() {
        // every set of knowledge kinds, one bit each
        for (int k = 0; k < 8; k++) {
            EnumSet<GoFishKnowledge> knowledge = GoFishKnowledge.of((k & 1) != 0, (k & 2) != 0, (k & 4) != 0);
            for (int s = 0; s < 20; s++) {
                GoFishGameState state = midGameState(s);
                int p = state.getCurrentPlayer();
                GoFishGameState search = (GoFishGameState) state.copy(p);
                search.restrictKnowledge(knowledge, p);
                // the search plays random actions from the agent's restricted observation
                Random rnd = new Random(s);
                for (int step = 0; step < 6 && search.isNotTerminal(); step++) {
                    List<AbstractAction> actions = fm.computeAvailableActions(search);
                    fm.next(search, actions.get(rnd.nextInt(actions.size())));
                    Object expected = key.getKey(search, p);
                    for (int i = 0; i < 20; i++)
                        assertEquals(knowledge + " seed " + s + " step " + step, expected,
                                key.getKey(search.copy(p), p));
                }
            }
        }
    }

    @Test
    public void withoutVoidsTheKeyHoldsNone() {
        GoFishGameState state = GoFishTestUtils.newState(valetParams(), 3, 1);
        giveHand(state, 0, card("7H"), card("2S"));
        giveHand(state, 1, card("3H"), card("4D"));
        giveHand(state, 2, card("4H"), card("5H"));
        state.getKnownVoids().record(2, 9);
        GoFishGameState obs = (GoFishGameState) state.copy(0);
        obs.restrictKnowledge(GoFishKnowledge.of(true, false, true), 0);
        fm.next(obs, new GoFishAsk(1, 7));       // Go fish: P1 is seen to hold no 7
        for (List<Integer> voids : key.getKey(obs, 0).knownVoids())
            assertEquals(List.of(), voids);
        fm.next(state, new GoFishAsk(1, 7));
        assertEquals(List.of(7), key.getKey(state, 0).knownVoids().get(1));
        assertEquals(List.of(9), key.getKey(state, 0).knownVoids().get(2));
    }

    @Test
    public void cardsPassedOnASuccessfulAskAreSeenByEveryone() {
        GoFishGameState state = GoFishTestUtils.newState(valetParams(), 3, 1);
        giveHand(state, 0, card("7H"), card("7D"), card("2S"));
        giveHand(state, 1, card("7C"), card("3H"));
        giveHand(state, 2, card("4H"), card("5H"));
        GoFishTightInfoKey.Key before = key.getKey(state, 2);
        fm.next(state, new GoFishAsk(1, 7));
        GoFishTightInfoKey.Key after = key.getKey(state, 2);
        assertNotEquals(before, after);
        // the asker showed one 7 to ask, and received the 7C face up
        assertTrue(after.knownHands().get(0).contains(code(card("7C"))));
        assertEquals(2, after.knownHands().get(0).size());
        assertEquals(List.of(code(card("3H"))), key.getKey(state, 1).knownHands().get(1));
    }

    @Test
    public void layingDownABookChangesTheKey() {
        GoFishGameState state = GoFishTestUtils.newState(valetParams(), 3, 1);
        giveHand(state, 0, card("7H"), card("7D"), card("7C"), card("2S"));
        giveHand(state, 1, card("7S"), card("3H"));
        giveHand(state, 2, card("4H"), card("5H"));
        GoFishTightInfoKey.Key before = key.getKey(state, 2);
        fm.next(state, new GoFishAsk(1, 7));
        GoFishTightInfoKey.Key after = key.getKey(state, 2);
        assertEquals(List.of(), before.books().get(0));
        assertEquals(List.of(7), after.books().get(0));
    }

    @Test
    public void swappingAnUnseenCardWithTheDrawDeckDoesNotChangeTheKey() {
        int swaps = 0;
        for (int s = 0; s < 20; s++) {
            GoFishGameState state = midGameState(s);
            int p = state.getCurrentPlayer();
            int opponent = (p + 1) % 3;
            PartialObservableDeck<FrenchCard> hand = state.getPlayerHands().get(opponent);
            Deck<FrenchCard> drawDeck = state.getDrawDeck();
            int idx = -1;
            for (int i = 0; i < hand.getSize(); i++)
                if (!hand.isVisibleTo(i, p) && hand.get(i).number != drawDeck.get(0).number)
                    idx = i;
            if (idx < 0) continue;
            Object before = key.getKey(state, p);
            Object ownerBefore = key.getKey(state, opponent);
            FrenchCard fromHand = hand.get(idx);
            hand.setComponent(idx, drawDeck.get(0));
            drawDeck.setComponent(0, fromHand);
            assertEquals("seed " + s, before, key.getKey(state, p));
            assertNotEquals("seed " + s, ownerBefore, key.getKey(state, opponent));
            swaps++;
        }
        assertTrue(swaps >= 15);
    }

    @Test
    public void mcgsAgentPlaysCompleteGames() {
        MCTSParams params = new MCTSParams();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", 50);
        params.setParameterValue("opponentTreePolicy", MCTSEnums.OpponentTreePolicy.MCGS);
        params.setParameterValue("MCGSStateKey", key);
        for (int g = 0; g < 20; g++) {
            Game game = GameType.GoFish.createGameInstance(3, g, valetParams());
            List<AbstractPlayer> players = List.of(new MCTSPlayer(params), new RandomPlayer(), new RandomPlayer());
            game.reset(players);
            game.run();
            assertFalse(game.getGameState().isNotTerminal());
        }
    }
}
