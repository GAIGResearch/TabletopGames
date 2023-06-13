package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.List;
import java.util.Objects;

import static games.dominion.DominionConstants.DeckType;
import static java.util.stream.Collectors.toList;

public class Chapel extends DominionAction implements IExtendedSequence {
    public Chapel(int playerId) {
        super(CardType.CHAPEL, playerId);
    }
    public Chapel(int playerId, boolean dummy) {
        super(CardType.CHAPEL, playerId, dummy);
    }

    boolean executed = false;

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    @Override
    public Chapel copy() {
        Chapel retValue = new Chapel(player, dummyAction);
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        // we can trash any card in hand, so create a TrashCard action for each
        List<DominionCard> cardsInHand = state.getDeck(DeckType.HAND, player).stream().collect(toList());
        List<AbstractAction> trashActions = cardsInHand.stream()
                .map(card -> new TrashCard(card.cardType(), player))
                .distinct()
                .collect(toList());
        // and then we can always choose to stop discarding
        trashActions.add(new DoNothing());
        return trashActions;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        // if the action is DoNothing, then we have stopped
        // else we continue discarding
        if (action instanceof DoNothing) {
            executed = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // Chapel is a purely personal sequence of actions - no reactions are needed
        return player;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Chapel) {
            Chapel other = (Chapel) obj;
            return super.equals(obj) && other.executed == executed;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + (executed ? 1 : 0);
    }
}
