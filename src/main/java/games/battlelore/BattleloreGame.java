package games.battlelore;

import core.AbstractPlayer;
import core.Game;
import games.GameType;

import java.util.List;
import java.util.Random;

public class BattleloreGame extends Game {
    public BattleloreGame(BattleloreGameParameters params) {
        super(GameType.Battlelore, new BattleloreForwardModel(), new BattleloreGameState(params, 2));
    }

    public BattleloreGame(List<AbstractPlayer> agents, BattleloreGameParameters params) {
        super(GameType.Battlelore, agents, new BattleloreForwardModel(), new BattleloreGameState(params, agents.size()));
    }

    public static void generateNewSeeds(long[] arr, int repetitions) {
        for (int i = 0; i < repetitions; i++) {
            arr[i] = new Random().nextInt();
        }
    }

}
