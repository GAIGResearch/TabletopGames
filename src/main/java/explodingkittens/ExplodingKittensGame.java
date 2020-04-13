package explodingkittens;

import actions.Action;
import actions.DrawCard;
import core.GUI;
import core.Game;

import java.util.HashSet;

public class ExplodingKittensGame extends Game {

    @Override
    public void run(GUI gui) {
        int turn = 0;
        int actionsPlayed = 0;
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;

        while (!ekgs.isGameOver()) {
            //System.out.println("Turn " + turn++);
            if (ekgs.gamePhase == ExplodingKittensGamePhase.PlayerMove) {
                System.out.println();
                ekgs.print();
            }

            int activePlayer = gameState.getActivePlayer();
            Action action = players.get(activePlayer).getAction(gameState);
            forwardModel.next(gameState, action);
            /*
            // Get actions of current active player for their turn
            int activePlayer = gameState.getActivePlayer();
            Action action = players.get(activePlayer).getAction(gameState);

            // Resolve actions and game rules for the turn
            forwardModel.next(gameState, action);
            actionsPlayed++;

            if (gui != null) {
                gui.update();
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("EXCEPTION " + e);
                }
            }
            */
        }

        System.out.println("Game Over");
        for (int i = 0; i < gameState.nPlayers(); i++){
            if (((ExplodingKittensGameState) gameState).playerActive[i])
                System.out.println("Player " + i + " won");
        }
    }

    @Override
    public boolean isEnded() {
        return gameOver;
    }

    @Override
    public HashSet<Integer> winners() {
        HashSet<Integer> winners = new HashSet<>();
        // TODO: all or nothing, check gamestate
        return winners;
    }
}
