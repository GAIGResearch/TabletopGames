package games.descent2e.descentTileBuild;

import core.AbstractPlayer;
import core.Game;
import gui.GUI;
import gui.GamePanel;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.Path;
import utilities.Pathfinder;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;

public class TileBuildGame {
    public static void main(String[] args) {
        TileBuildParameters tbp = new TileBuildParameters();
        ActionController ac = new ActionController();

        TileBuildState tbs = new TileBuildState(tbp, 1);
        Game game = new Game(null, new ArrayList<AbstractPlayer>(){{
            add(new HumanGUIPlayer(ac));
//            add(new RandomPlayer());
        }}, new TileBuildFM(), tbs);


        //dgs.addAllComponents();
        Pathfinder pf = new Pathfinder(tbs.tile);
        int orig = tbs.tile.getElement(1,1).getComponentID();
        int dest = tbs.tile.getElement(3,3).getComponentID();
        Path p = pf.getPath(tbs, orig, dest);

        //System.out.println(p.toString());
        int a = 0;


        GUI frame = new GUI();
        GamePanel gamePanel = new GamePanel();
        frame.setContentPane(gamePanel);

        TileBuildGUI gui = new TileBuildGUI(gamePanel, game, ac, new HashSet<Integer>() {{add(0);}});

        frame.setFrameProperties();
        frame.validate();
        frame.pack();

        Timer guiUpdater = new Timer((int) game.getCoreParameters().frameSleepMS, event -> game.updateGUI(gui, frame));
        guiUpdater.start();

        game.run();
        guiUpdater.stop();
        // and update GUI to final game state
        game.updateGUI(gui, frame);
    }
}
