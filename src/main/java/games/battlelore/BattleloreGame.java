package games.battlelore;

import core.AbstractPlayer;
import core.Game;
import evaluation.RoundRobinTournament;
import games.GameType;
import games.battlelore.player.RuleBasedPlayer;
import players.human.ActionController;
import players.human.HumanConsolePlayer;
import players.human.HumanGUIPlayer;
import players.mcts.MCTSParams;
import players.rmhc.RMHCParams;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static games.GameType.Battlelore;

public class BattleloreGame extends Game
{
    public BattleloreGame(BattleloreGameParameters params)
    {
        super(GameType.Battlelore, new BattleloreForwardModel(), new BattleloreGameState(params, 2));
    }

    public BattleloreGame(List<AbstractPlayer> agents, BattleloreGameParameters params)
    {
        super(GameType.Battlelore, agents, new BattleloreForwardModel(), new BattleloreGameState(params, agents.size()));
    }

    public static void main(String[] args)
    {
        /* 1. Action controller for GUI interactions. If set to null, running without visuals. */
        ActionController ac = new ActionController(); //null;

        /* 2. Game seed */
        long seed = System.currentTimeMillis(); //0;

        /* 3. Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>();

        MCTSParams params1 = new MCTSParams();
        RMHCParams params2 = new RMHCParams();

        //players.add(new RandomPlayer());
        //players.add(new RandomPlayer());
        players.add(new OSLAPlayer());
        //players.add(new RuleBasedPlayer());
        //players.add(new HumanGUIPlayer(ac));
        players.add(new RMHCPlayer(params2, new BattleloreHeuristic()));
        //players.add(new HumanConsolePlayer());

        /* 4. Run! */
        runOne(Battlelore, players, seed, ac, false, null);

        List<GameType> games = new ArrayList<GameType>();

        games.add(Battlelore);
        long[] seeds = new long[100];
        for (int i = 0; i < 100; i++)
        {
            seeds[i] = i;
        }
        //runMany(games, players, 2, seeds, null, false, null);
//        runMany(new ArrayList<GameType>() {{add(Uno);}}, players, null, 1000, null, false, false);

        LinkedList<AbstractPlayer> agents = new LinkedList<AbstractPlayer>();
        for (AbstractPlayer agent : players)
        {
            agents.add(agent);
        }

        RoundRobinTournament tournament = new RoundRobinTournament(agents, Battlelore, 2, 100, true);
        tournament.runTournament();
    }
}
