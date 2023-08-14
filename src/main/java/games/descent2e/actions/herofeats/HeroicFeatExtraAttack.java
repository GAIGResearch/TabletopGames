package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.NightStalker;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.PRE_ATTACK_ROLL;

public class HeroicFeatExtraAttack extends FreeAttack {

    // Grisban the Thirsty's Heroic Feat
    String heroName = "Grisban";

    public HeroicFeatExtraAttack(int attackingFigure, int defendingFigure, boolean isMelee) {
        super(attackingFigure, defendingFigure, isMelee);
    }

    @Override
    public boolean execute(DescentGameState state) {
        state.setActionInProgress(this);
        Hero f = (Hero) state.getActingFigure();
        boolean hasExtraAction = f.hasUsedExtraAction();
        f.setUsedExtraAction(false);
        f.setFeatAvailable(false);
        super.execute(state);

        // Restore the extra action if it was previously available
        // This is not considered an extra action (a Heroic Feat action), so it should not interfere with the extra action
        f.setUsedExtraAction(hasExtraAction);
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero f = (Hero) dgs.getActingFigure();
        return f.getName().contains(heroName) && f.isFeatAvailable();
    }

    public HeroicFeatExtraAttack copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HeroicFeatExtraAttack) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();
        defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");

        if (isMelee)
        {
            return String.format("Heroic Feat: Extra Attack (Melee) by " + attackerName + " on " + defenderName);
        }

        Figure attacker = (Figure) gameState.getComponentById(attackingFigure);
        Figure defender = (Figure) gameState.getComponentById(defendingFigure);
        String distance = Double.toString(getDistanceFromFigures(attacker, defender));

        return String.format("Heroic Feat: Extra Attack (Ranged) by " + attackerName + " on " + defenderName + " (Range: " + distance + ")");
    }

    @Override
    public String toString() {
        if (isMelee) return String.format("Free Attack (Melee) by %d on %d", attackingFigure, defendingFigure);
        return String.format("Free Attack (Ranged) by %d on %d", attackingFigure, defendingFigure);
    }
}
