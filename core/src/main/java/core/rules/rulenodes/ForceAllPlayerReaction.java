package core.rules.rulenodes;

import core.AbstractGameStateWithTurnOrder;
import core.CoreConstants;
import core.turnorders.ReactiveTurnOrder;
import core.rules.Node;
import core.rules.nodetypes.RuleNode;

/**
 * Forces all players to play a reaction, in games where a ReactiveTurnOrder is used.
 * Should be followed by PlayerAction rule nodes, one for each player, in sequence.
 */
public class ForceAllPlayerReaction extends RuleNode {

    public ForceAllPlayerReaction() {
        super();
    }

    /**
     * Copy constructor
     * @param forceAllPlayerReaction - Node to be copied
     */
    public ForceAllPlayerReaction(ForceAllPlayerReaction forceAllPlayerReaction) {
        super(forceAllPlayerReaction);
    }

    @Override
    protected boolean run(AbstractGameStateWithTurnOrder gs) {
        ((ReactiveTurnOrder)gs.getTurnOrder()).addAllReactivePlayers(gs);
        gs.setGamePhase(CoreConstants.DefaultGamePhase.PlayerReaction);
        return false;
    }

    @Override
    protected Node _copy() {
        return new ForceAllPlayerReaction(this);
    }
}
