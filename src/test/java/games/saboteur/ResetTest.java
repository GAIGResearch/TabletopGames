package games.saboteur;

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
 * Game with the same seed. The placements are listed from a set of positions, which must list them in the same order
 * however many games the set has held before.
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
    public void actionOrderMatchesFreshGameAfterReuse() {
        List<AbstractPlayer> players = players(3);
        Game reused = GameType.Saboteur.createGameInstance(3, 1L);
        for (int i = 0; i < 5; i++) {
            reused.reset(players, 100L + i);
            reused.run();
        }
        reused.reset(players, 7L);
        Game fresh = GameType.Saboteur.createGameInstance(3, 1L);
        fresh.reset(players, 7L);

        assertEquals(placements(fresh, fresh.getGameState()), placements(reused, reused.getGameState()));
        // and a copy lists them in the same order as the state it was copied from
        assertEquals(placements(reused, reused.getGameState()), placements(reused, reused.getGameState().copy()));
    }

    // the cards are named by component ID, which differs between two Games, so only the positions are compared
    private static String placements(Game game, core.AbstractGameState state) {
        return game.getForwardModel().computeAvailableActions(state).toString().replaceAll("Place \\d+", "Place");
    }
}
