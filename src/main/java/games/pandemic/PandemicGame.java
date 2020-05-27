package games.pandemic;

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

    public PandemicGame(List<AbstractPlayer> agents, PandemicParameters params) {
        super(agents,
                new ArrayList<ForwardModel>() {{
                    for (int i = 0; i < agents.size(); i++) {
                        add(new PandemicForwardModel(params, agents.size(), System.currentTimeMillis()));
                    }
                }},
                new PandemicForwardModel(params, agents.size(), params.getGameSeed()),
                new PandemicGameState(params, agents.size()));
    }

    public static void main(String[] args){

        ActionController ac = new ActionController();

        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new HumanGUIPlayer(ac));

        PandemicParameters params = new PandemicParameters("data/pandemic/");
        PandemicGame game = new PandemicGame(players, params);
        GUI gui = new PandemicGUI((PandemicGameState)game.getGameState(), ac);
        game.run(gui);
        System.out.println(game.gameState.getGameStatus());

//        runMany(players, forwardModel);
    }

    public static void runMany(List<AbstractPlayer> players, ForwardModel model) {
        HashMap<Utils.GameResult, Integer> results = new HashMap<>();
        for (Utils.GameResult r: Utils.GameResult.values()) {
            results.put(r, 0);
        }

        PandemicParameters params = new PandemicParameters("data/pandemic/");
        for (int i = 0; i < 10000; i++) {
            PandemicGame game = new PandemicGame(players, params);
            game.run(null);
            Utils.GameResult result = game.gameState.getGameStatus();
            int prevCount = results.get(result);
            results.put(result, prevCount + 1);
        }

        System.out.println(results.toString());
    }
}
