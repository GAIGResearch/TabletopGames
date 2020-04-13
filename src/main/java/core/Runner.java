package core;

import pandemic.PandemicGUI;

import java.util.List;

public class Runner {

    private Game game;

    public void setGame(Game g, GameState gs, ForwardModel fm, String dataPath)
    {
        game = g;
        game.gameState = gs;
        game.forwardModel = fm;

        game.gameState.init(g);
        game.setup(dataPath);
    }

    public void setPlayers(List<AIPlayer> players)
    {
        game.setPlayers(players);
    }

    public void run()
    {
        game.run(null);
    }

    public void play(GUI gui)
    {
        game.run(gui);
    }

}
