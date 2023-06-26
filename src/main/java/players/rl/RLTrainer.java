package players.rl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.AbstractPlayer;
import core.Game;
import evaluation.listeners.IGameListener;
import games.GameType;
import games.tictactoe.TicTacToeStateVector;
import players.human.ActionController;
import players.rl.dataStructures.QWeightsDataStructure;
import players.rl.dataStructures.TabularQWDS;
import players.rl.dataStructures.TurnSAR;

public class RLTrainer {

    Map<Integer, List<TurnSAR>> playerTurns;

    private String gameName;
    public final RLTrainingParams params;
    QWeightsDataStructure qwds;
    DataProcessor dp;

    RLTrainer(RLTrainingParams params) {
        // TODO set game name and more through RLTrainingParams
        this.gameName = "TicTacToe";
        this.params = params;
        this.qwds = new TabularQWDS();
        this.dp = new DataProcessor(qwds, gameName);
        resetTrainer();
    }

    public void addTurn(int playerId, TurnSAR turn) {
        if (!playerTurns.containsKey(playerId))
            playerTurns.put(playerId, new ArrayList<TurnSAR>());
        playerTurns.get(playerId).add(turn);
    }

    public void train(RLPlayer player) {
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

        players.add(new RLPlayer(qwds, playerParams, this));
        players.add(new RLPlayer(qwds, playerParams, this));

        dp.tryReadQWeightsFromFile();
        int nGames = params.nGames;

        System.out.println("Starting training...");
        for (int i = 1; i <= nGames; i++) {
            int splitSize = nGames / 100;
            if (splitSize != 0 && i % splitSize == 0) {
                System.out.println((i / splitSize) + "%");
                // Every 10%, write progress to file
                if ((i / splitSize) % 10 == 0)
                    writeData();
            }
            runGame(GameType.valueOf(gameName), gameParams, players, System.currentTimeMillis(), false, null,
                    useGUI ? new ActionController() : null, turnPause);
        }
        writeData();
        System.out.print("Training complete!");
    }

    private boolean firstWrite = true;

    private void writeData() {
        if (firstWrite) {
            dp.writeData();
            firstWrite = false;
        } else {
            dp.overwriteData();
        }
    }

    private void runGame(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed,
            boolean randomizeParameters, List<IGameListener> listeners, ActionController ac, int turnPause) {
        Game.runOne(gameToPlay, parameterConfigFile, players, seed, randomizeParameters, listeners, ac, turnPause);
        resetTrainer();
    }

    public static void main(String[] args) {
        RLTrainingParams params = new RLTrainingParams(10000);
        RLTrainer trainer = new RLTrainer(params);
        trainer.runTraining();
    }

}
