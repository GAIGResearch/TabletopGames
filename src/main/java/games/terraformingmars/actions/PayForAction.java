package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.*;

// Wrapper class for actions that need to be paid with resources before execution
public class PayForAction extends TMAction implements IExtendedSequence {
    public final TMAction action;

    int costPaid;
    int stage;
    TMTypes.Resource[] resourcesToPayWith;

    public PayForAction(TMTypes.ActionType type, int player, TMAction action, TMTypes.Resource resourceToPay, int costTotal, int cardID) {
        super(type, player, true);
        this.action = action;
        this.cost = Math.abs(costTotal);
        this.costResource = resourceToPay;
        this.cardID = cardID;  // -1 if no card needed
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();

        // Do extended sequence
        // Pay for card with resources until all paid
        // Second: execute action

        if (this.player == -1) player = gs.getCurrentPlayer();

        TMCard card = null;
        if (cardID > -1) {
            card = (TMCard) gs.getComponentById(cardID);
            cost -= gs.discountCardCost(card, player);  // Apply card discount
        } else {
            // Check action type discounts
            cost -= gs.discountActionTypeCost(this.action, player);
        }
        HashSet<TMTypes.Resource> resources = gs.canPlayerTransform(player, card, null, costResource);
        resources.add(costResource);  // Can always pay with itself

        resourcesToPayWith = resources.toArray(new TMTypes.Resource[0]);
        stage = 0;
        costPaid = 0;
        gameState.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        TMGameState gs = (TMGameState) state;
        TMTypes.Resource res = resourcesToPayWith[stage];
        // Find minimum that must be spent of this resource so that the card is still payable with remaining resources
        HashSet<TMTypes.Resource> resourcesRemaining = new HashSet<>(Arrays.asList(resourcesToPayWith).subList(stage + 1, resourcesToPayWith.length));

        TMCard card = (TMCard) gs.getComponentById(cardID);
        int sum = gs.playerResourceSum(player, card, resourcesRemaining, costResource);
        int remaining = cost - costPaid - sum;
        int min = Math.max(0, (int)(Math.ceil(remaining/gs.getResourceMapRate(res, costResource))));
        int max = Math.min(gs.getPlayerResources()[player].get(res).getValue(), (int)(Math.ceil((cost - costPaid)/gs.getResourceMapRate(res, costResource))));

        // Can pay between min and max of this resource
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            actions.add(new ModifyPlayerResource(player, -i, res, false));
        }
        if (actions.size() == 0) {
            actions.add(new TMAction(player));  // TODO: bad, this shouldn't happen
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (! (action instanceof ModifyPlayerResource)) {
            stage = resourcesToPayWith.length;  // TODO: bad, this shouldn't happen
            this.action.execute(state);
            return;
        }
        TMGameState gs = (TMGameState) state;
        TMTypes.Resource res = resourcesToPayWith[stage];
        costPaid += Math.abs(((ModifyPlayerResource)action).change) * gs.getResourceMapRate(res, costResource);
        stage++;
        TMCard card = (TMCard) gs.getComponentById(cardID);
        if (card != null && ((TMGameState)state).isCardFree(card, costPaid, player) || costPaid >= cost) {
            // Action paid for, execute
            this.action.execute(state);
            stage = resourcesToPayWith.length;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return stage == resourcesToPayWith.length || costPaid == cost;
    }

    @Override
    public PayForAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PayForAction)) return false;
        if (!super.equals(o)) return false;
        PayForAction that = (PayForAction) o;
        return costPaid == that.costPaid && stage == that.stage && Objects.equals(action, that.action) && Arrays.equals(resourcesToPayWith, that.resourcesToPayWith);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), action, costPaid, stage);
        result = 31 * result + Arrays.hashCode(resourcesToPayWith);
        return result;
    }

    @Override
    public String toString() {
        return "Pay " + cost + " " + costResource + " for " + action.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pay " + cost + " " + costResource + " for " + action.getString(gameState);
    }
}
