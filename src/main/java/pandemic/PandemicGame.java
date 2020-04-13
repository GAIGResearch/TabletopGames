package pandemic;

import actions.Action;
import core.GUI;
import core.Game;

import java.util.HashSet;

import static pandemic.Constants.GAME_ONGOING;
import static pandemic.Constants.GAME_WIN;

public class PandemicGame extends Game {

    @Override
    public void run(GUI gui) {
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


            if (gui != null) {
                gui.update();
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("EXCEPTION " + e);
                }
            }

        }

        System.out.println("Game Over");
        if (gameState.getGameStatus() == GAME_WIN) {
            System.out.println("Winners: " + winners().toString());
        } else {
            System.out.println("Lose");
        }
    }

    @Override
    public boolean isEnded() {
        return gameState.getGameStatus() != GAME_ONGOING;
    }

    @Override
    public HashSet<Integer> winners() {
        HashSet<Integer> winners = new HashSet<>();
        if (gameState.getGameStatus() == GAME_WIN) {
            for (int i = 0; i < players.size(); i++) winners.add(i);
        }
        return winners;
    }
}
