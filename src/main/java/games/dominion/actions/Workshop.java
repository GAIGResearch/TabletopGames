package games.dominion.actions;

import core.AbstractGameState;
import core.actions.*;
import games.dominion.DominionGameState;
import games.dominion.cards.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Workshop extends DominionAction implements IExtendedSequence {

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
    public List<AbstractAction> followOnActions(DominionGameState state) {
        List<AbstractAction> retValue = state.cardsAvailable().stream()
                .filter(c -> c.getCost() <= 4)
                .map(c -> new GainCard(c, state.getCurrentPlayer()))
                .collect(toList());
        if (retValue.isEmpty()) {
            retValue.add(new DoNothing());
            executed = true;
        }
        return retValue;
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        if (action instanceof GainCard && ((GainCard) action).buyingPlayer == player)
            executed = true;
    }

    @Override
    public boolean executionComplete(DominionGameState state) {
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
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, executed, CardType.WORKSHOP);
    }
}
