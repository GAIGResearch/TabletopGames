package games.battlelore;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.human.ActionController;
import players.human.HumanConsolePlayer;
import players.human.HumanGUIPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;

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
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        ActionController ac = new ActionController();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        //agents.add(new HumanGUIPlayer(ac));
        //
        // TODO_Ertugrul Add param constructor
        // agents.add(new RandomPlayer());
        //BattleloreGame params = new BattleloreParameters(System.currentTimeMillis());
        //Game game = new BattleloreGame(agents /* params*/);
        //game.run(mull);
    }
}
