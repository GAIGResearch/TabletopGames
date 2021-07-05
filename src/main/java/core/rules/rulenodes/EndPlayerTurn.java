package core.rules.rulenodes;

import core.AbstractGameState;
import core.rules.Node;
import core.rules.nodetypes.RuleNode;

/**
 * Ends the turn of the player, changing to the next one and resetting the game phase to the default main phase.
 */
public class EndPlayerTurn extends RuleNode {

    public EndPlayerTurn() {
        setNextPlayerNode();
    }

    /**
     * Copy constructor
     * @param endPlayerTurn - Node to be copied
     */
    public EndPlayerTurn(EndPlayerTurn endPlayerTurn) {
        super(endPlayerTurn);
    }

    @Override
    protected boolean run(AbstractGameState gs) {
        gs.getTurnOrder().endPlayerTurn(gs);
        gs.setMainGamePhase();
        return true;
    }

    @Override
    protected Node _copy() {
        return new EndPlayerTurn(this);
    }
}
