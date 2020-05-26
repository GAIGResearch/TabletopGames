package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.turnorder.ReactiveTurnOrder;

import static core.gamephase.DefaultGamePhase.PlayerReaction;

public class ForceAllEventReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        ((ReactiveTurnOrder)gs.getTurnOrder()).addAllReactivePlayers(gs);
        gs.setGamePhase(PlayerReaction);
        return false;
    }
}
