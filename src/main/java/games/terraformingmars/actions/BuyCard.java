package games.terraformingmars.actions;

import core.AbstractGameState;
import core.components.Component;
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
    public boolean _execute(TMGameState gs) {
        TMGameParameters gp = (TMGameParameters) gs.getGameParameters();

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
            gs.getPlayerHands()[player].add(card);
            gs.getPlayerCardChoice()[player].remove(card);
        }
        return true;
    }

    @Override
    public BuyCard _copy() {
        return new BuyCard(player, getCardID(), getCost());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Component c = gameState.getComponentById(getCardID());
        return "Buy " + c.getComponentName();
    }

    @Override
    public String toString() {
        return "Buy card id " + getCardID();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BuyCard)) return false;
        return super.equals(o);
    }
}
