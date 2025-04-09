package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.EndCurrentPhase;
import games.descent2e.actions.attack.TriggerAttributeTest;
import games.descent2e.actions.items.RerollAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.actions.attack.TriggerAttributeTest.GetAttributeTests.*;

public class Howl extends TriggerAttributeTest {

    List<Integer> heroes;
    int heroIndex = 0;
    public Howl(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets.get(0));
        this.heroes = targets;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Howl";
    }

    @Override
    public String toString() {
        return "Howl";
    }

    @Override
    public boolean execute(DescentGameState state) {
        super.execute(state);
        Figure monster = (Figure) state.getComponentById(attackingFigure);
        monster.getNActionsExecuted().increment();
        monster.setHasAttacked(true);
        monster.addActionTaken(toString());

        return true;
    }

    @Override
    protected void executePhase(DescentGameState state) {
        switch (phase) {
            case INDEX_CHECK:
                if (heroIndex < heroes.size() - 1) {
                    phase = PRE_TEST;
                    heroIndex++;
                    super.defendingFigure = heroes.get(heroIndex);
                    super.defendingPlayer = state.getComponentById(defendingFigure).getOwnerId();;
                } else {
                    phase = POST_TEST;
                }
                break;
            default:
                super.executePhase(state);
        }
        // and reset interrupts
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState dgs = (DescentGameState) state;
        Monster monster = (Monster) dgs.getComponentById(attackingFigure);
        List<AbstractAction> retVal = new ArrayList<>();

        // System.out.println(((Figure) dgs.getComponentById(defendingFigure)).getName());

        HowlTest howlTest = new HowlTest(defendingFigure, Figure.Attribute.Willpower, attackingFigure, monster.getNActionsExecuted().getValue());

        if (howlTest.canExecute(dgs)) {
            retVal.add(howlTest);
        }

        if (retVal.isEmpty())
        {
            List<AbstractAction> superRetVal = super._computeAvailableActions(state);
            if (superRetVal != null && !superRetVal.isEmpty())
            {
                retVal.addAll(superRetVal);
            }
        }

        return retVal;
    }

    @Override
    public Howl copy() {
        Howl retVal = new Howl(attackingFigure, heroes);
        copyComponentTo(retVal);
        return retVal;
    }

    public void copyComponentTo(Howl retVal)
    {
        retVal.heroIndex = heroIndex;
        super.copyComponentTo(retVal);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        return f instanceof Monster && (((Monster) f).hasAction(MonsterAbilities.MonsterAbility.HOWL));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Howl howl = (Howl) o;
        return heroIndex == howl.heroIndex && Objects.equals(heroes, howl.heroes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), heroes, heroIndex);
    }
}
