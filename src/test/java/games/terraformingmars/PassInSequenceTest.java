package games.terraformingmars;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.terraformingmars.actions.SellProjects;
import games.terraformingmars.actions.TMExtendedSequence;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.*;

public class PassInSequenceTest {

    /**
     * Selling projects is one action. The Pass that stops selling is part of it, and uses no further action point.
     */
    @Test
    public void passToStopSellingProjectsIsNotASeparateAction() {
        Game game = GameType.TerraformingMars.createGameInstance(3, 42);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        TMGameState state = (TMGameState) game.getGameState();
        TMForwardModel fm = (TMForwardModel) game.getForwardModel();
        Random rnd = new Random(1);

        Optional<AbstractAction> sell = Optional.empty();
        while (sell.isEmpty()) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if (state.getGamePhase() == TMGameState.TMPhase.Actions && !state.isActionInProgress() && state.nActionsTaken == 0)
                sell = actions.stream().filter(a -> a instanceof SellProjects).findFirst();
            if (sell.isEmpty())
                fm.next(state, actions.get(rnd.nextInt(actions.size())));
        }
        int player = state.getCurrentPlayer();

        fm.next(state, sell.get());
        fm.next(state, fm.computeAvailableActions(state).get(0));  // sell a card
        AbstractAction pass = fm.computeAvailableActions(state).stream().filter(TMExtendedSequence::isPass).findFirst().orElseThrow();
        fm.next(state, pass);

        assertFalse(state.isActionInProgress());
        assertEquals(1, state.nActionsTaken);
        assertFalse(state.hasPassed(player));
        assertEquals(player, state.getCurrentPlayer());
    }
}
