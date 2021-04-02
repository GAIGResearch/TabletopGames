package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;

import java.awt.*;
import java.util.Objects;

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
    public String getReasonForFailure(TMGameState gs) {
        return "Need " + amount + " " + resource + (production ? " production" : "");
    }

    @Override
    public Image[] getDisplayImages() {
        return null;
    }

    @Override
    public String toString() {
        return "Resource" + (production? " production" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceRequirement)) return false;
        ResourceRequirement that = (ResourceRequirement) o;
        return amount == that.amount && production == that.production && player == that.player && cardID == that.cardID && resource == that.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, amount, production, player, cardID);
    }
}
