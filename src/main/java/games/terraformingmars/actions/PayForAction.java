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
    public int costTotal;
    public final TMTypes.Resource resourceToPay;
    public final int cardIdx;

    int costPaid;
    int stage;
    TMTypes.Resource[] resourcesToPayWith;

    public PayForAction(TMTypes.ActionType type, int player, TMAction action, TMTypes.Resource resourceToPay, int costTotal, int cardIdx) {
        super(type, player, true);
        this.action = action;
        this.costTotal = Math.abs(costTotal);
        this.resourceToPay = resourceToPay;
        this.cardIdx = cardIdx;  // -1 if no card needed
    }

    public PayForAction(TMTypes.StandardProject project, int player, TMAction action, TMTypes.Resource resourceToPay, int costTotal, int cardIdx) {
        super(TMTypes.ActionType.StandardProject, player, true);
        this.action = action;
        this.costTotal = Math.abs(costTotal);
        this.resourceToPay = resourceToPay;
        this.cardIdx = cardIdx;  // -1 if no card needed
    }

    public PayForAction(int player, TMAction action, TMTypes.Resource resourceToPay, int costTotal, int cardIdx) {
        super(player, true);
        this.action = action;
        this.costTotal = Math.abs(costTotal);
        this.resourceToPay = resourceToPay;
        this.cardIdx = cardIdx;  // -1 if no card needed
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
        if (cardIdx > -1) {
            card = gs.getPlayerHands()[player].get(cardIdx);
            costTotal -= gs.discountCardCost(card, player);  // Apply card discount
        } else {
            // Check action type discounts
            costTotal -= gs.discountActionTypeCost(this.action, player);
        }
        HashSet<TMTypes.Resource> resources = gs.canPlayerTransform(player, card, null, resourceToPay);
        resources.add(resourceToPay);  // Can always pay with itself

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

        TMCard card = null;
        if (cardIdx > -1) card = gs.getPlayerHands()[player].get(cardIdx);
        int sum = gs.playerResourceSum(player, card, resourcesRemaining, TMTypes.Resource.MegaCredit);
        int remaining = costTotal - costPaid - sum;
        int min = Math.max(0, (int)(Math.ceil(remaining/gs.getResourceMapRate(res, resourceToPay))));
        int max = Math.min(gs.getPlayerResources()[player].get(res).getValue(), (int)(Math.ceil((costTotal - costPaid)/gs.getResourceMapRate(res, resourceToPay))));

        // Can pay between min and max of this resource
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            actions.add(new ResourceTransaction(player, res, -i));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        TMGameState gs = (TMGameState) state;
        TMTypes.Resource res = resourcesToPayWith[stage];
        costPaid += Math.abs(((ResourceTransaction)action).amount) * gs.getResourceMapRate(res, TMTypes.Resource.MegaCredit);
        stage++;
        TMCard card = null;
        if (cardIdx > -1) card = gs.getPlayerHands()[player].get(cardIdx);
        if (card != null && ((TMGameState)state).isCardFree(card, costPaid, player) || costPaid >= costTotal) {
            // Action paid for, execute
            this.action.execute(state);
            stage = resourcesToPayWith.length;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return stage == resourcesToPayWith.length || costPaid == costTotal;
    }

    @Override
    public PayForAction copy() {
        PayForAction p = new PayForAction(player, (TMAction) action.copy(), resourceToPay, costTotal, cardIdx);
        p.costPaid = costPaid;
        p.resourcesToPayWith = resourcesToPayWith.clone();
        p.stage = stage;
        return p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PayForAction)) return false;
        if (!super.equals(o)) return false;
        PayForAction that = (PayForAction) o;
        return costTotal == that.costTotal && cardIdx == that.cardIdx && player == that.player && costPaid == that.costPaid && stage == that.stage && Objects.equals(action, that.action) && resourceToPay == that.resourceToPay && Arrays.equals(resourcesToPayWith, that.resourcesToPayWith);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), action, costTotal, resourceToPay, cardIdx, player, costPaid, stage);
        result = 31 * result + Arrays.hashCode(resourcesToPayWith);
        return result;
    }

    @Override
    public String toString() {
        return "Pay " + costTotal + " " + resourceToPay + " for " + action.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pay " + costTotal + " " + resourceToPay + " for " + action.getString(gameState);
    }
}
