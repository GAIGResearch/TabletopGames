package games.pandemic;

import core.Game;
import core.GUI;

import java.util.List;
import games.pandemic.gui.PandemicGUI;
import players.AbstractPlayer;
import players.RandomPlayer;
import utilities.Utils;

import java.util.*;

public class PandemicGame extends Game {

    public PandemicGame(List<AbstractPlayer> agents) {
        super(agents);

        PandemicParameters params = new PandemicParameters("data/");
        forwardModel = new PandemicForwardModel(params, agents.size());
        gameState = new PandemicGameState(params, forwardModel, agents.size());
        ((PandemicGameState)gameState).setComponents(params.getDataPath());
        ((PandemicForwardModel) forwardModel).setup(gameState);
    }

    public static void main(String[] args){

        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(0, new Random()));
        players.add(new RandomPlayer(1, new Random()));
        players.add(new RandomPlayer(2, new Random()));
        players.add(new RandomPlayer(3, new Random()));

//        PandemicGame game = new PandemicGame(players);
//        GUI gui = new PandemicGUI((PandemicGameState) game.getGameState());
//        game.run(gui);
//        System.out.println(game.gameState.getGameStatus());

        runMany(players);
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
