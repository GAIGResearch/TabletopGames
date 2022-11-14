package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;
import utilities.Distance;

/**
 *   This works in exactly the same way as a Melee Attack
 *   Except that there is a different definition of 'missed' that takes into account the range rolled on the dice
 */
public class RangedAttack extends MeleeAttack {
    public RangedAttack(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure);
    }

    @Override
    public boolean attackMissed(DescentGameState state) {
        if (super.attackMissed(state))
            return true; // due to no damage done
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        // TODO: Chebyshev distance is not actually right, as it does not allow diagonal moves
        double distance = Distance.chebyshev_distance(attacker.getPosition(), defender.getPosition());
        return (state.getAttackDicePool().getRange() + extraRange < distance);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
        // TODO: Extend this to pull in details of card and figures involved
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return super.canExecute(dgs); // TODO
    }

    @Override
    public String toString() {
        return String.format("Ranged Attack (Wpn: %d on %d", attackingPlayer, attackingFigure);
    }
}
