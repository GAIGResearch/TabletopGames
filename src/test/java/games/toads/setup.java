package games.toads;

import org.junit.Test;

import static org.junit.Assert.*;

public class setup {

    @Test
    public void gameInitialisation() {
        ToadParameters params = new ToadParameters();
        ToadGameState state = new ToadGameState(params, 2);
        ToadForwardModel fm = new ToadForwardModel();
        fm.setup(state);

        assertEquals(5, state.playerDecks.get(0).getSize());
        assertEquals(5, state.playerDecks.get(1).getSize());
        assertEquals(4, state.playerHands.get(0).getSize());
        assertEquals(4, state.playerHands.get(1).getSize());
        assertEquals(0, state.playerDiscards.get(0).getSize());
        assertEquals(0, state.playerDiscards.get(1).getSize());
    }
}
