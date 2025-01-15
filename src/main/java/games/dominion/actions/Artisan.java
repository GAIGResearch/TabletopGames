package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;

public class Artisan extends DominionAction implements IExtendedSequence {
    public Artisan(int playerId) {
        super(CardType.ARTISAN, playerId);
    }
    public Artisan(int playerId, boolean dummy) {
        super(CardType.ARTISAN, playerId, dummy);
    }

    public final int MAX_COST_OF_GAINED_CARD = 5;

    public boolean gainedCard;
    public boolean putCardOnDeck;

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        if (!gainedCard) {
            return state.getCardsToBuy().stream()
                    .filter(c -> c.cost <= MAX_COST_OF_GAINED_CARD)
                    .map(c -> new GainCard(c, player, DeckType.HAND))
                    .collect(toList());
        } else {
            return state.getDeck(DeckType.HAND, player).stream()
                    .map(c -> new MoveCard(c.cardType(), player, DeckType.HAND, player, DeckType.DRAW, false))
                    .distinct()
                    .collect(toList());
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof GainCard && ((GainCard) action).buyingPlayer == player)
            gainedCard = true;
        if (action instanceof MoveCard && ((MoveCard) action).playerFrom == player)
            putCardOnDeck = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return gainedCard && putCardOnDeck;
    }

    @Override
    public Artisan copy() {
        Artisan retValue = new Artisan(player, dummyAction);
        retValue.putCardOnDeck = putCardOnDeck;
        retValue.gainedCard = gainedCard;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Artisan) {
            Artisan other = (Artisan) obj;
            return other.gainedCard == gainedCard && other.putCardOnDeck == putCardOnDeck && super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gainedCard, putCardOnDeck) + 31 * super.hashCode();
    }
}
