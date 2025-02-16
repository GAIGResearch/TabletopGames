package games.descent;

import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.DicePool;

public class MeleeAttackOverride extends MeleeAttack {

    DicePool attack;
    DicePool defence;

    public MeleeAttackOverride(int attackingFigure, int defendingFigure, DicePool attack, DicePool defence) {
        super(attackingFigure, defendingFigure);
        this.attack = attack;
        this.defence = defence;
    }


    @Override
    protected void defenceRoll(DescentGameState state) {
        if (defence == null)
            super.defenceRoll(state);
        else
            state.setDefenceDicePool(defence);
    }

    @Override
    protected void damageRoll(DescentGameState state) {
        if (attack == null)
            super.damageRoll(state);
        else
            state.setAttackDicePool(attack);
    }
}
