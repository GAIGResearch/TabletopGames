package games.dominion.actions;

import games.dominion.*;

public interface IDelayedAction {

    DominionConstants.TriggerType getTrigger();

    void execute(DominionGameState state);

    IDelayedAction copy();

}
