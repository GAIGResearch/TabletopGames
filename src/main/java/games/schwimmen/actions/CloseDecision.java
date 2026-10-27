package games.schwimmen.actions;

import core.actions.OneShotExtendedAction;

import java.util.List;

/**
 * After exchanging or passing, the player decides whether to close.
 */
public class CloseDecision extends OneShotExtendedAction {

    public CloseDecision(int player) {
        super("CloseDecision", player, state -> List.of(new Close(true), new Close(false)));
    }

    @Override
    public CloseDecision copy() {
        CloseDecision copy = new CloseDecision(player);
        copy.executed = executed;
        return copy;
    }
}
