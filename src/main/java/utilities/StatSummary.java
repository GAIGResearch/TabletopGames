package utilities;

import java.util.ArrayList;

public class StatSummary {
    public String name;
    private int n;
    private double sum;
    private double sumsq;
    private double min;
    private double max;
    private double mean;
    private double median;
    private double sd;
    private double lastAdded;
    private boolean valid;
    private ArrayList<Double> elements;

    public StatSummary() {
        this("");
    }

    public StatSummary(String name) {
        this.name = name;
        this.reset();
    }

    public final void reset() {
        this.n = 0;
        this.sum = 0.0F;
        this.sumsq = 0.0F;
        this.min = Double.POSITIVE_INFINITY;
        this.max = Double.NEGATIVE_INFINITY;
        this.valid = false;
        this.elements = new ArrayList();
    }

    public double max() {
        return this.max;
    }

    public double min() {
        return this.min;
    }

    public double mean() {
        if (!this.valid) {
            this.computeStats();
        }

        return this.mean;
    }

    public double sumSquareDiff() {
        return this.sumsq - (double)this.n * this.mean() * this.mean();
    }

    private void computeStats() {
        if (!this.valid) {
            this.mean = this.sum / (double)this.n;
            double num = this.sumsq - (double)this.n * this.mean * this.mean;
            if (num < (double)0.0F) {
                num = (double)0.0F;
            }

            this.sd = Math.sqrt(num / (double)(this.n - 1));
            this.valid = true;
        }

    }

    public double sd() {
        if (!this.valid) {
            this.computeStats();
        }

        return this.sd;
    }

    public int n() {
        return this.n;
    }

    public double stdErr() {
        return this.sd() / Math.sqrt((double)this.n);
    }

    public void add(StatSummary ss) {
        this.n += ss.n;
        this.sum += ss.sum;
        this.sumsq += ss.sumsq;
        this.max = Math.max(this.max, ss.max);
        this.min = Math.min(this.min, ss.min);
        this.lastAdded = ss.lastAdded;
        this.valid = false;
        this.elements.addAll(ss.getElements());
    }

    public void add(double d) {
        ++this.n;
        this.sum += d;
        this.sumsq += d * d;
        this.min = Math.min(this.min, d);
        this.max = Math.max(this.max, d);
        this.lastAdded = d;
        this.valid = false;
        this.elements.add(d);
    }

    public void add(Number n) {
        this.add(n.doubleValue());
    }

    public void add(double... xa) {
        for(double x : xa) {
            this.add(x);
        }

    }

    public double sum() {
        return this.sum;
    }

    public double getLastAdded() {
        return this.lastAdded;
    }

    public void discountLast(double discount) {
        this.lastAdded *= discount;
    }

    public String toString() {
        String s = this.name == null ? "" : this.name + "\n";
        s = s + " min   = " + this.min() + "\n" + " max   = " + this.max() + "\n" + " ave   = " + this.mean() + "\n" + " sd    = " + this.sd() + "\n" + " se    = " + this.stdErr() + "\n" + " sum   = " + this.sum + "\n" + " sumsq = " + this.sumsq + "\n" + " n     = " + this.n + "\n";
        return s;
    }

    public String shortString() {
        return this.shortString(false);
    }

    public String shortString(boolean scientificNotation) {
        return this.name == null ? "[" : this.name + ": [" + this.min() + ", " + this.max() + "]" + " avg=" + (scientificNotation ? String.format("%6.3e", this.mean()) : String.format("%.2f", this.mean())) + "; sd=" + (scientificNotation ? String.format("%6.3e", this.sd()) : String.format("%.2f", this.sd())) + "; se=" + (scientificNotation ? String.format("%6.3e", this.stdErr()) : String.format("%.2f", this.stdErr()));
    }

    public ArrayList<Double> getElements() {
        return this.elements;
    }

    public StatSummary copy() {
        StatSummary ss = new StatSummary();
        ss.name = this.name;
        ss.sum = this.sum;
        ss.sumsq = this.sumsq;
        ss.min = this.min;
        ss.max = this.max;
        ss.mean = this.mean;
        ss.sd = this.sd;
        ss.n = this.n;
        ss.valid = this.valid;
        ss.lastAdded = this.lastAdded;
        return ss;
    }
}
