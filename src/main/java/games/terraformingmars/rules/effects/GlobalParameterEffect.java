package games.terraformingmars.rules.effects;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.ModifyGlobalParameter;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.PlayCard;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;

import java.util.HashSet;

public class GlobalParameterEffect extends Effect {
    public TMTypes.GlobalParameter globalParameter;

    public GlobalParameterEffect(boolean mustBeCurrentPlayer, TMAction effectAction, TMTypes.GlobalParameter param) {
        super(mustBeCurrentPlayer, effectAction);
        this.globalParameter = param;
    }

    public void execute(TMGameState gs, TMAction actionTaken, int player) {
        if (canExecute(gs, actionTaken, player)) {
            ModifyGlobalParameter action = (ModifyGlobalParameter) actionTaken;
            effectAction.player = player;
            if (action.param == globalParameter) {
                this.effectAction.execute(gs);
            }
        }
    }

    @Override
    public Effect copy() {
        return new GlobalParameterEffect(mustBeCurrentPlayer, effectAction.copy(), globalParameter);
    }

    @Override
    public Effect copySerializable() {
        return new GlobalParameterEffect(mustBeCurrentPlayer, effectAction.copySerializable(), globalParameter);
    }

    @Override
    public boolean canExecute(TMGameState gameState, TMAction actionTaken, int player) {
        if (!(actionTaken instanceof ModifyGlobalParameter) || !super.canExecute(gameState, actionTaken, player)) return false;
        ModifyGlobalParameter action = (ModifyGlobalParameter) actionTaken;
        return action.param == globalParameter;
    }
}
