package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.DiceMonasteryTurnOrder;

public class GainVictoryPoints extends AbstractAction {

    public final int vp;
    public final boolean asReward;

    public GainVictoryPoints(int amount, boolean asReward) {
        vp = amount;
        this.asReward = asReward;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        state.addVP(vp, state.getCurrentPlayer());

        if (asReward) {
            DiceMonasteryTurnOrder dto = (DiceMonasteryTurnOrder) state.getTurnOrder();
            dto.setRewardTaken(state.getCurrentPlayer());
        }
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
            return other.vp == vp && other.asReward == asReward;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return vp + (asReward ? 31 : 0);
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
