package games.loveletter;

import core.AbstractPlayer;
import core.ForwardModel;
import core.GUI;
import core.Game;
import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;
import players.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static utilities.CoreConstants.VERBOSE;

public class LoveLetterGame extends Game {


    public LoveLetterGame(List<AbstractPlayer> agents, LoveLetterForwardModel forwardModel, LoveLetterGameState gameState) {
        super(agents, forwardModel, gameState);
    }

    @Override
    public void run(GUI gui) {
        while (!gameState.isTerminal()){
            System.out.println();
            System.out.println();
            if (true) System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());

            // Get player to ask for actions next
            int activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);
            // Get actions for the player
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(true));
            IObservation observation = gameState.getObservation(activePlayer);
            if (observation != null && true) {
                ((IPrintable) observation).printToConsole();
            }

            int action = players.get(activePlayer).getAction(observation, actions);
            forwardModel.next(gameState, actions.get(action));
        }

        System.out.println("Game Over");
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());

        for (int i=0; i<1; i++) {
            LoveLetterParameters params = new LoveLetterParameters();
            LoveLetterForwardModel forwardModel = new LoveLetterForwardModel();
            LoveLetterGameState tmp_gameState = new LoveLetterGameState(params, forwardModel, agents.size());

            Game game = new LoveLetterGame(agents, forwardModel, tmp_gameState);
            game.run(null);
            LoveLetterGameState gameState = (LoveLetterGameState) game.getGameState();

            gameState.print((LoveLetterTurnOrder) gameState.getTurnOrder());
            // ((IPrintable) gameState.getObservation(null)).PrintToConsole();
            System.out.println(Arrays.toString(gameState.getPlayerResults()));

            Utils.GameResult[] playerResults = gameState.getPlayerResults();
            for (int j = 0; j < gameState.getNPlayers(); j++){
                if (playerResults[j] == Utils.GameResult.GAME_WIN)
                    System.out.println("Player " + j + " won");
            }
        }
    }
}
