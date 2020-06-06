package core.rules.rulenodes;

import core.AbstractGameState;
import core.rules.nodetypes.RuleNode;
import core.turnorders.ReactiveTurnOrder;

import static core.AbstractGameState.DefaultGamePhase.PlayerReaction;

/**
 * Forces all players to play a reaction, in games where a ReactiveTurnOrder is used.
 * Should be followed by PlayerAction rule nodes, one for each player, in sequence.
 */
public class ForceAllPlayerReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        ((ReactiveTurnOrder)gs.getTurnOrder()).addAllReactivePlayers(gs);
        gs.setGamePhase(PlayerReaction);
        return false;
    }
}
