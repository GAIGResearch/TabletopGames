package games.pandemic.engine.rules;

import core.AbstractGameState;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicTurnOrder;

import static games.pandemic.PandemicGameState.GamePhase.EventReaction;

public class ForceAllEventReaction extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        ((PandemicTurnOrder)pgs.getTurnOrder()).addAllReactivePlayers(gs);
        pgs.setGamePhase(EventReaction);
        return false;
    }
}
