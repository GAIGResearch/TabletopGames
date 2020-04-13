import core.AIPlayer;
import core.Runner;
import explodingkittens.ExplodingKittensForwardModel;
import explodingkittens.ExplodingKittensGame;
import explodingkittens.ExplodingKittensGameState;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;

public class TestExplodingKittens {
    public static void main(String[] args) {

        Runner tabletopEngine = new Runner();
        List<AIPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());

        tabletopEngine.setGame(new ExplodingKittensGame(), new ExplodingKittensGameState(), new ExplodingKittensForwardModel(), "data/");
        tabletopEngine.setPlayers(players);

        tabletopEngine.run();
//        tabletopEngine.play();
    }
}
