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

    int executionCount = 0;
    CardType enthronedCard;

    @Override
    boolean _execute(DominionGameState state) {
        // No immediate effect, we simply move on to which card to enthrone
        if (state.getDeck(DeckType.HAND, player).stream().anyMatch(DominionCard::isActionCard)) {
            state.setActionInProgress(this);
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
            return state.getDeck(DeckType.HAND, player).stream()
                    .filter(DominionCard::isActionCard)
                    .map(c -> new EnthroneCard(c.cardType(), player, executionCount))
                    .distinct()
                    .collect(toList());
        } else {
            if (executionCount != 1)
                throw new AssertionError("Something has gone wrong with Throne Room");
            List<AbstractAction> retValue = new ArrayList<>();
            retValue.add(new EnthroneCard(enthronedCard, player, 1));
            retValue.add(new EnthroneCard(null, player, 1));
            return retValue;
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof EnthroneCard) {
            enthronedCard = ((EnthroneCard) action).enthronedCard;
            executionCount++;
            // The EnthroneCard does the rest
            if (executionCount > 2) {
                throw new AssertionError("Enthronement count should stop at 2");
            }
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executionCount == 2;
    }

    @Override
    public ThroneRoom copy() {
        ThroneRoom retValue = new ThroneRoom(player);
        retValue.enthronedCard = enthronedCard;
        retValue.executionCount = executionCount;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ThroneRoom) {
            ThroneRoom other = (ThroneRoom) obj;
            return other.player == player && other.enthronedCard == enthronedCard && other.executionCount == executionCount;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(CardType.THRONE_ROOM, executionCount, player, enthronedCard);
    }
}

