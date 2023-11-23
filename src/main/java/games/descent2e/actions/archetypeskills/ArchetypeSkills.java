package games.descent2e.actions.archetypeskills;

import core.actions.AbstractAction;
import core.components.Deck;
import games.descent2e.DescentGameState;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static games.descent2e.DescentHelper.getMeleeTargets;

public class ArchetypeSkills {

    public static List<AbstractAction> getArchetypeSkillActions (DescentGameState dgs, int figureID)
    {
        List<AbstractAction> actions = new ArrayList<>();
        Hero f = (Hero) dgs.getComponentById(figureID);

        Deck<DescentCard> skills = f.getSkills();
        if (skills == null || skills.getSize() == 0) return actions;

        for (DescentCard skill : (f.getSkills().getComponents())) {

            switch(skill.getProperty("name").toString())
            {
                // Berserker
                case "Rage":
                    actions.addAll(rageAttackActions(dgs, f));
                    break;

                // Disciple
                case "Prayer of Healing":
                    break;

                    // Runemaster
                case "Runic Knowledge":
                    break;

                    // Thief
                case "Greedy":
                    break;
            }

        }

        return actions;
    }

    private static List<AbstractAction> rageAttackActions(DescentGameState dgs, Figure f) {

        List<Integer> targets = getMeleeTargets(dgs, f);
        List<RageAttack> actions = new ArrayList<>();

        for (Integer target : targets) {
            RageAttack rage = new RageAttack(f.getComponentID(), target);
            if (rage.canExecute(dgs)) actions.add(rage);
        }

        Collections.sort(actions, Comparator.comparingInt(RageAttack::getDefendingFigure));

        List<AbstractAction> sortedActions = new ArrayList<>();

        sortedActions.addAll(actions);

        return sortedActions;
    }
}
