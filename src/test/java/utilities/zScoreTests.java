package utilities;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class zScoreTests {

    @Test
    public void zScoreTest() {
        assertEquals(1.282, Utils.standardZScore(0.10, 1), 0.001);
        assertEquals(1.645, Utils.standardZScore(0.05, 1), 0.001);
        assertEquals(1.96, Utils.standardZScore(0.025, 1), 0.001);
        assertEquals(1.64, Utils.standardZScore(0.10, 2), 0.01);
        // and then check that the exact zScore calculation is an improvement on a simple Bonferroni ad
        assertTrue(Utils.standardZScore(0.10, 2) < Utils.standardZScore(0.05, 1));
    }
}
