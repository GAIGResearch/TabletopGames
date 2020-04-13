package core;

import pandemic.PandemicGUI;

import java.util.List;

public class Runner {

    private Game game;

    public void setGame(Game g, GameParameters gp, GameState gs, ForwardModel fm, String dataPath)
    {
        game = g;
        game.gameParameters = gp;
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

    public void play()
    {
        PandemicGUI gui = new PandemicGUI(game);
        game.run(gui);
    }

}
