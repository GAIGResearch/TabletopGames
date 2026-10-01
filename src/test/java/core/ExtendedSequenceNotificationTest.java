package core;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.GameType;
import games.tictactoe.TicTacToeForwardModel;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Checks which IExtendedSequence StandardForwardModel tells about each action, and when.
 * TicTacToe is used only as a host game: its forward model accepts any action.
 */
public class ExtendedSequenceNotificationTest {

    AbstractGameState state;
    StandardForwardModel fm;

    @Before
    public void setup() {
        Game game = GameType.TicTacToe.createGameInstance(2);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer()));
        state = game.getGameState();
        fm = (StandardForwardModel) game.getForwardModel();
    }

    @Test
    public void ownerIsToldImmediatelyWhenTheChosenActionContinuesAsASequence() {
        Owner owner = new Owner(1);
        state.setActionInProgress(owner);
        SelfPushingChild child = new SelfPushingChild(1);

        fm.next(state, child);

        assertEquals(List.of(child), owner.told);
        assertSame(child, state.currentActionInProgress());
        assertTrue("child is not told about the action that started it", child.told.isEmpty());
    }

    @Test
    public void ownerIsNotToldAgainWhenTheChildCompletes() {
        Owner owner = new Owner(2);
        state.setActionInProgress(owner);
        SelfPushingChild child = new SelfPushingChild(1);
        fm.next(state, child);

        DoNothing childDecision = new DoNothing();
        fm.next(state, childDecision);

        assertEquals(List.of(childDecision), child.told);
        assertEquals(List.of(child), owner.told);
        assertSame(owner, state.currentActionInProgress());
    }

    @Test
    public void completedOwnerStaysOnStackUntilTheChildAboveItCompletes() {
        Owner owner = new Owner(1);
        state.setActionInProgress(owner);
        SelfPushingChild child = new SelfPushingChild(2);

        fm.next(state, child);
        assertTrue(owner.executionComplete(state));
        assertEquals(List.of(owner, child), new ArrayList<>(state.getActionsInProgress()));

        fm.next(state, new DoNothing());
        assertSame(child, state.currentActionInProgress());
        fm.next(state, new DoNothing());
        assertFalse(state.isActionInProgress());
        assertEquals(1, owner.told.size());
    }

    @Test
    public void siblingSequencesStartedByOneActionAreNotTreatedAsDecisions() {
        Owner owner = new Owner(2);
        state.setActionInProgress(owner);
        Owner first = new Owner(1);
        Owner second = new Owner(1);
        PushesTwoSequences action = new PushesTwoSequences(first, second);

        fm.next(state, action);
        assertEquals(List.of(action), owner.told);
        assertTrue(first.told.isEmpty());
        assertTrue(second.told.isEmpty());
        assertSame(second, state.currentActionInProgress());

        DoNothing decision = new DoNothing();
        fm.next(state, decision);  // completes second, which is removed; first is then on top
        assertEquals(List.of(decision), second.told);
        assertTrue(first.told.isEmpty());
        assertSame(first, state.currentActionInProgress());

        fm.next(state, new DoNothing());  // completes first
        assertEquals(List.of(action), owner.told);
        assertSame(owner, state.currentActionInProgress());
    }

    @Test
    public void forwardModelIsNotToldWhileASequenceIsInControl() {
        RecordingForwardModel rfm = new RecordingForwardModel();
        Owner owner = new Owner(2);
        state.setActionInProgress(owner);

        DoNothing first = new DoNothing();
        rfm.next(state, first);
        assertEquals(List.of(first), owner.toldBefore);
        assertEquals(List.of(first), owner.told);
        assertTrue(rfm.toldBefore.isEmpty());
        assertTrue(rfm.told.isEmpty());

        DoNothing last = new DoNothing();
        rfm.next(state, last);  // completes the sequence, so the stack is now empty
        assertFalse(state.isActionInProgress());
        assertTrue(rfm.toldBefore.isEmpty());
        assertEquals(List.of(last), rfm.told);
    }

    @Test
    public void forwardModelIsToldOnlyBeforeAnActionThatStartsASequence() {
        RecordingForwardModel rfm = new RecordingForwardModel();
        SelfPushingChild child = new SelfPushingChild(1);

        rfm.next(state, child);
        assertEquals(List.of(child), rfm.toldBefore);
        assertTrue(rfm.told.isEmpty());

        DoNothing decision = new DoNothing();
        rfm.next(state, decision);
        assertEquals(List.of(child), rfm.toldBefore);
        assertEquals(List.of(decision), rfm.told);
    }

    @Test
    public void forwardModelIsNotToldWhenASequenceCompletesWithAnotherBelowIt() {
        RecordingForwardModel rfm = new RecordingForwardModel();
        Owner owner = new Owner(2);
        state.setActionInProgress(owner);
        rfm.next(state, new SelfPushingChild(1));
        rfm.next(state, new DoNothing());  // completes the child, but not the owner

        assertSame(owner, state.currentActionInProgress());
        assertTrue(rfm.told.isEmpty());
    }

    /**
     * Records what the forward model itself is told, around TicTacToe's own behaviour.
     */
    static class RecordingForwardModel extends TicTacToeForwardModel {
        final List<AbstractAction> toldBefore = new ArrayList<>();
        final List<AbstractAction> told = new ArrayList<>();

        @Override
        protected void _beforeAction(AbstractGameState currentState, AbstractAction actionChosen) {
            toldBefore.add(actionChosen);
        }

        @Override
        protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
            told.add(action);
        }
    }

    /**
     * A sequence (not an action) that completes after a fixed number of decisions, and records what it is told.
     */
    static class Owner implements IExtendedSequence {
        final int decisions;
        final List<AbstractAction> told = new ArrayList<>();
        final List<AbstractAction> toldBefore = new ArrayList<>();

        Owner(int decisions) {
            this.decisions = decisions;
        }

        @Override
        public int getCurrentPlayer(AbstractGameState state) {
            return 0;
        }

        @Override
        public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
            return Collections.singletonList(new DoNothing());
        }

        @Override
        public void _beforeAction(AbstractGameState state, AbstractAction action) {
            toldBefore.add(action);
        }

        @Override
        public void _afterAction(AbstractGameState state, AbstractAction action) {
            told.add(action);
        }

        @Override
        public boolean executionComplete(AbstractGameState state) {
            return told.size() >= decisions;
        }

        @Override
        public Owner copy() {
            return this;  // not copied in these tests
        }
    }

    /**
     * An action that puts itself on the stack, and then needs a number of further decisions.
     */
    static class SelfPushingChild extends AbstractAction implements IExtendedSequence {
        final int decisions;
        final List<AbstractAction> told = new ArrayList<>();

        SelfPushingChild(int decisions) {
            this.decisions = decisions;
        }

        @Override
        public boolean execute(AbstractGameState gs) {
            gs.setActionInProgress(this);
            return true;
        }

        @Override
        public int getCurrentPlayer(AbstractGameState state) {
            return 0;
        }

        @Override
        public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
            return Collections.singletonList(new DoNothing());
        }

        @Override
        public void _afterAction(AbstractGameState state, AbstractAction action) {
            told.add(action);
        }

        @Override
        public boolean executionComplete(AbstractGameState state) {
            return told.size() >= decisions;
        }

        @Override
        public SelfPushingChild copy() {
            return this;  // not copied in these tests
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String getString(AbstractGameState gameState) {
            return "SelfPushingChild";
        }
    }

    /**
     * An action that starts two separate sequences, with the second on top.
     */
    static class PushesTwoSequences extends AbstractAction {
        final IExtendedSequence first, second;

        PushesTwoSequences(IExtendedSequence first, IExtendedSequence second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean execute(AbstractGameState gs) {
            gs.setActionInProgress(first);
            gs.setActionInProgress(second);
            return true;
        }

        @Override
        public PushesTwoSequences copy() {
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String getString(AbstractGameState gameState) {
            return "PushesTwoSequences";
        }
    }
}
