package players.jsonBagPlayers;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JensenShannonDistance}, the metric used to compare two JSON-Bags.
 */
public class JensenShannonDistanceTest {

    private static Map<String, Integer> bag(Object... keyCountPairs) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < keyCountPairs.length; i += 2)
            map.put((String) keyCountPairs[i], (Integer) keyCountPairs[i + 1]);
        return map;
    }

    @Test
    public void normalizeProducesAProbabilityDistribution() {
        Map<String, Double> norm = JensenShannonDistance.normalize(bag("a", 10, "b", 30));
        assertEquals(1.0, norm.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-12);
        assertEquals(0.25, norm.get("a"), 1e-12);
        assertEquals(0.75, norm.get("b"), 1e-12);
    }

    @Test
    public void normalizeOfEmptyDistributionIsEmpty() {
        assertTrue(JensenShannonDistance.normalize(Collections.<String, Number>emptyMap()).isEmpty());
    }

    @Test
    public void normalizeDoesNotMutateItsInput() {
        Map<String, Integer> input = bag("a", 1, "b", 3);
        JensenShannonDistance.normalize(input);
        assertEquals(1, input.get("a").intValue());
        assertEquals(3, input.get("b").intValue());
    }

    @Test
    public void identicalDistributionsHaveZeroDistance() {
        Map<String, Integer> p = bag("a", 3, "b", 1);
        assertEquals(0.0, JensenShannonDistance.jsd(p, bag("a", 3, "b", 1)), 1e-12);
        // scaling a distribution must not change the distance
        assertEquals(0.0, JensenShannonDistance.jsd(p, bag("a", 30, "b", 10)), 1e-12);
    }

    @Test
    public void disjointDistributionsHaveMaximalDistance() {
        assertEquals(1.0, JensenShannonDistance.jsd(bag("a", 1), bag("b", 1)), 1e-12);
    }

    @Test
    public void distanceIsSymmetric() {
        Map<String, Integer> p = bag("a", 10, "b", 20, "c", 30);
        Map<String, Integer> q = bag("a", 15, "b", 25, "d", 10);
        assertEquals(JensenShannonDistance.jsd(p, q), JensenShannonDistance.jsd(q, p), 1e-12);
    }

    @Test
    public void distanceIsBoundedBetweenZeroAndOne() {
        Map<String, Integer> p = bag("a", 1, "b", 2, "c", 3);
        Map<String, Integer> q = bag("a", 9, "d", 1);
        double d = JensenShannonDistance.jsd(p, q);
        assertTrue("distance must be non-negative but was " + d, d >= 0.0);
        assertTrue("distance must not exceed 1 but was " + d, d <= 1.0);
    }

    @Test
    public void distanceMatchesHandComputedValue() {
        // p = (1/2, 1/2), q = (1, 0): m = (3/4, 1/4)
        // JSD = 0.5*(0.5*log2(2/3) + 0.5*log2(2)) + 0.5*(1*log2(4/3))
        double expected = Math.sqrt(0.5 * (0.5 * (Math.log(0.5 / 0.75) / Math.log(2))
                + 0.5 * (Math.log(0.5 / 0.25) / Math.log(2))
                + 1.0 * (Math.log(1.0 / 0.75) / Math.log(2))));
        assertEquals(expected, JensenShannonDistance.jsd(bag("a", 1, "b", 1), bag("a", 1)), 1e-12);
    }

    @Test
    public void distanceIsSmallerForMoreSimilarDistributions() {
        Map<String, Integer> prototype = bag("a", 5, "b", 5);
        double near = JensenShannonDistance.jsd(prototype, bag("a", 6, "b", 4));
        double far = JensenShannonDistance.jsd(prototype, bag("a", 9, "b", 1));
        assertTrue("more similar distributions must be closer", near < far);
    }

    @Test
    public void emptyDistributionsAreHandled() {
        assertEquals(0.0, JensenShannonDistance.jsd(bag(), bag()), 1e-12);
        assertEquals(1.0, JensenShannonDistance.jsd(bag("a", 1), bag()), 1e-12);
        assertEquals(1.0, JensenShannonDistance.jsd(bag(), bag("a", 1)), 1e-12);
        assertEquals(0.0, JensenShannonDistance.jsd(null, null), 1e-12);
    }
}
