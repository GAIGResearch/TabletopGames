package games.descent2e.actions.attack;

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
        double distance = Distance.chebyshev_distance(attacker.getPosition(), defender.getPosition());
        return (state.getAttackDicePool().getRange() + extraRange < distance);
    }
}
