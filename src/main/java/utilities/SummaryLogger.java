package utilities;

import core.interfaces.IStatisticLogger;

import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Statistics Logger that just takes in Numeric data and maintains summery statistics for each type:
 * - mean, min, max, standard error, standard deviation, n
 * A summary is printed out in alphabetic order once finished.
 */
public class SummaryLogger implements IStatisticLogger {

    Map<String, StatSummary> allData = new HashMap<>();

    @Override
    public void record(String key, Object value) {
        if (value instanceof Number) {
            if (!allData.containsKey(key))
                allData.put(key, new StatSummary());
            allData.get(key).add((Number) value);
        }
    }

    /**
     * Any data that is not numeric will be silently ignored
     *
     * @param data A map of name -> Number pairs
     */
    @Override
    public void record(Map<String, ?> data) {
        for (String key : data.keySet()) {
            record(key, data.get(key));
        }
    }

    @Override
    public Map<String, StatSummary> summary() {
        return allData;
    }

    @Override
    public void processDataAndFinish() {
        System.out.println(toString());
    }

    @Override
    public String toString() {
        // We want to print out something vaguely pretty
        List<String> alphabeticOrder = allData.keySet().stream().sorted().collect(toList());
        StringBuilder sb = new StringBuilder();
        for (String key : alphabeticOrder) {
            StatSummary stats = allData.get(key);
            sb.append(String.format("%30s  Mean: %.3g +/- %.2g,\t[%d, %d], pop sd %.3g,\tn=%d\n", key,
                    stats.mean(), stats.stdErr(), (int) stats.min(), (int) stats.max(), stats.sd(), stats.n()));
        }
        return sb.toString();
    }
}
