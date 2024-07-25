package games.coltexpress;

import core.*;
import games.GameType;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;

public class ColtExpressGame extends Game {

    public ColtExpressGame(List<AbstractPlayer> agents, AbstractForwardModel forwardModel, ColtExpressGameState gameState) {
        super(GameType.ColtExpress, agents, forwardModel, gameState);
    }

    public ColtExpressGame(AbstractForwardModel forwardModel, AbstractGameState gameState) {
        super(GameType.ColtExpress, forwardModel, gameState);
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        ActionController ac = new ActionController();
        agents.add(new HumanGUIPlayer(ac));
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());

        for (int i=0; i<1; i++) {
            ColtExpressParameters params = new ColtExpressParameters();
            AbstractForwardModel forwardModel = new ColtExpressForwardModel();
            ColtExpressGameState tmp_gameState = new ColtExpressGameState(params, agents.size());

            Game game = new ColtExpressGame(agents, forwardModel, tmp_gameState);
//            game.run(new ColtExpressGUI(game, ac, 1)); TODO
            ColtExpressGameState gameState = (ColtExpressGameState) game.getGameState();

            //gameState.printToConsole();
            // ((IPrintable) gameState.getObservation(null)).PrintToConsole();
            //System.out.println(Arrays.toString(gameState.getPlayerResults()));

            CoreConstants.GameResult[] playerResults = gameState.getPlayerResults();
            for (int j = 0; j < gameState.getNPlayers(); j++){
                if (playerResults[j] == CoreConstants.GameResult.WIN_GAME)
                    System.out.println("Player " + j + " won");
            }
        }
    }
}
