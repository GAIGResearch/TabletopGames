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

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        List<AbstractAction> retValue = state.cardsToBuy().stream()
                .filter(c -> c.cost <= COST_OF_GAINED_CARD)
                .map(c -> new GainCard(c, state.getCurrentPlayer()))
                .collect(toList());
        if (retValue.isEmpty()) {
            retValue.add(new DoNothing());
            executed = true;
        }
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof GainCard && ((GainCard) action).buyingPlayer == player)
            executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    public Workshop copy() {
        Workshop retValue = new Workshop(player);
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Workshop) {
            Workshop other = (Workshop) obj;
            return executed == other.executed && player == other.player;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, executed, CardType.WORKSHOP);
    }
}
