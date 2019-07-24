import core.AIPlayer;
import core.Game;
import pandemic.PandemicGame;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        Game game = new PandemicGame();
        List<AIPlayer> players = new ArrayList<>();
        // TODO: make players
        game.setPlayers(players);
        game.run();
    }
}
