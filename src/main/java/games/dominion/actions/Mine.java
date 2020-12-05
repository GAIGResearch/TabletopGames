package games.dominion.actions;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Mine extends DominionAction implements IExtendedSequence {

    boolean trashedCard;
    boolean gainedCard;
    int trashValue;

    public Mine(int playerId) {
        super(CardType.MINE, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        List<AbstractAction> retValue;
        if (!trashedCard) {
            retValue = state.getDeck(DominionConstants.DeckType.HAND, player).stream()
                    .filter(DominionCard::isTreasureCard)
                    .map(c -> new TrashCard(c.cardType(), player))
                    .distinct().collect(toList());
        } else if (!gainedCard) {
            retValue = state.cardsAvailable().stream()
                    .filter(c -> c.getTreasure() > 0 && c.getCost() <= trashValue + 3)
                    .map(c -> new GainCard(c, player, DominionConstants.DeckType.HAND))
                    .collect(toList());
        } else {
            throw new AssertionError("Should not be here if we have already both trashed and gained a card");
        }
        if (retValue.isEmpty()) {
            retValue.add(new DoNothing());
            trashedCard = true;
            gainedCard = true;
        }
        return retValue;
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        if (!trashedCard && action instanceof TrashCard && ((TrashCard) action).player == player) {
            trashedCard = true;
            trashValue = ((TrashCard) action).trashedCard.getCost();
        }
        if (!gainedCard && action instanceof GainCard && ((GainCard) action).buyingPlayer == player) {
            gainedCard = true;
        }
    }

    @Override
    public boolean executionComplete(DominionGameState state) {
        return trashedCard && gainedCard;
    }

    @Override
    public Mine copy() {
       Mine retValue = new Mine(player);
       retValue.gainedCard = gainedCard;
       retValue.trashedCard = trashedCard;
       retValue.trashValue = trashValue;
       return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Mine) {
            Mine other = (Mine) obj;
            return other.player == player
                    && other.trashValue == trashValue
                    && other.trashedCard == trashedCard
                    && other.gainedCard == gainedCard;
        }
        return false;
    }
}
