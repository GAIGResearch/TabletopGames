package evaluation.summarisers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static evaluation.summarisers.TAGStatSummary.StatType.Numeric;

/**
 * This class is used to model the statistics of several numbers.  For the statistics
 * we choose here it is not necessary to store all the numbers - just keeping a running total
 * of how many, the sum and the sum of the squares is sufficient (plus max and min, for max and min).
 */

public class TAGNumericStatSummary extends TAGStatSummary {

    private double sum, sumsq;
    private double min, max;
    private double mean, median, sd;
    private double lastAdded;
    private boolean valid;

    private ArrayList<Double> elements;

    public TAGNumericStatSummary() {
        this("");
    }

    public TAGNumericStatSummary(String name) {
        super(name, Numeric);
    }

    public void reset() {
        super.reset();
        sum = 0;
        sumsq = 0;
        // Ensure that the first number to be added will fix up min and max to be that number
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
        valid = false;
        elements = new ArrayList<>();
    }

    public double max() {
        if (!valid)
            computeStats();
        return max;
    }

    public double min() {
        if (!valid)
            computeStats();
        return min;
    }

    public double mean() {
        if (!valid)
            computeStats();
        return mean;
    }

    public double median() {
        if (!valid)
            computeStats();
        return median;
    }

    public double kurtosis() {
        if (n < 4 || sd < 0.001) return 0.0;
        if (!valid)
            computeStats();
        double sumQuarticDiffs = elements.stream().mapToDouble(d -> Math.pow(d - mean, 4)).sum();
        return sumQuarticDiffs / Math.pow(sd, 4) * n * (n + 1) / (n - 1) / (n - 2) / (n - 3);
    }

    public double skew() {
        if (n < 3 || sd < 0.001) return 0.0;
        if (!valid)
            computeStats();
        double sumCubeDiffs = elements.stream().mapToDouble(d -> Math.pow(d - mean, 3)).sum();
        return sumCubeDiffs / Math.pow(sd, 3) * n / (n - 1) / (n - 2);
    }

    /**
     * @return the sum of the squares of the differences between the mean and the ith values
     */
    public double sumSquareDiff() {
        return sumsq - n * mean() * mean();
    }

    private void computeStats() {
        if (!valid) {
            if (!elements.isEmpty()) {
                max = elements.stream().mapToDouble(i -> i).max().getAsDouble();
                min = elements.stream().mapToDouble(i -> i).min().getAsDouble();
            } else {
                return;
            }
            mean = sum / n;
            double num = sumsq - (n * mean * mean);
            if (num < 0) {
                // Avoids tiny negative numbers possible through imprecision
                num = 0;
            }
            sd = Math.sqrt(num / (n - 1));
            Collections.sort(elements);
            median = elements.get(elements.size() / 2);
            valid = true;
        }
    }

    public double sd() {
        if (!valid)
            computeStats();
        return sd;
    }

    public double stdErr() {
        return sd() / Math.sqrt(n);
    }

    public void add(TAGNumericStatSummary ss) {
        super.add(ss);
        sum += ss.sum;
        sumsq += ss.sumsq;
        lastAdded = ss.lastAdded;
        valid = false;
        elements.addAll(ss.getElements());
    }

    public void add(double d) {
        n++;
        sum += d;
        sumsq += d * d;
        lastAdded = d;
        valid = false;
        elements.add(d);
    }

    public void add(Number n) {
        add(n.doubleValue());
    }

    public void add(double... xa) {
        for (double x : xa) {
            add(x);
        }
    }

    public double sum() {
        return sum;
    }

    public double getLastAdded() {
        return lastAdded;
    }

    public void discountLast(double discount) {
        lastAdded *= discount;
    }

    @Override
    public String toString() {
        String s = (name == null) ? "" : (name + "\n");
        s += " min   = " + min() + "\n" +
                " max   = " + max() + "\n" +
                " ave   = " + mean() + "\n" +
                " sd    = " + sd() + "\n" +
                " se    = " + stdErr() + "\n" +
                " sum   = " + sum + "\n" +
                " sumsq = " + sumsq + "\n" +
                " n     = " + n + "\n";
        return s;
    }

    public String shortString() {
        return shortString(false);
    }

    public String shortString(boolean scientificNotation) {
        return (name == null) ? "[" : (name + ": [") + min() + ", " + max() + "]"
                + " avg=" + (scientificNotation ? String.format("%6.3e", (mean())) : String.format("%.2f", mean()))
                + "; sd=" + (scientificNotation ? String.format("%6.3e", (sd())) : String.format("%.2f", sd()))
                + "; se=" + (scientificNotation ? String.format("%6.3e", (stdErr())) : String.format("%.2f", stdErr()))
                ;
    }

    public ArrayList<Double> getElements() {
        return elements;
    }

    public TAGNumericStatSummary copy() {
        TAGNumericStatSummary ss = new TAGNumericStatSummary();

        ss.name = this.name;
        ss.n = this.n;
        ss.type = this.type;

        ss.sum = this.sum;
        ss.sumsq = this.sumsq;
        ss.min = this.min;
        ss.max = this.max;
        ss.mean = this.mean;
        ss.sd = this.sd;
        ss.valid = this.valid;
        ss.lastAdded = this.lastAdded;

        return ss;
    }

//    @Override
//    public Map<String, Object> getSummary() {
//        Map<String, Object> data = new HashMap<>();
//        data.put(name + "Median", median());
//        data.put(name + "Mean", mean());
//        data.put(name + "Max", max());
//        data.put(name + "Min", min());
//        data.put(name + "VarCoeff", Math.abs(sd()/mean()));
//        data.put(name + "Skew", skew());
//        data.put(name + "Kurtosis", kurtosis());
//
//        TAGNumericStatSummary delta = elements.size() > 1 ?
//                IntStream.range(0, elements.size() - 1)
//                        .mapToObj(i -> !elements.get(i + 1).equals(elements.get(i)) ? 1.0 : 0.0)
//                        .collect(new TAGSummariser())
//                : new TAGNumericStatSummary();
//        data.put(name + "Delta", delta.mean()); // percentage of times this value changed consecutively
//        return data;
//    }
    @Override
    public Map<String, Object> getSummary() {
        Map<String, Object> data = new HashMap<>();
        data.put("Median", median());
        data.put("Mean", mean());
        data.put("Max", max());
        data.put("Min", min());
        data.put("VarCoeff", Math.abs(sd()/mean()));
        data.put("Skew", skew());
        data.put("Kurtosis", kurtosis());

        TAGNumericStatSummary delta = elements.size() > 1 ?
                IntStream.range(0, elements.size() - 1)
                        .mapToObj(i -> !elements.get(i + 1).equals(elements.get(i)) ? 1.0 : 0.0)
                        .collect(new TAGSummariser())
                : new TAGNumericStatSummary();
        data.put("Delta", delta.mean()); // percentage of times this value changed consecutively
        return data;
    }
}