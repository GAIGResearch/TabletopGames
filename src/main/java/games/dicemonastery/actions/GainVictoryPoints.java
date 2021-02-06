package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;

public class GainVictoryPoints extends AbstractAction {

    final int vp;

    public GainVictoryPoints(int amount) {
        vp = amount;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        state.addVP(vp, state.getCurrentPlayer());
        return true;
    }

    @Override
    public AbstractAction copy() {
        // no mutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GainVictoryPoints) {
            GainVictoryPoints other = (GainVictoryPoints) obj;
            return other.vp == vp;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return vp;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Player %d gains %d VP", gameState.getCurrentPlayer(), vp);
    }

    @Override
    public String toString() {
        return ("Gain " + vp + " VP");
    }
}
