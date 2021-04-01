package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;

import java.awt.*;

public class ResourceRequirement implements Requirement<TMGameState> {

    TMTypes.Resource resource;
    int amount;
    boolean production;
    int player;
    int cardID;

    public ResourceRequirement(TMTypes.Resource resource, int amount, boolean production, int player, int cardID) {
        this.resource = resource;
        this.amount = amount;
        this.production = production;
        this.player = player;
        this.cardID = cardID;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        if (amount == 0) return true;

        TMCard card = null;
        if (cardID != -1) {
            card = (TMCard) gs.getComponentById(cardID);
        }
        if (player == -1) {
            player = gs.getCurrentPlayer();
        } else if (player == -2) {
            // Can any player pay?
            for (int i = 0; i < gs.getNPlayers(); i++) {
                if (gs.canPlayerPay(i, card, null, resource, amount, production)) return true;
            }
            return false;
        }
        return gs.canPlayerPay(player, card, null, resource, amount, production);
    }

    @Override
    public boolean isMax() {
        return false;
    }

    @Override
    public boolean appliesWhenAnyPlayer() {
        return false;
    }

    @Override
    public String getDisplayText(TMGameState gs) {
        return null;
    }

    @Override
    public Image[] getDisplayImages() {
        return null;
    }
}
