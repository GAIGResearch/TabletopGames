package minigames.descentTileBuild;

import core.AbstractPlayer;
import core.Game;
import players.ActionController;
import players.HumanGUIPlayer;
import players.RandomPlayer;

import java.util.ArrayList;

public class TileBuildGame {
    public static void main(String[] args) {
        TileBuildParameters tbp = new TileBuildParameters(0);
        ActionController ac = new ActionController();

        Game game = new Game(null, new ArrayList<AbstractPlayer>(){{
            add(new HumanGUIPlayer(ac));
//            add(new RandomPlayer());
        }}, new TileBuildFM(), new TileBuildState(tbp, 1));

        TileBuildGUI gui = new TileBuildGUI(game.getGameState(), ac);
        game.run(gui);
    }
}
