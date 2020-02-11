package pandemic;

import actions.Action;
import core.Game;

import java.util.HashSet;

public class PandemicGame extends Game {

    @Override
    public void run() {
        int turn = 0;
        int actionsPlayed = 0;
        while (!isEnded()) {
            System.out.println(turn++);

            // Get actions of current active player for their turn
            int activePlayer = gameState.getActivePlayer();
            Action action = players.get(activePlayer).getAction(gameState);

            // Resolve actions and game rules for the turn
            forwardModel.next(gameState, action);
            actionsPlayed++;


            // TODO: GUI
        }

        System.out.println("Game Over");
        System.out.println(winners().toString());
    }

    @Override
    public boolean isEnded() {
        // TODO: check if game is over
        return false;
    }

    @Override
    public HashSet<Integer> winners() {
        // TODO: all or nothing, check gamestate
        return null;
    }
}
