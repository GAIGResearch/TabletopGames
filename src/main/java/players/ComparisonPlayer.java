package players;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.ITunableParameters;
import evaluation.metrics.Event;
import org.json.simple.JSONObject;
import players.mcts.MCTSPlayer;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static utilities.JSONUtils.loadClassFromJSON;
import static utilities.JSONUtils.loadJSONObjectsFromDirectory;
import static utilities.Utils.jensenShannonDistance;
import static utilities.Utils.pdf;

public class ComparisonPlayer extends AbstractPlayer {

    List<AbstractPlayer> playersCompared;
    List<String> playerNames;

    final String outputPath;

    double[] agreementMatrix;

    final String csv_name;

    final int nSamples = 100;

    int playerCount;
    int decisionCount = 0;

    public ComparisonPlayer(List<AbstractPlayer> playersCompared, String outputPath) {
        super(new PlayerParameters(), "ComparisonPlayer");
        this.playersCompared = new ArrayList<>();
        for (AbstractPlayer p : playersCompared) {
            this.playersCompared.add(p.copy());
        }
        this.outputPath = outputPath;
        this.playerCount = playersCompared.size();
        csv_name = "AgreementMatrix.csv";
    }

    public ComparisonPlayer(String playersDir, String outputPath, String csv_name) {
        super(new PlayerParameters(), "ComparisonPlayer");
        this.csv_name = csv_name;
        playerNames = new ArrayList<>();
        Map<String, JSONObject> playersJSON = loadJSONObjectsFromDirectory(playersDir, true, "");
        playersCompared = new ArrayList<>();
        for (Map.Entry<String, JSONObject> entry : playersJSON.entrySet()) {
            Object thing = loadClassFromJSON(entry.getValue());
            if (thing instanceof PlayerParameters playerParams) {
                AbstractPlayer p = playerParams.instantiate();
                p.setName(entry.getKey());
                playersCompared.add(p);
                playerNames.add(entry.getKey());
            }
            else if (thing instanceof ITunableParameters<?> params) {
                Object instance = params.instantiate();
                if (instance instanceof AbstractPlayer p) {
                    p.setName(entry.getKey());
                    playersCompared.add(p);
                    playerNames.add(entry.getKey());
                }
            }
            else if (thing instanceof AbstractPlayer p) {
                p.setName(entry.getKey());
                playersCompared.add(p);
                playerNames.add(entry.getKey());
            }
        }
        this.outputPath = outputPath;
        this.playerCount = playersCompared.size();
        System.out.println(playerNames);
    }

    @Override
    public void initializePlayer(AbstractGameState gameState) {
        File csvFile = new File(outputPath, csv_name);
        if (csvFile.exists()) {
            agreementMatrix = fromCVS();
        }
        else {
            agreementMatrix = new double[playerCount * playerCount];
            Arrays.fill(agreementMatrix, 0);
        }
        for (AbstractPlayer p : playersCompared) {
            p.initializePlayer(gameState.copy());
        }
    }

    @Override
    public void finalizePlayer(AbstractGameState gameState) {
//        for (int i = 0; i < agreementMatrix.length; i++) {
//            agreementMatrix[i] = agreementMatrix[i] / decisionCount;
//        }
        toCSV();
        for (AbstractPlayer p : playersCompared) {
            p.finalizePlayer(gameState.copy());
        }
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        if (possibleActions.size() == 1) {
            return possibleActions.get(0);
        }
        long t = System.nanoTime();
        decisionCount++;
//        List<AbstractAction> actions = getComparedPlayerActions(gameState, possibleActions);
//        simpleAgreementMatrixUpdate(actions);
//        AbstractAction chosenAction = actions.get(getRnd().nextInt(actions.size()));    // Choose from compared players' actions
        List<double[]> policies = new ArrayList<>();
        for (int i = 0; i < playersCompared.size(); i++) {
            if (playersCompared.get(i) instanceof MCTSPlayer mctsP) {
                policies.add(getMCTSPolicy(gameState, possibleActions, mctsP));
            }
            else {
                policies.add(samplePlayerPolicy(gameState, possibleActions, playersCompared.get(i)));
            }
        }
        policyAgreementMatrixUpdate(policies);
//        System.out.println("TIME SPENT: " + (System.nanoTime() - t)/1000000 + "ms");
//        for (int i = 0; i < policies.size() ; i++) {
//            System.out.println(playerNames.get(i) + " " + Arrays.toString(policies.get(i)));
//        }
//        System.out.println("--------------------");

        AbstractAction chosenAction = possibleActions.get(getRnd().nextInt(possibleActions.size())); // OR CHOOSE RANDOM
        // Set lastAction for MCTSPlayer in case did not choose the one they returned
        for (AbstractPlayer p : playersCompared) {
            if (p instanceof MCTSPlayer mctsP) {
                mctsP.setLastAction(chosenAction);
            }
        }
        return chosenAction;
    }


    private double[] samplePlayerPolicy(AbstractGameState gameState, List<AbstractAction> possibleActions, AbstractPlayer player) {
        double[] policy = new double[possibleActions.size()];
        Arrays.fill(policy, 0);
        for (int i = 0; i < nSamples - 1; i++) {
            AbstractPlayer pCopy = player.copy();
            AbstractAction a = pCopy.getAction(gameState.copy(), possibleActions);
            int aIndex = possibleActions.indexOf(a);
            if (aIndex < 0) {
                throw new AssertionError("PLAYER RETURN AN ACTION NOT IN POSSIBLE ACTIONS");
            }
            policy[aIndex] += 1;
        }
        // Let the actual player make decision to modify its internal state (if at all)
        AbstractAction a = player.getAction(gameState.copy(), possibleActions);
        int aIndex = possibleActions.indexOf(a);
        if (aIndex < 0) {
            throw new AssertionError("PLAYER RETURN AN ACTION NOT IN POSSIBLE ACTIONS");
        }
        policy[aIndex] += 1;
        return policy;
    }


    private double[] getMCTSPolicy(AbstractGameState gameState, List<AbstractAction> possibleActions, MCTSPlayer player) {
        player.getAction(gameState.copy(), possibleActions);
        return player.getSoftmaxPolicy(possibleActions, 1);
    }

    private List<AbstractAction> getComparedPlayerActions(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        List<AbstractAction> actions = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            AbstractAction a = playersCompared.get(i).getAction(gameState.copy(), possibleActions);
            if (!possibleActions.contains(a)) {
                throw new AssertionError("Action: " + a.toString() + " played by " + playerNames.get(i) + " that was not in the list of available actions: " + possibleActions);
            }
            actions.add(a);
        }
        return actions;
    }

    // Update based on if chosen actions are the same between two agents
    // Unreliable for comparing high variance agents (e.g. OSLA and Random)
    private void simpleAgreementMatrixUpdate(List<AbstractAction> chosenActions) {
        for (int i = 0; i < playerCount; i++) {
            for (int j = 0; j < playerCount; j++) {
                if (chosenActions.get(i).equals(chosenActions.get(j))) {
                    agreementMatrix[i * playerCount + j] += 1;
                }
            }
        }
    }

    private void policyAgreementMatrixUpdate(List<double[]> policies) {
        for (int i = 0; i < playerCount; i++) {
            for (int j = i; j < playerCount; j++) {
                if (i == j) {
                    agreementMatrix[i * playerCount + j] += 1;
                }
                else {
                    double jsd = jensenShannonDistance(pdf(policies.get(i)), pdf(policies.get(j)));
                    if (Double.isNaN(jsd)) {
                        throw new AssertionError("NaN SOMEHOW");
                    }
                    agreementMatrix[i * playerCount + j] += jsd;
                    agreementMatrix[j * playerCount + i] = agreementMatrix[i * playerCount + j];
                }
            }
        }
    }

    private double jaccardSimilarity(double[] p, double[] q) {
        p = pdf(p); q = pdf(q);
        double min_sum = 0;
        double max_sum = 0;
        for (int i = 0; i < p.length; i++) {
            min_sum += Math.min(p[i], q[i]);
            max_sum += Math.max(p[i], q[i]);
        }
        return min_sum/max_sum;
    }


    @Override
    public ComparisonPlayer copy() {
        System.out.println("AAAAA");
        ComparisonPlayer ret = new ComparisonPlayer(playersCompared, outputPath);
        ret.decisionCount = decisionCount;
        if (agreementMatrix != null) {
            ret.agreementMatrix = agreementMatrix.clone();
        }
        ret.setForwardModel(this.getForwardModel());
        return ret;
    }

    private void toCSV() {
        File csvFile = new File(outputPath, csv_name);
        try (FileWriter writer = new FileWriter(csvFile)) {
            if (playerNames != null) {
                for (int i = 0; i < playerCount; i++) {
                    writer.append(playerNames.get(i));
                    if (i < playerCount - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }
            for (int i = 0; i < playerCount; i++) {
                for (int j = 0; j < playerCount; j++) {
                    writer.append(String.valueOf(agreementMatrix[i * playerCount + j]));
                    if (j < playerCount - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private double[] fromCVS() {
        File csvFile = new File(outputPath, csv_name);
        double[] matrix = new double[playerCount * playerCount];
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int row = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                try {
                    double t = Double.parseDouble(values[0]);
                }
                catch (NumberFormatException e) {
                    continue;
                }
                for (int col = 0; col < values.length; col++) {
                    matrix[row * playerCount + col] = Double.parseDouble(values[col]);
                }
                row++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return matrix;
    }

    @Override
    public void setForwardModel(AbstractForwardModel model) {
        super.setForwardModel(model);
        for (AbstractPlayer p : playersCompared) {
            p.setForwardModel(model);
        }
    }

    @Override
    public void registerUpdatedObservation(AbstractGameState gameState) {
        super.registerUpdatedObservation(gameState);
        for (AbstractPlayer p : playersCompared) {
            p.registerUpdatedObservation(gameState.copy());
        }
    }

    @Override
    public void onEvent(Event event) {
        super.onEvent(event);
        for (AbstractPlayer p : playersCompared) {
            p.onEvent(event);
        }
    }
}
