package games.terraformingmars.rules.effects;

import games.terraformingmars.TMGameState;
import games.terraformingmars.actions.TMAction;

import java.util.Objects;

public abstract class Effect {
    boolean mustBeCurrentPlayer;  // if true, only applies when player is current player
    TMAction effectAction;
    String effectEncoding;

    public Effect(boolean mustBeCurrentPlayer, TMAction effectAction) {
        this.mustBeCurrentPlayer = mustBeCurrentPlayer;
        this.effectAction = effectAction;
    }

    public Effect(boolean mustBeCurrentPlayer, String effectEncoding) {
        this.mustBeCurrentPlayer = mustBeCurrentPlayer;
        this.effectEncoding = effectEncoding;
    }

    public boolean canExecute(TMGameState gameState, TMAction actionTaken, int player) {
        return !mustBeCurrentPlayer || gameState.getCurrentPlayer() == player;
    }

    public void execute(TMGameState gs, TMAction actionTaken, int player) {
        if (canExecute(gs, actionTaken, player)) {
            if (effectAction == null) {
                effectAction = TMAction.parseAction(gs, effectEncoding).a;
            }
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

    // TODO copy?
}
