package games.terraformingmars.rules.effects;

import games.terraformingmars.TMGameState;
import games.terraformingmars.actions.TMAction;

import java.util.Objects;

public abstract class Effect {
    public boolean mustBeCurrentPlayer;  // if true, only applies when player is current player
//    public boolean appliesToPlayer;  // if true, applies to the player, otherwise can apply to any TODO
//    public boolean mustApply;  // "up to X" type effects don't have to apply TODO
    public TMAction effectAction;

    public Effect(boolean mustBeCurrentPlayer, TMAction effectAction) {
        this.mustBeCurrentPlayer = mustBeCurrentPlayer;
        this.effectAction = effectAction;
    }

    public boolean canExecute(TMGameState gameState, TMAction actionTaken, int player) {
        return !mustBeCurrentPlayer || gameState.getCurrentPlayer() == player;
    }

    public void execute(TMGameState gs, TMAction actionTaken, int player) {
        if (canExecute(gs, actionTaken, player)) {
            effectAction.player = player;
            this.effectAction.execute(gs);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Effect)) return false;
        Effect effect = (Effect) o;
        return mustBeCurrentPlayer == effect.mustBeCurrentPlayer && Objects.equals(effectAction, effect.effectAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mustBeCurrentPlayer, effectAction);
    }

    public abstract Effect copy();
    public abstract Effect copySerializable();
}
