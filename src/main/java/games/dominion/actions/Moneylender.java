package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;


import static games.dominion.DominionConstants.*;

public class Moneylender extends DominionAction {

    public Moneylender(int playerId) {
        super(CardType.MONEYLENDER, playerId);
    }
    public Moneylender(int playerId, boolean dummy) {
        super(CardType.MONEYLENDER, playerId, dummy);
    }

    public final int BONUS_MONEY_FOR_TRASHING = 3;
    public final CardType TRASHABLE_CARD_TYPE = CardType.COPPER;

    /**
     * Technically this should permit the player to optionally Trash a COPPER for +3 spend
     * This implementation makes it obligatory to simplify the code and avoid implementing IExtendedSequence
     *
     * TODO: I can't see any cards for a while that will make ablind bit of difference to this. (It could make a difference
     * once we get cards that play off the number of actions already played, at which time this will need to be corrected)
     *
     * @param state
     * @return
     */
    @Override
    boolean _execute(DominionGameState state) {
        if (state.getDeck(DeckType.HAND, player).stream().anyMatch(c -> c.cardType() == TRASHABLE_CARD_TYPE)) {
            (new TrashCard(TRASHABLE_CARD_TYPE, player)).execute(state);
            state.changeAdditionalSpend(BONUS_MONEY_FOR_TRASHING);
        }
        return true;
    }


    @Override
    public AbstractAction copy() {
        // no mutable state
        return this;
    }
}
