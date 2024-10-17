package players.heuristics;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;

import static java.util.stream.Collectors.toList;

public class ActionValueHeuristic extends AbstractPlayer implements ToDoubleBiFunction<AbstractAction, AbstractGameState> {

    Random rnd = new Random(System.currentTimeMillis());

    String filename;

    protected double RND_WEIGHT;
    double defaultValue = 0.0;

    Map<String, Map<Integer, Double>> actionValues = new HashMap<>();
    Map<Integer, String> actionNames = new HashMap<>();
    Function<AbstractGameState, String> bucketFunction = s -> "";

    public ActionValueHeuristic(
            Map<String, Map<Integer, Double>> advantages,
            double rndWeight,
            Function<AbstractGameState, String> bucketFn,
            double defaultValue
    ) {
        super(null, "ActionValueHeuristic");
        actionValues = advantages;
        RND_WEIGHT = rndWeight;
        if (bucketFn == null)
            throw new IllegalArgumentException("Must specify a bucket function");
        bucketFunction = bucketFn;
        this.defaultValue = defaultValue;
    }

    public ActionValueHeuristic(String filename) {
        super(null, "ActionValueHeuristic");
        this.filename = filename;
        initialiseFromFile();
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        initialiseFromFile();
    }

    @Override
    public ActionValueHeuristic copy() {
        ActionValueHeuristic retValue = new ActionValueHeuristic(new HashMap<>(), RND_WEIGHT, bucketFunction, defaultValue);
        retValue.actionValues.putAll(this.actionValues);
        retValue.filename = this.filename;
        retValue.rnd = new Random(rnd.nextInt());
        retValue.actionNames.putAll(this.actionNames);
        return retValue;
    }

    @SuppressWarnings("unchecked")
    private void initialiseFromFile() {

        // TODO : Convert to Utils.loadFromFile()
        try {
            if (filename != null && (new File(filename)).exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(filename));
                String[] header = reader.readLine().split(",");
                RND_WEIGHT = Double.parseDouble(header[0]);
                if (header.length > 1) {
                    try {
                        Class<?> bucketClass = Class.forName(header[1]);
                        bucketFunction = (Function<AbstractGameState, String>) bucketClass.getConstructor().newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                        bucketFunction = s -> "";
                    }
                }
                reader.readLine();
                // we expect columns: bucket, hash, value estimate, name (optional)

                String nextLine = reader.readLine();
                while (nextLine != null) {
                    List<String> data = Arrays.stream(nextLine.split(",")).collect(toList());

                    String bucket = data.get(0).trim();
                    Map<Integer, Double> advantages = getActionValues(bucket);

                    int hash = Integer.parseInt(data.get(1).trim());
                    double advantage = Double.parseDouble(data.get(2).trim());
                    advantages.put(hash, advantage);
                    if (data.size() > 3) {
                        String name = data.get(3).trim();
                        actionNames.put(hash, name);
                    }
                    nextLine = reader.readLine();
                }

                reader.close();
            } else {
                System.out.println("File not found : " + filename);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<Integer, Double> getActionValues(String bucket) {
        if (!actionValues.containsKey(bucket))
            actionValues.put(bucket, new HashMap<>());
        return actionValues.get(bucket);
    }


    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        if (possibleActions.size() == 1)
            return possibleActions.get(0);

        double bestValue = Double.NEGATIVE_INFINITY;
        String bucket = bucketFunction.apply(gameState);
        AbstractAction retValue = possibleActions.get(0);

        for (AbstractAction action : possibleActions) {
            double actionValue = actionValue(action, bucket);
            if (actionValue > bestValue) {
                retValue = action;
                bestValue = actionValue;
            }
        }
        return retValue;
    }

    private double actionValue(AbstractAction action, String bucket) {
        Map<Integer, Double> actionValues = getActionValues(bucket);
        Map<Integer, Double> defaultValues = getActionValues("");
        int hash = action.hashCode();
        double actionValue = defaultValue;
        if (actionValues.containsKey(hash)) {
            actionValue = actionValues.get(hash);
        } else if (defaultValues.containsKey(hash)) {
            actionValue = defaultValues.get(hash);
        }

        actionValue += rnd.nextDouble() * RND_WEIGHT;
        actionNames.putIfAbsent(hash, action.toString());
        return actionValue;
    }

    Set<Integer> unknownHashCodes = new HashSet<>();

    @Override
    public double applyAsDouble(AbstractAction abstractAction, AbstractGameState gameState) {
        String bucket = bucketFunction.apply(gameState);
//        if (!actionValues.isEmpty() && !actionValues.containsKey(hash) && !unknownHashCodes.contains(hash)) {
//            unknownHashCodes.add(hash);
//            System.out.println("Action not found : " + hash + " " + abstractAction.toString() + " : " + bucket);
//        }
        return actionValue(abstractAction, bucket);
    }
}
