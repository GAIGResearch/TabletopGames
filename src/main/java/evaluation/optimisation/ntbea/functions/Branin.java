package evaluation.optimisation.ntbea.functions;

import static java.lang.Math.PI;

public class Branin extends NTBEAFunction {

    @Override
    public int dimension() {return 2;}

    @Override
    public double functionValue(double[] x) {
        double x1 = x[0] * 15.0 - 5.0;
        double x2 = x[1] * 15.0;
        double a = 1.0;
        double b = 5.1 / (4 * PI * PI);
        double c = 5.0 / PI;
        double d = 6.0;
        double e = 10.0;
        double f = 1.0 / 8.0 / PI;
        return Math.max(0.0, (-(a * Math.pow(x2 - b * x1 * x1 + c * x1 - d, 2) + e * (1.0 - f) * Math.cos(x1) + e) + 10.0) / 12.0);
    }
}
