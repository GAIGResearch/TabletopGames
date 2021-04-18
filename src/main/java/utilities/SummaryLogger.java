package utilities;

import core.interfaces.IStatisticLogger;

import java.io.*;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Statistics Logger that just takes in Numeric data and maintains summery statistics for each type:
 * - mean, min, max, standard error, standard deviation, n
 * A summary is printed out in alphabetic order once finished.
 */
public class SummaryLogger implements IStatisticLogger {

    File logFile;
    public boolean printToConsole = true;
    Map<String, StatSummary> allData = new HashMap<>();
    Map<String, String> otherData = new HashMap<>();

    public SummaryLogger() {
    }

    public SummaryLogger(String logFile) {
        this.logFile = new File(logFile);
    }

    @Override
    public void record(String key, Object value) {
        if (value instanceof Number) {
            if (!allData.containsKey(key))
                allData.put(key, new StatSummary());
            allData.get(key).add((Number) value);
        } else {
            otherData.put(key, value.toString());
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
        if (printToConsole)
            System.out.println(toString());

        if (logFile != null) {
            // We now write this to the file
            boolean exists = logFile.exists();
            StringBuilder header = new StringBuilder();
            StringBuilder data = new StringBuilder();
            for (String key : otherData.keySet()) {
                header.append(key).append("\t");
                data.append(otherData.get(key)).append("\t");
            }
            for (String key : allData.keySet()) {
                header.append(key).append("\t").append(key).append("_se\t");
                data.append(String.format("%.3g\t%.2g\t", allData.get(key).mean(), allData.get(key).stdErr()));
            }
            header.append("\n");
            data.append("\n");

            try {
                FileWriter writer = new FileWriter(logFile, true);
                if (!exists)
                    writer.write(header.toString());
                writer.write(data.toString());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
