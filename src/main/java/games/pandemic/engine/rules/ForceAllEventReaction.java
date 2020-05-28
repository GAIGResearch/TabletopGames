package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.turnorders.ReactiveTurnOrder;

import static core.AbstractGameState.DefaultGamePhase.PlayerReaction;

public class ForceAllEventReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        ((ReactiveTurnOrder)gs.getTurnOrder()).addAllReactivePlayers(gs);
        gs.setGamePhase(PlayerReaction);
        return false;
    }
}
