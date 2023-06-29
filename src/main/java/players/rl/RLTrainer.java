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
import games.tictactoe.TicTacToeStateVector;
import players.human.ActionController;
import players.rl.RLPlayer.RLType;

class RLTrainer {

    public final RLTrainingParams params;
    final RLParams playerParams;
    private Map<Integer, List<TurnSAR>> playerTurns;

    private String gameName;
    private QWeightsDataStructure qwds;
    private DataProcessor dp = null;

    private RLTrainer(RLTrainingParams params, RLParams playerParams) {
        // TODO set game name and more through RLTrainingParams
        this.gameName = "TicTacToe";
        this.params = params;
        this.playerParams = playerParams;
        this.qwds = new QWDSTabular();
        resetTrainer();
    }

    void initializeTrainer(AbstractGameState state) {
        if (this.dp == null)
            this.dp = new DataProcessor(qwds, gameName);
    }

    void addTurn(RLPlayer player, AbstractGameState state, AbstractAction action,
            List<AbstractAction> possibleActions) {
        int playerId = player.getPlayerID();

        // Calculate reward
        AbstractGameState evalState = state.copy(playerId);
        if (action != null) // For the final game state
            player.getForwardModel().next(evalState, action);
        double reward = params.heuristic.evaluateState(evalState, playerId);

        // Add the turn to playerTurns
        TurnSAR turn = new TurnSAR(state, action, possibleActions, reward);
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
        // TODO add params to method (nIterations, nPlayers, game, etc.)

        boolean useGUI = false;
        int turnPause = 0;
        String gameParams = null;

        ArrayList<AbstractPlayer> players = new ArrayList<>();

        players.add(new RLPlayer(playerParams, qwds, this));
        players.add(new RLPlayer(playerParams, qwds, this));

        int nGames = params.nGames;

        int nGamesSinceLastWrite = 0;

        System.out.println("Starting training...");
        for (int i = 1; i <= nGames; i++) {
            runGame(GameType.valueOf(gameName), gameParams, players, System.currentTimeMillis(), false, null,
                    useGUI ? new ActionController() : null, turnPause);
            nGamesSinceLastWrite++;
            int splitSize = nGames / 100;
            if (splitSize != 0 && i % splitSize == 0) {
                System.out.println((i / splitSize) + "%");
                // Every 10%, write progress to file
                if ((i / splitSize) % 10 == 0) {
                    dp.writeData(nGamesSinceLastWrite);
                    nGamesSinceLastWrite = 0;
                }
            }
        }
        dp.writeData(nGamesSinceLastWrite);
        System.out.print("Training complete!");
    }

    int writeData(int nGames) {
        dp.writeData(nGames);
        return 0;
    }

    private void runGame(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed,
            boolean randomizeParameters, List<IGameListener> listeners, ActionController ac, int turnPause) {
        Game.runOne(gameToPlay, parameterConfigFile, players, seed, randomizeParameters, listeners, ac, turnPause);
        resetTrainer();
    }

    public static void main(String[] args) {
        String inFile = "src/main/java/players/rl/resources/qWeights/TicTacToe/2023-06-29_00-56-09.json";
        RLTrainingParams params = new RLTrainingParams(151340);
        params.alpha = 0.253f;
        params.gamma = 0.253f;
        RLParams playerParams = new RLParams(inFile, new TicTacToeStateVector(), RLType.TABULAR);
        playerParams.epsilon = 0.353f;
        RLTrainer trainer = new RLTrainer(params, playerParams);
        trainer.runTraining();
    }

}
