package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.dominion.*;
import games.dominion.cards.CardType;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Poacher extends DominionAction implements IExtendedSequence {
    public Poacher(int playerId) {
        super(CardType.POACHER, playerId);
    }
    public Poacher(int playerId, boolean dummy) {
        super(CardType.POACHER, playerId, dummy);
    }

    int cardsDiscarded = 0;
    int cardsToDiscard = 0;

    @Override
    boolean _execute(DominionGameState state) {
        cardsToDiscard = (int) state.cardsIncludedInGame().stream()
                .filter(c -> state.cardsOfType(c, -1, DominionConstants.DeckType.SUPPLY) == 0)
                .count();
        if (cardsToDiscard > 0) {
            state.setActionInProgress(this);
        }
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        // we can discard any card in hand
        DominionGameState state = (DominionGameState) gs;
        return state.getDeck(DominionConstants.DeckType.HAND, player).stream()
                .map(c -> new DiscardCard(c.cardType(), player))
                .distinct().collect(toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof DiscardCard && ((DiscardCard) action).player == player) {
            cardsDiscarded++;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        if (state.getDeck(DominionConstants.DeckType.HAND, player).getSize() == 0) {
            // highly unusual event - but could occur as an edge case with 2 empty supply decks, and
            // Poacher as the last card in hand
            cardsDiscarded = cardsToDiscard;
        }
        return cardsToDiscard == cardsDiscarded;
    }

    @Override
    public Poacher copy() {
        Poacher retValue = new Poacher(player, dummyAction);
        retValue.cardsDiscarded = cardsDiscarded;
        retValue.cardsToDiscard = cardsToDiscard;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Poacher) {
            Poacher other = (Poacher) obj;
            return super.equals(obj) && other.cardsToDiscard == cardsToDiscard &&
                    other.cardsDiscarded == cardsDiscarded;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardsDiscarded, cardsToDiscard) + 31 * super.hashCode();
    }
}
