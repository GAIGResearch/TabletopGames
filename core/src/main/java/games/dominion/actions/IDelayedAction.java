package games.dominion.actions;

import games.dominion.DominionConstants;
import games.dominion.DominionGameState;

public interface IDelayedAction {

    DominionConstants.TriggerType getTrigger();

    void execute(DominionGameState state);

    IDelayedAction copy();

}
