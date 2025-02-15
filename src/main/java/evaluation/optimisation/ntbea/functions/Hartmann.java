package evaluation.optimisation.ntbea.functions;

public class Hartmann extends NTBEAFunction {

    double[][] a;
    double[] c;
    double[][] p;

    public Hartmann(double[][] a,
                    double[] c,
                    double[][] p) {
        this.a = a;
        this.c = c;
        this.p = p;
    }

    @Override
    public int dimension() {
        return a[0].length;
    }

    @Override
    public double functionValue(double[] x) {
        double retValue = 0.0;
        for (int i = 0; i < c.length; i++) {
            double intermediate = 0.0;
            for (int j = 0; j < a[0].length; j++) {
                intermediate -= a[i][j] * (x[j] - p[i][j]) * (x[j] - p[i][j]);
            }
            retValue += c[i] * Math.exp(intermediate);
        }
        return retValue / 4.0;
    }


    public static Hartmann Hartmann3 = new Hartmann(
            new double[][]{
                    {3.0, 10.0, 30.0},
                    {0.1, 10.0, 35.0},
                    {3.0, 10.0, 30.0},
                    {0.1, 10.0, 35.0}
            },
            new double[]{1.0, 1.2, 3.0, 3.2},
            new double[][]{
                    {0.3689, 0.1170, 0.2673},
                    {0.4699, 0.4387, 0.7470},
                    {0.1091, 0.8732, 0.5547},
                    {0.03815, 0.5743, 0.8828}
            });

    public static Hartmann Hartmann6 = new Hartmann(
            new double[][]{
                    {10.0, 3.0, 17.0, 3.5, 1.7, 8.0},
                    {0.05, 10.0, 17.0, 0.1, 8.0, 14.0},
                    {3.0, 3.5, 1.7, 10.0, 17.0, 8.0},
                    {17.0, 8.0, 0.05, 10.0, 0.1, 14.0}
            },
            new double[]{1.0, 1.2, 3.0, 3.2},
            new double[][]{
                    {0.1312, 0.1696, 0.5569, 0.0124, 0.8283, 0.5886},
                    {0.2329, 0.4135, 0.8307, 0.3736, 0.1004, 0.9991},
                    {0.2548, 0.1451, 0.3522, 0.2883, 0.3047, 0.665},
                    {0.4047, 0.8828, 0.8732, 0.5743, 0.1091, 0.0381}
            });
}
