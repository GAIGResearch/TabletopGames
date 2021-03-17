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
    final TMAction action;
    final int costTotal;
    final TMTypes.Resource resourceToPay;
    final int cardIdx;

    int player;
    int costPaid;
    int stage;
    TMTypes.Resource[] resourcesToPayWith;

    public PayForAction(TMAction action, TMTypes.Resource resourceToPay, int costTotal, int cardIdx) {
        super(true);
        this.action = action;
        this.costTotal = costTotal;
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

        HashSet<TMTypes.Resource> resources = gs.canPlayerTransform(null, null, resourceToPay);
        resources.add(resourceToPay);  // Can always pay with itself

        resourcesToPayWith = resources.toArray(new TMTypes.Resource[0]);
        player = gs.getCurrentPlayer();
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
        int sum = gs.playerResourceSum(card, resourcesRemaining);
        int min = Math.max(0, (int)((costTotal - sum) * 1.0/gs.getResourceMapRate(res, resourceToPay)));  // TODO; discount effects
        int max = Math.min(gs.getPlayerResources()[player].get(res).getValue(), (int)(Math.ceil(costTotal * 1.0/gs.getResourceMapRate(res, resourceToPay))));

        // Can pay between min and max of this resource
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            actions.add(new PayResources(res, i));
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
        costPaid += ((PayResources)action).amount * gs.getResourceMapRate(res, TMTypes.Resource.MegaCredit);
        stage++;
        TMCard card = null;
        if (cardIdx > -1) card = gs.getPlayerHands()[player].get(cardIdx);
        if (card != null && ((TMGameState)state).isCardFree(card, costPaid) || costPaid == costTotal) {
            // Action paid for, execute
            action.execute(state);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        TMGameState gs = (TMGameState)state;
        TMCard card = null;
        if (cardIdx > -1) card = gs.getPlayerHands()[player].get(cardIdx);
        return card != null && ((TMGameState)state).isCardFree(card, costPaid) || stage == resourcesToPayWith.length || costPaid == costTotal;
    }

    @Override
    public PayForAction copy() {
        PayForAction p = new PayForAction((TMAction) action.copy(), resourceToPay, costTotal, cardIdx);
        p.costPaid = costPaid;
        p.player = player;
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
