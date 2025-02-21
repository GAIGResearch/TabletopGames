package utilities;

public class Distance {

    /**
     * Credit: https://github.com/haifengl/smile/blob/master/math/src/main/java/smile/math/distance/MinkowskiDistance.java
     * <p>
     * Minkowski distance between the two arrays of type double.
     * NaN will be treated as missing values and will be excluded from the
     * calculation. Let m be the number non-missing values, and n be the
     * number of all values. The returned distance is pow(n * d / m, 1/p),
     * where d is the p-pow of distance between non-missing values.
     */
    public static double minkowski_distance(double[] x, double[] y, double p) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        int n = x.length;
        int m = 0;
        double dist = 0.0;

        for (int i = 0; i < x.length; i++) {
            if (!Double.isNaN(x[i]) && !Double.isNaN(y[i])) {
                m++;
                double d = Math.abs(x[i] - y[i]);
                dist += Math.pow(d, p);
            }
        }

        dist = n * dist / m;

        return Math.pow(dist, 1.0 / p);
    }

    /**
     * Java port by Raluca D. Gaina 2020, from https://github.com/mavillan/py-hausdorff
     * Calculates Manhattan distance.
     *
     * @param x - array with N dimensions
     * @param y - array with N dimensions
     * @return double
     * Distance between arrays
     */
    public static double manhattan_distance(double[] x, double[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        double ret = 0.0;
        for (int i = 0; i < x.length; i++) {
            ret += Math.abs(x[i] - y[i]);
        }
        return ret;
    }

    public static double manhattan_distance(int[] x, int[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        double ret = 0.0;
        for (int i = 0; i < x.length; i++) {
            ret += Math.abs(x[i] - y[i]);
        }
        return ret;
    }
    public static double manhattan_distance(Vector2D x, Vector2D y) {
        return Math.abs(x.getX()-y.getX()) + Math.abs(x.getY()-y.getY());
    }

    /**
     * Java port by Raluca D. Gaina 2020, from https://github.com/mavillan/py-hausdorff
     * Calculates Euclidian distance.
     *
     * @param x - array with N dimensions
     * @param y - array with N dimensions
     * @return double
     * Distance between arrays
     */
    public static double euclidian_distance(double[] x, double[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        double ret = 0.0;
        for (int i = 0; i < x.length; i++) {
            ret += Math.pow((x[i] - y[i]), 2);
        }
        return Math.sqrt(ret);
    }

    public static double euclidian_distance(int[] x, int[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        double ret = 0.0;
        for (int i = 0; i < x.length; i++) {
            ret += Math.pow((x[i] - y[i]), 2);
        }
        return Math.sqrt(ret);
    }

    /**
     * Java port by Raluca D. Gaina 2020, from https://github.com/mavillan/py-hausdorff
     * Calculates Chebyshev distance.
     *
     * @param x - array with N dimensions
     * @param y - array with N dimensions
     * @return double
     * Distance between arrays
     */
    public static double chebyshev_distance(double[] x, double[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        double ret = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < x.length; i++) {
            double d = Math.abs(x[i] - y[i]);
            if (d > ret) {
                ret = d;
            }
        }
        return ret;
    }

    public static double chebyshev_distance(Vector2D x, Vector2D y) {
        return chebyshev_distance(
                new double[]{x.getX(), x.getY()},
                new double[]{y.getX(), y.getY()}
        );
    }

    /**
     * Java port by Raluca D. Gaina 2020, from https://github.com/mavillan/py-hausdorff
     * Calculates cosine distance.
     *
     * @param x - array with N dimensions
     * @param y - array with N dimensions
     * @return double
     * Distance between arrays
     */
    public static double cosine_distance(double[] x, double[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        double xy_dot = 0.0;
        double x_norm = 0.0;
        double y_norm = 0.0;
        for (int i = 0; i < x.length; i++) {
            xy_dot += x[i] * y[i];
            x_norm += x[i] * x[i];
            y_norm += y[i] * y[i];
        }
        return 1.0 - xy_dot / (Math.sqrt(x_norm) * Math.sqrt(y_norm));
    }

    /**
     * Java port by Raluca D. Gaina 2020, from https://github.com/mavillan/py-hausdorff
     * Calculates Haversine distance.
     *
     * @param x - array with N dimensions
     * @param y - array with N dimensions
     * @return double
     * Distance between arrays
     */
    public static double haversine_distance(double[] x, double[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        double R = 6378.0;
        double radians = Math.PI / 180.0;
        double lat_x = radians * x[0];
        double lon_x = radians * x[1];
        double lat_y = radians * y[0];
        double lon_y = radians * y[1];
        double dlon = lon_y - lon_x;
        double dlat = lat_y - lat_x;
        double a = (Math.pow(Math.sin(dlat / 2.0), 2.0) + Math.cos(lat_x) *
                Math.cos(lat_y) * Math.pow(Math.sin(dlon / 2.0), 2.0));
        return R * 2 * Math.asin(Math.sqrt(a));
    }
}
