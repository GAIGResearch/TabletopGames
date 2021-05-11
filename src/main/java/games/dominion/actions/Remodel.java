package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Remodel extends DominionAction implements IExtendedSequence {

    CardType cardTrashed;
    CardType cardGained;

    public int BONUS_OVER_TRASHED_VALUE = 2;

    public Remodel(int playerId) {
        super(CardType.REMODEL, playerId);
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
        if (cardTrashed == null) {
            // Phase 1 - trash a card in hand
            List<DominionCard> cardsInHand = state.getDeck(DominionConstants.DeckType.HAND, player).stream().collect(toList());
            return cardsInHand.stream()
                    .map(card -> new TrashCard(card.cardType(), player))
                    .distinct()
                    .collect(toList());
        } else {
            //Phase 2 - gain a card costing up to 2 more
            int budget = cardTrashed.cost + BONUS_OVER_TRASHED_VALUE;
            return state.cardsToBuy().stream()
                    .filter(ct -> ct.cost <= budget)
                    .map(ct -> new GainCard(ct, player))
                    .collect(toList());
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
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
        Remodel retValue = new Remodel(player);
        retValue.cardGained = cardGained;
        retValue.cardTrashed = cardTrashed;
        return  retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Remodel) {
            Remodel other = (Remodel) obj;
            return other.player == player && other.cardTrashed == cardGained && other.cardGained == cardGained;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, cardGained, cardTrashed, CardType.REMODEL);
    }
}
