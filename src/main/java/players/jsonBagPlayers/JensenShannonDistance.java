package players.jsonBagPlayers;

import java.util.HashMap;
import java.util.Map;

public class JensenShannonDistance {

    /**
     * Normalize a map so that the sum of its values is 1.
     * @param p The map to normalize
     * @return A normalized map with double values
     */
    public static Map<String, Double> normalize(Map<String, ? extends Number> p) {
        Map<String, Double> result = new HashMap<>(p.size());
        if (p.isEmpty()) return result;
        double total = 0.0;
        Number sampleValue = null;
        for (Number value : p.values()) {
            if (value != null) {
                total += value.doubleValue();
                if (sampleValue == null) sampleValue = value;
            }
        }

        // Fast-path: if already normalized and values are known to be homogeneous, one sample check is enough.
        if (Math.abs(total - 1.0) < 0.00001 && sampleValue instanceof Double) {
            @SuppressWarnings("unchecked")
            Map<String, Double> same = (Map<String, Double>) p;
            return same;
        }

        if (Double.compare(total, 0.0) == 0) total = 1.0;

        for (Map.Entry<String, ? extends Number> entry : p.entrySet()) {
            double dv = entry.getValue().doubleValue();
            result.put(entry.getKey(), dv / total);
        }
        return result;
    }

    /**
     * Calculate the Jensen-Shannon distance between two probability distributions.
     * @param p First distribution as a map of string keys to integer counts
     * @param q Second distribution as a map of string keys to integer counts
     * @return The Jensen-Shannon distance (between 0 and 1)
     */
    public static double jsd(Map<String, ? extends Number> p, Map<String, ? extends Number> q) {
        // Handle empty cases
        if ((p == null || p.isEmpty()) && (q == null || q.isEmpty())) {
            return 0;
        }
        if (p == null || p.isEmpty() || q == null || q.isEmpty()) {
            return 1;
        }

        // Normalize the distributions
        Map<String, Double> pNorm = normalize(p);
        Map<String, Double> qNorm = normalize(q);

        double jsDist = 0;
        // Process keys in pNorm, looking up each in qNorm once
        for (Map.Entry<String, Double> entry : pNorm.entrySet()) {
            double pK = entry.getValue();
            Double qVal = qNorm.get(entry.getKey());
            double qK = (qVal != null) ? qVal : 0.0;
            double mK = (pK + qK) / 2;

            if (pK != 0) {
                jsDist += pK * log2(pK / mK);
            }
            if (qK != 0) {
                jsDist += qK * log2(qK / mK);
            }
        }
        // Process keys only in qNorm
        for (Map.Entry<String, Double> entry : qNorm.entrySet()) {
            if (!pNorm.containsKey(entry.getKey())) {
                double qK = entry.getValue();
                if (qK != 0) {
                    double mK = qK / 2;
                    jsDist += qK * log2(qK / mK);
                }
            }
        }

        return Math.sqrt(jsDist * 0.5);
    }

    /**
     * Calculate log base 2 of a value.
     * @param x The value
     * @return log2(x)
     */
    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    // Example usage
    public static void main(String[] args) {
        Map<String, Integer> p = new HashMap<>();
        p.put("a", 10);
        p.put("b", 20);
        p.put("c", 30);

        Map<String, Integer> q = new HashMap<>();
        q.put("a", 15);
        q.put("b", 25);
        q.put("d", 10);

        double distance = jsd(p, q);
        System.out.println("Jensen-Shannon Distance: " + distance);
    }
}
