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
        if (change > 0 && !c.isMaximum()) {
            gs.getPlayerResources()[gs.getCurrentPlayer()].get(TMTypes.Resource.TR).increment(1);
        } else if (change < 0 && !c.isMinimum()) {
            gs.getPlayerResources()[gs.getCurrentPlayer()].get(TMTypes.Resource.TR).increment(1);
        }
        return super.execute(gs);
    }
}
