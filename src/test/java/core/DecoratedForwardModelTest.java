package core;

import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.actions.SetGridValueAction;
import core.interfaces.IExtendedSequence;
import core.interfaces.IPlayerDecorator;
import games.GameType;
import games.tictactoe.TicTacToeForwardModel;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Checks that DecoratedForwardModel passes every call through to the forward model it wraps, and applies its
 * decorators to every list of actions. TicTacToe is used as a host game.
 */
public class DecoratedForwardModelTest {

    AbstractGameState state;
    RecordingForwardModel wrapped;
    DecoratedForwardModel decorated;

    @Before
    public void setup() {
        Game game = GameType.TicTacToe.createGameInstance(2, 42);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer()));
        state = game.getGameState();
        wrapped = new RecordingForwardModel();
        decorated = new DecoratedForwardModel(wrapped);
    }

    @Test
    public void playerSpecificActionComputationOfWrappedModelIsUsed() {
        wrapped.playerSpecific = List.of(new DoNothing());
        decorated.addDecorator(-1, new KeepAll());

        assertEquals(List.of(new DoNothing()), decorated.computeAvailableActions(state));
        assertEquals(List.of(new DoNothing()), decorated.computeAvailableActions(state, ActionSpace.Default, 1));
        assertEquals(Integer.valueOf(1), wrapped.askedForPlayer);
    }

    @Test
    public void actionSpaceComputationOfWrappedModelIsUsed() {
        ActionSpace deep = new ActionSpace(ActionSpace.Structure.Deep);
        wrapped.actionSpaceSpecific = List.of(new DoNothing());
        decorated.addDecorator(-1, new KeepAll());

        assertEquals(List.of(new DoNothing()), decorated.computeAvailableActions(state, deep));
        assertEquals(deep, wrapped.askedForActionSpace);
    }

    @Test
    public void decoratorsAreAppliedWhenASequenceIsInProgress() {
        state.setActionInProgress(new OfferSequence());
        decorated.addDecorator(-1, new RemoveDoNothing());

        List<AbstractAction> actions = decorated.computeAvailableActions(state);

        assertFalse(actions.isEmpty());
        assertTrue("decorator should have removed DoNothing", actions.stream().noneMatch(a -> a instanceof DoNothing));
    }

    @Test
    public void playerSpecificDecoratorUsesThePlayerAskedFor() {
        decorated.addDecorator(1, new FirstOnly());
        assertEquals(0, state.getCurrentPlayer());

        assertEquals(1, decorated.computeAvailableActions(state, ActionSpace.Default, 1).size());
        assertEquals(9, decorated.computeAvailableActions(state, ActionSpace.Default, 0).size());
    }

    @Test
    public void decoratorsAreAppliedOnlyOnce() {
        CountingDecorator counter = new CountingDecorator();
        decorated.addDecorator(-1, counter);
        decorated.computeAvailableActions(state);
        assertEquals(1, counter.calls);
    }

    @Test
    public void endGameIsPassedThrough() {
        decorated.endGame(state);
        assertTrue(wrapped.endGameCalled);
    }

    @Test
    public void setupIsPassedThrough() {
        decorated.setup(state);
        assertTrue(wrapped.abstractSetupCalled);
    }

    @Test
    public void illegalActionIsPassedThrough() {
        decorated.illegalActionPlayed(state, null);
        assertTrue(wrapped.illegalActionCalled);
    }

    /**
     * TicTacToe forward model that records which hooks are called, and can be told to return particular actions
     */
    static class RecordingForwardModel extends TicTacToeForwardModel {
        List<AbstractAction> playerSpecific, actionSpaceSpecific;
        Integer askedForPlayer;
        ActionSpace askedForActionSpace;
        boolean endGameCalled, abstractSetupCalled, illegalActionCalled;

        @Override
        protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, int activePlayer) {
            askedForPlayer = activePlayer;
            return playerSpecific != null ? playerSpecific : super._computeAvailableActions(gameState, activePlayer);
        }

        @Override
        protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace) {
            askedForActionSpace = actionSpace;
            return actionSpaceSpecific != null ? actionSpaceSpecific : super._computeAvailableActions(gameState, actionSpace);
        }

        @Override
        protected void endGame(AbstractGameState gs) {
            endGameCalled = true;
        }

        @Override
        protected void abstractSetup(AbstractGameState firstState) {
            abstractSetupCalled = true;
            super.abstractSetup(firstState);
        }

        @Override
        protected void illegalActionPlayed(AbstractGameState gameState, AbstractAction action) {
            illegalActionCalled = true;
        }
    }

    static class KeepAll implements IPlayerDecorator {
        @Override
        public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
            return possibleActions;
        }
    }

    static class RemoveDoNothing implements IPlayerDecorator {
        @Override
        public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
            return possibleActions.stream().filter(a -> !(a instanceof DoNothing)).toList();
        }
    }

    static class FirstOnly implements IPlayerDecorator {
        @Override
        public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
            return possibleActions.subList(0, 1);
        }
    }

    static class CountingDecorator implements IPlayerDecorator {
        int calls;

        @Override
        public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
            calls++;
            return possibleActions;
        }
    }

    /**
     * A sequence for player 0 that offers DoNothing and one TicTacToe move
     */
    static class OfferSequence implements IExtendedSequence {
        @Override
        public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
            List<AbstractAction> actions = new ArrayList<>();
            actions.add(new DoNothing());
            actions.add(new SetGridValueAction(-1, 0, 0, -1));
            return actions;
        }

        @Override
        public int getCurrentPlayer(AbstractGameState state) {
            return 0;
        }

        @Override
        public void _afterAction(AbstractGameState state, AbstractAction action) {
        }

        @Override
        public boolean executionComplete(AbstractGameState state) {
            return false;
        }

        @Override
        public OfferSequence copy() {
            return this;
        }
    }
}
