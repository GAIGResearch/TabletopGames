package players.rl;

import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;

class TurnSAR {
    final AbstractGameState s;
    final AbstractAction a;
    final float r;

    final List<AbstractAction> possibleActions;

    TurnSAR(AbstractGameState s, AbstractAction a, List<AbstractAction> possibleActions, float r) {
        this.s = s;
        this.a = a;
        this.r = r;
        this.possibleActions = possibleActions;
    }
}
