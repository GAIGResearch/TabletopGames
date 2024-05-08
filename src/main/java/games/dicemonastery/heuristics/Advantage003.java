//package games.dicemonastery.heuristics;
//
//import core.AbstractGameState;
//import core.AbstractPlayer;
//import core.actions.AbstractAction;
//import evaluation.metrics.AbstractMetric;
//import evaluation.metrics.Event;
//import games.dicemonastery.DiceMonasteryGameState;
//import games.dicemonastery.stats.DiceMonasteryMetrics;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.util.*;
//import java.util.function.ToDoubleBiFunction;
//
//import static java.util.stream.Collectors.toList;
//
//public class Advantage003 extends AbstractPlayer implements ToDoubleBiFunction<AbstractAction, AbstractGameState> {
//
//    Random rnd = new Random(System.currentTimeMillis());
//
//    private double RND_WEIGHT;
//
//    double[][] coefficients = new double[300][20];
//    Map<Integer, Integer> hashToRowIndex = new HashMap<>();
//    List<AbstractMetric> features = new ArrayList<>();
//
//    String name;
//
//    Set<Integer> unknownHashCodes = new HashSet<>();
//
//    private Advantage003(Advantage003 toCopy) {
//        //only used for copying
//        this.RND_WEIGHT = toCopy.RND_WEIGHT;
//        this.rnd = new Random(toCopy.rnd.nextInt());
//        this.coefficients = toCopy.coefficients.clone();
//        this.hashToRowIndex.putAll(toCopy.hashToRowIndex);
//        this.features.addAll(toCopy.features);
//        this.name = toCopy.name;
//        this.unknownHashCodes.addAll(toCopy.unknownHashCodes);
//    }
//    public Advantage003(String filename) {
//        name = filename;
//
//        try {
//            BufferedReader reader = new BufferedReader(new FileReader(filename));
//            String weight = reader.readLine();
//            RND_WEIGHT = Double.parseDouble(weight);
//            List<String> header = Arrays.asList(reader.readLine().split(","));
//
//            Collections.addAll(features, new DiceMonasteryMetrics().getAllMetrics());
//            // assume the first two columns are the Hash and Intercept
////            features = header.subList(2, header.size()).stream().map().collect(toList());
//
//            //   List<List<Double>> data = new ArrayList<>();
//            String nextLine = reader.readLine();
//            int counter = 0;
//            while (nextLine != null) {
//                List<Double> data = Arrays.stream(nextLine.split(",")).map(Double::valueOf).collect(toList());
//
//                int hash = data.get(0).intValue();
//                hashToRowIndex.put(hash, counter);
//                coefficients[counter] = data.subList(1, data.size()).stream().mapToDouble(Double::valueOf).toArray();
//                counter++;
//                nextLine = reader.readLine();
//            }
//
//            reader.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//    }
//
//    @Override
//    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
//
//        if (possibleActions.size() == 1)
//            return possibleActions.get(0);
//
//        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
//        int player = state.getCurrentPlayer();
//
//        // first we calculate each feature for the state
//
//        double[] featureVal = features.stream().mapToDouble(f -> {
//            Object obj = f.run(null, Event.createEvent(Event.GameEvent.GAME_EVENT, state, player));
//            if (obj instanceof Number) return ((Number) obj).doubleValue();
//            if (obj instanceof Boolean) return (Boolean) obj ? 1.0 : 0.0;
//            return 0.0;
//        }).toArray();
//
//        double bestValue = Double.NEGATIVE_INFINITY;
//        AbstractAction retValue = possibleActions.get(0);
//        for (AbstractAction action : possibleActions) {
//            int hash = action.hashCode();
//            double actionValue = 0.0;
//            if (hashToRowIndex.containsKey(hash)) {
//                double[] coeffs = coefficients[hashToRowIndex.get(hash)];
//                actionValue = coeffs[0]; // the intercept
//                for (int i = 1; i <= features.size(); i++) {
//                    actionValue += coeffs[i] * featureVal[i - 1];
//                }
//            }
//            actionValue += rnd.nextDouble() * RND_WEIGHT;
//            if (actionValue > bestValue) {
//                retValue = action;
//                bestValue = actionValue;
//            }
//        }
//        return retValue;
//    }
//
//    @Override
//    public double applyAsDouble(AbstractAction abstractAction, AbstractGameState gameState) {
//
//        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
//        int player = state.getCurrentPlayer();
//
//        // first we calculate each feature for the state
//        double[] featureVal = new double[features.size()];
////        double[] featureVal = features.stream().mapToDouble(f -> {
////            Object obj = f.get(null, Event.createEvent(Event.GameEvent.GAME_EVENT, state, player));
////            if (obj instanceof Number) return ((Number) obj).doubleValue();
////            return 0.0;
////        }).toArray();
//
//        int hash = abstractAction.hashCode();
//        double actionValue = 0.0;
//        if (hashToRowIndex.containsKey(hash)) {
//            double[] coeffs = coefficients[hashToRowIndex.get(hash)];
//            actionValue = coeffs[0]; // the intercept
//            for (int i = 1; i <= features.size(); i++) {
//                actionValue += coeffs[i] * featureVal[i - 1];
//            }
//        } else if (!unknownHashCodes.contains(hash)) {
//            unknownHashCodes.add(hash);
//            System.out.println("Action not found : " + hash + " " + abstractAction.toString() );
//        }
//
//        return actionValue;
//    }
//
//    @Override
//    public String toString() {
//        return "Advantage003 : " + name;
//    }
//
//    @Override
//    public Advantage003 copy() {
//        return new Advantage003(this);
//    }
//}
