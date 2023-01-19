package evaluation;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.simple.RandomPlayer;
import players.PlayerType;

import java.util.ArrayList;
import java.util.List;

import static games.GameType.*;
import static players.PlayerType.*;

public class AIReport {

    static int nRep = 50;

    /**
     * How fast is the player at making decisions.
     * @param player - player to test.
     * @param game - game to test.
     */
    public static void actSpeed(PlayerType player, GameType game) {
        System.out.println("--------------------\nAgent Speed Test: " + game.name() + " + " + player.name() + "\n--------------------");

        int nPlayers = game.getMaxPlayers();
        double nSpeed = 0;
        for (int i = 0; i < nRep; i++) {
            Game g = game.createGameInstance(nPlayers);
            List<AbstractPlayer> players = new ArrayList<>();
            for (int j = 0; j < nPlayers-1; j++) {
                players.add(new RandomPlayer());
            }
            players.add(player.createPlayerInstance());

            if (g != null) {
                g.reset(players);
                g.run();
                nSpeed += g.getAgentTime();
            }
        }

        if (nSpeed != 0) {
            System.out.println("Player.getAction(): " + String.format("%6.3e", 1e+9 / (nSpeed / nRep)) + " executions/second");
        }

        System.out.println();
    }

    /**
     * Runs the test with given player
     * @param player - player to test.
     * @param game - game to test.
     */
    public static void run(PlayerType player, GameType game) {
        actSpeed(player, game);
//        stochasticity(player, game);
//        searchTreeSize(player, game);
    }

    public static void main(String[] args) {
        for (PlayerType pt: Property.Human.getAllPlayersExcluding()) {
            run(pt, LoveLetter);
        }
//        run(Random, Pandemic);
    }
}
