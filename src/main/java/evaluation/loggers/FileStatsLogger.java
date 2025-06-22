package evaluation.loggers;

import core.interfaces.IStatisticLogger;
import evaluation.summarisers.TAGOccurrenceStatSummary;
import evaluation.summarisers.TAGStatSummary;
import utilities.Utils;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/**
 * A Class to log details to file for later analysis
 */
public class FileStatsLogger implements IStatisticLogger {

    private String fileName;
    private String actionName;
    private final boolean append;
    private final String delimiter;
    private FileWriter writer;
    public String doubleFormat = "%.3g";
    public String intFormat = "%d";
    private boolean headerNeeded = true;

    private Set<String> allKeys = new LinkedHashSet<>();

    /**
     * Note that one line will be output to the file per {@code Map<String, ?>}
     * provided via record()
     *
     * @param fileName  The full location of the file to write results to
     * @param delimiter The delimiter to use in the file between data items
     */
    public FileStatsLogger(String fileName, String delimiter, boolean append) {
        this.delimiter = delimiter;
        this.fileName = fileName;
        this.append = append;
    }

    private void initialise() {
        try {
            File file = new File(fileName);
            if (file.exists() && append)
                headerNeeded = false;
            writer = new FileWriter(fileName, append);
        } catch (Exception e) {
            throw new AssertionError("Problem opening file " + fileName + " : " + e.getMessage());
        }
    }

    public FileStatsLogger(String fileName) {
        this(fileName, "\t", true);
    }


    public void setOutPutDirectory(String... nestedDirectories) {
        if (writer != null) {
            processDataAndFinish();
            writer = null;
        }
        String folder = Utils.createDirectory(nestedDirectories);
        this.fileName = folder + File.separator + this.fileName;
    }
    /**
     * Use to register a set of data in one go. It is not possible to add new keys after the first call
     * of record(Map). Data linked to new, previously unseen keys will be ignored (and logged to console)
     *
     * @param rawData A map of name -> value pairs
     */
    @Override
    public void record(Map<String, ?> rawData) {
        if (writer == null) initialise();
        // first we preprocess data to remove nesting
        // Use a LinkedHashMap to preserve order
        Map<String, Object> data = new LinkedHashMap<>();
        for (String key : rawData.keySet()) {
            Object thing = rawData.get(key);
            if (thing instanceof Map) {
                data.putAll((Map<? extends String, ?>) thing);
            } else {
                data.put(key, rawData.get(key));
            }
        }
        try {
            if (allKeys.isEmpty()) {
                allKeys = data.keySet();
                // then write a header line to the file
                if (headerNeeded) {
                    String outputLine = String.join(delimiter, allKeys) + "\n";
                    outputLine = outputLine.replaceAll(":" + actionName + delimiter, delimiter);
                    outputLine = outputLine.replaceAll(":" + actionName + "\\n", "\n");
                    writer.write(outputLine);
                }
            } else {
                data.keySet().forEach(s -> {
                            if (!allKeys.contains(s)) {
//                                System.out.println("Unknown key in FileStatsLogger : " + s);
                            }
                        }
                );
            }
            List<String> outputData = new ArrayList<>();
            for (String key: allKeys) {
                Object datum = data.get(key);
                if (datum == null) {
                    outputData.add("NA");
                    continue;
                }
                // If this is a summary, then we return the single most common occurrence
                if (datum instanceof TAGOccurrenceStatSummary) {
                    TAGOccurrenceStatSummary summary = (TAGOccurrenceStatSummary) datum;
                    datum = summary.getHighestOccurrence().a;
                }
                if (datum instanceof Integer) {
                    outputData.add(String.format(intFormat, datum));
                    continue;
                }
                if (datum instanceof Double) {
                    outputData.add(String.format(doubleFormat, datum));
                    continue;
                }
                if (datum instanceof Map) {
                    Map<String, ?> map = (Map<String, ?>) datum;
                    if (map.size() == 1)
                        outputData.add(map.values().iterator().next().toString());
                    else
                        outputData.add(map.toString());
                    continue;
                }

                outputData.add(datum.toString());
            }

            if (!outputData.isEmpty()) {
                String outputLine = String.join(delimiter, outputData) + "\n";
                writer.write(outputLine);
            }
        } catch (IOException e) {
            throw new AssertionError("Problem writing to file " + writer.toString() + " : " + e.getMessage());
        }
    }

    @Override
    public void record(String key, Object datum) {
        //   System.out.println("Datum ignored - FileStatsLogger only to be used with other record() : " + key);
    }

    /**
     * This just closes the file
     */
    @Override
    public void processDataAndFinish() {
        if (writer == null) return;
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
        if (writer == null) return;
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

    @Override
    public FileStatsLogger emptyCopy(String id) {
        String[] fileParts = fileName.split(Pattern.quote("."));
        if (fileParts.length != 2)
            throw new AssertionError("Filename does not conform to expected <stem>.<type>");
        String newFileName = fileParts[0] + "_" + id + "." + fileParts[1];
        FileStatsLogger retValue = new FileStatsLogger(newFileName, delimiter, append);
        retValue.actionName = id;
        return retValue;
    }


    public String getFileName() {
        return fileName;
    }

    public String getActionName() {
        return actionName;
    }

    public boolean isAppend() {
        return append;
    }

    public String getDelimiter() {
        return delimiter;
    }
}
