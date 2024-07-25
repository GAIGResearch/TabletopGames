package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.dominion.*;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;

public class Cellar extends DominionAction implements IExtendedSequence {
    public Cellar(int playerId) {
        super(CardType.CELLAR, playerId);
    }
    public Cellar(int playerId, boolean dummy) {
        super(CardType.CELLAR, playerId, dummy);
    }

    int cardsDiscarded = 0;
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
    public Cellar copy() {
        Cellar retValue = new Cellar(player, dummyAction);
        retValue.cardsDiscarded = cardsDiscarded;
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        // we can discard any card in hand, so create a DiscardCard action for each
        List<AbstractAction> discardActions = state.getDeck(DeckType.HAND, player).stream()
                .map(card -> new DiscardCard(card.cardType(), player))
                .distinct()
                .collect(toList());
        // and then we can always choose to stop discarding
        discardActions.add(new DoNothing());
        return discardActions;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        DominionGameState state = (DominionGameState) gs;
        // if the action is DoNothing, then we have stopped
        // else we continue discarding
        if (action instanceof DoNothing) {
            for (int i = 0; i < cardsDiscarded; i++) {
                state.drawCard(player);
            }
            cardsDiscarded = 0;
            executed = true;
        }
        if (action instanceof DiscardCard)
            cardsDiscarded++;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // Cellar is a purely personal sequence of actions - no reactions are needed
        return player;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Cellar) {
            Cellar other = (Cellar) obj;
            return super.equals(obj) && other.cardsDiscarded == cardsDiscarded && other.executed == executed;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(executed, cardsDiscarded) + 31 * super.hashCode();
    }
}
