package games.mastermind;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * A Game object reused for several games (as in every tournament) must start each game in the same state as a fresh
 * Game with the same seed. The peg colours are shared static components, so the answer code must hold copies of
 * them, or a peg drawn in one game keeps the owner the answer deck gave it.
 * Components are equal only by ID, so states from two Games are never equal: the fields that leaked are compared instead.
 */
public class ResetTest {

    private static List<AbstractPlayer> players(int n) {
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < n; p++)
            players.add(new RandomPlayer(new Random(42 + p)));
        return players;
    }

    @Test
    public void sharedPegsKeepNoOwner() {
        List<AbstractPlayer> players = players(1);
        Game game = GameType.Mastermind.createGameInstance(1, 1L);
        for (int i = 0; i < 3; i++) {
            game.reset(players, 100L + i);
            game.run();
        }
        game.reset(players, 7L);
        for (core.components.BoardNode peg : MMConstants.guessColours)
            assertEquals(peg.getComponentName(), -1, peg.getOwnerId());
        for (core.components.BoardNode peg : MMConstants.resultColours)
            assertEquals(peg.getComponentName(), -1, peg.getOwnerId());
    }
}
