package evaluation;

import core.*;
import games.coltexpress.ColtExpressForwardModel;
import games.coltexpress.ColtExpressGame;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.loveletter.LoveLetterForwardModel;
import games.loveletter.LoveLetterGame;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.pandemic.PandemicForwardModel;
import games.pandemic.PandemicGame;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;

import java.util.LinkedList;

public abstract class AbstractTournament {

    public enum Game {
        Carcassonne,
        ColtExpress,
        ExplodingKittens,
        LoveLetter,
        Pandemic,
        TicTacToe,
        Uno
    }

    LinkedList<AbstractPlayer> agents;

    public AbstractTournament(LinkedList<AbstractPlayer> agents){
        this.agents = agents;
    }

    public abstract void runTournament();

    public static AbstractGame createGameInstance(Game game, int nPlayers) {
        AbstractGameParameters params;
        AbstractForwardModel forwardModel;
        AbstractGameState gameState;

        switch(game){
            case Pandemic:
                params = new PandemicParameters("data/pandemic/");
                forwardModel = new PandemicForwardModel((PandemicParameters) params, nPlayers);
                gameState = new PandemicGameState(params, nPlayers);
                return new PandemicGame(forwardModel, gameState);
            case LoveLetter:
                params = new LoveLetterParameters();
                forwardModel = new LoveLetterForwardModel();
                gameState = new LoveLetterGameState((LoveLetterParameters) params, forwardModel, nPlayers);
                return new LoveLetterGame(forwardModel, gameState);
            case ColtExpress:
                params = new ColtExpressParameters();
                forwardModel = new ColtExpressForwardModel();
                gameState = new ColtExpressGameState((ColtExpressParameters)params, forwardModel, nPlayers);
                return new ColtExpressGame(forwardModel, gameState);
        }

        return null;
    }

}
