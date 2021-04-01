package games.terraformingmars.actions;

import core.AbstractGameState;
import core.components.Counter;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

public class BuyCard extends TMAction {

    public BuyCard(int player, int cardID, int cost) {
        super(player, true);
        this.setActionCost(TMTypes.Resource.MegaCredit, cost, -1);
        this.setCardID(cardID);
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();

        TMCard card = (TMCard) gs.getComponentById(getCardID());
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
                gs.getPlayerExtraActions()[player].add(a);
            }

            // Add discountEffects to player's discounts
            gs.addDiscountEffects(card.discountEffects);
            gs.addResourceMappings(card.resourceMappings, true);

            // Add persisting effects
            gs.addPersistingEffects(card.persistingEffects);
        } else {
            Counter c = gs.getPlayerResources()[player].get(TMTypes.Resource.MegaCredit);
            gs.getPlayerHands()[player].add(card);
            c.decrement(gp.getProjectPurchaseCost());
        }
        gs.getPlayerCardChoice()[player].remove(card);
        return super.execute(gs);
    }

    @Override
    public TMAction copy() {
        return this;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Buy " + gameState.getComponentById(getCardID()).getComponentName();
    }

    @Override
    public String toString() {
        return "Buy card id " + getCardID();
    }

}
