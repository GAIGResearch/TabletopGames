package players.mcts;

import org.junit.Test;
import utilities.Utils;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class UtilsTests {

    Random rnd = new Random(4094);

    @Test
    public void testPDFWithLargeValues() {
        double[] values = new double[]{1e100, 1e100, 1e100, 1e100, 1e100};

        for (int exponent = 1; exponent < 10; exponent++) {
            for (int i = 0; i < values.length; i++) {
                values[i] = Math.pow(values[i], exponent);
            }
            double[] pdf = Utils.pdf(values);
            for (double p : pdf) {
                assertEquals(0.2, p, 1e-6);
            }
        }
    }

    @Test
    public void testPDFWithLargeValues2() {
        double[] values = new double[]{1e300, 3e100, 1e300, 0, 1e100};
        double[] expectedP = new double[]{0.5, 0.0, 0.5, 0.0, 0.0};

        double[] pdf = Utils.pdf(values);
        for (int i = 0; i < pdf.length; i++) {
            assertEquals(expectedP[i], pdf[i], 1e-6);
        }
    }

    @Test
    public void testPDFWithLargeValues3() {
        // at this point we get a numerical error
        // so default to uniform distribution
        double[] values = new double[]{1e308, 3e100, 1e308, 0, 1e100};
        double[] expectedP = new double[]{0.2, 0.2, 0.2, 0.2, 0.2};

        double[] pdf = Utils.pdf(values);
        for (int i = 0; i < pdf.length; i++) {
            assertEquals(expectedP[i], pdf[i], 1e-6);
        }
    }
}
