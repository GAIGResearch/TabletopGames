package games.pandemic.rules.rules;

import core.AbstractGameState;
import core.rules.rulenodes.EndPlayerTurn;
import games.pandemic.PandemicGameState;

public class NextPlayer extends EndPlayerTurn {

    public NextPlayer() {
        setNextPlayerNode();
    }

    @Override
    protected boolean run(AbstractGameState gs) {
        ((PandemicGameState)gs).setNCardsDrawn(0);
        return super.run(gs);
    }
}
