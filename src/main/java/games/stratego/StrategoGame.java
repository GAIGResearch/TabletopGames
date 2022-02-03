package games.stratego;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import gui.AbstractGUIManager;
import gui.GUI;
import gui.GamePanel;
import players.human.ActionController;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;

import java.util.ArrayList;
import java.util.List;

public class StrategoGame extends Game {

    public StrategoGame(List<AbstractPlayer> players, StrategoParams params) {
        super(GameType.Stratego, players, new StrategoForwardModel(), new StrategoGameState(params, players.size()));
    }

    public StrategoGame(StrategoParams params) {
        super(GameType.Stratego, new StrategoForwardModel(), new StrategoGameState(params, 2));
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RMHCPlayer());
        agents.add(new OSLAPlayer());


        StrategoParams params = new StrategoParams(System.currentTimeMillis());
        Game game = new StrategoGame(agents, params);

        GUI frame = new GUI();
        AbstractGUIManager gui;
        GamePanel gamePanel = new GamePanel();
        ActionController ac = new ActionController();
        frame.setContentPane(gamePanel);
        gui = GameType.Stratego.createGUIManager(gamePanel, game, ac);
        frame.setFrameProperties();
        game.run(gui, frame); //Run with this line for gui, otherwise run with the line below
        //game.run(null);
    }
}
