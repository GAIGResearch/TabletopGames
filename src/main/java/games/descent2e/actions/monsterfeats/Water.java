package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.TriggerAttributeTest;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.actions.attack.TriggerAttributeTest.GetAttributeTests.POST_TEST;
import static games.descent2e.actions.attack.TriggerAttributeTest.GetAttributeTests.PRE_TEST;

public class Water extends TriggerAttributeTest {

    List<Integer> heroes;
    int heroIndex = 0;
    public Water(int attackingFigure, List<Integer> targets) {
        super(attackingFigure, targets.get(0));
        this.heroes = targets;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String attackerName = ((Figure) gameState.getComponentById(attackingFigure)).getName().replace("Hero: ", "");;
        String string = "Water by "+ attackerName + " on ";

        for (int i = 0; i < heroes.size(); i++) {
            Figure defender = (Figure) gameState.getComponentById(heroes.get(i));
            String defenderName = defender.getComponentName().replace("Hero: ", "");
            string += defenderName;

            if (i < heroes.size() - 1) {
                string += " and ";
            }
        }

        return string;
    }

    @Override
    public String toString() {

        String string = "Water by " + attackingFigure + " on ";
        for (int i = 0; i < heroes.size(); i++) {
            string += heroes.get(i);
            if (i < heroes.size() - 1) {
                string += " and ";
            }
        }

        return string;
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

        WaterTest waterTest = new WaterTest(defendingFigure, Figure.Attribute.Willpower, attackingFigure, monster.getNActionsExecuted().getValue());

        if (waterTest.canExecute(dgs)) {
            retVal.add(waterTest);
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
    public Water copy() {
        Water retVal = new Water(attackingFigure, heroes);
        copyComponentTo(retVal);
        return retVal;
    }

    public void copyComponentTo(Water retVal)
    {
        retVal.heroIndex = heroIndex;
        super.copyComponentTo(retVal);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (heroes.isEmpty()) return false;
        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        return f instanceof Monster && (((Monster) f).hasAction(MonsterAbilities.MonsterAbility.WATER));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Water water = (Water) o;
        return heroIndex == water.heroIndex && Objects.equals(heroes, water.heroes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), heroes, heroIndex);
    }
}
