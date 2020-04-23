package carcassonne;

import actions.Action;
import core.GUI;
import core.Game;
import explodingkittens.ExplodingKittensGamePhase;
import explodingkittens.ExplodingKittensGameState;

import java.util.HashSet;

public class CarcassonneGame extends Game {
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
            gameState.next(action);
            /*
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
        for (int i = 0; i < gameState.getNPlayers(); i++){
            if (((ExplodingKittensGameState) gameState).playerActive[i])
                System.out.println("Player " + i + " won");
        }
    }

    @Override
    public boolean isEnded() {
        return false;
    }

    @Override
    public HashSet<Integer> winners() {
        return null;
    }
}
