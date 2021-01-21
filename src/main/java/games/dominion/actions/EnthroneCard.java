package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.Objects;

public class EnthroneCard extends AbstractAction {

    final CardType enthronedCard;
    final int player;
    final int cardExecutions;

    public EnthroneCard(CardType card, int playerId, int executionsSoFar) {
        enthronedCard = card;
        player = playerId;
        cardExecutions = executionsSoFar;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        if (cardExecutions == 0) {
            // this is the first Enthrone action, so we move a card from hand to the tableau
            if (!state.moveCard(enthronedCard, player, DominionConstants.DeckType.HAND, player, DominionConstants.DeckType.TABLE)) {
                throw new AssertionError("No such card in hand to be Enthroned : " + enthronedCard);
            }
        }
        if (enthronedCard != null) {
            DominionCard card = DominionCard.create(enthronedCard);
            card.getAction(player).executeCoreCardTypeFunctionality(state);
            card.getAction(player)._execute(state);
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        // immutable state only
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EnthroneCard) {
            EnthroneCard other = (EnthroneCard) obj;
            return other.enthronedCard == enthronedCard && other.cardExecutions == cardExecutions && other.player == player;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enthronedCard, player, cardExecutions);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return enthronedCard != null ? "Play " + enthronedCard + " using Throne Room" : "Do not use the second action";
    }
}
