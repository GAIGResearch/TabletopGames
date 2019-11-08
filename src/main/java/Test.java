import core.AIPlayer;
import core.Game;
import pandemic.PandemicGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Test {
    public static void main(String[] args) {
        Game game = new PandemicGame(4);
        List<AIPlayer> players = new ArrayList<>();
        // TODO: make players
        game.setPlayers(players);

        //game.run(); //At the moment this crashes.
    }
}
