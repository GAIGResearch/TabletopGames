package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;
import utilities.Distance;
import utilities.Vector2D;

/**
 *   This works in exactly the same way as a Melee Attack
 *   Except that there is a different definition of 'missed' that takes into account the range rolled on the dice
 */
public class RangedAttack extends MeleeAttack {

    // How many tiles we check at maximum for Ranged Attacks
    public static final int MAX_RANGE = 10;

    public RangedAttack(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure);
    }

    @Override
    public boolean attackMissed(DescentGameState state) {
        if (super.attackMissed(state))
            return true; // due to no damage done
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        double distance = getDistanceFromFigures(attacker, defender);
        return (state.getAttackDicePool().getRange() + extraRange < distance);
    }

    public double getDistanceFromFigures(Figure attacker, Figure defender) {
        // TODO: Chebyshev distance is not actually right, as it does not allow diagonal moves
        return Distance.chebyshev_distance(attacker.getPosition(), defender.getPosition());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();
        defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");

        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);
        Figure defender = (Figure) gameState.getComponentById(defendingFigure);
        String distance = Double.toString(getDistanceFromFigures(attacker, defender));

        return String.format("Ranged Attack by " + attackerName + " on " + defenderName + " (Range: " + distance + ")");
        //return toString();
        // TODO: Extend this to pull in details of card and figures involved, including distance
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
