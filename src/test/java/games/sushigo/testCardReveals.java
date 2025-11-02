package games.sushigo;

import core.actions.AbstractAction;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class testCardReveals {

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
        // first we add two dumplings to player 0's hand
        state.getPlayerHands().get(0).draw();
        state.getPlayerHands().get(0).draw();
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.Dumpling));
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.Dumpling));
        for (int i = 0; i < 8; i++) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            AbstractAction action = (i % 4 != 0) ?
                    actions.stream().map(a -> (ChooseCard) a)
                            .filter(a -> state.getPlayerHands().get(0).get(a.cardIdx).type == SGCard.SGCardType.Dumpling)
                            .findFirst().orElseThrow(() -> new RuntimeException("No dumpling found")) :
                    actions.get(0);
            fm.next(state, action);
        }
        assertEquals(2, state.getPlayedCards().get(0).stream().filter(c -> c.type == SGCard.SGCardType.Dumpling).count());
        assertEquals(3, state.getGameScore(0), 0.001); // 2 dumplings = 3 points
    }
}
