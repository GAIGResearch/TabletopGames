package test.games.descent;

import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.DicePool;

import java.util.Collections;

public class MeleeAttackDamageOnly extends MeleeAttack {
    public MeleeAttackDamageOnly(int attackingFigure, int defendingFigure) {
        super(attackingFigure, defendingFigure);
    }

    @Override
    public void defenceRoll(DescentGameState state) {
        // we set the dice pool to not roll any defence
        state.setDefenceDicePool(new DicePool(Collections.emptyList()));
    }
}
