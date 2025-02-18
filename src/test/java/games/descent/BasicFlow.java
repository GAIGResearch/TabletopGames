package games.descent;

import games.descent2e.DescentForwardModel;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import org.junit.Before;
import org.junit.Test;

public class BasicFlow {

    DescentGameState state;
    DescentForwardModel fm = new DescentForwardModel();

    @Before
    public void setup() {
        // a rather cruddy way of ensuring we get the right hero in the right place
            DescentParameters params = new DescentParameters();
            params.setRandomSeed(234);
            state = new DescentGameState(new DescentParameters(), 2);
            fm.setup(state);
    }

    @Test
    public void testEndTurnProcess() {
        // we run through checking that:
        // Each hero has all their actions in turn first
        // then We move through the monsters in order before the round ends
    }

    @Test
    public void testMonsterRemoved() {
        // we run through checking that:
        // A monster is removed from the board when it is defeated (and does not take its turn)
    }

}
