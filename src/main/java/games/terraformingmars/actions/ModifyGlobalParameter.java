package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.ModifyCounter;

public class ModifyGlobalParameter extends ModifyCounter {

    public ModifyGlobalParameter(int counterID, int change) {
        super(counterID, change);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean success = super.execute(gs);

        return success;
    }
}
