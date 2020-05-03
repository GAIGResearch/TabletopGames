import core.AIPlayer;
import core.Runner;
import uno.*;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUno {
    public static void main(String[] args) {
        Runner tabletopEngine = new Runner();

        List<AIPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));
        players.add(new RandomPlayer(new Random()));

        tabletopEngine.setGame(new UnoGame(), new UnoParameters(), new UnoGameState(), new UnoForwardModel(),
                "data/uno/", players);

        tabletopEngine.play();
    }
}

