package evaluation.loggers;

import core.interfaces.IStatisticLogger;
import utilities.TimeStamp;
import evaluation.summarisers.TimeStampSummary;
import evaluation.summarisers.TAGNumericStatSummary;
import evaluation.summarisers.TAGTimeSeriesSummary;
import utilities.Pair;
import evaluation.summarisers.TAGOccurrenceStatSummary;
import evaluation.summarisers.TAGStatSummary;

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
    Map<String, TAGStatSummary> data = new HashMap<>();

    public SummaryLogger() {}

    public SummaryLogger(String logFile) {
        this.logFile = new File(logFile);
    }

    @Override
    public void record(String key, Object value) {
        TAGStatSummary summary = data.get(key);
        if (value instanceof Number) {
            // A number, record details numeric statistics
            if (!data.containsKey(key)) {
                summary = new TAGNumericStatSummary(key);
                data.put(key, summary);
            }
            ((TAGNumericStatSummary) summary).add((Number) value);

        }else if(value instanceof TimeStamp){
            if (!data.containsKey(key)) {
                summary = new TAGTimeSeriesSummary(key);
                data.put(key, summary);
            }
            ((TAGTimeSeriesSummary) summary).append((TimeStamp) value);
        } else if(value instanceof ArrayList && ((ArrayList<?>) value).get(0) instanceof TimeStamp){
            TimeStamp ts = (TimeStamp) ((ArrayList<?>) value).get(0);
            if(!(ts instanceof TimeStampSummary)) return;

            if (!data.containsKey(key)) {
                summary = new TAGTimeSeriesSummary(key);
                data.put(key, summary);
            }
            for(TimeStampSummary tst : ((ArrayList<TimeStampSummary>) value))
                ((TAGTimeSeriesSummary) summary).append(tst);

            return; //Nothing to do here.
        }else {
            if (value instanceof Map && ((Map<?, ?>) value).keySet().iterator().next() instanceof String) {
                // A collection of other stats that should be recorded separately, ignore key // TODO: maybe we want to keep the key too in the name of records?
                record((Map<String, ?>) value);
            } else {
                // Some other kind of object, record occurrences
                if (!data.containsKey(key)) {
                    summary = new TAGOccurrenceStatSummary(key);
                    data.put(key, summary);
                }
                ((TAGOccurrenceStatSummary) summary).add(value);
            }
        }
    }

    /**
     * Any data that is not numeric will be silently ignored
     *
     * @param data A map of name -> Number pairs
     */

    public void record(Map<String, ?> data) {
        for (String key : data.keySet()) {
            record(key, data.get(key));
        }
    }

    @Override
    public Map<String, TAGStatSummary> summary() {
        return data;
    }

    @Override
    public SummaryLogger emptyCopy(String id) {
        if (logFile == null) return new SummaryLogger();
        return new SummaryLogger(logFile.getPath()); // TODO include id in filename
    }

    @Override
    public void processDataAndFinish() {
        if (printToConsole && data.size() > 0) {
//            System.out.println();
            System.out.println(this);
        }

        if (logFile != null) {

            try {
                if(logFile.exists())
                {
                    Pair<String, String> data = getFileOutput();
                    if(data != null) {
                        FileWriter writer = new FileWriter(logFile, true);
                        writer.write(data.a); //header
                        writer.write(data.b); //body
                        writer.flush();
                        writer.close();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Pair<String, String> getFileOutput()
    {
        if(data.size() == 0) return null;

        // We now write this to the file
        StringBuilder header = new StringBuilder();
        StringBuilder outputData = new StringBuilder();
        for (String key : data.keySet()) {
            TAGStatSummary summary = data.get(key);
            if (summary instanceof TAGOccurrenceStatSummary) {
                header.append(key).append("\t");
                outputData.append(data.get(key)).append("\t");
            } else if (summary instanceof TAGNumericStatSummary) {
                TAGNumericStatSummary statSummary = (TAGNumericStatSummary) summary;
                if (data.get(key).n() == 1) {
                    header.append(key).append("\t");
                    outputData.append(String.format("%.3g\t", statSummary.mean()));
                } else {
                    header.append(key).append("\t").append(key).append("_se\t");
                    outputData.append(String.format("%.3g\t%.2g\t", statSummary.mean(), statSummary.stdErr()));
                    header.append(key).append("_sd\t");
                    outputData.append(String.format("%.3g\t", statSummary.sd()));
                    header.append(key).append("_median\t");
                    outputData.append(String.format("%.3g\t", statSummary.median()));
                    header.append(key).append("_min\t");
                    outputData.append(String.format("%.3g\t", statSummary.min()));
                    header.append(key).append("_max\t");
                    outputData.append(String.format("%.3g\t", statSummary.max()));
                    header.append(key).append("_skew\t");
                    outputData.append(String.format("%.3g\t", statSummary.skew()));
                    header.append(key).append("_kurtosis\t");
                    outputData.append(String.format("%.3g\t", statSummary.kurtosis()));
                }
            } else if (summary instanceof TAGTimeSeriesSummary) {
                header.append(key).append("\t").append("Not implemented\n");
            }
        }
        header.append("\n");
        outputData.append("\n");
        return new Pair<>(header.toString(), outputData.toString());
    }

    @Override
    public void processDataAndNotFinish() {
        // do nothing until we have full finished
    }


    @Override
    public String toString() {
        // We want to print out something vaguely pretty
        StringBuilder sb = new StringBuilder();

        // Group data by event of recorded metric (if saved)
        // TODO: assumes format "METRIC (PARAM1_VALUE, PARAM2_VALUE ...):EVENT"
        Map<String, Map<String, Map<String, TAGStatSummary>>> groupedData = new HashMap<>();
        int keyMaxLength = 0;
        int expectedColonChunks = 3;
        for (String key: data.keySet()) {
            String[] split = key.split(":");
            String group = "Other";
            if (split.length == expectedColonChunks)
                group = split[expectedColonChunks-1];
            else
                group = split[split.length-1];
            if (!groupedData.containsKey(group))
                groupedData.put(group, new HashMap<>());
            Map<String, Map<String, TAGStatSummary>> eventGroupData = groupedData.get(group);

            // Group parameterized data further by metric
            split[0] = split[0].replace(")(", " > ");
            String[] split2 = split[0].split("\\(");
            String metricName = split2[0];
            if(split.length > 2)
                metricName += ":" + split[1];

            String params = "";
            if (split2.length == 2)
                params = split2[1].replace(")", "");
            if (!eventGroupData.containsKey(metricName))
                eventGroupData.put(metricName, new HashMap<>());
            eventGroupData.get(metricName).put(params, data.get(key));

            if (params.length() > keyMaxLength)
                keyMaxLength = params.length();
            if (metricName.length() > keyMaxLength)
                keyMaxLength = metricName.length();
        }

        for (String event: groupedData.keySet()) {

            // To sort the way in which we print the metrics.
            PriorityQueue<Pair<String, Map<String, TAGStatSummary>>> printInOrder = new PriorityQueue<>(new Comparator<Pair<String, Map<String, TAGStatSummary>>>() {
                @Override
                public int compare(Pair<String, Map<String, TAGStatSummary>> o1, Pair<String, Map<String, TAGStatSummary>> o2) {
                    return Integer.compare(o1.b.size(), o2.b.size());
                }
            });

            //Sort prints so metrics that have more than one value recorded are printed together (and at the end) for this event.
            Map<String, Map<String, TAGStatSummary>> eventData = groupedData.get(event);
            for (String metric: eventData.keySet()) {
                Map<String, TAGStatSummary> d = eventData.get(metric);
                printInOrder.add(new Pair<>(metric, d));
            }

            //Prints event header
            String eventHeader = "Event: " + event;
            int nEH = eventHeader.length();
            sb.append("\n");
            for(int i = 0; i < nEH; i++) sb.append("#");
            sb.append("\n").append(eventHeader).append("\n");
            for(int i = 0; i < nEH; i++) sb.append("#");
            sb.append("\n");

            // Print each metric
            while(!printInOrder.isEmpty())
            {
                Pair<String, Map<String, TAGStatSummary>> data = printInOrder.poll();
                Map<String, TAGStatSummary> d = data.b;
                String metric = data.a;

                if (d.size() > 1) {
                    sb.append("\n");
                }
                sb.append(String.format("%-" + keyMaxLength + "s", metric));
                if (d.size() > 1) {
                    sb.append("\n");
                }

                List<String> alphabeticOrder = d.keySet().stream().sorted().collect(toList());
                for (String key : alphabeticOrder) {
                    TAGStatSummary summary = d.get(key);
                    if (summary instanceof TAGNumericStatSummary) {
                        // Print numeric data, stat summaries
                        TAGNumericStatSummary stats = (TAGNumericStatSummary) summary;
                        if (d.size() > 1) {
                            sb.append(String.format(" * %-" + keyMaxLength + "s", key)).append("\t");
                        }
                        if (stats.n() == 1) {
                            sb.append(String.format("\tValue: %8.3g\n", stats.mean()));
                        } else {
                            sb.append(String.format("\tMean: %8.3g +/- %6.2g,\tMedian: %8.3g,\tSum: %8.3g,\tRange: [%3d, %3d],\tPop sd: %8.3g,\tSkew: %8.3g,\tKurtosis: %8.3g,\tN: %d\n",
                                    stats.mean(), stats.stdErr(), stats.median(), stats.sum(), (int) stats.min(), (int) stats.max(), stats.sd(), stats.skew(), stats.kurtosis(), stats.n()));
                        }
                    } else if (summary instanceof TAGTimeSeriesSummary) {
                        TAGTimeSeriesSummary stats = (TAGTimeSeriesSummary) summary;
                        sb.append(key).append("\n");
                        ArrayList<TimeStamp> series = (ArrayList<TimeStamp>) stats.getElements();
                        int lastX = -1;

                        ArrayList<TimeStamp> oneSeries = new ArrayList<>();
                        for (int i = 0; i < series.size(); i++) {
                            TimeStamp ts = series.get(i);

                            int x = ts.x;
                            if (x <= lastX) {
                                //new series
                                String seriesString = seriesToString(oneSeries, (ts instanceof TimeStampSummary));
                                sb.append(seriesString);
                                oneSeries.clear();
                                oneSeries.add(ts);
                            } else oneSeries.add(ts);

                            if (i == series.size() - 1) {
                                String seriesString = seriesToString(oneSeries, (ts instanceof TimeStampSummary));
                                sb.append(seriesString);
                            }

                            lastX = x;
                        }

                    } else {
                        // Print other data, each item toString + percentage of times it was that value
                        TAGOccurrenceStatSummary stats = (TAGOccurrenceStatSummary) summary;
                        sb.append("\n").append(stats.stringSummary());
                    }
                }

            }

        }

        return sb.toString();
    }

    private String seriesToString(ArrayList<TimeStamp> oneSeries, boolean isSummary)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[n: ").append(oneSeries.size()).append("; ");

        for (TimeStamp ts : oneSeries) {
            if(!isSummary)
                sb.append(ts.v).append(",");
            else {
                TimeStampSummary tss = (TimeStampSummary) ts;
                sb.append(tss.values.mean()).append(";");
            }
        }

        sb.append("]\n");
        return sb.toString().replace(",]", "]");

    }

}
