package utilities;

import core.interfaces.IStatisticLogger;

import java.io.*;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Statistics Logger that just takes in Numeric data and maintains summary statistics for each type:
 * - mean, min, max, standard error, standard deviation, n
 * A summary is printed out in alphabetic order once finished.
 */
public class SummaryLogger implements IStatisticLogger {

    File logFile;
    public boolean printToConsole = true;
    Map<String, TAGStatSummary> allData = new LinkedHashMap<>();
    Map<String, String> otherData = new LinkedHashMap<>();

    public SummaryLogger() {
    }

    public SummaryLogger(String logFile) {
        this.logFile = new File(logFile);
    }

    @Override
    public void record(String key, Object value) {
        if (value instanceof Number) {
            if (!allData.containsKey(key))
                allData.put(key, new TAGStatSummary());
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
    public Map<String, TAGStatSummary> summary() {
        return allData;
    }

    @Override
    public void processDataAndFinish() {
        if (printToConsole)
            System.out.println(this);

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
                if (allData.get(key).n() == 1) {
                    header.append(key).append("\t");
                    data.append(String.format("%.3g\t", allData.get(key).mean()));
                } else {
                    header.append(key).append("\t").append(key).append("_se\t");
                    data.append(String.format("%.3g\t%.2g\t", allData.get(key).mean(), allData.get(key).stdErr()));
                    header.append(key).append("_sd\t");
                    data.append(String.format("%.3g\t", allData.get(key).sd()));
                    header.append(key).append("_median\t");
                    data.append(String.format("%.3g\t", allData.get(key).median()));
                    header.append(key).append("_min\t");
                    data.append(String.format("%.3g\t", allData.get(key).min()));
                    header.append(key).append("_max\t");
                    data.append(String.format("%.3g\t", allData.get(key).max()));
                    header.append(key).append("_skew\t");
                    data.append(String.format("%.3g\t", allData.get(key).skew()));
                    header.append(key).append("_kurtosis\t");
                    data.append(String.format("%.3g\t", allData.get(key).kurtosis()));
                }
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
    public void processDataAndNotFinish() {
        // do nothing until we have full finished
    }


    @Override
    public String toString() {
        // We want to print out something vaguely pretty
        List<String> alphabeticOrder = allData.keySet().stream().sorted().collect(toList());
        StringBuilder sb = new StringBuilder();
        for (String key : alphabeticOrder) {
            TAGStatSummary stats = allData.get(key);
            if (stats.n() == 1) {
                sb.append(String.format("%30s  %8.3g\n", key, stats.mean()));
            } else {
                sb.append(String.format("%30s  Mean: %8.3g +/- %6.2g,\tMedian: %8.3g,\tRange: [%3d, %3d],\tpop sd %8.3g,\tskew %8.3g,\tkurtosis %8.3g,\tn=%d\n", key,
                        stats.mean(), stats.stdErr(), stats.median(), (int) stats.min(), (int) stats.max(), stats.sd(), stats.skew(), stats.kurtosis(), stats.n()));
            }
        }
        return sb.toString();
    }
}
