package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.sirius.*;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.assertTrue;

public class TestCopy {


    Game game;
    SiriusGameState state;
    SiriusForwardModel fm = new SiriusForwardModel();
    List<AbstractPlayer> players = new ArrayList<>();
    SiriusParameters params = new SiriusParameters();

    @Before
    public void setup() {
        players = Arrays.asList(
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer()
        );
        game = GameType.Sirius.createGameInstance(3, 34, params);
        game.reset(players);
        state = (SiriusGameState) game.getGameState();
    }

    @Test
    public void copyMetropolis() {
        SiriusGameState stateCopy = (SiriusGameState) state.copy();
        assertTrue(state.getMoon(4) instanceof Metropolis);
        assertTrue(stateCopy.getMoon(4) instanceof Metropolis);
    }
}
