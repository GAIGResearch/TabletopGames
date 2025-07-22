package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.GridBoard;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.archetypeskills.Heal;
import games.descent2e.actions.attack.TriggerAttributeTest;
import games.descent2e.components.*;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.actions.attack.TriggerAttributeTest.GetAttributeTests.POST_TEST;
import static games.descent2e.actions.attack.TriggerAttributeTest.GetAttributeTests.PRE_TEST;
import static utilities.Utils.getNeighbourhood;

public class Air extends DescentAction {

    int figureID;

    public Air(Integer figureID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.figureID = figureID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Air: Immune to non-adjacent attacks for 1 turn";
    }

    @Override
    public String toString() {
        return "Air";
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Monster monster = (Monster) dgs.getComponentById(figureID);
        monster.addPassive(MonsterAbilities.MonsterPassive.AIR);
        monster.getNActionsExecuted().increment();
        monster.setHasAttacked(true);
        monster.addActionTaken(getString(dgs));

        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = (Figure) dgs.getComponentById(figureID);
        if (f.getNActionsExecuted().isMaximum()) return false;
        if (f instanceof Monster && !f.hasAttacked())
        {
            // If the monster already has the Air Immunity passive from using this action, there is no point in using it again
            return !((Monster) f).hasPassive(MonsterAbilities.MonsterPassive.AIR);
        }

        return false;
    }

    // At the start of the monster's turn, remove the Air immunity if it already had it
    public static void removeAirImmunity(DescentGameState dgs, Monster monster) {
        if (monster.hasPassive(MonsterAbilities.MonsterPassive.AIR)) {
            monster.removePassive(MonsterAbilities.MonsterPassive.AIR);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        return figureID == ((Air) o).figureID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), figureID);
    }

    @Override
    public DescentAction copy()
    {
        return new Air(figureID);
    }
}
