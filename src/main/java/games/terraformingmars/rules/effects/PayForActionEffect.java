package games.terraformingmars.rules.effects;

import games.terraformingmars.TMGameState;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.TMAction;

public class PayForActionEffect extends Effect {
    public int minCost;

    public PayForActionEffect(boolean mustBeCurrentPlayer, TMAction effectAction, int minCost) {
        super(mustBeCurrentPlayer, effectAction);
        this.minCost = minCost;
    }
    public PayForActionEffect(boolean mustBeCurrentPlayer, String effectAction, int minCost) {
        super(mustBeCurrentPlayer, effectAction);
        this.minCost = minCost;
    }

    @Override
    public boolean canExecute(TMGameState gameState, TMAction actionTaken, int player) {
        if (!(actionTaken instanceof PayForAction)) return false;
        PayForAction action = (PayForAction) actionTaken;
        return super.canExecute(gameState, actionTaken, player) && action.costTotal > minCost;
    }
}
