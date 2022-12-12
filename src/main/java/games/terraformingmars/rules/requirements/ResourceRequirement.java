package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;

import java.awt.*;
import java.util.Objects;

public class ResourceRequirement implements Requirement<TMGameState> {

    final TMTypes.Resource resource;
    final int amount;
    final boolean production;
    final int cardID;
    final int player;

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
        int p = player;
        if (p == -1) {
            p = gs.getCurrentPlayer();
        } else if (p == -2) {
            // Can any player pay?
            if (resource == TMTypes.Resource.Card) {
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    if (gs.getPlayerHands()[i].getSize() >= amount) return true;
                }
            } else {
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    if (gs.canPlayerPay(i, card, null, resource, amount, production)) return true;
                }
            }
            return gs.getNPlayers() == 1;  // In solo play this is always true
        }
        if (resource == TMTypes.Resource.Card) {
            return gs.getPlayerHands()[p].getSize() >= amount;
        }
        return gs.canPlayerPay(p, card, null, resource, amount, production);
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
    public ResourceRequirement copy() {
        return this;
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
        return amount == that.amount && production == that.production && cardID == that.cardID && player == that.player && resource == that.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, amount, production, cardID, player);
    }
}
