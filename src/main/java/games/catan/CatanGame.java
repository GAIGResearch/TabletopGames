package games.catan;

import core.AbstractPlayer;
import core.AbstractForwardModel;
import core.Game;
import games.GameType;
import games.catan.gui.CatanGUI;
import players.PlayerConstants;
import players.human.ActionController;
import players.human.HumanConsolePlayer;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCParams;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

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
        agents.add(new OSLAPlayer());

        // RHEA
//        RMHCParams rmhcParams = new RMHCParams();
////        rmhcParams.budgetType = PlayerConstants.BUDGET_TIME;
//        rmhcParams.fmCallsBudget = 10;
////        rmhcParams.iterationsBudget = 1;
//        agents.add(new RMHCPlayer(rmhcParams));


        // MCTS
//        MCTSParams mctsParams = new MCTSParams();
//        mctsParams.budgetType = PlayerConstants.BUDGET_FM_CALLS;
//        mctsParams.fmCallsBudget = 4000;
//        agents.add(new MCTSPlayer(mctsParams));

//        agents.add(new RandomPlayer(new Random()));
        agents.add(new RandomPlayer(new Random()));
        agents.add(new RandomPlayer(new Random()));
        agents.add(new RandomPlayer(new Random()));

        CatanParameters params = new CatanParameters("data/", System.currentTimeMillis());
        CatanForwardModel forwardModel = new CatanForwardModel(params, agents.size());
        CatanGameState gs = new CatanGameState(params, agents.size());

        CatanGame game = new CatanGame(agents, params, forwardModel, gs);

        game.run(new CatanGUI(game, new ActionController()));
        System.out.println(game.gameState.getGameStatus());

//        runMany(players, forwardModel);
    }
}
