package uno;

import actions.Action;
import core.GUI;
import core.Game;
import players.AbstractPlayer;
import players.RandomPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static pandemic.Constants.GAME_ONGOING;
import static pandemic.Constants.GAME_WIN;
import static pandemic.Constants.GameResult.GAME_ONGOING;

public class UnoGame extends Game {
    @Override
    public void run(GUI gui) {
        int turn          = 0;
        int actionsPlayed = 0;

        while (!isEnded()) {
            System.out.println(turn++);

            // Get actions of current active player for their turn
            int   activePlayer = gameState.getActivePlayer();
            Action action      = players.get(activePlayer).getAction(gameState);

            gameState.next(action);
            actionsPlayed++;

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
        // TODO: add in winners the id of the winner
        // Now the winners is always player 0
        HashSet<Integer> winners = new HashSet<>();
        winners.add(0);
        return winners;
    }

    public static void main(String[] args){
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(0, new Random()));
        players.add(new RandomPlayer(1, new Random()));
        players.add(new RandomPlayer(2, new Random()));
        players.add(new RandomPlayer(3, new Random()));

        UnoGame game = new UnoGame(players);
        GUI gui = new UnoGUI((UnoGameState) game.getGameState(), (UnoTurnOrder) game.getTurnOrder());
        game.run(gui);
    }
}