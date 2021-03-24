package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.*;

public class AddResourceOnCard extends TMAction implements IExtendedSequence {
    public final TMTypes.Resource resource;
    public final int amount;  // Can be negative for removing resources

    public boolean chooseAny;  // if true, can choose cards that take resources from any player, otherwise own ones
    public TMTypes.Tag tagRequirement;  // tag target card must have
    public int minResRequirement;

    public AddResourceOnCard(int player, int cardID, TMTypes.Resource resource, int amount, boolean free) {
        super(player, free);
        this.cardID = cardID;
        this.resource = resource;
        this.amount = amount;

        if (amount < 0) {
            this.costResource = resource;
            this.cost = Math.abs(amount);
        }
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        if (cardID != -1) {
            TMGameState gs = (TMGameState) gameState;
            TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();
            TMCard card = (TMCard) gs.getComponentById(cardID);
            card.nResourcesOnCard += amount;
            return super.execute(gs);
        }
        gameState.setActionInProgress(this);
        if (player == -1) player = gameState.getCurrentPlayer();
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        TMGameState gs = (TMGameState) state;
        if (chooseAny) {
            for (int i = 0; i < state.getNPlayers(); i++) {
                addDeckActions(actions, gs, i);
            }
        } else {
            addDeckActions(actions, gs, player);
        }
        if (actions.size() == 0) {
            actions.add(new TMAction(player));  // Pass, can't do any legal actions
        }
        return actions;
    }

    private void addDeckActions(List<AbstractAction> actions, TMGameState gs, int player) {
        for (TMCard card: gs.getPlayerComplicatedPointCards()[player].getComponents()) {
            if (card.resourceOnCard != null) {
                if (resource != null && card.resourceOnCard == resource ||
                    resource == null && card.nResourcesOnCard > minResRequirement) {
                    if (amount > 0 || amount < 0 && card.nResourcesOnCard > amount) {
                        if (tagRequirement == null || contains(card.tags, tagRequirement)) {
                            actions.add(new AddResourceOnCard(player, card.getComponentID(), resource, amount, true));
                        }
                    }
                }
            }
        }
    }

    private boolean contains(TMTypes.Tag[] array, TMTypes.Tag object) {
        for (TMTypes.Tag t: array) {
            if (object == t) return true;
        }
        return false;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof AddResourceOnCard) {
            cardID = ((AddResourceOnCard) action).cardID;
        } else {
            cardID = 0;  // No cards to add resource to
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return cardID != -1;
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
        return amount == that.amount && chooseAny == that.chooseAny && minResRequirement == that.minResRequirement && resource == that.resource && tagRequirement == that.tagRequirement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resource, amount, chooseAny, tagRequirement, minResRequirement);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (cardID != -1) {
            TMCard card = (TMCard) gameState.getComponentById(cardID);
            return "Add " + amount + " " + resource + " on card " + card.getComponentName();
        }
        return "Add " + amount + " " + resource + " on card";
    }

    @Override
    public String toString() {
        return "Add " + amount + " " + resource + " on card";
    }

}
