package games.pandemic;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.properties.PropertyString;
import games.GameType;
import games.pandemic.actions.Forecast;
import games.pandemic.actions.MovePlayer;
import games.pandemic.gui.PandemicPlanner;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.stream.IntStream;

import static games.pandemic.PandemicConstants.playerCardHash;
import static games.pandemic.PandemicConstants.playerLocationHash;
import static org.junit.Assert.*;

/**
 * The planner with which a player plans the actions of their turn in the web server's page, and sends them together:
 * the first turn of a 2-player game, everyone in Atlanta.
 */
public class PandemicPlannerTest {

    Game game;
    PandemicGameState state;
    PandemicForwardModel fm;
    PandemicPlanner planner;
    int player;

    @Before
    public void setup() {
        game = GameType.Pandemic.createGameInstance(2, 34);
        game.reset(IntStream.range(0, 2).mapToObj(p -> (AbstractPlayer) new RandomPlayer()).toList());
        state = (PandemicGameState) game.getGameState();
        fm = (PandemicForwardModel) game.getForwardModel();
        planner = new PandemicPlanner(fm);
        player = state.getCurrentPlayer();
    }

    String location(PandemicGameState s) {
        return ((PropertyString) s.getComponent(playerCardHash, player).getProperty(playerLocationHash)).value;
    }

    MovePlayer drive(PandemicGameState s) {
        return (MovePlayer) planner.options(s, player).stream()
                .filter(a -> a instanceof MovePlayer m && m.getMoveType() == MovePlayer.MoveType.DriveFerry)
                .findFirst().orElseThrow();
    }

    @Test
    public void theOptionsAreTheForwardModelsButForecast() {
        assertTrue(planner.plans(state, player));
        assertFalse(planner.plans(state, 1 - player));
        List<AbstractAction> available = fm.computeAvailableActions(state.copy());
        List<AbstractAction> options = planner.options(state, player);
        assertEquals(available.stream().filter(a -> !(a instanceof Forecast)).toList(), options);
    }

    @Test
    public void actionsArePlannedOnTheCopyAndCountAsStepsOfTheTurn() {
        PandemicGameState planned = (PandemicGameState) state.copy();
        MovePlayer move = drive(planned);
        planner.apply(planned, move);
        assertEquals(move.getDestination(), location(planned));
        assertEquals(1, ((PandemicTurnOrder) planned.getTurnOrder()).getTurnStep());
        assertTrue(planner.warnings(planned, player).get(0).startsWith("3 actions left"));
        // the game itself is untouched
        assertEquals("Atlanta", location(state));
        assertEquals(0, ((PandemicTurnOrder) state.getTurnOrder()).getTurnStep());
    }

    @Test
    public void thePlanEndsAfterTheTurnsActionsBeforeAnyCardIsDrawn() {
        PandemicGameState planned = (PandemicGameState) state.copy();
        int deck = planned.getComponent(PandemicConstants.playerDeckHash) instanceof core.components.Deck<?> d ? d.getSize() : -1;
        for (int i = 0; i < 4; i++)
            planner.apply(planned, drive(planned));
        assertTrue(planner.options(planned, player).isEmpty());
        assertTrue(planner.warnings(planned, player).isEmpty());
        assertEquals(deck, ((core.components.Deck<?>) planned.getComponent(PandemicConstants.playerDeckHash)).getSize());
    }

    @Test
    public void thePlannedActionsAreTheOnesTheGameTakes() {
        // planned on a copy, then taken by the game one by one: the same moves are offered and lead to the same city
        PandemicGameState planned = (PandemicGameState) state.copy();
        MovePlayer first = drive(planned);
        planner.apply(planned, first);
        MovePlayer second = drive(planned);
        planner.apply(planned, second);
        assertTrue(fm.computeAvailableActions(state).contains(first));
        fm.next(state, first);
        assertTrue(fm.computeAvailableActions(state).contains(second));
        fm.next(state, second);
        assertEquals(location(planned), location(state));
        assertEquals(2, ((PandemicTurnOrder) state.getTurnOrder()).getTurnStep());
    }
}
