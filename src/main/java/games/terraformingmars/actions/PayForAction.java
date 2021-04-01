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
        this.setActionCost(resourceToPay, Math.abs(costTotal), cardID);
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();

        // if only one option to pay left, just do it, don't ask again

        // Do extended sequence
        // Pay for card with resources until all paid
        // Second: execute action

        if (this.player == -1) player = gs.getCurrentPlayer();

        TMCard card = null;
        if (getCardID() > -1) {
            card = (TMCard) gs.getComponentById(getCardID());
            setCost(getCost() - gs.discountCardCost(card, player));  // Apply card discount
        } else {
            // Check action type discounts
            setCost(getCost() - gs.discountActionTypeCost(this.action, player));
        }
        HashSet<TMTypes.Resource> resources = gs.canPlayerTransform(player, card, null, getCostResource());
        resources.add(getCostResource());  // Can always pay with itself

        resourcesToPayWith = resources.toArray(new TMTypes.Resource[0]);
        stage = 0;
        costPaid = 0;

        if (stage == resourcesToPayWith.length-1) {
            List<AbstractAction> actions = _computeAvailableActions(gs);
            if (actions.size() == 1) {
                // If only 1 option, just do it
                TMAction a = (TMAction) actions.get(0);
                boolean s1 = a.execute(gs);
                boolean s2 = this.action.execute(gs);
                stage = resourcesToPayWith.length;
                return s1 && s2;
            } else {
                gameState.setActionInProgress(this);
                return true;
            }
        } else {
            gameState.setActionInProgress(this);
            return true;
        }
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        TMGameState gs = (TMGameState) state;
        TMTypes.Resource res = resourcesToPayWith[stage];
        // Find minimum that must be spent of this resource so that the card is still payable with remaining resources
        HashSet<TMTypes.Resource> resourcesRemaining = new HashSet<>(Arrays.asList(resourcesToPayWith).subList(stage + 1, resourcesToPayWith.length));

        TMCard card = (TMCard) gs.getComponentById(getCardID());
        int sum = gs.playerResourceSum(player, card, resourcesRemaining, getCostResource());
        int remaining = getCost() - costPaid - sum;
        int min = Math.max(0, (int)(Math.ceil(remaining/gs.getResourceMapRate(res, getCostResource()))));
        int max = Math.min(gs.getPlayerResources()[player].get(res).getValue(), (int)(Math.ceil((getCost() - costPaid)/gs.getResourceMapRate(res, getCostResource()))));

        // Can pay between min and max of this resource
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            actions.add(new ModifyPlayerResource(player, -i, res, false));
        }
        if (actions.size() == 0) {
            int a = 0;  // TODO: bad, this shouldn't happen
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
        costPaid += Math.abs(((ModifyPlayerResource)action).change) * gs.getResourceMapRate(res, getCostResource());
        stage++;
        if (costPaid >= getCost()) {
            // Action paid for, execute
            this.action.execute(state);
            stage = resourcesToPayWith.length;
        } else if (stage == resourcesToPayWith.length-1) {
            List<AbstractAction> actions = _computeAvailableActions(gs);
            if (actions.size() == 1) {
                // If only 1 option left, just do it
                TMAction a = (TMAction) actions.get(0);
                a.execute(gs);
                this.action.execute(state);
                stage = resourcesToPayWith.length;
            }
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return stage == resourcesToPayWith.length || costPaid == getCost();
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
        return "Pay " + getCost() + " " + getCostResource() + " for " + action.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pay " + getCost() + " " + getCostResource() + " for " + action.getString(gameState);
    }
}
