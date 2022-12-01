package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.sirius.*;
import games.sirius.actions.MoveToMoon;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;
import java.util.*;

import static org.junit.Assert.*;

public class TestTurnOrder {

    Game game;
    SiriusGameState state;
    SiriusForwardModel fm = new SiriusForwardModel();
    List<AbstractPlayer> players = new ArrayList<>();

    @Before
    public void setup() {
        players = Arrays.asList(new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer());
        game = GameType.Sirius.createGameInstance(3, 34, new SiriusParameters());
        game.reset(players);
        state = (SiriusGameState) game.getGameState();
    }

    @Test
    public void testNextPlayer() {
        SiriusTurnOrder sto = (SiriusTurnOrder) state.getTurnOrder();
        assertEquals(1, sto.nextPlayer(state));
        fm.next(state, new MoveToMoon(1));
        assertEquals(2, sto.nextPlayer(state));
        fm.next(state, new MoveToMoon(1));
        assertEquals(0, sto.nextPlayer(state));
    }

}
