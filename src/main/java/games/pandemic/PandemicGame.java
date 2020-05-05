package games.pandemic;

import core.Game;
import core.GUI;

import java.util.List;
import games.pandemic.gui.PandemicGUI;
import players.AbstractPlayer;
import players.ActionController;
import players.HumanGUIPlayer;
import players.RandomPlayer;
import utilities.Utils;

import java.util.*;

@SuppressWarnings("UnusedAssignment")
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
        ActionController ac = new ActionController();

        List<AbstractPlayer> players = new ArrayList<>();
        int pid = 0;
        players.add(new HumanGUIPlayer(pid++, ac));
        players.add(new RandomPlayer(pid++, new Random()));
        players.add(new RandomPlayer(pid++, new Random()));
        players.add(new RandomPlayer(pid++, new Random()));

        PandemicGame game = new PandemicGame(players);
        GUI gui = new PandemicGUI((PandemicGameState) game.getGameState(), ac);
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
