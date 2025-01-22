package games.conquest.players;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;
import games.conquest.CQGameState;
import games.conquest.actions.ApplyCommand;
import games.conquest.actions.AttackTroop;
import games.conquest.actions.EndTurn;
import games.conquest.actions.MoveTroop;
import games.conquest.components.Cell;
import games.conquest.components.CommandType;
import games.conquest.components.Troop;
import utilities.Pair;
import utilities.Vector2D;

import java.util.List;
import java.util.Map;

public class CQActionHeuristic implements IActionHeuristic {
    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        CQGameState cqgs = (CQGameState) state;
        if (action instanceof AttackTroop) {
            // If able to attack something, always evaluate the result first
            return 1.0;
        } else if (action instanceof ApplyCommand) {
            ApplyCommand ac = (ApplyCommand) action;
            // When evaluating a turn, evaluate using commands last
            if (ac.getCmdType().enemy) {
                return 0.6; // TODO: some conditional to check proximity to friendly troop
            } else if (ac.targetsTroop(cqgs.getSelectedTroop())) {
                return 0.7;
            } else {
                return 0.6;
            }
        } else if (action instanceof EndTurn) {
            // First explore what happens if you end your turn, as a baseline to compare to applying commands later
            return 0.61;
        } else if (action instanceof MoveTroop && false) {
            Cell target = cqgs.getCell(((MoveTroop) action).getHighlight());
            int minDistance = 999999;
            for (Troop troop : cqgs.getTroops(cqgs.getCurrentPlayer() ^ 1)) {
                int d = target.getChebyshev(troop.getLocation());
                if (d < minDistance) {
                    minDistance = d;
                    if (minDistance <= cqgs.getSelectedTroop().getRange()) {
                        // if we can move into attack range of an enemy troop, prioritize
                        return 0.9;
                    }
                }
            }
            return 0.7;
        } else {
            // SelectTroop, no specific troops gets prioritized
            return 0.7;
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
