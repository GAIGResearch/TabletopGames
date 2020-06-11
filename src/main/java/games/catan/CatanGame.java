package games.catan;

import core.AbstractPlayer;
import core.AbstractForwardModel;
import core.Game;
import games.GameType;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CatanGame extends Game {
    public CatanGame(List<AbstractPlayer> agents, CatanParameters params) {
        super(GameType.Catan, agents, new CatanForwardModel(params, agents.size()), new CatanGameState(params, agents.size()));
    }

    public CatanGame(List<AbstractPlayer> agents, CatanParameters params, CatanForwardModel model, CatanGameState gameState) {
        super(GameType.Catan, agents, model, gameState);
    }

    public static void main(String[] args){

        List<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer(new Random()));
        agents.add(new RandomPlayer(new Random()));
        agents.add(new RandomPlayer(new Random()));
        agents.add(new RandomPlayer(new Random()));

        CatanParameters params = new CatanParameters("data/", System.currentTimeMillis());
        CatanForwardModel forwardModel = new CatanForwardModel(params, agents.size());
        CatanGameState gs = new CatanGameState(params, agents.size());

        CatanGame game = new CatanGame(agents, params, forwardModel, gs);

        // todo add GUI
        game.run(null);
        System.out.println(game.gameState.getGameStatus());

//        runMany(players, forwardModel);
    }
}
