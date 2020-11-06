package utilities;

import core.interfaces.IStatisticLogger;

import java.util.*;

import static java.util.stream.Collectors.*;

public class SummaryLogger implements IStatisticLogger {

    Map<String, StatSummary> allData = new HashMap<>();

    @Override
    public void record(String key, Number value) {
        if (!allData.containsKey(key))
            allData.put(key, new StatSummary());
        allData.get(key).add(value);
    }

    @Override
    public void record(Map<String, Number> data) {
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
