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

class RLTrainer {

    public final RLTrainingParams params;
    private Map<Integer, List<TurnSAR>> playerTurns;

    private String gameName;
    private QWeightsDataStructure qwds;
    private DataProcessor dp;

    private RLTrainer(RLTrainingParams params) {
        // TODO set game name and more through RLTrainingParams
        this.gameName = "TicTacToe";
        this.params = params;
        this.qwds = new QWDSTabular();
        resetTrainer();
    }

    void initializeTrainer(AbstractGameState state) {
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

        RLParams playerParams = new RLParams(new TicTacToeStateVector());
        playerParams.epsilon = 0.35f;
        playerParams.qWeightsFileId = 0;

        players.add(new RLPlayer(qwds, playerParams, this));
        players.add(new RLPlayer(qwds, playerParams, this));

        int nGames = params.nGames;

        int fileId = -1;
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
                    fileId = writeData(fileId, nGamesSinceLastWrite);
                    nGamesSinceLastWrite = 0;
                }
            }
        }
        writeData(fileId, nGamesSinceLastWrite);
        System.out.print("Training complete!");
    }

    int writeData(int fileId, int nGames) {
        if (fileId == -1)
            fileId = dp.writeData(nGames);
        else
            dp.writeData(fileId, nGames);
        return fileId;
    }

    private void runGame(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed,
            boolean randomizeParameters, List<IGameListener> listeners, ActionController ac, int turnPause) {
        Game.runOne(gameToPlay, parameterConfigFile, players, seed, randomizeParameters, listeners, ac, turnPause);
        resetTrainer();
    }

    public static void main(String[] args) {
        RLTrainingParams params = new RLTrainingParams(1);
        params.alpha = 0.25f;
        params.gamma = 0.25f;
        RLTrainer trainer = new RLTrainer(params);
        trainer.runTraining();
    }

}
