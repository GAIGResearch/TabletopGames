package games.perudo;

import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import games.perudo.actions.Dudo;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static games.perudo.PerudoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Dudo on its own (execute on an arranged state): who loses a die, and what is left for the next round.
 * The forward model's handling afterwards (new round, elimination, game end) is in PerudoRoundFlowTest.
 */
public class PerudoDudoTest {

    PerudoGameState state;

    @Before
    public void setup() {
        state = newState(3, 7);
        // fours: 2 + 1 + 1 = 4; Pacos: 1 + 0 + 2 = 3; so 7 dice count towards a bid on fours
        setAllDice(state,
                faces(1, 0, 0, 2, 1, 1),
                faces(0, 2, 0, 1, 1, 1),
                faces(2, 0, 1, 1, 0, 1));
        // player 2 made the bid; player 0 (next in seat order) calls Dudo
        state.setTurnOwner(0);
    }

    @Test
    public void bidderLosesADieWhenFewerDiceMatchThanBid() {
        state.setBid(8, 4, 2);       // 7 < 8: the bid was wrong
        new Dudo().execute(state);
        assertEquals(5, state.getNDice(0));
        assertEquals(5, state.getNDice(1));
        assertEquals(4, state.getNDice(2));
        assertEquals(2, state.getNextRoundStarter());
        assertBidCleared();
    }

    @Test
    public void callerLosesADieWhenExactlyTheBidMatches() {
        state.setBid(7, 4, 2);       // 7 == 7: the bid was right
        new Dudo().execute(state);
        assertEquals(4, state.getNDice(0));
        assertEquals(5, state.getNDice(1));
        assertEquals(5, state.getNDice(2));
        assertEquals(0, state.getNextRoundStarter());
        assertBidCleared();
    }

    @Test
    public void callerLosesADieWhenMoreDiceMatchThanBid() {
        state.setBid(5, 4, 2);       // 7 > 5: the bid was right
        new Dudo().execute(state);
        assertEquals(4, state.getNDice(0));
        assertEquals(5, state.getNDice(2));
        assertEquals(0, state.getNextRoundStarter());
        assertBidCleared();
    }

    @Test
    public void pacosCountForTheBidFace() {
        // sixes: 1 + 1 + 1 = 3 showing, + 3 Pacos = 6. Without the Pacos six sixes would be wrong.
        state.setBid(6, 6, 2);
        new Dudo().execute(state);
        assertEquals(4, state.getNDice(0));   // the caller loses: the bid was exactly right
        assertEquals(5, state.getNDice(2));
    }

    @Test
    public void dudoRevealsTheDiceAsAGameEvent() {
        List<Event> events = new ArrayList<>();
        state.addListener(new IGameListener() {
            public void onEvent(Event event) { events.add(event); }
            public void report() { }
            public void setGame(Game game) { }
            public Game getGame() { return null; }
        });
        state.setBid(8, 4, 2);
        new Dudo().execute(state);
        assertTrue("no GAME_EVENT logged by Dudo",
                events.stream().anyMatch(e -> e.type == Event.GameEvent.GAME_EVENT));
    }

    private void assertBidCleared() {
        assertFalse(state.hasBid());
        assertEquals(0, state.getBidQuantity());
        assertEquals(-1, state.getBidder());
    }
}
