package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.toads.ToadConstants;
import games.toads.ToadGameState;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AssaultCannonInterrupt implements IExtendedSequence {

    final int player;
    boolean complete = false;

    public AssaultCannonInterrupt(int player) {
        this.player = player;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ToadGameState toadState = (ToadGameState) state;
        // we filter out the known card that is our tiebreaker, plus the known cards in the opponent's discard pile
        List<ForceOpponentDiscard> retValue = toadState.getCardTypesInPlay().stream()
                .filter(a -> toadState.getTieBreaker(player) == null || toadState.getTieBreaker(player).type != a)
                .filter(a -> toadState.getDiscards(1 - player).stream().noneMatch(b -> b.type == a))
                .map(ForceOpponentDiscard::new)
                .collect(Collectors.toList());
        retValue.add(new ForceOpponentDiscard(ToadConstants.ToadCardType.NONE_OF_THESE));
        return retValue.stream().map(a -> (AbstractAction) a).toList();
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        complete = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public AssaultCannonInterrupt copy() {
        AssaultCannonInterrupt retValue = new AssaultCannonInterrupt(player);
        retValue.complete = complete;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AssaultCannonInterrupt && ((AssaultCannonInterrupt) obj).player == player;
    }

    @Override
    public int hashCode() {
        return player - 30334 + (complete ? 3330 : 0);
    }

    @Override
    public String toString() {
        return "Assault Cannon Interrupt for player " + player;
    }
}
