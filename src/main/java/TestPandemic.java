import core.AIPlayer;
import core.Runner;
import pandemic.*;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestPandemic {
    public static void main(String[] args) {

        Runner tabletopEngine = new Runner();
        List<AIPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());

        PandemicGame game = new PandemicGame();
        tabletopEngine.setGame(game, new PandemicParameters(), new PandemicGameState(), new PandemicForwardModel(), "data/");
        tabletopEngine.setPlayers(players);

//        tabletopEngine.run();
        tabletopEngine.play(new PandemicGUI(game));
    }
}
