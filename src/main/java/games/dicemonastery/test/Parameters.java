package games.dicemonastery.test;

import games.dicemonastery.DiceMonasteryParams;
import org.junit.*;

import static org.junit.Assert.*;

public class Parameters {

    @Test
    public void tunableParametersCopied() {
        DiceMonasteryParams params = new DiceMonasteryParams(43);
        params.setParameterValue("COST_PER_TREASURE_VP", 8);

        DiceMonasteryParams copy = (DiceMonasteryParams) params.copy();

        assertEquals(8, copy.COST_PER_TREASURE_VP);

    }
}
