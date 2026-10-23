package games.sushigo;

import core.actions.AbstractAction;
import core.components.Deck;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static games.sushigo.cards.SGCard.SGCardType.Dumpling;
import static games.sushigo.cards.SGCard.SGCardType.EggNigiri;
import static org.junit.Assert.assertEquals;

public class CardRevealsTest {

    SGGameState state;
    SGParameters params;
    SGForwardModel fm = new SGForwardModel();

    @Before
    public void setup() {
        params = new SGParameters();
        params.setRandomSeed(1234);
        state = new SGGameState(params, 4);
        fm.setup(state);
    }

    @Test
    public void testTwoDumplings() {
        // two dumplings in every hand, so player 0 always has one, whichever hand it is passed
        for (Deck<SGCard> hand : state.getPlayerHands()) {
            int size = hand.getSize();
            List<SGCard> others = new ArrayList<>(hand.stream().filter(c -> c.type != Dumpling).toList());
            while (others.size() < size - 2)
                others.add(new SGCard(EggNigiri));
            hand.clear();
            hand.add(new SGCard(Dumpling));
            hand.add(new SGCard(Dumpling));
            hand.add(others.subList(0, size - 2));
        }
        // for two rounds, player 0 always plays a dumpling and the others never do
        for (int i = 0; i < 2 * state.getNPlayers(); i++) {
            int player = state.getCurrentPlayer();
            Deck<SGCard> hand = state.getPlayerHands().get(player);
            Predicate<SGCard> wanted = player == 0 ? c -> c.type == Dumpling : c -> c.type != Dumpling;
            AbstractAction action = fm.computeAvailableActions(state).stream()
                    .map(a -> (ChooseCard) a)
                    .filter(a -> !a.useChopsticks && wanted.test(hand.get(a.cardIdx)))
                    .findFirst().orElseThrow(() -> new AssertionError("No card to choose for player " + player));
            fm.next(state, action);
        }
        assertEquals(2, state.getPlayedCards().get(0).stream().filter(c -> c.type == Dumpling).count());
        assertEquals(3, state.getGameScore(0), 0.001); // 2 dumplings = 3 points
    }
}
