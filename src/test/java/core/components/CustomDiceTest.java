package core.components;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Random;

public class CustomDiceTest {

    @Test
    public void testCustomDiceRollsAsExpected() {
        Dice dice = new Dice("src/test/resources/custom_d6.json");
        int[] counts = new int[6];
        Random rnd = new Random(4);
        for (int i = 0; i < 1000; i++) {
            dice.roll(rnd);
            int v = dice.getValue();
            assertTrue(v >= 1 && v <= 6);
            counts[v - 1]++;
        }
        int[] expected = new int[] {50, 100, 100, 200, 250, 300};
        for (int i = 0; i < 6; i++) {
            assertEquals("Side " + (i+1) + " count mismatch", expected[i], counts[i], 25);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCustomDiceInvalidPdfThrows() {
        Dice dice = new Dice("src/test/resources/invalid_custom_d6.json");
        dice.roll(new Random());
    }

    @Test
    public void testCustomDiceCopyHasSameBehaviour() {
        Dice original = new Dice("src/test/resources/custom_d6.json");
        Dice copy = original.copy();

        int[] countsOriginal = new int[6];
        int[] countsCopy = new int[6];
        Random rndOriginal = new Random(5);
        Random rndCopy = new Random(5);

        for (int i = 0; i < 1000; i++) {
            original.roll(rndOriginal);
            copy.roll(rndCopy);
            int vOrig = original.getValue();
            int vCopy = copy.getValue();
            assertTrue(vOrig >= 1 && vOrig <= 6);
            assertTrue(vCopy >= 1 && vCopy <= 6);
            countsOriginal[vOrig - 1]++;
            countsCopy[vCopy - 1]++;
            assertEquals("Rolls should match for same seed", vOrig, vCopy);
        }
        int[] expected = new int[] {50, 100, 100, 200, 250, 300};
        for (int i = 0; i < 6; i++) {
            assertEquals("Original side " + (i+1) + " count mismatch", expected[i], countsOriginal[i], 25);
            assertEquals("Copy side " + (i+1) + " count mismatch", expected[i], countsCopy[i], 25);
        }
    }
}
