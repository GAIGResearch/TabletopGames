package games.pandemic;

import core.actions.IAction;
import core.GUI;
import core.Game;

import java.util.HashSet;
import java.util.List;
import core.observations.Observation;
import players.AbstractPlayer;
import players.RandomPlayer;
import utilities.Pair;

import java.util.*;


public class PandemicGame extends Game {

    public PandemicGame(List<AbstractPlayer> agents)
    {
        PandemicParameters params = new PandemicParameters("data/");

        players = agents;
        forwardModel = new PandemicForwardModel(params);

        gameState = new PandemicGameState(params, agents.size());
        ((PandemicGameState)gameState).setComponents(params.getDataPath());
        ((PandemicForwardModel) forwardModel).setup(gameState);
    }

    @Override
    public void run(GUI gui) {
        int turn = 0;

        while (!isEnded()){

            System.out.println(turn++);

            // Get core.actions of current active player for their turn
            Pair<Integer, List<IAction>> activePlayer = ((PandemicGameState)gameState).getActingPlayer();
            List<IAction> actions = Collections.unmodifiableList(gameState.setAvailableActions(activePlayer.b, activePlayer.a));
            int action = players.get(activePlayer.a).getAction((Observation) gameState, actions);
            gameState.setAvailableActions(null, activePlayer.a);  // Reset core.actions for next player

            // Resolve core.actions and game rules for the turn
            forwardModel.next(gameState, actions.get(action));

            // Add all players as reactions for event cards
            for (int i = 0; i < players.size(); i++) {
                ((PandemicGameState)gameState).addReactivePlayer(i, ((PandemicGameState) gameState).getEventActions(i));
            }

            if (gui != null) {
                gui.update(gameState);
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
        GUI gui = new PandemicGUI((PandemicGameState) game.getGameState());
        game.run(gui);
    }
}
