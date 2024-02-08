package core;

import games.puertorico.PuertoRicoParameters;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParameterCopy {

    @Test
    public void copyingAbstractParametersShouldNotChangeOriginal() {
        PuertoRicoParameters params = new PuertoRicoParameters();
        params.setRandomSeed(1234);
        PuertoRicoParameters paramsCopy = (PuertoRicoParameters) params.copy();
        assertEquals(1234, params.getRandomSeed());
        assertNotEquals(params.getRandomSeed(), paramsCopy.getRandomSeed());
        assertEquals(params.hashCode(), paramsCopy.hashCode());
    }
}
