package games.pandemic;

import core.AbstractGameState;
import core.ForwardModel;
import core.Game;
import core.GUI;

import java.util.List;
import games.pandemic.gui.PandemicGUI;
import players.ActionController;
import players.HumanGUIPlayer;
import core.AbstractPlayer;
import players.RandomPlayer;
import utilities.Utils;

import java.util.*;

public class PandemicGame extends Game {

    public PandemicGame(List<AbstractPlayer> agents, ForwardModel model, AbstractGameState gameState) {
        super(agents, model, gameState);
    }

    public static void main(String[] args){

        ActionController ac = new ActionController();

        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new HumanGUIPlayer(ac));

        PandemicParameters params = new PandemicParameters("data/");
        ForwardModel forwardModel = new PandemicForwardModel(params, players.size());
        PandemicGameState gameState = new PandemicGameState(params, forwardModel, players.size());

        PandemicGame game = new PandemicGame(players, forwardModel, gameState);
        GUI gui = new PandemicGUI(gameState, ac);
        game.run(gui);
        System.out.println(game.gameState.getGameStatus());

//        runMany(players, forwardModel);
    }

    public static void runMany(List<AbstractPlayer> players, ForwardModel model) {
        HashMap<Utils.GameResult, Integer> results = new HashMap<>();
        for (Utils.GameResult r: Utils.GameResult.values()) {
            results.put(r, 0);
        }

        PandemicParameters params = new PandemicParameters("data/");
        for (int i = 0; i < 10000; i++) {
            PandemicGame game = new PandemicGame(players, model, new PandemicGameState(params, model, players.size()));
            game.run(null);
            Utils.GameResult result = game.gameState.getGameStatus();
            int prevCount = results.get(result);
            results.put(result, prevCount + 1);
        }

        System.out.println(results.toString());
    }
}
