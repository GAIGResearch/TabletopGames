package games.terraformingmars.actions;

import core.AbstractGameState;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.Objects;

// TODO: extended, "another" = yours, "any" = any player's
public class AddResourceOnCard extends TMAction {
    final int cardID;
    final TMTypes.Resource resource;
    final int amount;

    public AddResourceOnCard(int player, int cardID, TMTypes.Resource resource, int amount, boolean free) {
        super(TMTypes.ActionType.PlayCard, player, free);
        this.cardID = cardID;
        this.resource = resource;
        this.amount = amount;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();
        TMCard card = (TMCard) gs.getComponentById(cardID);
        card.resourceOnCard.put(resource, card.resourceOnCard.get(resource) + amount);
        return super.execute(gs);
    }

    @Override
    public AddResourceOnCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddResourceOnCard)) return false;
        if (!super.equals(o)) return false;
        AddResourceOnCard that = (AddResourceOnCard) o;
        return cardID == that.cardID && amount == that.amount && resource == that.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardID, resource, amount);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        TMCard card = (TMCard) gameState.getComponentById(cardID);
        return "Add " + amount + " " + resource + " on card " + card.getComponentName();
    }

    @Override
    public String toString() {
        return "Add " + amount + " " + resource + " on card";
    }

    public int getCardID() {
        return cardID;
    }
}
