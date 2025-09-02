package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.Figure;

import java.util.Objects;

public class Subdue extends DescentAction {

    protected int figureID;
    protected DescentTypes.DescentCondition condition;
    public Subdue(int figureID, DescentTypes.DescentCondition condition) {
        super(Triggers.END_ATTACK);
        this.figureID = figureID;
        this.condition = condition;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(figureID);
        return String.format("Subdue: Inflict %s with %s", f.getComponentName().replace("Hero: ", ""), condition.name());
    }

    public String toString() {
        return String.format("Subdue: Inflict %s with %s", figureID, condition.name());
    }

    @Override
    public boolean execute(DescentGameState gs) {

        MeleeAttack currentAttack = (MeleeAttack) gs.currentActionInProgress();
        assert currentAttack != null;
        Figure f = (Figure) gs.getComponentById(figureID);
        f.addCondition(condition);

        // Once the condition has been applied, set Subdue flag to false
        // This prevents multiple attempts from the same Surge
        currentAttack.setSubdue(false);

        return true;
    }

    @Override
    public DescentAction copy() {
        return new Subdue(figureID, condition);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Subdue subdue) {
            return this.figureID == subdue.figureID && this.condition.equals(subdue.condition) && super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), figureID);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        MeleeAttack currentAttack = (MeleeAttack) dgs.currentActionInProgress();
        if (currentAttack == null)
            return false;
        if (!currentAttack.isSubdue()) return false;

        // No point calling this if we missed our attack
        if (currentAttack.attackMissed(dgs)) return false;

        if (condition == null) return false;

        // We can only apply a condition from Subdue if we do not already have it
        Figure f = (Figure) dgs.getComponentById(figureID);
        return !f.hasCondition(condition);
    }
}
