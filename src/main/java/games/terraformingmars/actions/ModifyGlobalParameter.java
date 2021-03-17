package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.ModifyCounter;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

public class ModifyGlobalParameter extends TMModifyCounter {

    public ModifyGlobalParameter(int counterID, int change, boolean free) {
        super(counterID, change, free);
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        // When global parameters change, TR is increased
        TMGameState gs = (TMGameState) gameState;
        Counter c = (Counter)gs.getComponentById(counterID);
        int player = gs.getCurrentPlayer();
        if (change > 0 && !c.isMaximum() || change < 0 && !c.isMinimum()) {
            gs.getPlayerResources()[player].get(TMTypes.Resource.TR).increment(1);
            gs.getPlayerResourceIncreaseGen()[player].put(TMTypes.Resource.TR, true);
        }
        return super.execute(gs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Modify global parameter " + gameState.getComponentById(counterID).getComponentName() + " by " + change;
    }

    @Override
    public String toString() {
        return "Modify global parameter " + counterID + " by " + change;
    }
}
