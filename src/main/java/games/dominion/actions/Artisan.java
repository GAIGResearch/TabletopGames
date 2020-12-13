package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;

public class Artisan extends DominionAction implements IExtendedSequence {
    public Artisan(int playerId) {
        super(CardType.ARTISAN, playerId);
    }

    public boolean gainedCard;
    public boolean putCardOnDeck;

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        if (!gainedCard) {
            return state.cardsToBuy().stream()
                    .filter(c -> c.cost <= 5)
                    .map(c -> new GainCard(c, player, DeckType.HAND))
                    .collect(toList());
        } else {
            return state.getDeck(DeckType.HAND, player).stream()
                    .map(c -> new MoveCard(c.cardType(), player, DeckType.HAND, player, DeckType.DRAW))
                    .distinct()
                    .collect(toList());
        }
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        if (action instanceof GainCard && ((GainCard) action).buyingPlayer == player)
            gainedCard = true;
        if (action instanceof MoveCard && ((MoveCard) action).playerFrom == player)
            putCardOnDeck = true;
    }

    @Override
    public boolean executionComplete(DominionGameState state) {
        return gainedCard && putCardOnDeck;
    }

    @Override
    public Artisan copy() {
        Artisan retValue = new Artisan(player);
        retValue.putCardOnDeck = putCardOnDeck;
        retValue.gainedCard = gainedCard;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Artisan) {
            Artisan other = (Artisan) obj;
            return other.gainedCard == gainedCard && other.putCardOnDeck == putCardOnDeck && other.player == player;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gainedCard, putCardOnDeck, player, CardType.ARTISAN);
    }
}
