package core.rules.rulenodes;

import core.AbstractGameStateWithTurnOrder;
import core.rules.Node;
import core.rules.nodetypes.RuleNode;
import core.turnorders.ReactiveTurnOrder;

import static core.CoreConstants.DefaultGamePhase.PlayerReaction;

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
        gs.setGamePhase(PlayerReaction);
        return false;
    }

    @Override
    protected Node _copy() {
        return new ForceAllPlayerReaction(this);
    }
}
