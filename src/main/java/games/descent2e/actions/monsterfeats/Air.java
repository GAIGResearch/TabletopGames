package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.*;
import java.util.Objects;

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
        monster.addActionTaken(getString(dgs));

        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = (Figure) dgs.getComponentById(figureID);
        if (f.getNActionsExecuted().isMaximum()) return false;
        if (f instanceof Monster)
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

    // Check for Attacks if we can legally target the Monster
    public static boolean checkAir(DescentGameState dgs, Figure attacker, Figure target)
    {
        // Must be a Monster, who used the Air ability, and not adjacent to the attacker
        if ((target instanceof Monster))
            if (((Monster) target).hasPassive(MonsterAbilities.MonsterPassive.AIR))
                return !DescentHelper.checkAdjacent(dgs, attacker, target);
        return false;
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
