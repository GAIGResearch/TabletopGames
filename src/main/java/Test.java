import core.AIPlayer;
import core.Game;
import core.Runner;
import pandemic.PandemicForwardModel;
import pandemic.PandemicGame;
import pandemic.PandemicGameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Test {
    public static void main(String[] args) {

        Runner tabletopEngine = new Runner();
        List<AIPlayer> players = new ArrayList<>();

        tabletopEngine.setGame(new PandemicGame(), new PandemicGameState(), new PandemicForwardModel(), "data/");
        tabletopEngine.setPlayers(players);

        tabletopEngine.run();

    }
}
