package games.catan;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.AbstractForwardModel;
import core.Game;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CatanGame extends Game {
    public CatanGame(List<AbstractPlayer> players, AbstractForwardModel model, AbstractGameState gameState) {
        super(players, model, gameState);
    }

    public static void main(String[] args){


        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));

        CatanParameters params = new CatanParameters("data/");
        AbstractForwardModel forwardModel = new CatanForwardModel(params, players.size());
        CatanGameState gameState = new CatanGameState(params, forwardModel, players.size());

        CatanGame game = new CatanGame(players, forwardModel, gameState);
        // todo add GUI?
        game.run(null);
        System.out.println(game.gameState.getGameStatus());

//        runMany(players, forwardModel);
    }
}
