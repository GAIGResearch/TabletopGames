package players.heuristics;

import core.*;
import core.actions.AbstractAction;
import evaluation.metrics.Event;
import players.mcts.ITreeProcessor;
import players.mcts.SingleTreeNode;
import utilities.Pair;
import utilities.Utils;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class LearnedValue extends AbstractPlayer implements ITreeProcessor {

    String filename, suffix;
    int count = 0;
    int threshold = 50;
    double gameGamma = 1.0;
    int anchorVisits = 5000;
    long timestamp;
    boolean useAdvantage = true;
    double defaultValue = 0.0;
    ActionValueHeuristic valueHeuristic;
    Map<String, Map<Integer, Pair<Integer, Double>>> statsByBucketAndHash = new HashMap<>();
    Map<String, Map<Integer, Pair<Integer, Double>>> newData = new HashMap<>();
    Function<AbstractGameState, String> bucketingFunction;

    @SuppressWarnings("unchecked")
    public LearnedValue(String constructionString) {
        super(null, "LearnedValue");

        String[] stuff = constructionString.split(Pattern.quote(":"));
        filename = stuff[0].split(Pattern.quote("."))[0];
        suffix = stuff[0].split(Pattern.quote("."))[1];
        double RND_WEIGHT = Double.parseDouble(stuff[1]);
        if (stuff.length > 2)
            threshold = Integer.parseInt(stuff[2]);
        if (stuff.length > 3)
            gameGamma = Double.parseDouble(stuff[3]);
        if (stuff.length > 4)
            anchorVisits = Integer.parseInt(stuff[4]);
        if (stuff.length > 5)
            useAdvantage = Boolean.parseBoolean(stuff[5]);
        if (stuff.length > 6)
            defaultValue = Double.parseDouble(stuff[6]);
        if (stuff.length > 7) {
            try {
                // TODO : Convert to Utils.loadFromFile()
                Class<?> bucketClass = Class.forName(stuff[7]);
                bucketingFunction = (Function<AbstractGameState, String>) bucketClass.getConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                bucketingFunction = null;
            }
        }

        statsByBucketAndHash.put("", new HashMap<>());
        timestamp = initialiseFromFile();
        statsByBucketAndHash = decayStats();
        valueHeuristic = new ActionValueHeuristic(getValues(), RND_WEIGHT, bucketingFunction, defaultValue);
    }

    private Map<String, Map<Integer, Pair<Integer, Double>>> decayStats() {
        return statsByBucketAndHash.keySet().stream()
                .collect(toMap(bucket -> bucket, bucket -> Utils.decay(statsByBucketAndHash.get(bucket), gameGamma)));
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        timestamp = initialiseFromFile();
        statsByBucketAndHash = decayStats();
        newData = new HashMap<>();
        newData.put("", new HashMap<>());
        valueHeuristic = new ActionValueHeuristic(getValues(), valueHeuristic.RND_WEIGHT, bucketingFunction, defaultValue);
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        return valueHeuristic._getAction(gameState, possibleActions);
    }

    private long initialiseFromFile() {

        File file = new File(filename + "_Stats." + suffix);

        try {
            if (file.exists()) {
                long retValue = file.lastModified();

                BufferedReader reader = new BufferedReader(new FileReader(file));

                // we expect four columns; bucket, hash, visits, totAdvantage
                reader.readLine(); // skip header
                String nextLine = reader.readLine();
                while (nextLine != null) {
                    String[] data = nextLine.split(",");
                    String bucket = data[0];
                    if (!statsByBucketAndHash.containsKey(bucket))
                        statsByBucketAndHash.put(bucket, new HashMap<>());
                    int hash = Integer.parseInt(data[1].trim());
                    int visits = Integer.parseInt(data[2].trim());
                    double totValue = Double.parseDouble(data[3].trim());
                    Pair<Integer, Double> stats = new Pair<>(visits, totValue);
                    statsByBucketAndHash.get(bucket).put(hash, stats);
                    nextLine = reader.readLine();
                }

                reader.close();

                return retValue;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void mergeWithAndWriteToFile() {
        // the file may have been updated since we read it in

        statsByBucketAndHash = new HashMap<>(); // clear data (but keep newData)
        statsByBucketAndHash.put("", new HashMap<>());
        long newTimestamp = initialiseFromFile(); // pick up latest data
        //    System.out.printf("%d into %d %n", currentData.size(), statsByHash.size());
        //      System.out.printf("Writing file - from TS %d to %d%n", timestamp, newTimestamp);
        if (newTimestamp == timestamp) {
            // file has not been changed, so we are responsible for decaying the current file contents before writing
            statsByBucketAndHash = decayStats();
            //         System.out.printf("\tWriting all data : %d%n", newData.size());
        } else {
            // we do not decay the current file contents, as that has been done already
            //         System.out.printf("\tWriting only newData : %d%n", newData.size());
        }

        for (String bucket : statsByBucketAndHash.keySet()) {
            Map<Integer, Pair<Integer, Double>> statsByHash = statsByBucketAndHash.get(bucket);
            if (!newData.containsKey(bucket))
                newData.put(bucket, new HashMap<>());
            Map<Integer, Pair<Integer, Double>> newDataByHash = newData.get(bucket);
            for (Integer key : statsByHash.keySet()) {
                newDataByHash.merge(key, statsByHash.get(key),
                        (val1, val2) -> new Pair<>(val1.a + val2.a, val1.b + val2.b));
            }
        }

        try {
            BufferedWriter advWriter = new BufferedWriter(new FileWriter(filename + "_A002." + suffix));
            BufferedWriter advWriter2 = new BufferedWriter(new FileWriter(filename + "_A002_" + count + "." + suffix));
            BufferedWriter statsWriter = new BufferedWriter(new FileWriter(filename + "_Stats." + suffix));
            BufferedWriter statsWriter2 = new BufferedWriter(new FileWriter(filename + "_Stats_" + count + "." + suffix));


            advWriter.write(valueHeuristic.RND_WEIGHT + "," + (bucketingFunction == null ? "" : bucketingFunction.getClass().getName()));
            advWriter.newLine();
            advWriter.write("Bucket, ActionHash, Value, Name");
            advWriter.newLine();
            advWriter2.write(valueHeuristic.RND_WEIGHT + "," + (bucketingFunction == null ? "" : bucketingFunction.getClass().getName()));
            advWriter2.newLine();
            advWriter2.write("Bucket, ActionHash, Value, Name");
            advWriter2.newLine();
            // we expect two columns; hash and advantage estimate


            statsWriter.write("Bucket, ActionHash, Visits, TotAdvantage");
            statsWriter.newLine();
            statsWriter2.write("Bucket, ActionHash, Visits, TotAdvantage");
            statsWriter2.newLine();


            //  Map<String, Map<Integer, Double>> actionAdvantageByBucket = getValues();
            Map<Integer, String> actionNames = valueHeuristic.actionNames;
            for (String bucket : newData.keySet()) {
                Map<Integer, Pair<Integer, Double>> newDataByHash = newData.get(bucket);
                //   Map<Integer, Double> actionAdvantage = actionAdvantageByBucket.getOrDefault(bucket, new HashMap<>());
                for (Integer hash : newDataByHash.keySet()) {
                    double specificVisits = newDataByHash.get(hash).a;
                    double specificAdvantage = newDataByHash.get(hash).b;
                    double generalAdvantage = newData.get("").get(hash).b;
                    double generalVisits = newData.get("").get(hash).a;
                    double generalWeight = Math.sqrt(anchorVisits / (3.0 * specificVisits + anchorVisits));
                    double specificResult = specificAdvantage / (specificVisits + anchorVisits);
                    double generalResult = generalAdvantage / (generalVisits + anchorVisits);
                    double finalAdvantage = specificResult * (1.0 - generalWeight) + generalResult * generalWeight;

                    advWriter.write(String.format("%s, %d, %.3f, %s", bucket, hash, finalAdvantage, actionNames.getOrDefault(hash, "")));
                    advWriter.newLine();
                    advWriter2.write(String.format("%s, %d, %.3f, %s", bucket, hash, finalAdvantage, actionNames.getOrDefault(hash, "")));
                    advWriter2.newLine();

                    statsWriter.write(String.format("%s, %d, %.0f, %.6f", bucket, hash, specificVisits, specificAdvantage));
                    statsWriter.newLine();
                    statsWriter2.write(String.format("%s, %d, %.0f, %.6f", bucket, hash, specificVisits, specificAdvantage));
                    statsWriter2.newLine();
                }
            }

            advWriter.close();
            advWriter2.close();
            statsWriter.close();
            statsWriter2.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.GAME_OVER) {
            count++;
            mergeWithAndWriteToFile();
        }
    }


    @Override
    public void process(SingleTreeNode node) {

        Queue<SingleTreeNode> queue = new ArrayDeque<>();
        if (node.getVisits() >= threshold)
            queue.add(node);

        int actor = node.getActor();
        AbstractForwardModel forwardModel = node.getForwardModel();

        while (!queue.isEmpty()) {
            SingleTreeNode n = queue.poll();
            double meanValue = useAdvantage ? n.nodeValue(actor) : 0.0;
            // we then get all the data for the actions taken
            // for each hashcode, we record the number of visits, and the mean advantage
            // if this is a previously unseen hashcode (not in statsByHash), then we add a baseline of anchorVisits
            // to dampen any rapid changes

            List<String> buckets = new ArrayList<>();
            buckets.add("");
            if (bucketingFunction != null)
                buckets.add(bucketingFunction.apply(n.getState()));

            List<AbstractAction> actionsFromState = forwardModel.computeAvailableActions(n.getState());
            //no point calculating if only one action!
            if (actionsFromState.size() > 1) {
                List<Pair<AbstractAction, SingleTreeNode>> actionsToNodes = n.getChildren().keySet().stream()
                        .filter(action -> n.getChildren().get(action) != null &&
                                n.getChildren().get(action)[actor] != null &&
                                actionsFromState.contains(action))
                        .map(action -> new Pair<>(action, n.getChildren().get(action)[actor]))
                        .collect(toList());


                for (String bucket : buckets) {

                    if (!statsByBucketAndHash.containsKey(bucket))
                        statsByBucketAndHash.put(bucket, new HashMap<>());
                    if (!newData.containsKey(bucket))
                        newData.put(bucket, new HashMap<>());

                    Map<Integer, Pair<Integer, Double>> statsByHash = statsByBucketAndHash.get(bucket);

                    // if useAdvantage then we subtract meanValue * visits
                    Map<Integer, Pair<Integer, Double>> data = actionsToNodes.stream()
                            .collect(toMap(p -> p.a.hashCode(),
                                    p -> new Pair<>(p.b.getVisits(), p.b.getVisits() * (p.b.nodeValue(actor) - meanValue))));


                    // now we merge this data into the existing map
                    Map<Integer, Pair<Integer, Double>> newDataByHash = newData.get(bucket);
                    for (Integer hash : data.keySet()) {
                        newDataByHash.merge(hash, data.get(hash),
                                (val1, val2) -> new Pair<>(val1.a + val2.a, val1.b + val2.b));
                        statsByHash.merge(hash, data.get(hash),
                                (val1, val2) -> new Pair<>(val1.a + val2.a, val1.b + val2.b));
                    }

                }
            }
            // and then add the child node that have sufficient visits
            queue.addAll(n.getChildren().values().stream()
                    .filter(Objects::nonNull)
                    .map(arr -> arr[actor])
                    .filter(it -> it != null && it.getVisits() >= threshold).collect(Collectors.toList()));

        }
    }

    private Map<String, Map<Integer, Double>> getValues() {
        return statsByBucketAndHash.keySet().stream().collect(toMap(
                bucket -> bucket,
                bucket -> {
                    Map<Integer, Pair<Integer, Double>> statsByHash = statsByBucketAndHash.get(bucket);
                    return statsByBucketAndHash.get(bucket).keySet().stream().collect(toMap(
                            hash -> hash,
                            hash -> {
                                int visits = statsByHash.get(hash).a;
                                return visits == 0 ? 0.0 : statsByHash.get(hash).b / (double) visits;
                            }
                    ));
                }));
    }

    @Override
    public LearnedValue copy() {
        throw new AssertionError("Not yet supported");
    }
}
