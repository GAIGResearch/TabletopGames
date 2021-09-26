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

    public static void main(String[] args) {
        /* 1. Action controller for GUI interactions. If set to null, running without visuals. */
        ActionController ac = new ActionController(); //null;

        /* 2. Game seed */
        long seed = System.currentTimeMillis(); //0;

        /* 3. Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>();

        MCTSParams params1 = new MCTSParams();
        RMHCParams params2 = new RMHCParams();

        players.add(new RandomPlayer());
        players.add(new OSLAPlayer());
        players.add(new RuleBasedPlayer());
        players.add(new BasicMCTSPlayer(new Random().nextLong()));
        players.add(new RMHCPlayer(params2, new BattleloreHeuristic()));
        //players.add(new MCTSPlayer(new Random().nextLong()));
        //players.add(new HumanConsolePlayer());
        //players.add(new HumanGUIPlayer(ac));

        /* 4. Run! */
        //------------------------DEBUG----------------------------------------//
        players.clear();
        //players.add(new BasicMCTSPlayer(new Random().nextLong()));
        //players.add(new RMHCPlayer(params2, new BattleloreHeuristic()));
        //players.add(new RMHCPlayer(params2, new BattleloreHeuristic()));
        players.add(new OSLAPlayer());
        players.add(new RuleBasedPlayer());
        runOne(Battlelore, players, seed, ac, false, null);

        //------------------------DEBUG----------------------------------------//
        List<GameType> games = new ArrayList<GameType>();

        games.add(Battlelore);

        final int repetitions = 500;
        long[] seeds = new long[repetitions];

        for (int i = 0; i < players.size(); i++) {
            for (int k = 0; k < players.size(); k++) {
                if (i != k) {
                    generateNewSeeds(seeds, repetitions);
                    System.out.println(players.get(i).toString() + " vs " + players.get(k).toString() + ":");
                    ArrayList<AbstractPlayer> currentPlayers = new ArrayList<AbstractPlayer>();
                    currentPlayers.add(players.get(i));
                    currentPlayers.add(players.get(k));
                    runMany(games, currentPlayers, repetitions, seeds, null, false, null);
                }
            }
        }
    }
}
