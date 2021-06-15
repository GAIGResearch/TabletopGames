package games.pandemic.rules.rules;

import core.AbstractGameState;
import core.rules.Node;
import core.rules.rulenodes.EndPlayerTurn;
import games.pandemic.PandemicGameState;

public class NextPlayer extends EndPlayerTurn {

    public NextPlayer() {
        setNextPlayerNode();
    }
    /**
     * Copy constructor
     * @param nextPlayer - Node to be copied
     */
    public NextPlayer(NextPlayer nextPlayer) {
        super(nextPlayer);
    }

    @Override
    protected boolean run(AbstractGameState gs) {
        ((PandemicGameState)gs).setNCardsDrawn(0);
        return super.run(gs);
    }

    @Override
    protected Node _copy() {
        return new NextPlayer(this);
    }
}
