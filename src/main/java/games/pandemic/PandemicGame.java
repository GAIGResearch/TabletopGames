package games.pandemic;

import core.AbstractForwardModel;
import core.AbstractGame;
import core.AbstractGUI;

import java.util.List;
import games.pandemic.gui.PandemicGUI;
import players.ActionController;
import players.HumanGUIPlayer;
import core.AbstractPlayer;
import players.RandomPlayer;
import utilities.Utils;

import java.util.*;

public class PandemicGame extends AbstractGame {

    public PandemicGame(List<AbstractPlayer> agents, PandemicParameters params) {
        super(agents,
                new ArrayList<AbstractForwardModel>() {{
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
        AbstractGUI gui = new PandemicGUI((PandemicGameState)game.getGameState(), ac);
        game.run(gui);
        System.out.println(game.gameState.getGameStatus());

//        runMany(players, forwardModel);
    }

    public static void runMany(List<AbstractPlayer> players, AbstractForwardModel model) {
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
