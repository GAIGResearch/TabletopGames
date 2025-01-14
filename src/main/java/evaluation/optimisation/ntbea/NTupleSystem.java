package evaluation.optimisation.ntbea;

import utilities.StatSummary;

import java.util.*;

public class NTupleSystem implements LandscapeModel {

    protected double epsilon = 0.10;

    List<int[]> sampledPoints = new ArrayList<>();
    List<NTuple> tuples = new ArrayList<>();

    public boolean use1Tuple = true;
    public boolean use2Tuple = true;
    public boolean use3Tuple = false;
    public boolean useNTuple = true;

    int minTupleSize = 2;

    SearchSpace searchSpace;

    public NTupleSystem(SearchSpace space) {
        searchSpace = space;
    }

    @Override
    public int[] getBestSampled() {
        int[] retValue = new int[0];
        double best = Double.NEGATIVE_INFINITY;
        for (int[] p : sampledPoints) {
            if (getMeanEstimate(p) > best) {
                retValue = p;
            }
        }
        return retValue;
    }

    public void addTuples() {
        // this should only be called AFTER setting up the search space
        tuples = new ArrayList<>();
        if (use1Tuple) add1Tuples();
        if (use2Tuple) add2Tuples();
        if (use3Tuple) add3Tuples();
        if (useNTuple) addNTuple();
    }

    @Override
    public void reset() {
        sampledPoints = new ArrayList<>();
        for (NTuple t : tuples) {
            t.reset();
        }
    }

    @Override
    public LandscapeModel copy() {
        throw new IllegalArgumentException("Not yet implemented");
    }

    @Override
    public SearchSpace getSearchSpace() {
        return searchSpace;
    }

    @Override
    public void addPoint(int[] datapoint, double value) {
        for (NTuple tuple : tuples) {
            tuple.add(datapoint, value);
        }
        sampledPoints.add(datapoint);
    }

    @Override
    public void recalculateModel() {
        throw new IllegalArgumentException("Not yet implemented");
    }

    public double getMeanEstimate(int[] x) {
        // we could get an average ...
        var ssTot = new StatSummary();
        for (NTuple tuple : tuples) {
            var ss = tuple.getStats(x);
            if (ss != null) {
                if (tuple.tuple.length >= minTupleSize) {
                    var mean = ss.mean();
                    if (!java.lang.Double.isNaN(mean))
                        ssTot.add(mean);
                }
            }
        }
        return ssTot.mean();
    }

    @Override
    public double getLowerBound(int[] x) {
        return getMeanEstimate(x) - getExplorationEstimate(x);
    }

    @Override
    public double getUpperBound(int[] x) {
        return getMeanEstimate(x) + getExplorationEstimate(x);
    }

    protected double getExplorationEstimate(int[] x) {
        // just takes the average of the exploration vector
        double[] vec = getExplorationVector(x);
        return Arrays.stream(vec).sum() / vec.length;
    }

    protected double[] getExplorationVector(int[] x) {
        // idea is simple: we just provide a summary over all
        // the samples, comparing each to the maximum in that N-Tuple
        double[] retValue = new double[tuples.size()];
        for (int i = 0; i < retValue.length; i++) {
            NTuple tuple = tuples.get(i);
            var ss = tuple.getStats(x);
            if (ss != null) {
                retValue[i] = Math.sqrt(Math.log((1 + tuple.nSamples())) / (epsilon + ss.n()));
            } else {
                retValue[i] = Math.sqrt(Math.log((1 + tuple.nSamples)) / epsilon);
            }
        }
        return retValue;
    }


    // note that there is a smarter way to add different n-tuples, but this way is easiest

    public void add1Tuples() {
        for (int i = 0; i < searchSpace.nDims(); i++) {
            tuples.add(new NTuple(searchSpace, new int[]{i}));
        }
    }

    public void add2Tuples() {
        for (int i = 0; i < searchSpace.nDims(); i++) {
            for (int j = i + 1; j < searchSpace.nDims(); j++) {
                tuples.add(new NTuple(searchSpace, new int[]{i, j}));
            }
        }
    }

    public void add3Tuples() {
        for (int i = 0; i < searchSpace.nDims(); i++) {
            for (int j = i + 1; j < searchSpace.nDims(); j++) {
                for (int k = j + 1; k < searchSpace.nDims(); k++) {
                    tuples.add(new NTuple(searchSpace, new int[]{i, j, k}));
                }
            }
        }
    }

    public void addNTuple() {
        int[] allTuple = new int[searchSpace.nDims()];
        Arrays.setAll(allTuple, i -> i);
        tuples.add(new NTuple(searchSpace, allTuple));
    }

    public int numberOfSamples() {
        return sampledPoints.size();
    }

    public List<NTuple> getTuples() {
        return tuples;
    }

    public List<int[]> getSampledPoints() {
        return sampledPoints;
    }
}


