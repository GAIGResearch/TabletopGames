package games.catan;

import core.AbstractGameState;
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
        super(GameType.Catan, agents, new CatanForwardModel(), new CatanGameState(params, agents.size()));
    }

    public static void main(String[] args){


        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));

        CatanParameters params = new CatanParameters("data/", System.currentTimeMillis());
        AbstractForwardModel forwardModel = new CatanForwardModel(params, players.size());

        CatanGame game = new CatanGame(players, params);
        // todo add GUI?
        game.run(null);
        System.out.println(game.gameState.getGameStatus());

//        runMany(players, forwardModel);
    }
}
