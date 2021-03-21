package games.terraformingmars.actions;

import core.AbstractGameState;
import core.components.Counter;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.Objects;


public class BuyCard extends TMAction {
    final int cardIdx;

    public BuyCard(int player, int cardIdx, boolean free) {
        super(player, free);
        this.cardIdx = cardIdx;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();

        TMCard card = gs.getPlayerCardChoice()[player].pick(cardIdx);
        if (card.cardType == TMTypes.CardType.Corporation) {
            // 1 card chosen, the rest are discarded
            gs.getPlayerCorporations()[player] = card;
            gs.getPlayerCardChoice()[player].clear();

            // Execute immediate effect of corporation (starting bonus)
            for (TMAction aa: card.immediateEffects) {
                aa.player = player;
                aa.execute(gs);
            }
            // Add actions
            for (TMAction a: card.actions) {
                a.player = player;
                gs.getPlayerCardsPlayedActions()[player].add(a);
            }

            // Add discountEffects to player's discounts
            gs.addDiscountEffects(card.discountEffects);
            gs.addResourceMappings(card.resourceMappings, true);

            // Add persisting effects
            gs.addPersistingEffects(card.persistingEffects);

            return super.execute(gs);
        } else {
            Counter c = gs.getPlayerResources()[player].get(TMTypes.Resource.MegaCredit);
            // TODO: maybe allow use of other resources
            if (c.getValue() >= gp.getProjectPurchaseCost()) {
                gs.getPlayerHands()[player].add(card);
                c.decrement(gp.getProjectPurchaseCost());
                return super.execute(gs);
            } else {
                // Can't pay for it, discard instead
                gs.getDiscardCards().add(card);
                super.execute(gs);
                return false;
            }
        }
    }

    @Override
    public TMAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuyCard)) return false;
        if (!super.equals(o)) return false;
        BuyCard buyCard = (BuyCard) o;
        return cardIdx == buyCard.cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIdx);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Buy card idx " + cardIdx;
    }

    @Override
    public String toString() {
        return "Buy card idx " + cardIdx;
    }
}
