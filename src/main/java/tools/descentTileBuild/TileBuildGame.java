package tools.descentTileBuild;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import gui.AbstractGUIManager;
import gui.GUI;
import gui.GamePanel;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.util.ArrayList;

public class TileBuildGame {
    public static void main(String[] args) {
        TileBuildParameters tbp = new TileBuildParameters(0);
        ActionController ac = new ActionController();

        Game game = new Game(null, new ArrayList<AbstractPlayer>(){{
            add(new HumanGUIPlayer(ac));
//            add(new RandomPlayer());
        }}, new TileBuildFM(), new TileBuildState(tbp, 1));


        GUI frame = new GUI();
        GamePanel gamePanel = new GamePanel();
        frame.setContentPane(gamePanel);

        TileBuildGUI gui = new TileBuildGUI(gamePanel, game.getGameState(), ac);

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
