package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;

import java.util.Objects;

public class JainTurnDamageIntoFatigue extends DescentAction {

    // Jain Fairwood Hero Ability
    String heroName = "Jain Fairwood";
    int jain;
    int reduce;
    public JainTurnDamageIntoFatigue(int hero, int reduce) {
        super(Triggers.TAKE_DAMAGE);
        this.jain = hero;
        this.reduce = reduce;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        int reduction = HeroAbilities.jain(dgs, reduce);
        if (reduction > 0) {
            MeleeAttack currentAttack = (MeleeAttack) dgs.currentActionInProgress();
            assert currentAttack != null;
            currentAttack.reduceDamage(reduction);
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
        MeleeAttack currentAttack = dgs.getActingFigure().getCurrentAttack();
        if (currentAttack == null)
            return false;
        if (currentAttack.skip)
            return false;
        int damage = currentAttack.getDamage();
        // Prevents us from increasing our Fatigue more than the damage we took
        if (damage - reduce < 0)
            return false;
        Figure f = (Figure) dgs.getComponentById(jain);
        return (reduce + f.getAttributeValue(Figure.Attribute.Fatigue)) <= f.getAttributeMax(Figure.Attribute.Fatigue);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JainTurnDamageIntoFatigue) {
            JainTurnDamageIntoFatigue other = (JainTurnDamageIntoFatigue) obj;
            return other.jain == jain && other.reduce == reduce;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jain, reduce);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Hero Ability: Convert " + reduce + " damage taken to Fatigue";
    }

    public String toString() {
        return "CONVERT_" + reduce + "_DAMAGE_TO_FATIGUE : " + jain;
    }
}