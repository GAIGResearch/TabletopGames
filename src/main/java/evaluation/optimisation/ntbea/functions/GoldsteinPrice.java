package evaluation.optimisation.ntbea.functions;

import static java.lang.Math.max;

public class GoldsteinPrice extends NTBEAFunction {

    @Override
    public int dimension() {return 2;}

    @Override
    public double functionValue(double[] x) {
        double x1 = x[0] * 4.0 - 2.0;
        double x2 = x[1] * 4.0 - 2.0;
        return max(0.0, (400.0 - (1.0 + Math.pow(x1 + x2 + 1, 2) * (19.0 - 14 * x1 + 3 * Math.pow(x1, 2) - 14 * x2 + 6 * x1 * x2 + 3 * Math.pow(x2, 2)) *
                (30.0 + Math.pow(2 * x1 - 3 * x2, 2) * Math.pow(18.0 - 32 * x1 + 12 * x1, 2) + 48 * x2 - 36 * x1 * x2 + 27 * Math.pow(x2, 2)))) / 500.0) / 4.0;

    }
}
