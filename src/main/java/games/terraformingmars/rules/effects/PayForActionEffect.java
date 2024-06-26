package games.terraformingmars.rules.effects;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.TMAction;

public class PayForActionEffect extends Effect {
    public int minCost;
    public TMTypes.ActionType actionType;

    public PayForActionEffect(boolean mustBeCurrentPlayer, TMAction effectAction, TMTypes.ActionType at) {
        super(mustBeCurrentPlayer, effectAction);
        this.actionType = at;
    }

    public PayForActionEffect(boolean mustBeCurrentPlayer, TMAction effectAction, int minCost) {
        super(mustBeCurrentPlayer, effectAction);
        this.minCost = minCost;
    }

    @Override
    public boolean canExecute(TMGameState gameState, TMAction actionTaken, int player) {
        if (!(actionTaken instanceof PayForAction) || !super.canExecute(gameState, actionTaken, player)) return false;
        PayForAction action = (PayForAction) actionTaken;
        if (actionType != null) return action.actionType == actionType;
        return action.getCost() >= minCost;
    }

    @Override
    public Effect copy() {
        PayForActionEffect copy = new PayForActionEffect(mustBeCurrentPlayer, effectAction.copy(), actionType);
        copy.minCost = minCost;
        return copy;
    }

    @Override
    public Effect copySerializable() {
        PayForActionEffect copy = new PayForActionEffect(mustBeCurrentPlayer, effectAction.copySerializable(), actionType);
        copy.minCost = minCost;
        return copy;
    }
}
