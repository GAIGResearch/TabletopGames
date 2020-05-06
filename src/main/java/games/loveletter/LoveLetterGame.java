package games.loveletter;

import core.GUI;
import core.Game;
import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;
import players.AbstractPlayer;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static games.pandemic.PandemicConstants.VERBOSE;

public class LoveLetterGame extends Game {

    public LoveLetterGame(List<AbstractPlayer> agents) {
        super(agents);
        LoveLetterParameters params = new LoveLetterParameters();
        forwardModel = new LoveLetterForwardModel();
        gameState = new LoveLetterGameState(params, forwardModel, agents.size());
    }

    @Override
    public void run(GUI gui) {
        while (!gameState.isTerminal()){
            if (VERBOSE) System.out.println("Round: " + gameState.getTurnOrder().getRoundCounter());

            // Get player to ask for actions next
            int activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);
            // Get actions for the player
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(true));
            IObservation observation = gameState.getObservation(activePlayer);
            if (observation != null && VERBOSE) {
                ((IPrintable) observation).printToConsole();
            }

            int action = players.get(activePlayer).getAction(observation, actions);
            gameState.getTurnOrder().endPlayerTurnStep(gameState);

            // Resolve core.actions and game rules for the turn
            forwardModel.next(gameState, actions.get(action));

            if (gui != null) {
                gui.update(gameState);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("EXCEPTION " + e);
                }
            }
        }

        gameState.endGame();

        System.out.println("Game Over");
    }

    public static void main(String[] args){
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer(0));
        agents.add(new RandomPlayer(1));
        agents.add(new RandomPlayer(2));
        agents.add(new RandomPlayer(3));

        for (int i=0; i<1; i++) {
            Game game = new LoveLetterGame(agents);
            game.run(null);
            LoveLetterGameState gameState = (LoveLetterGameState) game.getGameState();

            gameState.print((LoveLetterTurnOrder) gameState.getTurnOrder());
            // ((IPrintable) gameState.getObservation(null)).PrintToConsole();
            System.out.println(Arrays.toString(gameState.getPlayerResults()));

            for (int j = 0; j < gameState.getNPlayers(); j++){
                if (gameState.isPlayerAlive(j))
                    System.out.println("Player " + j + " won");
            }
        }
    }
}
