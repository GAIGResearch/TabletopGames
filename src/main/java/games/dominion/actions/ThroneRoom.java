package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionGameState;
import games.dominion.cards.*;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;

public class ThroneRoom extends DominionAction implements IExtendedSequence {
    public ThroneRoom(int playerId) {
        super(CardType.THRONE_ROOM, playerId);
    }

    public ThroneRoom(int playerId, boolean dummy) {
        super(CardType.THRONE_ROOM, playerId, dummy);
    }

    int executionCount = 0;
    CardType enthronedCard;

    @Override
    boolean _execute(DominionGameState state) {
        // No immediate effect, we simply move on to which card to enthrone
        if (state.getDeck(DeckType.HAND, player).stream().anyMatch(DominionCard::isActionCard)) {
            state.setActionInProgress(this);
            state.changeActions(1); // and do not count the action
            return true;
        }
        return false; // no action cards in hand
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        if (enthronedCard == null) {
            if (executionCount != 0)
                throw new AssertionError("Something has gone wrong with Throne Room");
            List<AbstractAction> options = state.getDeck(DeckType.HAND, player).stream()
                    .filter(DominionCard::isActionCard)
                    .map(c -> DominionCard.create(c.cardType()).getAction(player))
                    .distinct()
                    .collect(toList());
            if (options.isEmpty()) {
                // this is possible if we throne room a throne room (etc.) and then have no action cards
                executionCount = 2; // and we are done
                options.add(new EndPhase(DominionGameState.DominionGamePhase.Play));
            }
            return options;
        } else {
            if (executionCount != 1)
                throw new AssertionError("Something has gone wrong with Throne Room");
            return Collections.singletonList(DominionCard.create(enthronedCard).getAction(player, true));
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof EndPhase) {
            executionCount = 2;
            return;
        }
        DominionAction da = (DominionAction) action;
        if (enthronedCard == null) {
            enthronedCard = da.type;
        }
        if (enthronedCard == da.type) {
            executionCount++;
        } else
            throw new AssertionError("Enthrone action should be the same as the one selected");
        if (executionCount > 2) {
            throw new AssertionError("Enthronement count should stop at 2");
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executionCount == 2;
    }

    @Override
    public ThroneRoom copy() {
        ThroneRoom retValue = new ThroneRoom(player, dummyAction);
        retValue.enthronedCard = enthronedCard;
        retValue.executionCount = executionCount;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ThroneRoom) {
            ThroneRoom other = (ThroneRoom) obj;
            return super.equals(obj) && other.enthronedCard == enthronedCard && other.executionCount == executionCount;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionCount, enthronedCard) + 31 * super.hashCode();
    }
}

