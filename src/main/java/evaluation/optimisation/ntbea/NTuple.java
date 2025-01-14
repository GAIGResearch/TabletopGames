package evaluation.optimisation.ntbea;

import utilities.StatSummary;

import java.util.*;

public class NTuple {

    SearchSpace searchSpace;
    public int[] tuple;
    public Map<IntArrayPattern, StatSummary> ntMap;

    public int nSamples;
    int nEntries;

    public NTuple(SearchSpace searchSpace, int[] tuple) {
        this.searchSpace = searchSpace;
        this.tuple = tuple;
        reset();
    }

    public void reset() {
        nSamples = 0;
        nEntries = 0;
        ntMap = new HashMap<>();
    }

    public void add(int[] x, double v) {
        // for each address that occurs, we're going to store something
        StatSummary ss = getStatsForceCreate(x);
        ss.add(v);
        nSamples++;
    }

    public void add(int[] x, StatSummary ssIncoming) {
        // for each address that occurs, we're going to store something
        StatSummary ss = getStatsForceCreate(x);
        ss.add(ssIncoming);
        nSamples++;
    }

    public void printNonEmpty() {
        TreeSet<IntArrayPattern> orderedKeys = new TreeSet<>();
        orderedKeys.addAll(ntMap.keySet());
        for (IntArrayPattern key : orderedKeys) {
            StatSummary ss = ntMap.get(key);
            System.out.println(key + "\t " + ss.n() + "\t " + ss.mean() + "\t " + ss.sd());
            // System.out.println();
        }
    }

    /**
     * Get stats but force creation if it does not already exists
     *
     * @param x
     * @return
     */
    public StatSummary getStatsForceCreate(int[] x) {
        IntArrayPattern key = new IntArrayPattern().setPattern(x, tuple);
        StatSummary ss = ntMap.get(key);
        if (ss == null) {
            ss = new StatSummary();
            nEntries++;
            ntMap.put(key, ss);
        }
        return ss;
    }

    /**
     * For reporting we only want to know about the stats if they already exist.
     *
     * So this version provides that.
     *
     * @param x
     * @return
     */
    public StatSummary getStats(int[] x) {
        IntArrayPattern key = new IntArrayPattern().setPattern(x, tuple);
        return ntMap.get(key);
    }

    public int nSamples() {
        return nSamples;
    }

    public String toString() {
        return tuple.length + "\t " + Arrays.toString(tuple) + "\t " + nSamples  + "\t " + nEntries;
    }

}
