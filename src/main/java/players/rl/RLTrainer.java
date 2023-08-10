package players.rl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.RoundRobinTournament;
import evaluation.tournaments.AbstractTournament.TournamentMode;
import games.GameType;
import players.PlayerConstants;
import players.heuristics.WinOnlyHeuristic;
import players.human.ActionController;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rl.RLPlayer.RLType;
import players.rl.RLTrainingParams.Solver;
import players.rl.RLTrainingParams.WriteSegmentType;
import players.rl.featureVectors.SushiGo2PlayerFeatureVector;
import players.rl.featureVectors.SushiGoFeatureVector;
import players.rl.utils.TurnSAR;
import players.simple.RandomPlayer;
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
            dp.initNextSegmentFile(getNextSegmentThreshold(dp.nGamesPlayedFromInfile));
            dp.updateAndWriteFile(0);
        }

        System.out.println("Starting Training!");

        int gamesPlayedSinceLastWrite = 0;
        int progress = -1;
        for (int _i = 0; _i < trainingParams.nGames; _i++) {
            int i = _i + dp.nGamesPlayedFromInfile;
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
            progress = (100 * (i - dp.nGamesPlayedFromInfile + 1)) / trainingParams.nGames;
            if (progress != _progress)
                System.out.print("\r" + progress + "%");
        }

        dp.updateAndWriteFile(gamesPlayedSinceLastWrite);
        System.out.println("\tTraining complete!");
    }

    private int getNextSegmentThreshold(int n) {
        WriteSegmentType type = trainingParams.writeSegmentType;
        if (type == WriteSegmentType.NONE || trainingParams.writeSegmentFactor <= 0)
            return trainingParams.nGames + dp.nGamesPlayedFromInfile;

        int nextSegmentNIterations = Math.max(type.n0, trainingParams.writeSegmentMinIterations);
        while (nextSegmentNIterations <= n)
            nextSegmentNIterations = type.operator.operate(nextSegmentNIterations, trainingParams.writeSegmentFactor);

        return Math.min(nextSegmentNIterations, trainingParams.nGames + dp.nGamesPlayedFromInfile);
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

    public static void main_train() {
        RLTrainingParams params = new RLTrainingParams("SushiGo", 2, 409600);
        params.writeSegmentType = WriteSegmentType.LOGARITHMIC;
        params.writeSegmentFactor = 2;
        params.writeSegmentMinIterations = 100;
        params.updateXIterations = 2500;
        params.alpha = 0.001f;
        params.gamma = 0.875f;
        params.solver = Solver.Q_LEARNING;
        params.heuristic = new WinOnlyHeuristic();

        // IStateFeatureVector featureVector = new SushiGoFeatureVector();
        IActionFeatureVector featureVector = new SushiGo2PlayerFeatureVector();
        RLType type = RLType.LinearApprox;

        int n = 1;

        long seed = System.currentTimeMillis();
        String infilePath = null;
        // String infilePath = "LinearApprox/Test_2P02_Agent_03_n=819200.json";
        params.outfilePrefix = String.format("Test_H2P01_Agent_%02d", n);

        RLParams playerParams = infilePath == null
                ? new RLParams(featureVector, type, seed)
                : new RLParams(infilePath, seed);
        playerParams.epsilon = 0.375f;

        RLTrainer trainer = new RLTrainer(params, playerParams);
        trainer.runTraining();
    }

    public static void main_tournament() {
        List<AbstractPlayer> agents = new LinkedList<AbstractPlayer>();
        String gameName = "SushiGo";
        int playersPerGame = 2;
        int gamesPerMatchup = 500;
        TournamentMode mode = TournamentMode.NO_SELF_PLAY;
        AbstractParameters gameParams = null;
        String finalDir = null;
        String destDir = null;

        // agents.add(new RLPlayer(new
        // RLParams("LinearApprox/Test_01a_Agent_02_n=819200.json")));
        // agents.add(new RLPlayer(new
        // RLParams("LinearApprox/Test_01b_Agent_05_n=1638400.json")));

        // for (int i = 1; i <= 4; i++) {
        // RLPlayer p = new RLPlayer(new
        // RLParams(String.format("LinearApprox/Test_2P01_Agent_%02d_n=51200.json",
        // i)));
        // p.setName("RL" + i);
        // agents.add(p);
        // }

        // agents.add(new RLPlayer(new
        // RLParams("LinearApprox/Test_2P01_Agent_01_n=409600.json")));

        MCTSParams p_mcts = new MCTSParams();
        p_mcts.budgetType = PlayerConstants.BUDGET_TIME;
        p_mcts.budget = 16;
        agents.add(new MCTSPlayer(p_mcts));

        for (int n = 1; n <= 1; n++) {
            RLPlayer p = new RLPlayer(
                    new RLParams(String.format("LinearApprox/Test_2P03_Agent_%02d_n=2048000.json", n)));
            p.setName("RL_" + n);
            agents.add(p);
        }

        RoundRobinTournament rrt = new RoundRobinTournament(agents, GameType.valueOf(gameName), playersPerGame,
                gamesPerMatchup, mode, gameParams, finalDir, destDir);
        // rrt.verbose = false;
        rrt.run();
    }

    public static void main(String[] args) {
        main_train();
        // main_tournament();
    }

}
