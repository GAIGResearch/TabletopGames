package games.puertorico;

import games.puertorico.*;
import games.puertorico.actions.SelectRole;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestProspector {

    PuertoRicoForwardModel fm = new PuertoRicoForwardModel();
    PuertoRicoGameState state = new PuertoRicoGameState(new PuertoRicoParameters(), 4);

    @Before
    public void setup() {
        fm.setup(state);
    }

    @Test
    public void prospectorAddsOneDoubloonAndPassesStraightOnToNextSelection() {
        assertEquals(2, state.getDoubloons(0));
        fm.next(state, new SelectRole(PuertoRicoConstants.Role.PROSPECTOR));
        assertEquals(3, state.getDoubloons(0));
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());
    }
}
