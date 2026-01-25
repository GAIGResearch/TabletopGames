package games.descent2e.actions.archetypeskills;

import core.actions.AbstractAction;
import core.components.Deck;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.attack.Surge;
import games.descent2e.actions.attack.SurgeAttackAction;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.tokens.DToken;

import java.util.*;

import static games.descent2e.DescentHelper.*;

public class ArchetypeSkills {

    public static List<AbstractAction> getArchetypeSkillActions (DescentGameState dgs, int figureID)
    {
        List<AbstractAction> actions = new ArrayList<>();
        Hero f = (Hero) dgs.getComponentById(figureID);

        Deck<DescentCard> skills = f.getSkills();
        if (skills == null || skills.getSize() == 0) return actions;

        for (DescentCard skill : (f.getSkills().getComponents())) {

            // If the skill is exhausted, skip it
            if(f.isExhausted(skill)) continue;

            switch(skill.getProperty("name").toString())
            {
                // Berserker
                case "Rage":
                    actions.addAll(rageAttackActions(dgs, f));
                    break;

                // Disciple
                case "Prayer of Healing":
                    for (Hero hero : dgs.getHeroes())
                    {
                        Heal heal = new Heal(hero.getComponentID(), skill.getComponentID());
                        if (heal.canExecute(dgs)) actions.add(heal);
                    }
                    break;

                    // Runemaster
                case "Runic Knowledge":
                    SurgeAttackAction surge = new SurgeAttackAction(Surge.RUNIC_KNOWLEDGE, f.getComponentID());
                    // Can only use the surge if we have the Fatigue to spare
                    // and we have a Magic or Rune item equipped
                    if (!f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) {
                        Deck<DescentCard> hand = f.getHandEquipment();
                        if (hand != null) {
                            boolean hasMagicOrRuneItem = false;
                            for (DescentCard item : hand.getComponents()) {
                                String[] equipmentType = ((PropertyStringArray) item.getProperty("equipmentType")).getValues();
                                if (equipmentType == null) continue;
                                if (Arrays.asList(equipmentType).contains("Magic") || Arrays.asList(equipmentType).contains("Rune")) {
                                    hasMagicOrRuneItem = true;
                                    break;
                                }
                            }
                            if (hasMagicOrRuneItem) {
                                if (!f.getAbilities().contains(surge)) {
                                    f.addAbility(surge);
                                }
                                break;
                            }
                        }
                    }
                    f.removeAbility(surge);
                    break;

                    // Thief
                case "Greedy":
                    // Search for Search tokens within 3 spaces that we can see
                    for (DToken token : dgs.getTokens()) {
                        if (token.getDescentTokenType() == DescentTypes.DescentToken.Search
                                && token.getPosition() != null
                                && (inRange(f.getPosition(), token.getPosition(), 3))
                                && hasLineOfSight(dgs, f.getPosition(), token.getPosition())) {
                            for (DescentAction da : token.getEffects()) {
                                if(da.canExecute(dgs)) actions.add(da.copy());
                            }
                        }
                    }
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
