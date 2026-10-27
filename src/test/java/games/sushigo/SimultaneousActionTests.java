package games.sushigo;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import core.components.Deck;
import games.GameType;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Applying one hand-built joint action, covering every player, to a SushiGo state: the forward
 * model must resolve the whole simultaneous turn in that single call, exactly as it does when the
 * players' choices arrive one at a time.
 */
public class SimultaneousActionTests {

    private static List<Integer> cardIds(Deck<SGCard> hand) {
        List<Integer> ids = new ArrayList<>();
        for (SGCard c : hand.getComponents()) ids.add(c.getComponentID());
        return ids;
    }

    private static List<String> cardNames(Deck<SGCard> hand) {
        List<String> names = new ArrayList<>();
        for (SGCard c : hand.getComponents()) names.add(c.toString());
        return names;
    }

    @Test
    public void jointActionResolvesTheWholeTurn() {
        Game g = GameType.SushiGo.createGameInstance(3, 12345);
        g.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        SGGameState state = (SGGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();

        int handSize = state.getPlayerHands().get(0).getSize();
        assertEquals(List.of(0, 1, 2), state.getCurrentSimultaneousPlayers());

        // everyone plays the first card in their hand; remember what each hand held apart from it
        Map<Integer, AbstractAction> choices = new HashMap<>();
        List<List<Integer>> remainder = new ArrayList<>();
        for (int p = 0; p < 3; p++) {
            choices.put(p, new ChooseCard(p, 0, false));
            List<Integer> ids = cardIds(state.getPlayerHands().get(p));
            ids.remove(0);
            remainder.add(ids);
        }
        fm.next(state, new SimultaneousAction(choices));

        // the turn has been resolved: choices cleared, one card gone from every hand
        for (int p = 0; p < 3; p++) {
            assertTrue("choices not cleared for P" + p, state.getCardChoices().get(p).isEmpty());
            assertEquals(handSize - 1, state.getPlayerHands().get(p).getSize());
            assertEquals(1, state.getPlayedCards().get(p).getSize());
        }
        // hands have rotated: player p now holds what player p+1 had, minus the card they played
        for (int p = 0; p < 3; p++)
            assertEquals("hand " + p + " after rotation", remainder.get((p + 1) % 3), cardIds(state.getPlayerHands().get(p)));
        // and it is a fresh simultaneous turn for everyone
        assertEquals(List.of(0, 1, 2), state.getCurrentSimultaneousPlayers());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void jointActionEqualsSequentialApplication() {
        // the same three choices applied one at a time reach the same state
        Game g1 = GameType.SushiGo.createGameInstance(3, 777);
        g1.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        Game g2 = GameType.SushiGo.createGameInstance(3, 777);
        g2.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        SGGameState joint = (SGGameState) g1.getGameState();
        SGGameState sequential = (SGGameState) g2.getGameState();

        Map<Integer, AbstractAction> choices = new HashMap<>();
        for (int p = 0; p < 3; p++) choices.put(p, new ChooseCard(p, 1, false));
        g1.getForwardModel().next(joint, new SimultaneousAction(choices));
        for (int p = 0; p < 3; p++) {
            assertEquals(p, sequential.getCurrentPlayer());
            g2.getForwardModel().next(sequential, new ChooseCard(p, 1, false));
        }
        // component ids are allocated globally, so two game instances differ in them: compare by card
        for (int p = 0; p < 3; p++) {
            assertEquals(cardNames(sequential.getPlayerHands().get(p)), cardNames(joint.getPlayerHands().get(p)));
            assertEquals(cardNames(sequential.getPlayedCards().get(p)), cardNames(joint.getPlayedCards().get(p)));
        }
        // Both start a fresh simultaneous turn for everyone. Note the turn owner differs: applied
        // one at a time, _afterAction hands the next turn to whoever chose last (P2 here); applied
        // jointly, the turn owner is unchanged (P0). Harmless, since the acting set is everyone
        // either way, but it is a visible difference between the two application paths.
        assertEquals(List.of(0, 1, 2), sequential.getCurrentSimultaneousPlayers());
        assertEquals(List.of(0, 1, 2), joint.getCurrentSimultaneousPlayers());
    }
}
