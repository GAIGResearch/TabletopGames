package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;

import java.util.Objects;

public class NotMeSwap extends DescentAction {

    int target;
    int splig;
    public NotMeSwap(int target, int splig) {
        super(Triggers.FORCED);
        this.target = target;
        this.splig = splig;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(splig);
        f.addActionTaken(toString());

        if (splig != target)
        {
            Figure t = (Figure) dgs.getComponentById(target);
            t.addActionTaken(toString());
        }
        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        Figure f = (Figure) dgs.getComponentById(splig);
        Figure t = (Figure) dgs.getComponentById(target);
        if (f == null || t == null) return false;
        return !f.isOffMap() && !t.isOffMap();
    }

    public int getTarget()
    {
        return target;
    }

    public int getSource()
    {
        return splig;
    }

    public NotMeSwap copy() {
        return new NotMeSwap(target, splig);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NotMeSwap that = (NotMeSwap) o;
        return that.target == target && that.splig == splig;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(target, splig);
    }

    @Override
    public String getString(AbstractGameState gameState) {

        Figure f = (Figure) gameState.getComponentById(splig);
        String name = f.getName().replace("Hero: ", "");

        if (splig == target) return "Not Me!: " + name + " takes the attack as intended.";

        Figure t = (Figure) gameState.getComponentById(target);
        String targetName = t.getName().replace("Hero: ", "");

        return "Not Me!: " + name + " makes " + targetName + " take the attack instead!";

    }

    @Override
    public String toString() {
        if (splig != target) return "Not Me!: " + splig + " makes " + target + " take the attack instead!";
        else return "Not Me! " + splig + " takes the attack as intended.";
    }
}
