package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class Remodel extends DominionAction implements IExtendedSequence {

    public int BONUS_OVER_TRASHED_VALUE = 2;
    CardType cardTrashed;
    CardType cardGained;

    public Remodel(int playerId) {
        super(CardType.REMODEL, playerId);
    }
    public Remodel(int playerId, boolean dummy) {
        super(CardType.REMODEL, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
        if (state.getDeck(DominionConstants.DeckType.HAND, player).getSize() == 0) {
            // this is to indicate we are finished...if we do not trash anything, we cannot buy anything
        } else {
            state.setActionInProgress(this);
        }
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        // We get a list of all cards in Hand. We must trash one of them (unless we have none, in which case we DoNothing)
        List<AbstractAction> retValue = new ArrayList<>();
        if (cardTrashed == null) {
            // Phase 1 - trash a card in hand
            List<DominionCard> cardsInHand = state.getDeck(DominionConstants.DeckType.HAND, player).stream().collect(toList());
            retValue = cardsInHand.stream()
                    .map(card -> new TrashCard(card.cardType(), player))
                    .distinct()
                    .collect(toList());
        } else {
            //Phase 2 - gain a card costing up to 2 more
            int budget = cardTrashed.cost + BONUS_OVER_TRASHED_VALUE;
            retValue = state.getCardsToBuy().stream()
                    .filter(ct -> ct.cost <= budget)
                    .map(ct -> new GainCard(ct, player))
                    .collect(toList());
        }
        if (retValue.isEmpty())
            retValue.add(new DoNothing());  // either because we have nothing in hand, or because there is nothing to buy
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof TrashCard) {
            TrashCard tc = (TrashCard) action;
            if (tc.player == player)
                cardTrashed = tc.trashedCard;
        }
        if (action instanceof GainCard) {
            GainCard bc = (GainCard) action;
            if (bc.buyingPlayer == player)
                cardGained = bc.cardType;
        }
        if (action instanceof DoNothing) {
            if (cardTrashed == null) {
                cardTrashed = CardType.COPPER;
                cardGained = CardType.COPPER;
            }
            if (cardGained == null)
                cardGained = CardType.COPPER;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return cardTrashed != null && cardGained != null;
    }

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    @Override
    public Remodel copy() {
        Remodel retValue = new Remodel(player, dummyAction);
        retValue.cardGained = cardGained;
        retValue.cardTrashed = cardTrashed;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Remodel) {
            Remodel other = (Remodel) obj;
            return super.equals(obj) && other.cardTrashed == cardGained && other.cardGained == cardGained;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardGained, cardTrashed) + 31 * super.hashCode();
    }
}
