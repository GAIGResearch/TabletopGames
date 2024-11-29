package games.dominion.actions;

import core.AbstractGameState;
import core.actions.*;
import core.interfaces.IExtendedSequence;
import games.dominion.*;
import games.dominion.cards.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Mine extends DominionAction implements IExtendedSequence {

    boolean trashedCard;
    boolean gainedCard;
    int trashValue;

    public final int BONUS_OVER_TRASHED_VALUE = 3;

    public Mine(int playerId) {
        super(CardType.MINE, playerId);
    }

    public Mine(int playerId, boolean dummy) {
        super(CardType.MINE, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
        if (state.getDeck(DominionConstants.DeckType.HAND, player).stream().anyMatch(DominionCard::isTreasureCard)) {
            state.setActionInProgress(this);
            return true;
        }
        trashedCard = true;
        gainedCard = true;
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        List<AbstractAction> retValue;
        if (!trashedCard) {
            retValue = state.getDeck(DominionConstants.DeckType.HAND, player).stream()
                    .filter(DominionCard::isTreasureCard)
                    .map(c -> new TrashCard(c.cardType(), player))
                    .distinct().collect(toList());
        } else if (!gainedCard) {
            retValue = state.getCardsToBuy().stream()
                    .filter(c -> c.isTreasure && c.cost <= trashValue + BONUS_OVER_TRASHED_VALUE)
                    .map(c -> new GainCard(c, player, DominionConstants.DeckType.HAND))
                    .collect(toList());
        } else {
            throw new AssertionError("Should not be here if we have already both trashed and gained a card");
        }
        if (retValue.isEmpty()) {
            throw new AssertionError("We should always be able to gain a COPPER");
        }
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        DominionGameState state = (DominionGameState) gs;
        if (!trashedCard && action instanceof TrashCard && ((TrashCard) action).player == player) {
            trashedCard = true;
            trashValue = ((TrashCard) action).trashedCard.cost;
            if (state.getCardsToBuy().stream().noneMatch(c -> c.isTreasure && c.cost <= trashValue + BONUS_OVER_TRASHED_VALUE))
                gainedCard = true; // there are no valid cards to gain, so we skip the next decision
            // this is rare, but can happen if SILVER is exhausted with random players, say
        }
        if (!gainedCard && action instanceof GainCard && ((GainCard) action).buyingPlayer == player) {
            gainedCard = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return trashedCard && gainedCard;
    }

    @Override
    public Mine copy() {
        Mine retValue = new Mine(player, dummyAction);
        retValue.gainedCard = gainedCard;
        retValue.trashedCard = trashedCard;
        retValue.trashValue = trashValue;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Mine) {
            Mine other = (Mine) obj;
            return super.equals(obj)
                    && other.trashValue == trashValue
                    && other.trashedCard == trashedCard
                    && other.gainedCard == gainedCard;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(trashedCard, trashValue, gainedCard) + 31 * super.hashCode();
    }
}
