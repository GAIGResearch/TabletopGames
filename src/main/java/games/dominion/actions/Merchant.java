package games.dominion.actions;

import core.actions.AbstractAction;
import core.components.Deck;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

public class Merchant extends DominionAction {
    public Merchant(int playerId) {
        super(CardType.MERCHANT, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.drawCard(player);
        state.changeActions(1);
        state.addDelayedAction(new MerchantBuyEffect(player));
        return true;
    }

    @Override
    public AbstractAction copy() {
        // immutable class
        return this;
    }

}


class MerchantBuyEffect implements IDelayedAction {

    final int player;

    public MerchantBuyEffect(int player) {
        this.player = player;
    }

    @Override
    public void execute(DominionGameState state) {
        if (state.getCurrentPlayer() != player) {
            throw new AssertionError("Should only be executed when current player if " + player);
        }
        Deck<DominionCard> hand = state.getDeck(DominionConstants.DeckType.HAND, player);
        if (hand.stream().anyMatch(c -> c.cardType() == CardType.SILVER)) {
            state.changeAdditionalSpend(1);
        }
    }

    @Override
    public DominionConstants.TriggerType getTrigger() {
        return DominionConstants.TriggerType.StartBuy;
    }

    @Override
    public IDelayedAction copy() {
        // no mutable state
        return this;
    }
}
