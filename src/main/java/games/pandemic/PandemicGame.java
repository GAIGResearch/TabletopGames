package games.pandemic;

import core.Game;
import core.actions.IAction;
import core.GUI;

import java.util.List;
import core.observations.IObservation;
import games.pandemic.gui.PandemicGUI;
import players.AbstractPlayer;
import players.RandomPlayer;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

import static games.pandemic.PandemicConstants.VERBOSE;


public class PandemicGame extends Game {

    public PandemicGame(List<AbstractPlayer> agents)
    {
        PandemicParameters params = new PandemicParameters("data/");

        players = agents;
        forwardModel = new PandemicForwardModel(params, agents.size());

        gameState = new PandemicGameState(params, agents.size());
        gameState.setForwardModel(forwardModel);
        ((PandemicGameState)gameState).setComponents(params.getDataPath());
        ((PandemicForwardModel) forwardModel).setup(gameState);
    }

    @Override
    public void run(GUI gui) {
        int turn = 0;

        while (!isEnded()){

            if (VERBOSE) System.out.println(turn++);

            // Get core.actions of current active player for their turn
            Pair<Integer, List<IAction>> activePlayer = ((PandemicGameState)gameState).getActingPlayer();
            List<IAction> actions = Collections.unmodifiableList(gameState.setAvailableActions(activePlayer.b, activePlayer.a));
            int action = players.get(activePlayer.a).getAction((IObservation) gameState, actions);
            gameState.setAvailableActions(null, activePlayer.a);  // Reset core.actions for next player

            // Resolve core.actions and game rules for the turn
            forwardModel.next(gameState, actions.get(action));

            if (gui != null) {
                gui.update(gameState);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("EXCEPTION " + e);
                }
            }
        }

//        System.out.println("Game Over");
        if (VERBOSE) {
            if (gameState.getGameStatus() == Utils.GameResult.GAME_WIN) {
                System.out.println("Win");
            } else {
                System.out.println("Lose");
            }
        }
    }

    @Override
    public boolean isEnded() {
        return gameState.getGameStatus() != Utils.GameResult.GAME_ONGOING;
    }

    public static void main(String[] args){

        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(0, new Random()));
        players.add(new RandomPlayer(1, new Random()));
        players.add(new RandomPlayer(2, new Random()));
        players.add(new RandomPlayer(3, new Random()));

        PandemicGame game = new PandemicGame(players);
        GUI gui = new PandemicGUI((PandemicGameState) game.getGameState());
        game.run(gui);
        System.out.println(game.gameState.getGameStatus());

//        runMany(players);
    }

    public static void runMany(List<AbstractPlayer> players) {
        HashMap<Utils.GameResult, Integer> results = new HashMap<>();
        for (Utils.GameResult r: Utils.GameResult.values()) {
            results.put(r, 0);
        }

        for (int i = 0; i < 10000; i++) {
            PandemicGame game = new PandemicGame(players);
            game.run(null);
            Utils.GameResult result = game.gameState.getGameStatus();
            int prevCount = results.get(result);
            results.put(result, prevCount + 1);
        }

        System.out.println(results.toString());
    }
}
