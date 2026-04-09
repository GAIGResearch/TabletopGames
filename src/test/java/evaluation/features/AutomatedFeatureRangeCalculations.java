package evaluation.features;

import core.interfaces.IStateFeatureVector;
import org.junit.Before;
import org.junit.Test;
import utilities.Pair;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class AutomatedFeatureRangeCalculations {

    // create ASF based on simple dominion state features
    IStateFeatureVector features = new TestFeatures();
    int estateIndex = Arrays.asList(features.names()).indexOf("estateCount");
    AutomatedFeatures asf = new AutomatedFeatures(features);

    @Before
    public void setUp() {
        assertEquals(0, estateIndex);
        asf.setBuckets(estateIndex, 3);
    }

    @Test
    public void testSimpleBucketing() {
        // details 0, 0, 0, 1, 1, 2, 2, 2, 3, 10, 12
        asf.processData("tmp.txt", 100, "src/test/java/evaluation/features/SimpleBucketData.txt");
        // now check correct buckets
        assertEquals(3, asf.getBuckets(estateIndex));
        assertEquals(4, asf.names().length);
        assertEquals("estateCount_B0", asf.names()[1]);
        assertEquals("estateCount_B1", asf.names()[2]);
        assertEquals("estateCount_B2", asf.names()[3]);
        assertEquals(Pair.of(Double.NEGATIVE_INFINITY, 1.0), asf.getColumnDetails().get(1).range());
        assertEquals(Pair.of(1.0, 2.0), asf.getColumnDetails().get(2).range());
        assertEquals(Pair.of(2.0, Double.POSITIVE_INFINITY), asf.getColumnDetails().get(3).range());
    }

    @Test
    public void testComplexBucketingI() {
        // details 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 3, 10, 12, 20
        asf.processData("tmp.txt", 100, "src/test/java/evaluation/features/ComplexBucketData_1.txt");
        assertEquals(3, asf.getBuckets(estateIndex));
        assertEquals(4, asf.names().length);
        assertEquals(Pair.of(Double.NEGATIVE_INFINITY, 1.0), asf.getColumnDetails().get(1).range());
        assertEquals(Pair.of(1.0, 2.0), asf.getColumnDetails().get(2).range());
        assertEquals(Pair.of(2.0, Double.POSITIVE_INFINITY), asf.getColumnDetails().get(3).range());
    }

    @Test
    public void testComplexBucketingII() {
        // in which the break occurs after the large number of identical values
        asf.processData("tmp.txt", 100, "src/test/java/evaluation/features/ComplexBucketData_2.txt");
        assertEquals(3, asf.getBuckets(estateIndex));
        assertEquals(4, asf.names().length);
        assertEquals(Pair.of(Double.NEGATIVE_INFINITY, 1.0), asf.getColumnDetails().get(1).range());
        assertEquals(Pair.of(1.0, 2.0), asf.getColumnDetails().get(2).range());
        assertEquals(Pair.of(2.0, Double.POSITIVE_INFINITY), asf.getColumnDetails().get(3).range());
    }

    @Test
    public void testComplexBucketingIII() {
        // in which the break occurs before the large number of identical values
        asf.processData("tmp.txt", 100, "src/test/java/evaluation/features/ComplexBucketData_3.txt");
        assertEquals(4, asf.getBuckets(estateIndex));
        assertEquals(5, asf.names().length);
        assertEquals(Pair.of(Double.NEGATIVE_INFINITY, 5.0), asf.getColumnDetails().get(1).range());
        assertEquals(Pair.of(5.0, 6.0), asf.getColumnDetails().get(2).range());
        assertEquals(Pair.of(6.0, 7.0), asf.getColumnDetails().get(3).range());
        assertEquals(Pair.of(7.0, Double.POSITIVE_INFINITY), asf.getColumnDetails().get(4).range());
    }
}
