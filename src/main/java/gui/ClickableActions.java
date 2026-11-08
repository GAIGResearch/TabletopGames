package gui;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.human.ActionController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The actions a human player may choose by clicking on a game's views, rather than on the action buttons.
 * <p>
 * The GUI manager offers the forward model's legal actions once per GUI tick, from its updateActionButtons (which runs
 * only when the actions are to be shown), and withdraws them otherwise. Views then look up what a click or a hover
 * refers to with {@link #matching} and submit one of those actions with {@link #submit}. Every submitted action is
 * one the forward model offered, so a view never builds an action itself or repeats a rule.
 * <p>
 * Everything here runs on the Swing event thread: the GUI ticks and the mouse handlers both do.
 */
public class ClickableActions {

    private final ActionController ac;
    private final Set<Integer> humanPlayers;
    private List<AbstractAction> offered = List.of();
    private int player = -1;

    public ClickableActions(ActionController ac, Set<Integer> humanPlayers) {
        this.ac = ac;
        this.humanPlayers = humanPlayers;
    }

    /**
     * Offers the actions for clicking, if the player they were computed for is a human; otherwise withdraws them.
     *
     * @param playerId the player being asked for an action (the player passed to update). With simultaneous moves
     *                 this need not be the state's current player, so it is never taken from the state.
     */
    public void offer(AbstractGameState state, List<AbstractAction> actions, int playerId) {
        if (state.isNotTerminal() && humanPlayers.contains(playerId)) {
            offered = List.copyOf(actions);
            player = playerId;
        } else {
            withdraw();
        }
    }

    /**
     * Nothing can be chosen by clicking until the next offer.
     */
    public void withdraw() {
        offered = List.of();
        player = -1;
    }

    /**
     * Whether a human player may choose an action by clicking now.
     */
    public boolean isOffered() {
        return !offered.isEmpty();
    }

    /**
     * The human player who may click, or -1 when nothing is offered.
     */
    public int player() {
        return player;
    }

    /**
     * The offered actions that pass the test (none when nothing is offered).
     */
    public List<AbstractAction> matching(Predicate<? super AbstractAction> test) {
        List<AbstractAction> result = new ArrayList<>();
        for (AbstractAction a : offered)
            if (test.test(a))
                result.add(a);
        return result;
    }

    /**
     * The offered actions of the given class that pass the test.
     */
    public <A extends AbstractAction> List<A> matching(Class<A> type, Predicate<? super A> test) {
        List<A> result = new ArrayList<>();
        for (AbstractAction a : offered)
            if (type.isInstance(a) && test.test(type.cast(a)))
                result.add(type.cast(a));
        return result;
    }

    /**
     * Chooses the action, if it is one of those offered, and withdraws the rest so that a second click cannot submit
     * another. Returns whether it was submitted.
     */
    public boolean submit(AbstractAction action) {
        if (action == null || !offered.contains(action))
            return false;
        ac.addAction(action);
        withdraw();
        return true;
    }
}
