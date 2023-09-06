package players.rl.utils;

import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class TurnSAR {
    public final AbstractGameState s;
    public final AbstractAction a;
    public final double r;

    public final List<AbstractAction> possibleActions;

    public TurnSAR(AbstractGameState s, AbstractAction a, List<AbstractAction> possibleActions, double r) {
        this.s = s;
        this.a = a;
        this.r = r;
        this.possibleActions = possibleActions;
    }
}
