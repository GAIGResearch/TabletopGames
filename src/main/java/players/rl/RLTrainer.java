package players.rl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IStateFeatureVector;
import evaluation.listeners.IGameListener;
import games.GameType;
import games.dominion.stats.DomStateFeaturesReduced;
import players.heuristics.WinOnlyHeuristic;
import players.human.ActionController;
import players.rl.RLPlayer.RLType;
import players.rl.RLTrainingParams.Solver;
import players.rl.RLTrainingParams.WriteSegmentType;
import players.rl.featureVectors.SushiGoFeatureVector;
import players.rl.utils.TurnSAR;
import players.rl.DataProcessor.Field;

class RLTrainer {

    public final RLTrainingParams trainingParams;
    final RLParams playerParams;
    private Map<Integer, List<TurnSAR>> playerTurns;

    private QWeightsDataStructure qwds;
    private DataProcessor dp = null;

    private RLTrainer(RLTrainingParams params, RLParams playerParams) {
        this.trainingParams = params;
        this.playerParams = playerParams;
        RLType type = getRLType();
        this.qwds = type == RLType.Tabular ? new QWDSTabular()
                : type == RLType.LinearApprox ? new QWDSLinearApprox() : null;
        resetTrainer();
    }

    private RLType getRLType() {
        if (playerParams.getType() != null)
            return playerParams.getType();
        JsonNode data = DataProcessor.readInputFile(trainingParams.gameName, playerParams.infileNameOrPath);
        return (RLType.valueOf(data.get("Metadata").get(Field.Type.name()).asText()));
    }

    void addTurn(RLPlayer player, AbstractGameState state, AbstractAction action,
            List<AbstractAction> possibleActions) {
        int playerId = player.getPlayerID();
        double reward = trainingParams.heuristic.evaluateState(state, playerId);

        // Add the turn to playerTurns
        TurnSAR turn = new TurnSAR(state.copy(playerId), action, possibleActions, reward);
        if (!playerTurns.containsKey(playerId))
            playerTurns.put(playerId, new ArrayList<TurnSAR>());
        playerTurns.get(playerId).add(turn);
    }

    void train(RLPlayer player, AbstractGameState finalGameState) {
        addTurn(player, finalGameState, null, null);
        // TODO implement other methods than qLearning
        qwds.qLearning(player, playerTurns.get(player.getPlayerID()));
    }

    private void resetTrainer() {
        playerTurns = new HashMap<>();
    }

    private void runTraining() {
        boolean useGUI = false;
        int turnPause = 0;
        String gameParams = null;

        ArrayList<AbstractPlayer> players = new ArrayList<>();

        for (int i = 0; i < trainingParams.nPlayers; i++) {
            RLPlayer p = new RLPlayer(playerParams, qwds, this);
            p.initializePlayer(trainingParams.gameName);
            players.add(p);
        }

        if (dp == null) {
            dp = new DataProcessor(qwds);
            dp.initNextSegmentFile(getNextSegmentThreshold(0));
            dp.updateAndWriteFile(0);
        }

        System.out.println("Starting Training!");

        int gamesPlayedSinceLastWrite = 0;
        int progress = -1;
        for (int i = 0; i < trainingParams.nGames; i++) {
            runGame(GameType.valueOf(trainingParams.gameName), gameParams, players, System.currentTimeMillis(), false,
                    null,
                    useGUI ? new ActionController() : null, turnPause);
            if (shouldWriteSegment(i)) {
                dp.updateAndWriteFile(gamesPlayedSinceLastWrite);
                dp.initNextSegmentFile(getNextSegmentThreshold(i));
                gamesPlayedSinceLastWrite = 0;
                dp.updateAndWriteFile(gamesPlayedSinceLastWrite);
            }
            else if (shouldUpdate(i)) {
                dp.updateAndWriteFile(gamesPlayedSinceLastWrite);
                gamesPlayedSinceLastWrite = 0;
            }
            gamesPlayedSinceLastWrite++;
            // Print progress
            int _progress = progress;
            progress = (100 * (i + 1)) / trainingParams.nGames;
            if (progress != _progress)
                System.out.print("\r" + progress + "%");
        }

        dp.updateAndWriteFile(gamesPlayedSinceLastWrite);
        System.out.println("\tTraining complete!");
    }

    private int getNextSegmentThreshold(int n) {
        WriteSegmentType type = trainingParams.writeSegmentType;
        if (type == WriteSegmentType.NONE || trainingParams.writeSegmentFactor <= 0)
            return trainingParams.nGames;

        int nextSegmentNIterations = Math.max(type.n0, trainingParams.writeSegmentMinIterations);
        while (nextSegmentNIterations <= n)
            nextSegmentNIterations = type.operator.operate(nextSegmentNIterations, trainingParams.writeSegmentFactor);

        return Math.min(nextSegmentNIterations, trainingParams.nGames);
    }

    private boolean shouldUpdate(int iterations) {
        if (iterations == 0)
            return false;
        return iterations % trainingParams.updateXIterations == 0;
    }

    private boolean shouldWriteSegment(int iterations) {
        if (iterations < trainingParams.writeSegmentMinIterations)
            return false;
        switch (trainingParams.writeSegmentType) {
            case LINEAR:
                return (iterations - trainingParams.writeSegmentMinIterations) % trainingParams.writeSegmentFactor == 0;
            case LOGARITHMIC:
                int factor = iterations / trainingParams.writeSegmentMinIterations;
                // Check if 'iterations' is a perfect factor of 'minIterations'
                if (trainingParams.writeSegmentMinIterations * factor == iterations) {
                    double log = Math.log(factor) / Math.log(trainingParams.writeSegmentFactor);
                    return Math.abs(log - Math.round(log)) < 1e-10;
                }
                return false;
            case NONE:
            default:
                return false;
        }
    }

    private void runGame(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed,
            boolean randomizeParameters, List<IGameListener> listeners, ActionController ac, int turnPause) {
        Game.runOne(gameToPlay, parameterConfigFile, players, seed, randomizeParameters, listeners, ac, turnPause);
        resetTrainer();
    }

    public static void main(String[] args) {
        RLTrainingParams params = new RLTrainingParams("SushiGo", 4, 500000);
        params.writeSegmentType = WriteSegmentType.LOGARITHMIC;
        params.writeSegmentFactor = 3;
        params.writeSegmentMinIterations = 1000;
        params.updateXIterations = 1000;
        params.alpha = 0.001f;
        params.gamma = 0.875f;
        params.solver = Solver.Q_LEARNING;
        params.heuristic = new WinOnlyHeuristic();
        // params.outfilePrefix = "1";

        // long seed = System.currentTimeMillis();
        // long seed = 1688997020795l;

        String infilePath = null;
        // String infilePath = "LinearApprox/Attempt_01_n=10000.json";

        IStateFeatureVector featureVector = new SushiGoFeatureVector();
        // IActionFeatureVector featureVector = null;
        RLType type = RLType.LinearApprox;

        for (int i = 0; i < 5; i++) {
            long seed = System.currentTimeMillis();
            params.outfilePrefix = String.format("Attempt_%02d", i + 1);
            RLParams playerParams = infilePath == null
                    ? new RLParams(featureVector, type, seed)
                    : new RLParams(infilePath, seed);
            playerParams.epsilon = 0.375f;

            RLTrainer trainer = new RLTrainer(params, playerParams);
            trainer.runTraining();
        }
    }

}
