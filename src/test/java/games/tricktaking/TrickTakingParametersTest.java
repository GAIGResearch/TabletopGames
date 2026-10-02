package games.tricktaking;

import games.agram.AgramParameters;
import games.hearts.HeartsParameters;
import games.spades.SpadesParameters;
import games.whist.WhistParameters;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The general rememberVoids flag (ITrickTakingParameters) on each trick-taking game's parameters.
 */
public class TrickTakingParametersTest {

    /**
     * On a fresh object both the field (its initialiser - _reset has not run) and the registered tunable default
     * must be true; a mismatch between the two would change behaviour on the first setParameterValue of anything.
     */
    @Test
    public void rememberVoidsDefaultsToTrue() {
        assertTrue(new WhistParameters().rememberVoids());
        assertTrue(new AgramParameters().rememberVoids());
        assertTrue(new SpadesParameters().rememberVoids());
        assertTrue(new HeartsParameters().rememberVoids);

        assertEquals(true, new WhistParameters().getParameterValue("rememberVoids"));
        assertEquals(true, new AgramParameters().getParameterValue("rememberVoids"));
        assertEquals(true, new SpadesParameters().getParameterValue("rememberVoids"));
        assertEquals(true, new HeartsParameters().getParameterValue("rememberVoids"));
    }

    @Test
    public void rememberVoidsCanBeSwitchedOffAndSurvivesCopy() {
        WhistParameters whist = new WhistParameters();
        whist.setParameterValue("rememberVoids", false);
        assertFalse(whist.rememberVoids());
        assertFalse(((ITrickTakingParameters) whist.copy()).rememberVoids());

        AgramParameters agram = new AgramParameters();
        agram.setParameterValue("rememberVoids", false);
        assertFalse(agram.rememberVoids());
        assertFalse(((ITrickTakingParameters) agram.copy()).rememberVoids());
    }
}
