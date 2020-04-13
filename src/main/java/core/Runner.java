package core;

import pandemic.PandemicGUI;

import java.util.List;
import java.util.Random;

public class Runner {

    private Game game;

    public void setGame(Game g, GameParameters gp, GameState gs, ForwardModel fm, String dataPath, List<AIPlayer> players)
    {
        game = g;
        game.init(gp, gs, fm);
        game.setup(dataPath, players);
    }

    public void run()
    {
        game.run(null);
    }

    public void play()
    {
        PandemicGUI gui = new PandemicGUI(game);
        game.run(gui);
    }

}
