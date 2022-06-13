package games.catan;

import core.AbstractPlayer;
import core.AbstractForwardModel;
import core.Game;
import games.GameType;
import games.catan.gui.CatanGUI;
import gui.AbstractGUIManager;
import gui.GUI;
import gui.GamePanel;
import players.PlayerConstants;
import players.human.ActionController;
import players.human.HumanConsolePlayer;
import players.human.HumanGUIPlayer;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCParams;
import players.rmhc.RMHCPlayer;
import players.simple.CatanRuleBasedPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CatanGame extends Game {
    public CatanGame(List<AbstractPlayer> agents, CatanParameters params) {
        super(GameType.Catan, agents, new CatanForwardModel(params, agents.size()), new CatanGameState(params, agents.size()));
    }

    public CatanGame(List<AbstractPlayer> agents, CatanParameters params, CatanForwardModel model, CatanGameState gameState) {
        super(GameType.Catan, agents, model, gameState);
    }

    public static void main(String[] args){

        List<AbstractPlayer> agents = new ArrayList<>();
//        ActionController ac = new ActionController();
//        agents.add(new HumanGUIPlayer(ac));
        agents.add(new OSLAPlayer());

        // RHEA
        RMHCParams rmhcParams = new RMHCParams();
////        rmhcParams.budgetType = PlayerConstants.BUDGET_TIME;
//        rmhcParams.fmCallsBudget = 10;
////        rmhcParams.iterationsBudget = 1;
        agents.add(new RMHCPlayer(rmhcParams));


        // MCTS
        MCTSParams mctsParams = new MCTSParams();
        mctsParams.rolloutType = MCTSEnums.Strategies.RANDOM;
        agents.add(new MCTSPlayer(mctsParams));

        agents.add(new RandomPlayer(new Random()));
//        agents.add(new CatanRuleBasedPlayer(new Random()));

        CatanParameters params = new CatanParameters("data/", System.currentTimeMillis());
        CatanForwardModel forwardModel = new CatanForwardModel(params, agents.size());
        CatanGameState gs = new CatanGameState(params, agents.size());

        CatanGame game = new CatanGame(agents, params, forwardModel, gs);

//        GamePanel panel = new GamePanel();
//        GamePanel gamePanel = new GamePanel();

//        GUI frame = new GUI();
//        frame.setContentPane(panel);
//        AbstractGUIManager gui = new CatanGUI(game, new ActionController(), panel);
//        frame.setFrameProperties();
        ActionController ac = new ActionController();

        game.runOne(GameType.Catan, "",  agents, System.currentTimeMillis(), false, null, ac, 200);
        System.out.println(game.gameState.getGameStatus());

//        runMany(players, forwardModel);
    }
}
