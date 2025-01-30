package games.conquest.players;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import games.conquest.CQGameState;
import games.conquest.actions.ApplyCommand;
import games.conquest.actions.AttackTroop;
import games.conquest.actions.EndTurn;
import games.conquest.actions.MoveTroop;
import games.conquest.components.CommandType;

import java.util.List;

public class CQRandomSearchHeuristic implements IActionHeuristic {
    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        CQGameState cqgs = (CQGameState) state;
        if (action instanceof AttackTroop) {
            // If able to attack something, always evaluate the result first
            return 1.0;
        } else if (action instanceof ApplyCommand) {
            ApplyCommand ac = (ApplyCommand) action;
            if (ac.getCmdType().equals(CommandType.Charge)) {
                return 1.0; // prioritize Charge, to be able to explore MoveTroop actions further down
            } else if (!ac.getCmdType().enemy && ac.targetsTroop(cqgs.getSelectedTroop())) {
                return 0.75;
            } else {
                return 0.5;
            }
        } else if (action instanceof EndTurn) {
            return 0.5;
        } else if (action instanceof MoveTroop && cqgs.getSelectedTroop().hasCommand(CommandType.Charge)) {
            // If charge is applied, only consider moves that allow the troop to attack.
            if (cqgs.canAttackEnemy(((MoveTroop) action).getHighlight())) {
                return 1.0;
            }
            return 0.0;
        } else {
            // SelectTroop, no specific troops gets prioritized
            return 0.5;
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
        return obj instanceof CQRandomSearchHeuristic;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
