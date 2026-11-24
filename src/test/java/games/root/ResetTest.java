package games.root;

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
 * Game with the same seed. The Vagabond's character is chosen in each game, and must not be kept from the first one.
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
    public void vagabondCharacterIsClearedAfterReuse() {
        List<AbstractPlayer> players = players(4);
        Game reused = GameType.Root.createGameInstance(4, 1L);
        reused.reset(players, 100L);
        reused.run();
        RootGameState state = (RootGameState) reused.getGameState();
        assertNotNull("test needs a game with the Vagabond", state.vagabondCharacter);

        reused.reset(players, 7L);
        assertNull(state.vagabondCharacter);
    }
}
