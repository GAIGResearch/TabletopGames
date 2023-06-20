package players.rl.dataStructures;

import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class TurnSAR {
    public final AbstractGameState s;
    public final AbstractAction a;
    public final float r;

    public final List<AbstractAction> possibleActions;

    public TurnSAR(AbstractGameState s, AbstractAction a, List<AbstractAction> possibleActions, float r) {
        this.s = s;
        this.a = a;
        this.r = r;
        this.possibleActions = possibleActions;
    }
}
