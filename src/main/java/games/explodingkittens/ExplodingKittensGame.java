package games.explodingkittens;

import core.*;
import games.GameType;
import players.simple.RandomPlayer;

import java.util.*;

public class ExplodingKittensGame extends Game {

    public ExplodingKittensGame(List<AbstractPlayer> agents, AbstractForwardModel model, AbstractGameState gameState) {
        super(GameType.ExplodingKittens, agents, model, gameState);
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());

        ExplodingKittensParameters params = new ExplodingKittensParameters(System.currentTimeMillis());
        AbstractForwardModel forwardModel = new ExplodingKittensForwardModel();

        for (int i=0; i<1000; i++) {
            Game game = new ExplodingKittensGame(agents, forwardModel, new ExplodingKittensGameState(params, agents.size()));
            game.run();
        }
    }
}
