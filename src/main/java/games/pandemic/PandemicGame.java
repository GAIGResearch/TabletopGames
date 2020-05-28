package games.pandemic;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.AbstractGame;
import core.AbstractGUI;

import java.util.List;
import games.pandemic.gui.PandemicGUI;
import players.ActionController;
import players.HumanGUIPlayer;
import core.AbstractPlayer;
import players.OSLA;
import players.RandomPlayer;
import utilities.Utils;

import java.util.*;

public class PandemicGame extends AbstractGame {

    public PandemicGame(List<AbstractPlayer> agents, AbstractForwardModel model, AbstractGameState gameState) {
        super(agents, model, gameState);
    }
    public PandemicGame(AbstractForwardModel model, AbstractGameState gameState) {
        super(model, gameState);
    }

    public static void main(String[] args){

        ActionController ac = new ActionController();

        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
//        players.add(new HumanGUIPlayer(ac));
        players.add(new OSLA());

        PandemicParameters params = new PandemicParameters("data/pandemic/");
        AbstractForwardModel forwardModel = new PandemicForwardModel(params, players.size());
        PandemicGameState gameState = new PandemicGameState(params, forwardModel, players.size());

        PandemicGame game = new PandemicGame(players, forwardModel, gameState);
        AbstractGUI gui = new PandemicGUI(gameState, ac);
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
            PandemicGame game = new PandemicGame(players, model, new PandemicGameState(params, model, players.size()));
            game.run(null);
            Utils.GameResult result = game.gameState.getGameStatus();
            int prevCount = results.get(result);
            results.put(result, prevCount + 1);
        }

        System.out.println(results.toString());
    }
}
