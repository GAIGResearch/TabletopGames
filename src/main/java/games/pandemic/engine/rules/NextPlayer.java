package games.pandemic.engine.rules;

import core.AbstractGameState;
import games.pandemic.PandemicGameState;

public class NextPlayer extends RuleNode {

    @Override
    protected boolean run(AbstractGameState gs) {
        gs.getTurnOrder().endPlayerTurn(gs);
        ((PandemicGameState)gs).setNCardsDrawn(0);
        gs.setMainGamePhase();
        return true;
    }
}
