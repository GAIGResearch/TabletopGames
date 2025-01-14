package evaluation.optimisation.ntbea;

import java.util.*;

public class IntArrayPattern implements Comparable<IntArrayPattern> {

    public static void main(String[] args) {
        int[] x = {0, 1, 2, 3, 3};
        int[] ix1 = {0, 3};
        int[] ix2 = {0, 4};
        int[] ix3 = {0, 1};
        IntArrayPattern p1 = new IntArrayPattern().setPattern(x, ix1);
        IntArrayPattern p2 = new IntArrayPattern().setPattern(x, ix2);
        IntArrayPattern p3 = new IntArrayPattern().setPattern(x, ix3);

        Map<IntArrayPattern, Integer> test = new HashMap<>();
        test.put(p1, 1);
        test.put(p2, 2);
        test.put(p3, 3);
        System.out.println(test);

    }

    public int[] v;

    public IntArrayPattern setPattern(int[] v) {
        this.v = v;
        return this;
    }

    public IntArrayPattern setPattern(int[] x, int[] ix) {
        v = new int[ix.length];
        for (int i = 0; i < ix.length; i++) {
            v[i] = x[ix[i]];
        }
        return this;
    }

    public int hashCode() {
        return Arrays.hashCode(v);
    }

    public boolean equals(Object pattern) {
        if (pattern instanceof IntArrayPattern p) {
            for (int i = 0; i < v.length; i++) {
                if (v[i] != p.v[i]) return false;
            }
            return true;
        }
        return false;
    }

    public int compareTo(IntArrayPattern p) {
        // now iterate over all the values
        for (int i = 0; i < v.length; i++) {
            if (v[i] > p.v[i]) {
                return 1;
            }
            if (v[i] < p.v[i]) {
                return -1;
            }
        }
        return 0;
    }

    public String toString() {
        return Arrays.toString(v);
    }

}
