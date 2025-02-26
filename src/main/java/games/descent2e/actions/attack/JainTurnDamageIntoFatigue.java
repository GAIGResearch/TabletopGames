package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;

import java.util.Objects;

public class JainTurnDamageIntoFatigue extends DescentAction {

    int jain;
    int reduce;
    public JainTurnDamageIntoFatigue(int hero, int reduce) {
        super(Triggers.TAKE_DAMAGE);
        this.jain = hero;
        this.reduce = reduce;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        int reduction = HeroAbilities.jain(dgs, jain, reduce);
        if (reduction > 0) {
            MeleeAttack currentAttack = (MeleeAttack) dgs.currentActionInProgress();
            assert currentAttack != null;
            currentAttack.reduceDamage(reduction);
            ((Figure) dgs.getComponentById(jain)).setCurrentAttack(currentAttack);
            //System.out.println("Reduced the damage by " + reduction + " and turned it into Fatigue!");
        }
        ((Figure) dgs.getComponentById(jain)).addActionTaken(toString());
        return true;
    }

    @Override
    public JainTurnDamageIntoFatigue copy() {
        return new JainTurnDamageIntoFatigue(jain, reduce);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        // Can only reduce damage from an Attack action
        Figure f = (Figure) dgs.getComponentById(jain);
        MeleeAttack currentAttack = f.getCurrentAttack();
        if (currentAttack == null)
            return false;

        // Can't reduce if we already did so
        if (currentAttack.reduced)
            return false;

        // Can't reduce if we're not the target
        if (currentAttack.defendingFigure != jain)
            return false;

        // Can't reduce if we chose not to
        if (currentAttack.skip)
            return false;

        // Can only reduce before damage is applied
        if (currentAttack.getPhase() != MeleeAttack.AttackPhase.PRE_DAMAGE)
            return false;

        // Prevents us from increasing our Fatigue more than the damage we took
        int damage = currentAttack.getDamage();
        if (damage - reduce < 0)
            return false;

        return (reduce + f.getAttributeValue(Figure.Attribute.Fatigue)) <= f.getAttributeMax(Figure.Attribute.Fatigue);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JainTurnDamageIntoFatigue other) {
            return other.jain == jain && other.reduce == reduce;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), jain, reduce);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Hero Ability: Convert " + reduce + " damage taken to Fatigue";
    }

    public String toString() {
        return "CONVERT_" + reduce + "_DAMAGE_TO_FATIGUE : " + jain;
    }
}