package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.*;
import games.dominion.cards.CardType;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Poacher extends DominionAction implements IExtendedSequence {
    public Poacher(int playerId) {
        super(CardType.POACHER, playerId);
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
    public List<AbstractAction> followOnActions(DominionGameState state) {
        // we can discard any card in hand
        List<AbstractAction> retValue = state.getDeck(DominionConstants.DeckType.HAND, player).stream()
                .map(c -> new DiscardCard(c.cardType(), player))
                .distinct().collect(toList());
        return retValue;
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        if (action instanceof DiscardCard && ((DiscardCard) action).player == player) {
            cardsDiscarded++;
        }
    }

    @Override
    public boolean executionComplete(DominionGameState state) {
        if (state.getDeck(DominionConstants.DeckType.HAND, player).getSize() == 0) {
            // highly unusual event - but could occur as an edge case with 2 empty supply decks, and
            // Poacher as the last card in hand
            cardsDiscarded = cardsToDiscard;
        }
        return cardsToDiscard == cardsDiscarded;
    }

    @Override
    public Poacher copy() {
        Poacher retValue = new Poacher(player);
        retValue.cardsDiscarded = cardsDiscarded;
        retValue.cardsToDiscard = cardsToDiscard;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Poacher) {
            Poacher other = (Poacher) obj;
            return other.player == player && other.cardsToDiscard == cardsToDiscard && other.cardsDiscarded == cardsDiscarded;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, cardsDiscarded, cardsToDiscard, CardType.POACHER);
    }
}
