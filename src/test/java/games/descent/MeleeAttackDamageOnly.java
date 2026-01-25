package games.descent;

import games.descent2e.DescentGameState;
import games.descent2e.components.DicePool;

import java.util.Collections;

public class MeleeAttackDamageOnly extends MeleeAttackOverride {
    public MeleeAttackDamageOnly(int attackingFigure, int defendingFigure){
        super(attackingFigure, defendingFigure, null, null);
    }
    public MeleeAttackDamageOnly(int attackingFigure, int defendingFigure, DicePool attack, DicePool defence) {
        super(attackingFigure, defendingFigure, attack, defence);
    }

    @Override
    public void defenceRoll(DescentGameState state) {
        // we set the dice pool to not roll any defence
        state.setDefenceDicePool(new DicePool(Collections.emptyList()));
    }
}
