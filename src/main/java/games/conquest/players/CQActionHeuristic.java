package games.conquest.players;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;
import games.conquest.actions.ApplyCommand;
import games.conquest.actions.AttackTroop;
import games.conquest.actions.EndTurn;
import utilities.Pair;

import java.util.List;
import java.util.Map;

public class CQActionHeuristic implements IActionHeuristic {
    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        if (action instanceof AttackTroop) {
            // If able to attack something, always evaluate the result first
            return 1.0;
        } else if (action instanceof ApplyCommand) {
            // When evaluating a turn, evaluate using commands last
            return 0.8;
        } else if (action instanceof EndTurn) {
            // Consider ending your turn last, after having checked possible useful moves
            return 0.4;
        } else {
            // SelectTroop or MoveTroop action
            return 0.6;
        }
    }

    public AbstractAction bestAction(List<AbstractAction> actions, AbstractGameState gameState) {
        if (actions.isEmpty()) {
            return null;
        }
        double[] evaluations = this.evaluateAllActions(actions, gameState);
        int maxI = 0;
        for (int i=0; i<evaluations.length; i++) {
            if (evaluations[i] > evaluations[maxI]) {
                maxI = i;
            }
        }
        return actions.get(maxI);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CQActionHeuristic;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
