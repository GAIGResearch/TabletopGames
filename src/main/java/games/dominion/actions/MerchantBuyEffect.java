package games.dominion.actions;

import core.components.Deck;
import games.dominion.*;
import games.dominion.cards.*;

public class MerchantBuyEffect implements IBuyPhaseEffect {

    @Override
    public boolean apply(DominionGameState state) {
        Deck<DominionCard> hand = state.getDeck(DominionConstants.DeckType.HAND, state.getCurrentPlayer());
        if (hand.stream().anyMatch(c -> c.cardType() == CardType.SILVER)) {
            state.changeAdditionalSpend(1);
        }
        return true;
    }
}
