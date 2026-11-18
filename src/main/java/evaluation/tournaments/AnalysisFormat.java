package evaluation.tournaments;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for the text tables written by the tournament analyses: tab-separated columns that line up, and
 * confidence intervals from a mean and standard error.
 */
public class AnalysisFormat {

    // above this many independent observations confidence intervals use the Normal, otherwise the t-distribution
    public static final int NORMAL_THRESHOLD = 100;

    // characters per tab stop, as in most consoles and editors
    public static final int TAB_WIDTH = 8;

    /**
     * Lays out rows of cells (the first row is usually the header) as lines with tab-separated columns. Each column
     * starts at a tab stop past the end of the longest cell in the previous column, so that columns line up for
     * names and numbers of any length.
     */
    public static List<String> tabTable(List<String[]> rows) {
        int nCols = rows.stream().mapToInt(r -> r.length).max().orElse(0);
        int[] stops = new int[nCols]; // width of each column in tab stops
        for (String[] row : rows)
            for (int c = 0; c < row.length; c++)
                stops[c] = Math.max(stops[c], row[c].length() / TAB_WIDTH + 1);
        List<String> lines = new ArrayList<>();
        for (String[] row : rows) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < row.length; c++) {
                sb.append(row[c]);
                if (c < row.length - 1)
                    sb.append("\t".repeat(stops[c] - row[c].length() / TAB_WIDTH));
            }
            lines.add(sb.append("\n").toString());
        }
        return lines;
    }

    /**
     * Two-sided critical value for a confidence interval on a mean estimated from n independent observations
     * (games, or seeds for a seed-clustered SE). This uses the Normal for n > 100, and otherwise the t-distribution
     * with n - 1 degrees of freedom. NaN if n < 2.
     *
     * @param confidence - e.g. 0.95 for a 95% interval
     */
    public static double criticalValue(double confidence, int n) {
        double p = 1.0 - (1.0 - confidence) / 2.0;
        if (n > NORMAL_THRESHOLD)
            return new NormalDistribution().inverseCumulativeProbability(p);
        if (n < 2)
            return Double.NaN;
        return new TDistribution(n - 1).inverseCumulativeProbability(p);
    }

    /**
     * Describes the distribution that criticalValue uses for n independent observations.
     */
    public static String distribution(int n) {
        if (n > NORMAL_THRESHOLD) return "the Normal";
        if (n < 2) return "no distribution (too few observations)";
        return "the t-distribution with " + (n - 1) + " degrees of freedom";
    }

    /**
     * The interval mean +/- critical * se, as "[lower, upper]" with each bound to the given number of decimal places.
     */
    public static String interval(double mean, double se, double critical, int decimals) {
        String fmt = "[%." + decimals + "f, %." + decimals + "f]";
        return String.format(fmt, mean - critical * se, mean + critical * se);
    }
}
