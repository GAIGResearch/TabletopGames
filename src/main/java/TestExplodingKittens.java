import core.AIPlayer;
import core.Runner;
//import explodingkittens.ExplodingKittensForwardModel;
//import explodingkittens.ExplodingKittensGame;
//import explodingkittens.ExplodingKittensGameState;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestExplodingKittens {
    public static void main(String[] args) {

        Runner tabletopEngine = new Runner();
        List<AIPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));

//        tabletopEngine.setGame(new ExplodingKittensGame(), null, new ExplodingKittensGameState(), new ExplodingKittensForwardModel(), "data/", players);

        tabletopEngine.run();
//        tabletopEngine.play();
    }
}
