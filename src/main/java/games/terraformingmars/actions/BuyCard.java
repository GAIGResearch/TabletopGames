package games.terraformingmars.actions;

import core.AbstractGameState;
import core.components.Component;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

public class BuyCard extends TMAction {

    public BuyCard() { super(); } // This is needed for JSON Deserializer

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
                a.setCardID(card.getComponentID());
                gs.getPlayerExtraActions()[player].add(a);
            }
            if (card.firstAction != null) {
                card.firstAction.setCardID(card.getComponentID());
            }

            // Add discountEffects to player's discounts
            gs.addDiscountEffects(card.discountEffects);
            gs.addResourceMappings(card.resourceMappings, true);

            // Add persisting effects
            gs.addPersistingEffects(card.persistingEffects);

            // If solo and Tharsis Republic chosen, player gets the X extra money production from initial neutral cities placed
            if (gs.getNPlayers() == 1 && card.getComponentName().equals("Tharsis Republic")) {
                int current = gs.getPlayerProduction()[player].get(TMTypes.Resource.MegaCredit).getValue();
                gs.getPlayerProduction()[player].get(TMTypes.Resource.MegaCredit).setValue(current + ((TMGameParameters)gs.getGameParameters()).getSoloCities());
            }
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
