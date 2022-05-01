package utilities;

import core.interfaces.IStatisticLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * A Class to log details to file for later analysis
 */
public class FileStatsLogger implements IStatisticLogger {

    private String delimiter;
    private FileWriter writer;
    public String doubleFormat = "%.3g";
    public String intFormat = "%d";
    private boolean headerNeeded = true;

    private Set<String> allKeys = new LinkedHashSet<>();

    /**
     * Note that one line will be output to the file per Map<String, ?>
     * provided via record()
     *
     * @param fileName  The full location of the file to write results to
     * @param delimiter The delimiter to use in the file between data items
     */
    public FileStatsLogger(String fileName, String delimiter, boolean append) {
        this.delimiter = delimiter;
        try {
            File file = new File(fileName);
            if (file.exists() && append)
                headerNeeded = false;
            writer = new FileWriter(fileName, append);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem opening file " + fileName + " : " + e.getMessage());
        }
    }

    public FileStatsLogger(String fileName) {
        this(fileName, "\t", true);
    }

    /**
     * Use to register a set of data in one go. It is not possible to add new keys after the first call
     * of record(Map). Data linked to new, previously unseen keys will be ignored (and logged to console)
     *
     * @param data A map of name -> value pairs
     */
    @Override
    public void record(Map<String, ?> data) {
        try {
            if (allKeys.isEmpty()) {
                allKeys = data.keySet();
                // then write a header line to the file
                if (headerNeeded) {
                    String outputLine = String.join(delimiter, allKeys) + "\n";
                    writer.write(outputLine);
                }
            } else {
                data.keySet().forEach(s -> {
                            if (!allKeys.contains(s)) {
                                System.out.println("Unknown key in FileStatsLogger : " + s);
                            }
                        }
                );
            }
            List<String> outputData = allKeys.stream().map(key -> {
                Object datum = data.get(key);
                if (datum == null) return "";
                if (datum instanceof Integer) return String.format(intFormat, datum);
                if (datum instanceof Double) return String.format(doubleFormat, datum);
                return datum.toString();
            }).collect(toList());

            String outputLine = String.join(delimiter, outputData) + "\n";

            writer.write(outputLine);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("Problem writing to file " + writer.toString() + " : " + e.getMessage());
        }
    }

    @Override
    public void record(String key, Object datum) {
        System.out.println("Datum ignored - FileStatsLogger only to be used with other record()");
    }

    public void flush() {
        try {
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This just closes the file
     */
    @Override
    public void processDataAndFinish() {
        try {
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem closing file " + writer.toString() + " : " + e.getMessage());
        }
    }

    @Override
    public void processDataAndNotFinish() {
        try {
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem flushing file " + writer.toString() + " : " + e.getMessage());
        }
    }

    /**
     * This always returns an empty Map
     *
     * @return A summary of the data
     */
    @Override
    public Map<String, TAGStatSummary> summary() {
        return new HashMap<>();
    }
}
