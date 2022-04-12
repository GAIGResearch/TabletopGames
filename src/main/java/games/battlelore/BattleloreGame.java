package games.battlelore;

import core.AbstractPlayer;
import core.Game;
import evaluation.RoundRobinTournament;
import games.GameType;
import games.battlelore.player.RuleBasedPlayer;
import players.human.ActionController;
import players.human.HumanConsolePlayer;
import players.human.HumanGUIPlayer;
import players.mcts.BasicMCTSPlayer;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCParams;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static games.GameType.Battlelore;

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
