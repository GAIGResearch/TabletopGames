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
    public Merchant(int playerId, boolean dummy) {
        super(CardType.MERCHANT, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
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
    public final int SPEND_PER_CARD = 1;
    public final CardType BONUS_SPEND_CARD_TYPE = CardType.SILVER;

    public MerchantBuyEffect(int player) {
        this.player = player;
    }

    @Override
    public void execute(DominionGameState state) {
        if (state.getCurrentPlayer() != player) {
            throw new AssertionError("Should only be executed when current player is " + player);
        }
        Deck<DominionCard> hand = state.getDeck(DominionConstants.DeckType.HAND, player);
        if (hand.stream().anyMatch(c -> c.cardType() == BONUS_SPEND_CARD_TYPE)) {
            state.changeAdditionalSpend(SPEND_PER_CARD);
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
