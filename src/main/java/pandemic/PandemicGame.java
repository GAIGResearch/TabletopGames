package pandemic;

import actions.IAction;
import core.GUI;
import core.Game;
import observations.Observation;
import players.AbstractPlayer;
import players.RandomPlayer;

import java.util.*;


public class PandemicGame extends Game {

    public PandemicGame(List<AbstractPlayer> agents)
    {
        players = agents;
        turnOrder = new PandemicTurnOrder(agents);
        forwardModel = new PandemicForwardModel();

        PandemicParameters params = new PandemicParameters(agents.size(), "data/");
        gameState = new PandemicGameState(params);
        ((PandemicForwardModel) forwardModel).setup(gameState);
    }

    @Override
    public void run(GUI gui) {
        int turn = 0;

        while (!isEnded()){

            System.out.println(turn++);

            // Get actions of current active player for their turn
            int activePlayer = ((PandemicGameState)gameState).getActingPlayer(); // TODO: any specific constraints on game state for reaction?
            List<IAction> actions = Collections.unmodifiableList(gameState.getActions(players.get(activePlayer)));
            int action = players.get(activePlayer).getAction((Observation) gameState, actions);

            // Resolve actions and game rules for the turn
            forwardModel.next(gameState, turnOrder, actions.get(action));

            if (gui != null) {
                gui.update(gameState, turnOrder);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("EXCEPTION " + e);
                }
            }
        }

        System.out.println("Game Over");
        if (gameState.getGameStatus() == Constants.GameResult.GAME_WIN) {
            System.out.println("Winners: " + winners().toString());
        } else {
            System.out.println("Lose");
        }
    }

    @Override
    public boolean isEnded() {
        return gameState.getGameStatus() != Constants.GameResult.GAME_ONGOING;
    }

    @Override
    public HashSet<Integer> winners() {
        HashSet<Integer> winners = new HashSet<>();
        if (gameState.getGameStatus() == Constants.GameResult.GAME_WIN) {
            for (int i = 0; i < players.size(); i++) winners.add(i);
        }
        return winners;
    }

    public static void main(String[] args){

        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(0, new Random()));
        players.add(new RandomPlayer(1, new Random()));
        players.add(new RandomPlayer(2, new Random()));
        players.add(new RandomPlayer(3, new Random()));

        PandemicGame game = new PandemicGame(players);
        GUI gui = new PandemicGUI((PandemicGameState) game.getGameState(), (PandemicTurnOrder) game.getTurnOrder());
        game.run(gui);
    }
}
