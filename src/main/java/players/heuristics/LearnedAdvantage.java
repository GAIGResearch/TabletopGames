package players.heuristics;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameListener;
import players.mcts.ITreeProcessor;
import players.mcts.SingleTreeNode;
import utilities.Pair;
import utilities.Utils;

import java.io.*;
import java.util.*;
import java.util.function.ToDoubleBiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class LearnedAdvantage extends AbstractPlayer implements IGameListener, ITreeProcessor {

    String filename, suffix;
    int count = 0;
    int threshold = 50;
    double gameGamma = 1.0;
    long timestamp;
    ActionAdvantageHeuristic advantageHeuristic;
    Map<Integer, Pair<Integer, Double>> statsByHash = new HashMap<>();
    Map<Integer, Pair<Integer, Double>> newData = new HashMap<>();

    public LearnedAdvantage(String constructionString) {

        String[] stuff = constructionString.split(Pattern.quote(":"));
        filename = stuff[0].split(Pattern.quote("."))[0];
        suffix = stuff[0].split(Pattern.quote("."))[1];
        double RND_WEIGHT = Double.parseDouble(stuff[1]);
        if (stuff.length > 2)
            threshold = Integer.parseInt(stuff[2]);
        if (stuff.length > 3)
            gameGamma = Double.parseDouble(stuff[3]);

        timestamp = initialiseFromFile();
        statsByHash = Utils.decay(statsByHash, gameGamma);
        advantageHeuristic = new ActionAdvantageHeuristic(getAdvantages(), RND_WEIGHT);
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        timestamp = initialiseFromFile();
        statsByHash = Utils.decay(statsByHash, gameGamma);
        newData = new HashMap<>();
        advantageHeuristic = new ActionAdvantageHeuristic(getAdvantages(), advantageHeuristic.RND_WEIGHT);
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        return advantageHeuristic.getAction(gameState, possibleActions);
    }

    private long initialiseFromFile() {

        File file = new File(filename + "_Stats." + suffix);

        try {
            if (file.exists()) {
                long retValue = file.lastModified();

                BufferedReader reader = new BufferedReader(new FileReader(file));

                // we expect three columns; hash, visits, totAdvantage
                reader.readLine(); // skip header
                String nextLine = reader.readLine();
                while (nextLine != null) {
                    List<Double> data = Arrays.stream(nextLine.split(",")).map(Double::valueOf).collect(toList());
                    int hash = data.get(0).intValue();
                    Pair<Integer, Double> stats = new Pair<>(data.get(1).intValue(), data.get(2));
                    statsByHash.put(hash, stats);
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

        statsByHash = new HashMap<>(); // clear data (but keep newData)
        long newTimestamp = initialiseFromFile(); // pick up latest data
        //    System.out.printf("%d into %d %n", currentData.size(), statsByHash.size());
        //      System.out.printf("Writing file - from TS %d to %d%n", timestamp, newTimestamp);
        if (newTimestamp == timestamp) {
            // file has not been changed, so we are responsible for decaying the current file contents before writing
            statsByHash = Utils.decay(statsByHash, gameGamma);
            //         System.out.printf("\tWriting all data : %d%n", newData.size());
        } else {
            // we do not decay the current file contents, as that has been done already
            //         System.out.printf("\tWriting only newData : %d%n", newData.size());
        }

        for (Integer key : statsByHash.keySet()) {
            newData.merge(key, statsByHash.get(key),
                    (val1, val2) -> new Pair<>(val1.a + val2.a, val1.b + val2.b));
        }

        try {
            BufferedWriter advWriter = new BufferedWriter(new FileWriter(filename + "_A002." + suffix));
            BufferedWriter advWriter2 = new BufferedWriter(new FileWriter(filename + "_A002_" + count + "." + suffix));
            BufferedWriter statsWriter = new BufferedWriter(new FileWriter(filename + "_Stats." + suffix));
            BufferedWriter statsWriter2 = new BufferedWriter(new FileWriter(filename + "_Stats_" + count + "." + suffix));


            advWriter.write(String.valueOf(advantageHeuristic.RND_WEIGHT));
            advWriter.newLine();
            advWriter.write("ActionHash, Advantage, Name");
            advWriter.newLine();
            advWriter2.write(String.valueOf(advantageHeuristic.RND_WEIGHT));
            advWriter2.newLine();
            advWriter2.write("ActionHash, Advantage, Name");
            advWriter2.newLine();
            // we expect two columns; hash and advantage estimate


            statsWriter.write("ActionHash, Visits, TotAdvantage");
            statsWriter.newLine();
            statsWriter2.write("ActionHash, Visits, TotAdvantage");
            statsWriter2.newLine();


            Map<Integer, Double> actionAdvantage = getAdvantages();
            Map<Integer, String> actionNames = advantageHeuristic.actionNames;
            for (Integer hash : newData.keySet()) {
                advWriter.write(String.format("%d, %.3f, %s", hash, actionAdvantage.getOrDefault(hash, 0.0), actionNames.getOrDefault(hash, "")));
                advWriter.newLine();
                advWriter2.write(String.format("%d, %.3f, %s", hash, actionAdvantage.getOrDefault(hash, 0.0), actionNames.getOrDefault(hash, "")));
                advWriter2.newLine();

                statsWriter.write(String.format("%d, %d, %.6f", hash, newData.get(hash).a, newData.get(hash).b));
                statsWriter.newLine();
                statsWriter2.write(String.format("%d, %d, %.6f", hash, newData.get(hash).a, newData.get(hash).b));
                statsWriter2.newLine();
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
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            // we record the current state
            count++;
            mergeWithAndWriteToFile();
        }
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        onGameEvent(type, null);
    }

    @Override
    public void process(SingleTreeNode node) {

        // statsByHash = Utils.decay(statsByHash, 1.0);

        Queue<SingleTreeNode> queue = new ArrayDeque<>();
        if (node.getVisits() >= threshold)
            queue.add(node);

        int actor = node.getActor();

        while (!queue.isEmpty()) {
            SingleTreeNode n = queue.poll();
            double meanValue = n.getTotValue()[actor] / (double) n.getVisits();
            // we then get all the data for the actions taken
            // for each hashcode, we record the number of visits, and the mean advantage
            Map<Integer, Pair<Integer, Double>> data = n.getChildren().keySet().stream()
                    .filter(action -> n.getChildren().get(action) != null &&
                            n.getChildren().get(action)[actor] != null)
                    .map(action -> new Pair<>(action, n.getChildren().get(action)[actor]))
                    .collect(toMap(p -> p.a.hashCode(),
                            p -> new Pair<>(p.b.getVisits(),
                                    p.b.getTotValue()[actor] - meanValue * p.b.getVisits())));
            // now we merge this data into the existing map
            for (Integer hash : data.keySet()) {
                newData.merge(hash, data.get(hash),
                        (val1, val2) -> new Pair<>(val1.a + val2.a, val1.b + val2.b));
                statsByHash.merge(hash, data.get(hash),
                        (val1, val2) -> new Pair<>(val1.a + val2.a, val1.b + val2.b));
            }
            // and then add the child node that have sufficient visits
            queue.addAll(n.getChildren().values().stream()
                    .filter(Objects::nonNull)
                    .map(arr -> arr[actor])
                    .filter(it -> it != null && it.getVisits() >= threshold).collect(Collectors.toList()));
        }
    }

    private Map<Integer, Double> getAdvantages() {
        return statsByHash.keySet().stream().collect(toMap(
                key -> key,
                key -> {
                    int visits = statsByHash.get(key).a;
                    return visits == 0 ? 0.0 : statsByHash.get(key).b / (double) visits;
                }
        ));
    }
}
