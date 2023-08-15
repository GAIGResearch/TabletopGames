package players.rl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.RoundRobinTournament;
import evaluation.tournaments.AbstractTournament.TournamentMode;
import games.GameType;
import games.dotsboxes.DBStateFeaturesReduced;
import games.sushigo.SGHeuristic;
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
import players.rl.featureVectors.TicTacToeDim1StateVector;
import players.rl.featureVectors.TicTacToeDim2StateVector;
import players.rl.featureVectors.TicTacToeDim3StateVector;
import players.rl.utils.ApplyActionStateFeatureVector;
import players.rl.utils.TurnSAR;
import players.simple.RandomPlayer;
import players.rl.DataProcessor.Field;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class RLTrainer {

    public final RLTrainingParams trainingParams;
    final RLParams playerParams;
    private Map<Integer, List<TurnSAR>> playerTurns;

    private QWeightsDataStructure qwds;
    private DataProcessor dp = null;

    private final static int nAgents = 6;
    private static int[] progress = new int[nAgents];

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

    private void runTraining(int agentID) {
        boolean useGUI = false;
        int turnPause = 0;
        String gameParams = null;

        ArrayList<AbstractPlayer> players = new ArrayList<>();

        for (int i = 0; i < trainingParams.nPlayers; i++) {
            RLPlayer p = new RLPlayer(playerParams, qwds, this);
            p.initializePlayer(trainingParams.gameName, i);
            players.add(p);
        }

        if (dp == null) {
            dp = new DataProcessor(qwds);
            dp.initNextSegmentFile(getNextSegmentThreshold(dp.nGamesPlayedFromInfile));
            dp.updateAndWriteFile(0);
        }

        System.out.println("\rStarting Training!" + " ".repeat(23));

        int gamesPlayedSinceLastWrite = 0;
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
            int _progress = progress[agentID];
            progress[agentID] = (100 * (i - dp.nGamesPlayedFromInfile + 1)) / trainingParams.nGames;
            if (_progress != progress[agentID]) {
                String progressString = "| ";
                for (int t = 0; t < progress.length; t++) {
                    progressString += String.format("%d%% | ", progress[t]);
                }
                System.out.print("\r" + progressString);
            }
        }

        dp.updateAndWriteFile(gamesPlayedSinceLastWrite);
        System.out.println("\rTraining complete!" + " ".repeat(23));
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

    static RLTrainingParams params_phase(String gamename, int n) {
        RLTrainingParams params = new RLTrainingParams(gamename, 2, n != 3 ? 409600 : 819200);
        if (n == 1) {
            params.writeSegmentType = WriteSegmentType.LOGARITHMIC;
            params.writeSegmentFactor = 2;
            params.writeSegmentMinIterations = 100;
        } else {
            params.writeSegmentType = WriteSegmentType.LINEAR;
            params.writeSegmentFactor = 204800;
            params.writeSegmentMinIterations = 0;
        }
        return params;
    }

    public static void main_train(String gameName, Object featureVector, RLType type, int phase,
            int bestAgentFromPrevPhase, int agent) {
        RLTrainingParams params = params_phase(gameName, phase);
        params.updateXIterations = 2500;
        params.alpha = 0.001f;
        params.gamma = 0.75f;
        params.solver = Solver.Q_LEARNING;
        params.heuristic = new WinOnlyHeuristic();


        int[] gamesAfterPhase = { 409600, 819200, 1638400 };

        long seed = System.currentTimeMillis();
        String infilePath = phase == 1 ? null
                : String.format("%s/%s_Phase%02d_Agent%02d_n=%d.json", type.name(),
                        featureVector.getClass().getSimpleName(),
                        phase - 1,
                        bestAgentFromPrevPhase, gamesAfterPhase[phase - 2]);
        params.outfilePrefix = String.format("%s_Phase%02d_Agent%02d", featureVector.getClass().getSimpleName(), phase,
                agent);

        RLParams playerParams = infilePath == null
                ? (featureVector instanceof IActionFeatureVector)
                        ? new RLParams((IActionFeatureVector) featureVector, type, seed)
                        : new RLParams((IStateFeatureVector) featureVector, type, seed)
                : new RLParams(infilePath, seed);
        playerParams.epsilon = 0.1f;

        RLTrainer trainer = new RLTrainer(params, playerParams);
        trainer.runTraining(agent - 1);
    }

    public static void main_tournament(String gameName, Object featureVector, RLType type, int phase, int nGames) {
        List<AbstractPlayer> agents = new LinkedList<AbstractPlayer>();
        int playersPerGame = 2;
        int gamesPerMatchup = 5000;
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
        p_mcts.budget = 20;
        // agents.add(new MCTSPlayer(p_mcts));

        // agents.add(new RandomPlayer());

        for (int n = 1; n <= nAgents; n++) {
            RLPlayer p = new RLPlayer(
                    new RLParams(String.format("%s/%s_Phase%02d_Agent%02d_n=%d.json",
                            type.name(), featureVector.getClass().getSimpleName(), phase, n, nGames)));
            p.setName("RL" + n);
            agents.add(p);
        }

        RoundRobinTournament rrt = new RoundRobinTournament(agents, GameType.valueOf(gameName), playersPerGame,
                gamesPerMatchup, mode, gameParams, finalDir, destDir);
        // rrt.verbose = false;
        rrt.run();
    }

    public static void custom_tournament() {
        List<AbstractPlayer> agents = new LinkedList<AbstractPlayer>();
        int playersPerGame = 2;
        int gamesPerMatchup = 500;
        TournamentMode mode = TournamentMode.NO_SELF_PLAY;
        AbstractParameters gameParams = null;
        String finalDir = null;
        String destDir = null;

        MCTSParams p_mcts = new MCTSParams();
        p_mcts.budgetType = PlayerConstants.BUDGET_TIME;
        p_mcts.budget = 20;
        agents.add(new MCTSPlayer(p_mcts));

        // agents.add(new RandomPlayer());
        agents.add(
                new RLPlayer(new RLParams("LinearApprox/SushiGo2PlayerFeatureVector_Phase03_Agent01_n=1638400.json")));

        RoundRobinTournament rrt = new RoundRobinTournament(agents, GameType.valueOf("SushiGo"), playersPerGame,
                gamesPerMatchup, mode, gameParams, finalDir, destDir);
        // rrt.verbose = false;
        rrt.run();
    }

    public static void main(String[] args) {
        int[] nGames = { 409600, 819200, 1638400, 2457600 };
        int[][] filesToDeletePerPhase = {
                { 100, 200, 400, 800, 1600, 3200, 6400, 12800, 25600, 51200, 102400, 204800 },
                { 614400 },
                { 1024000, 1228800, 1433600 }
        };

        RLType type = RLType.LinearApprox;
        boolean runExperiments = false;

        Map<String, Object> gamesToRun = new HashMap<String, Object>() {
            {
                put("TicTacToe", new TicTacToeDim2StateVector());
                put("DotsAndBoxes", new DBStateFeaturesReduced());
                put("SushiGo", new SushiGo2PlayerFeatureVector());
            }
        };

        for (Entry<String, Object> gtr : gamesToRun.entrySet()) {
            String gameName = gtr.getKey();
            Object featureVector = gtr.getValue();
            int[] bestAgents = new int[4];
            if (runExperiments) {
                for (int phase = 1; phase <= 3; phase++) {
                    final int currentPhase = phase;
                    final int currentBestAgentFromPrevPhase = phase == 1 ? 0 : bestAgents[phase - 2];
                    ExecutorService executor = Executors.newFixedThreadPool(nAgents);
                    for (int agent = 1; agent <= nAgents; agent++) {
                        final int currentAgent = agent;
                        executor.execute(() -> {
                            main_train(gameName, featureVector, type, currentPhase, currentBestAgentFromPrevPhase,
                                    currentAgent);
                        });
                        Random rng = new Random();
                        try {
                            Thread.sleep(rng.nextInt(5000) + 1);
                        } catch (InterruptedException e) {
                            System.err.println("Error sleeping");
                        }
                    }
                    executor.shutdown();
                    try {
                        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        System.err.println("Error waiting for threads to finish");
                    }

                    CombinedOutputStream outputStream = new CombinedOutputStream();
                    PrintStream originalPrintStream = System.out;
                    System.setOut(new PrintStream(outputStream));

                    if (nAgents == 1) {
                        bestAgents[phase - 1] = 1;
                        continue;
                    }

                    main_tournament(gameName, featureVector, type, phase, nGames[phase - 1]);

                    String capturedOutput = outputStream.getCapturedOutput();
                    String winner = capturedOutput.split("---- Ranking ----")[1].substring(4, 5);
                    bestAgents[phase - 1] = Integer.parseInt(winner);

                    for (int i = 0; i < filesToDeletePerPhase[phase - 1].length; i++) {
                        for (int agent = 1; agent <= nAgents; agent++) {
                            if (agent == bestAgents[phase - 1])
                                continue;
                            String filePath = String.format(
                                    "src/main/java/players/rl/resources/qWeights/%s/%s/%s_Phase%02d_Agent%02d_n=%d.json",
                                    gameName, type.name(), featureVector.getClass().getSimpleName(), phase, agent,
                                    filesToDeletePerPhase[phase - 1][i]);
                            File file = new File(filePath);
                            file.delete();
                        }
                    }

                    System.setOut(originalPrintStream);
                }
                String bestAgentsString = "| ";
                for (int i = 0; i < bestAgents.length; i++) {
                    bestAgentsString += bestAgents[i] + " | ";
                }
                System.out.println("Best agents were: " + bestAgentsString);
            } else {
                custom_tournament();
            }
        }
    }

}

class CombinedOutputStream extends OutputStream {
    private final OutputStream consoleOutput;
    private final ByteArrayOutputStream byteArrayOutput;

    public CombinedOutputStream() {
        this.consoleOutput = System.out;
        this.byteArrayOutput = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
        consoleOutput.write(b); // Forward to console
        byteArrayOutput.write(b); // Capture into ByteArrayOutputStream
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        consoleOutput.write(b, off, len); // Forward to console
        byteArrayOutput.write(b, off, len); // Capture into ByteArrayOutputStream
    }

    public String getCapturedOutput() {
        return byteArrayOutput.toString();
    }

    public void restoreConsoleOutput() {
        System.setOut((PrintStream) consoleOutput);
    }
}
