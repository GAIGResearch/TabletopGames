package players.rl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import games.GameType;
import players.heuristics.WinOnlyHeuristic;
import players.human.ActionController;
import players.rl.RLPlayer.RLType;
import players.rl.RLTrainingParams.Solver;
import players.rl.RLTrainingParams.WriteSegmentType;
import players.rl.resources.featureVectors.*;

class RLTrainer {

    public final RLTrainingParams params;
    final RLParams playerParams;
    private Map<Integer, List<TurnSAR>> playerTurns;

    private QWeightsDataStructure qwds;
    private DataProcessor dp = null;

    private RLTrainer(RLTrainingParams params, RLParams playerParams, String infileNameOrAbsPath) {
        this.params = params;
        this.playerParams = playerParams;
        this.qwds = new QWDSLinearApprox(infileNameOrAbsPath);
        resetTrainer();
        prematurelySetupQWDS();
    }

    void prematurelySetupQWDS() {
        // Note: This is done because these functions are usually called from RLPlayer
        // inside RLPlayer::initializePlayer. However, we need this information before
        // that call, and are therefore calling these functions manually from here
        qwds.setPlayerParams(playerParams);
        qwds.setTrainingParams(params);
        qwds.initialize(params.gameName);
    }

    void addTurn(RLPlayer player, AbstractGameState state, AbstractAction action,
            List<AbstractAction> possibleActions) {
        int playerId = player.getPlayerID();
        double reward = params.heuristic.evaluateState(state, playerId);

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

        for (int i = 0; i < params.nPlayers; i++)
            players.add(new RLPlayer(playerParams, qwds, this));

        this.dp = new DataProcessor(qwds, params.gameName);
        dp.initAndWriteSegmentFile(getNextSegmentThreshold(0));

        System.out.println("Starting Training!");

        int gamesPlayedSinceLastWrite = 0;
        int progress = -1;
        for (int i = 0; i < params.nGames; i++) {
            runGame(GameType.valueOf(params.gameName), gameParams, players, System.currentTimeMillis(), false,
                    null,
                    useGUI ? new ActionController() : null, turnPause);
            if (shouldWriteSegment(i)) {
                dp.updateAndWriteFile(gamesPlayedSinceLastWrite);
                dp.initAndWriteSegmentFile(getNextSegmentThreshold(i));
                gamesPlayedSinceLastWrite = 0;
            }
            if (shouldUpdate(i)) {
                dp.updateAndWriteFile(gamesPlayedSinceLastWrite);
                gamesPlayedSinceLastWrite = 0;
            }
            gamesPlayedSinceLastWrite++;
            // Print progress
            int _progress = progress;
            progress = (100 * (i + 1)) / params.nGames;
            if (progress != _progress)
                System.out.print("\r" + progress + "%");
        }

        dp.initAndWriteSegmentFile(params.nGames);
        System.out.println("\tTraining complete!");
    }

    private int getNextSegmentThreshold(int n) {
        final int factorThreshold = Math.max(params.writeSegmentMinIterations - 1, n);
        int factor;
        switch (params.writeSegmentType) {
            case LINEAR:
                factor = 0;
                while (factor <= factorThreshold)
                    factor += params.writeSegmentFactor;
                return Math.min(factor, params.nGames);
            case LOGARITHMIC:
                factor = 1;
                while (factor <= factorThreshold)
                    factor *= params.writeSegmentFactor;
                return Math.min(factor, params.nGames);
            case NONE:
                return params.nGames;
            default:
                throw new IllegalArgumentException(
                        "Write Segment Type has illegal value: " + params.writeSegmentType.toString());
        }
    }

    private boolean shouldUpdate(int iterations) {
        if (iterations == 0)
            return false;
        return iterations % params.updateXIterations == 0;
            }

            private boolean shouldWriteSegment(int iterations) {
                if (iterations < params.writeSegmentMinIterations)
                    return false;
                switch (params.writeSegmentType) {
                    case LINEAR:
                        return iterations % params.writeSegmentFactor == 0;
                    case LOGARITHMIC:
                        double log = Math.log(iterations) / Math.log(params.writeSegmentFactor);
                        return Math.abs(log - Math.round(log)) < 1e-10;
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
        RLTrainingParams params = new RLTrainingParams("TicTacToe", 2, 10000000);
        params.writeSegmentType = WriteSegmentType.LOGARITHMIC;
        params.writeSegmentFactor = 2;
        params.writeSegmentMinIterations = 16384;
        // params.updateXIterations = 10000;
        params.alpha = 0.0001f;
        params.gamma = 0.875f;
        params.solver = Solver.Q_LEARNING;
        params.heuristic = new WinOnlyHeuristic();

        // long seed = System.currentTimeMillis();
        long seed = 1688424067512l;

        RLParams playerParams = new RLParams(new TicTacToeDim3StateVector(), RLType.LinearApprox, seed);
        playerParams.epsilon = 0.375f;

        String infilePath = null;

        RLTrainer trainer = new RLTrainer(params, playerParams, infilePath);
        trainer.runTraining();
    }

}
