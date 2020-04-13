import core.AIPlayer;
import core.Runner;
import pandemic.PandemicForwardModel;
import pandemic.PandemicGame;
import pandemic.PandemicGameState;
import pandemic.PandemicParameters;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {

        Runner tabletopEngine = new Runner();
        List<AIPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());

        tabletopEngine.setGame(new PandemicGame(), new PandemicParameters(), new PandemicGameState(),
                new PandemicForwardModel(), "data/");
        tabletopEngine.setPlayers(players);

//        tabletopEngine.run();
        tabletopEngine.play();

    }
}
