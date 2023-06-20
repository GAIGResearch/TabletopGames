package players.rl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.AbstractPlayer;
import core.Game;
import core.interfaces.IStateFeatureVector;
import evaluation.listeners.IGameListener;
import games.GameType;
import games.tictactoe.TicTacToeStateVector;
import players.human.ActionController;
import players.rl.dataStructures.QWeightsDataStructure;
import players.rl.dataStructures.TabularQWDS;
import players.rl.dataStructures.TurnSAR;

public class RLTrainer {

    Map<Integer, List<TurnSAR>> playerTurns;

    public final RLTrainerParams params;

    IStateFeatureVector features;

    QWeightsDataStructure qwds;

    // FIXME these are temp variables
    private final String resourcesPath = "src/main/java/players/rl/resources/";
    private String gameName;

    RLTrainer(RLTrainerParams params) {
        // TODO set game name and more through RLTrainerParams
        this.gameName = "TicTacToe";
        this.params = params;
        this.features = new TicTacToeStateVector();
        qwds = new TabularQWDS(features, this);
        qwds.tryReadBetaFromFile(resourcesPath + gameName + "/beta.txt");
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
        int nIterations = 100000;
        for (int i = 1; i <= 100000; i++) {
            if (i % (nIterations / 100) == 0) {
                System.out.println((i / (nIterations / 100)) + "%");
                qwds.writeBetaToFile(resourcesPath, gameName);
            }
            runGame(GameType.valueOf(gameName), gameParams, players, System.currentTimeMillis(), false, null,
                    useGUI ? new ActionController() : null, turnPause);
        }
        qwds.writeBetaToFile(resourcesPath, gameName);

    }

    private void runGame(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed,
            boolean randomizeParameters, List<IGameListener> listeners, ActionController ac, int turnPause) {
        Game.runOne(gameToPlay, parameterConfigFile, players, seed, randomizeParameters, listeners, ac, turnPause);
        resetTrainer();
    }

    public static void main(String[] args) {
        RLTrainerParams params = new RLTrainerParams();
        RLTrainer trainer = new RLTrainer(params);
        trainer.runTraining();
    }

}
