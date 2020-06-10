package core.rules.rulenodes;

import core.AbstractGameState;
import core.rules.nodetypes.RuleNode;

/**
 * Ends the turn of the player, changing to the next one and resetting the game phase to the default main phase.
 */
public class EndPlayerTurn extends RuleNode {

    public EndPlayerTurn() {
        setNextPlayerNode();
    }

    @Override
    protected boolean run(AbstractGameState gs) {
        gs.getTurnOrder().endPlayerTurn(gs);
        gs.setMainGamePhase();
        return true;
    }
}
