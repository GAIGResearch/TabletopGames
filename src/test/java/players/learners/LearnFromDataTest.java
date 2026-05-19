package players.learners;

import org.junit.Test;
import players.heuristics.GLMHeuristic;
import java.io.File;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class LearnFromDataTest {

    OLSLearner ols = new OLSLearner(1.0, 0.01, AbstractLearner.Target.SCORE, new TestFeatures());

    // This test requires --add-opens=java.base/sun.nio.ch=ALL-UNNAMED to be added to the VM command line arguments
    @Test
    public void testQuadraticFeatureIsUsed() {
        LearnFromData learnFromData = new LearnFromData("src\\test\\java\\players\\learners\\QuadraticTest.txt",
                new TestFeatures(), null,
                "src\\test\\java\\players\\learners\\QuadraticResults.txt",
                ols, 5, 10);
        learnFromData.debug = true;
        GLMHeuristic heuristic = (GLMHeuristic) learnFromData.learn();
        double[] coefficients = heuristic.coefficients();

        assertEquals(3, coefficients.length);
        assertEquals(0.0, coefficients[0], 0.25); // Bias
        assertEquals(0.0, coefficients[1], 0.25); // X
        assertEquals(0.0, coefficients[2], 0.01); // Y

        assertEquals(1, heuristic.interactionCoefficients().length);
        assertEquals(1.0, heuristic.interactionCoefficients()[0], 0.05);
        assertEquals(0, heuristic.interactions()[0][0]);
        assertEquals(0, heuristic.interactions()[0][1]);

        // then remove the files created
        File dir = new File("src\\test\\java\\players\\learners");
        for (File file : Objects.requireNonNull(dir.listFiles(f -> f.getName().startsWith("Improve")))) {
            file.delete();
        }
    }

    @Test
    public void testLinearInteraction() {
        LearnFromData learnFromData = new LearnFromData("src\\test\\java\\players\\learners\\InteractionTest.txt",
                new TestFeatures(), null,
                "src\\test\\java\\players\\learners\\LinearResults.txt",
                ols, 5, 10);
        learnFromData.debug = true;
        GLMHeuristic heuristic = (GLMHeuristic) learnFromData.learn();
        double[] coefficients = heuristic.coefficients();

        assertEquals(3, coefficients.length);
        assertEquals(0.0, coefficients[0], 0.25); // Bias
        assertEquals(1.0, coefficients[1], 0.05); // X
        assertEquals(0.5, coefficients[2], 0.05); // Y

        assertEquals(0, heuristic.interactionCoefficients().length);

        // then remove the files created
        File dir = new File("src\\test\\java\\players\\learners");
        for (File file : Objects.requireNonNull(dir.listFiles(f -> f.getName().startsWith("Improve")))) {
            file.delete();
        }
    }
}