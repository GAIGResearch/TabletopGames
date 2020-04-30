//import carcassonne.CarcassonneForwardModel;
//import carcassonne.CarcassonneGameState;
//import carcassonne.CarcassonneGame;
//import carcassonne.CarcassonneParameters;
import core.AIPlayer;
import core.Runner;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestCarcassonne {
    public static void main(String[] args) {

        Runner tabletopEngine = new Runner();
        List<AIPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));

        //tabletopEngine.setGame(new CarcassonneGame(), new CarcassonneParameters(), new CarcassonneGameState(), new CarcassonneForwardModel(), "data/", players);

        tabletopEngine.run();
//        tabletopEngine.play();
    }
}
