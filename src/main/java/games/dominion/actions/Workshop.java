package games.dominion.actions;

import core.AbstractGameState;
import core.actions.*;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionGameState;
import games.dominion.cards.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Workshop extends DominionAction implements IExtendedSequence {

    public final int COST_OF_GAINED_CARD = 4;

    boolean executed;

    public Workshop(int playerId) {
        super(CardType.WORKSHOP, playerId);
    }
    public Workshop(int playerId, boolean dummy) {
        super(CardType.WORKSHOP, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        List<AbstractAction> retValue = state.getCardsToBuy().stream()
                .filter(c -> c.cost <= COST_OF_GAINED_CARD)
                .map(c -> new GainCard(c, state.getCurrentPlayer()))
                .collect(toList());
        if (retValue.isEmpty()) {
            retValue.add(new DoNothing());
        }
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof GainCard && ((GainCard) action).buyingPlayer == player)
            executed = true;

        // There are no cards that can be obtained with the workshop
        else if (action instanceof DoNothing) {
            executed = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    public Workshop copy() {
        Workshop retValue = new Workshop(player, dummyAction);
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Workshop) {
            Workshop other = (Workshop) obj;
            return executed == other.executed && super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + (executed ? 1 : 0);
    }
}
