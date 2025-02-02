package evaluation.optimisation.ntbea.functions;

public class TestFunction002 extends NTBEAFunction {
    private static final double PI = Math.PI;

    @Override
    public double functionValue(double[] x) {
        double retValue = x[0]  *
                (1 - x[1] * x[1]) *
                Math.sin(2 * PI * x[2]) *
                Math.cos(2 * PI * x[3]) *
                (x[0] + x[1] + x[2] + x[3] + x[4]) *
                (x[0] * x[1] - x[2] * x[3]) *
                (x[4] * Math.sqrt(x[1] * x[2]));
        return Math.min(Math.max(0.3, Math.abs(retValue)), 0.95);
    }

    @Override
    public int dimension() {
        return 6;
    }
}
