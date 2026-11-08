package utilities;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for the Jensen-Shannon helpers in {@link Utils}, which are what JSON-Bag uses to compare
 * two token bags and what {@code ComparisonPlayer} uses to compare two policies.
 */
public class UtilsJensenShannonTest {

    private static Map<String, Integer> bag(Object... keyCountPairs) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < keyCountPairs.length; i += 2)
            map.put((String) keyCountPairs[i], (Integer) keyCountPairs[i + 1]);
        return map;
    }

    @Test
    public void normaliseProducesAProbabilityDistribution() {
        Map<String, Double> norm = Utils.normaliseDistribution(bag("a", 10, "b", 30));
        assertEquals(1.0, norm.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-12);
        assertEquals(0.25, norm.get("a"), 1e-12);
        assertEquals(0.75, norm.get("b"), 1e-12);
    }

    @Test
    public void normaliseOfEmptyDistributionIsEmpty() {
        assertTrue(Utils.normaliseDistribution(Collections.<String, Number>emptyMap()).isEmpty());
    }

    @Test
    public void normaliseDoesNotMutateItsInput() {
        Map<String, Integer> input = bag("a", 1, "b", 3);
        Utils.normaliseDistribution(input);
        assertEquals(1, input.get("a").intValue());
        assertEquals(3, input.get("b").intValue());
    }

    @Test
    public void normaliseOfZeroTotalGivesZerosRatherThanThrowing() {
        Map<String, Double> norm = Utils.normaliseDistribution(Map.of("a", 0.0, "b", 0.0));
        assertEquals(0.0, norm.get("a"), 1e-12);
        assertEquals(0.0, norm.get("b"), 1e-12);
    }

    @Test
    public void identicalDistributionsHaveZeroDistance() {
        Map<String, Integer> p = bag("a", 3, "b", 1);
        assertEquals(0.0, Utils.jensenShannonDistance(p, bag("a", 3, "b", 1)), 1e-12);
        // scaling a distribution must not change the distance
        assertEquals(0.0, Utils.jensenShannonDistance(p, bag("a", 30, "b", 10)), 1e-12);
    }

    @Test
    public void disjointDistributionsHaveMaximalDistance() {
        assertEquals(1.0, Utils.jensenShannonDistance(bag("a", 1), bag("b", 1)), 1e-12);
    }

    @Test
    public void distanceIsSymmetric() {
        Map<String, Integer> p = bag("a", 10, "b", 20, "c", 30);
        Map<String, Integer> q = bag("a", 15, "b", 25, "d", 10);
        assertEquals(Utils.jensenShannonDistance(p, q), Utils.jensenShannonDistance(q, p), 1e-12);
    }

    @Test
    public void distanceIsBoundedBetweenZeroAndOne() {
        Map<String, Integer> p = bag("a", 1, "b", 2, "c", 3);
        Map<String, Integer> q = bag("a", 9, "d", 1);
        double d = Utils.jensenShannonDistance(p, q);
        assertTrue("distance must be non-negative but was " + d, d >= 0.0);
        assertTrue("distance must not exceed 1 but was " + d, d <= 1.0);
    }

    @Test
    public void distanceMatchesHandComputedValue() {
        // p = (1/2, 1/2), q = (1, 0): m = (3/4, 1/4)
        // JSD = sqrt(0.5 * (0.5*log2(2/3) + 0.5*log2(2) + 1*log2(4/3)))
        double expected = Math.sqrt(0.5 * (0.5 * (Math.log(0.5 / 0.75) / Math.log(2))
                + 0.5 * (Math.log(0.5 / 0.25) / Math.log(2))
                + 1.0 * (Math.log(1.0 / 0.75) / Math.log(2))));
        assertEquals(expected, Utils.jensenShannonDistance(bag("a", 1, "b", 1), bag("a", 1)), 1e-12);
    }

    @Test
    public void distanceIsSmallerForMoreSimilarDistributions() {
        Map<String, Integer> prototype = bag("a", 5, "b", 5);
        double near = Utils.jensenShannonDistance(prototype, bag("a", 6, "b", 4));
        double far = Utils.jensenShannonDistance(prototype, bag("a", 9, "b", 1));
        assertTrue("more similar distributions must be closer", near < far);
    }

    @Test
    public void emptyDistributionsAreHandled() {
        assertEquals(0.0, Utils.jensenShannonDistance(bag(), bag()), 1e-12);
        assertEquals(1.0, Utils.jensenShannonDistance(bag("a", 1), bag()), 1e-12);
        assertEquals(1.0, Utils.jensenShannonDistance(bag(), bag("a", 1)), 1e-12);
        Map<String, Integer> noBag = null;
        assertEquals(0.0, Utils.jensenShannonDistance(noBag, noBag), 1e-12);
    }

    @Test
    public void keyedAndArrayDistancesAgree() {
        double[] p = {0.25, 0.75};
        double[] q = {0.5, 0.5};
        assertEquals(Utils.jensenShannonDistance(bag("a", 1, "b", 3), bag("a", 1, "b", 1)),
                Utils.jensenShannonDistance(p, q), 1e-12);
    }

    @Test
    public void divergenceIsTheSquareOfTheDistance() {
        double[] p = {0.1, 0.2, 0.7};
        double[] q = {0.3, 0.3, 0.4};
        assertEquals(Utils.jensenShannonDivergence(p, q),
                Utils.jensenShannonDistance(p, q) * Utils.jensenShannonDistance(p, q), 1e-12);
    }

    @Test
    public void arrayDistributionsMustHaveEqualLength() {
        try {
            Utils.jensenShannonDivergence(new double[]{0.5, 0.5}, new double[]{1.0});
            fail("expected IllegalArgumentException for mismatched lengths");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("equal length"));
        }
    }

    @Test
    public void negativeAndNaNMassesAreRejected() {
        try {
            Utils.jensenShannonDivergence(new double[]{-0.1, 1.1}, new double[]{0.5, 0.5});
            fail("expected IllegalArgumentException for negative mass");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Negative probability"));
        }
        try {
            Utils.jensenShannonDivergence(new double[]{Double.NaN, 1.0}, new double[]{0.5, 0.5});
            fail("expected IllegalArgumentException for NaN");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("NaN"));
        }
        try {
            Utils.normaliseDistribution(Map.of("a", -3.0, "b", 1.0));
            fail("expected IllegalArgumentException for negative counts");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Negative count"));
        }
    }
}
