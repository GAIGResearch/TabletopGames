package core;

import core.actions.ActionSpace;
import org.junit.Test;

import static org.junit.Assert.*;

public class CoreParametersTest {

    @Test
    public void equalsComparesValues() {
        CoreParameters a = new CoreParameters();
        CoreParameters b = new CoreParameters();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        b.setParameterValue("verbose", true);
        assertNotEquals(a, b);
    }

    @Test
    public void copyKeepsParameterValues() {
        CoreParameters params = new CoreParameters();
        params.setParameterValue("verbose", true);
        params.setParameterValue("actionSpaceStructure", ActionSpace.Structure.Deep);

        CoreParameters copy = (CoreParameters) params.copy();

        assertTrue(copy.verbose);
        assertEquals(ActionSpace.Structure.Deep, copy.actionSpace.structure);
        assertEquals(params, copy);
    }

    @Test
    public void defaultActionSpaceMatchesTheTunableDefaults() {
        CoreParameters params = new CoreParameters();
        assertTrue(params.actionSpace.isDefault());
        // a copy rebuilds the action space from the tunable parameter values
        assertEquals(params.actionSpace, ((CoreParameters) params.copy()).actionSpace);
    }
}
