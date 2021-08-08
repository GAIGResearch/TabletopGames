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
    ActionAdvantageHeuristic advantageHeuristic;
    Map<Integer, Pair<Integer, Double>> statsByHash = new HashMap<>();

    public LearnedAdvantage(String constructionString) {

        String[] stuff = constructionString.split(Pattern.quote(":"));
        filename = stuff[0].split(Pattern.quote("."))[0];
        suffix = stuff[0].split(Pattern.quote("."))[1];
        double RND_WEIGHT = Double.parseDouble(stuff[1]);
        if (stuff.length > 2)
            threshold = Integer.parseInt(stuff[2]);
        if (stuff.length > 3)
            gameGamma = Double.parseDouble(stuff[3]);

        initialiseFromFile();
        statsByHash = Utils.decay(statsByHash, gameGamma);
        advantageHeuristic = new ActionAdvantageHeuristic(getAdvantages(), RND_WEIGHT);
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        initialiseFromFile();
        statsByHash = Utils.decay(statsByHash, gameGamma);
        advantageHeuristic = new ActionAdvantageHeuristic(getAdvantages(), advantageHeuristic.RND_WEIGHT);
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        return advantageHeuristic.getAction(gameState, possibleActions);
    }

    private void initialiseFromFile() {

        File file = new File(filename + "_Stats." + suffix);

        try {
            if (file.exists()) {

                BufferedReader reader = new BufferedReader(new FileReader(file));


                // we expect three columns; hash, visits, totAdvantage
                String nextLine = reader.readLine();
                nextLine = reader.readLine(); // skip header
                while (nextLine != null) {
                    List<Double> data = Arrays.stream(nextLine.split(",")).map(Double::valueOf).collect(toList());
                    int hash = data.get(0).intValue();
                    Pair<Integer, Double> stats = new Pair<>(data.get(1).intValue(), data.get(2));
                    statsByHash.put(hash, stats);
                    nextLine = reader.readLine();
                }

                reader.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mergeWithAndWriteToFile() {
        Map<Integer, Pair<Integer, Double>> currentData = statsByHash;
        statsByHash = new HashMap<>();
        initialiseFromFile(); // pick up latest data
    //    System.out.printf("%d into %d %n", currentData.size(), statsByHash.size());
        for (Integer key : currentData.keySet()) {
            statsByHash.merge(key, currentData.get(key),
                    (val1, val2) -> new Pair<>(val1.a + val2.a, val1.b + val2.b));
        }

        try {
            BufferedWriter advWriter = new BufferedWriter(new FileWriter(filename + "_A002." + suffix));
            BufferedWriter advWriter2 = new BufferedWriter(new FileWriter(filename + "_A002_" + count + "." + suffix));
            BufferedWriter statsWriter = new BufferedWriter(new FileWriter(filename + "_Stats." + suffix));
            BufferedWriter statsWriter2 = new BufferedWriter(new FileWriter(filename + "_Stats_" + count + "." + suffix));


            advWriter.write(String.valueOf(advantageHeuristic.RND_WEIGHT));
            advWriter.newLine();
            advWriter.write("ActionHash, Advantage");
            advWriter.newLine();
            advWriter2.write(String.valueOf(advantageHeuristic.RND_WEIGHT));
            advWriter2.newLine();
            advWriter2.write("ActionHash, Advantage");
            advWriter2.newLine();
            // we expect two columns; hash and advantage estimate


            statsWriter.write("ActionHash, Visits, TotAdvantage");
            statsWriter.newLine();
            statsWriter2.write("ActionHash, Visits, TotAdvantage");
            statsWriter2.newLine();


            Map<Integer, Double> actionAdvantage = getAdvantages();
            for (Integer hash : statsByHash.keySet()) {
                advWriter.write(String.format("%d, %.3f", hash, actionAdvantage.getOrDefault(hash, 0.0)));
                advWriter.newLine();
                advWriter2.write(String.format("%d, %.3f", hash, actionAdvantage.getOrDefault(hash, 0.0)));
                advWriter2.newLine();

                statsWriter.write(String.format("%d, %d, %.6f", hash, statsByHash.get(hash).a, statsByHash.get(hash).b));
                statsWriter.newLine();
                statsWriter2.write(String.format("%d, %d, %.6f", hash, statsByHash.get(hash).a, statsByHash.get(hash).b));
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
                                    p.b.getTotValue()[actor] / p.b.getVisits() - meanValue)));
            // now we merge this data into the existing map
            for (Integer hash : data.keySet()) {
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
                key -> statsByHash.get(key).b / (double) statsByHash.get(key).a
        ));
    }
}
