package games.descent2e.actions.archetypeskills;

import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.Deck;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.attack.Surge;
import games.descent2e.actions.attack.SurgeAttackAction;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import games.descent2e.components.tokens.DToken;

import java.util.*;

import static core.CoreConstants.coordinateHash;
import static core.CoreConstants.playersHash;
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

            switch (skill.getProperty("name").toString()) {

                // --- HEALER SKILLS ---
                // Disciple
                case "Prayer of Healing" -> {
                    for (Hero hero : dgs.getHeroes()) {
                        PrayerOfHealing heal = new PrayerOfHealing(hero.getComponentID(), skill.getComponentID());
                        if (heal.canExecute(dgs)) actions.add(heal);
                    }
                }

                case "Armor of Faith" -> {
                    for (AbstractAction action : actions) {
                        if (action instanceof PrayerOfHealing)
                            ((PrayerOfHealing) action).setArmorOfFaith(true);
                    }
                }

                case "Cleansing Touch" -> {
                    for (AbstractAction action : actions) {
                        if (action instanceof PrayerOfHealing)
                            ((PrayerOfHealing) action).setCleansingTouch(true);
                    }
                }

                case "Blessed Strike" -> {
                    // Blessed Strike requires a melee weapon equipped
                    DescentTypes.AttackType attackType = getAttackType(f);
                    if (attackType == DescentTypes.AttackType.MELEE || attackType == DescentTypes.AttackType.BOTH) {
                        // Blessed Strike can still be used to heal only ourselves, so we don't check if we are the ally
                        for (Hero hero : dgs.getHeroes()) {
                            boolean reach = checkReach(dgs, f);
                            BlessedStrike blessedStrike = new BlessedStrike(f.getComponentID(), hero.getComponentID(), reach);
                            if (blessedStrike.canExecute(dgs))
                                actions.add(blessedStrike);
                        }
                    }

                }

                case "Divine Fury" -> {
                    for (AbstractAction action : actions) {
                        if (action instanceof PrayerOfHealing)
                            ((PrayerOfHealing) action).setDivineFury(true);
                    }
                }

                case "Prayer of Peace" -> {
                    PrayerOfPeace peace = new PrayerOfPeace(skill.getComponentID());
                    if (peace.canExecute(dgs)) actions.add(peace);
                }

                case "Time of Need" -> {
                    TimeOfNeed time = new TimeOfNeed(skill.getComponentID());
                    if (time.canExecute(dgs)) actions.add(time);
                }

                case "Holy Power" -> {
                    // Holy Power allows us to target up to two adjacent Heroes with Prayer of Healing
                    // So we need to find all possible pair combinations
                    // then, take out the old options, and add in the new ones
                    List<PrayerOfHealing> toRemove = new ArrayList<>();
                    List<PrayerOfHealing> toAdd = new ArrayList<>();
                    for (AbstractAction action : actions) {
                        if (action instanceof PrayerOfHealing) {
                            ((PrayerOfHealing) action).setHolyPower(true);
                            toRemove.add((PrayerOfHealing) action);
                        }
                    }

                    // If there were no Prayers of Healing to begin with, don't bother
                    if (toRemove.isEmpty()) break;

                    PrayerOfHealing example = toRemove.get(0);
                    int cardID = example.getCardID();                   // Make sure we use Prayer of Healing here
                    boolean armorOfFaith = example.isArmorOfFaith();
                    boolean cleansingTouch = example.isCleansingTouch();
                    boolean divineFury = example.isDivineFury();
                    boolean holyPower = true;                           // It's this skill, of course it's going to be true.

                    List<Integer> healTargets = new ArrayList<>();
                    healTargets.add(f.getComponentID());
                    healTargets.addAll(getAdjacentTargets(dgs, f, true));

                    for (int i = 0; i < healTargets.size(); i++) {
                        int targetA = healTargets.get(i);
                        if (!(dgs.getComponentById(targetA) instanceof Hero)) continue;
                        for (int j = i + 1; j < healTargets.size(); j++) {
                            int targetB = healTargets.get(j);
                            if (!(dgs.getComponentById(targetB) instanceof Hero)) continue;
                            List<Integer> targetPair = new ArrayList<>();
                            targetPair.add(targetA);
                            targetPair.add(targetB);

                            PrayerOfHealing healing = new PrayerOfHealing(targetPair, cardID);
                            if (healing.canExecute(dgs))
                            {
                                healing.setArmorOfFaith(armorOfFaith);
                                healing.setCleansingTouch(cleansingTouch);
                                healing.setDivineFury(divineFury);
                                healing.setHolyPower(holyPower);
                                toAdd.add(healing);
                            }
                        }
                    }
                    // No point replacing the old actions if there's nothing new to replace them with
                    if (toAdd.isEmpty()) break;
                    actions.removeAll(toRemove);
                    actions.addAll(toAdd);
                }

                case "Radiant Light" -> {
                    List<Integer> targets = new ArrayList<>();
                    targets.add(f.getComponentID());

                    // Check every single figure on the board for lines of sight
                    for (Hero hero : dgs.getHeroes()) {
                        if (hero.equals(f)) continue;
                        if (hasLineOfSight(dgs, f.getPosition(), hero.getPosition()))
                            targets.add(hero.getComponentID());
                    }
                    for (List<Monster> monsters : dgs.getMonsters()) {
                        for (Monster monster : monsters) {
                            if (hasLineOfSight(dgs, f.getPosition(), monster.getPosition()))
                                targets.add(monster.getComponentID());
                        }
                    }

                    RadiantLight radiantLight = new RadiantLight(targets, skill.getComponentID());
                    if (radiantLight.canExecute(dgs))
                        actions.add(radiantLight);
                }

                // --- MAGE SKILLS ---

                // Runemaster
                case "Runic Knowledge" -> {
                    SurgeAttackAction surge = new SurgeAttackAction(Surge.RUNIC_KNOWLEDGE, f.getComponentID());
                    // Can only use the surge if we have the Fatigue to spare
                    // and we have a Magic or Rune item equipped
                    if (!f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) {
                        Deck<DescentCard> hand = f.getHandEquipment();
                        if (hand != null) {
                            boolean hasMagicOrRuneItem = f.hasBonus(DescentTypes.SkillBonus.InscribeRune);
                            if (!hasMagicOrRuneItem) {
                                for (DescentCard item : hand.getComponents()) {
                                    String[] equipmentType = ((PropertyStringArray) item.getProperty("equipmentType")).getValues();
                                    if (equipmentType == null) continue;
                                    if (Arrays.asList(equipmentType).contains("Magic") || Arrays.asList(equipmentType).contains("Rune")) {
                                        hasMagicOrRuneItem = true;
                                        break;
                                    }
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
                }

                case "Ghost Armor" -> {
                    GhostArmor ghostArmor = new GhostArmor(f.getComponentID(), skill.getComponentID());
                    if (!f.hasAbility(ghostArmor))
                        f.addAbility(ghostArmor);
                }

                case "Exploding Rune" -> {

                }

                case "Inscribe Rune" -> {
                    // Most Runemaster abilities require a weapon with a Rune trait to use
                    // This effectively enables the weapon to have the Rune even if it doesn't normally
                    if (!f.hasBonus(DescentTypes.SkillBonus.InscribeRune))
                        f.addBonus(DescentTypes.SkillBonus.InscribeRune);
                }

                case "Runic Sorcery" -> {

                }

                case "Iron Will" -> {
                    if (!f.hasBonus(DescentTypes.SkillBonus.IronWill)) {
                        SurgeAttackAction oldFatigue = new SurgeAttackAction(Surge.RECOVER_1_FATIGUE, f.getComponentID());
                        if (f.getAbilities().contains(oldFatigue)) {
                            // Increase our max Fatigue by +1, and every Surge now recovers +2 Fatigue instead of +1
                            f.getAttribute(Figure.Attribute.Fatigue).setMaximum(f.getAttributeMax(Figure.Attribute.Fatigue) + 1);
                            f.removeAbility(oldFatigue);
                            SurgeAttackAction newFatigue = new SurgeAttackAction(Surge.RECOVER_2_FATIGUE, f.getComponentID());
                            f.addAbility(newFatigue);
                            f.addBonus(DescentTypes.SkillBonus.IronWill);
                        }
                    }
                }

                case "Rune Mastery" -> {

                    Deck<DescentCard> hand = f.getHandEquipment();
                    if (hand == null) break;
                    boolean hasRuneItem = f.hasBonus(DescentTypes.SkillBonus.InscribeRune);
                    if (!hasRuneItem) {
                        for (DescentCard item : hand.getComponents()) {
                            String[] equipmentType = ((PropertyStringArray) item.getProperty("equipmentType")).getValues();
                            if (equipmentType == null) continue;
                            if (Arrays.asList(equipmentType).contains("Rune")) {
                                hasRuneItem = true;
                                break;
                            }
                        }
                    }

                    RuneMastery runeMastery = new RuneMastery(skill.getComponentID());

                    // Enable the ability only if we have a suitable weapon
                    if (hasRuneItem) {
                        if (!f.hasAbility(runeMastery))
                            f.addAbility(runeMastery);
                    }
                    else if (f.hasAbility(runeMastery))
                        f.removeAbility(runeMastery);

                }

                case "Break the Rune" -> {
                    Deck<DescentCard> hand = f.getHandEquipment();
                    if (hand == null) break;
                    boolean hasRuneItem = f.hasBonus(DescentTypes.SkillBonus.InscribeRune);
                    if (!hasRuneItem) {
                        for (DescentCard item : hand.getComponents()) {
                            String[] equipmentType = ((PropertyStringArray) item.getProperty("equipmentType")).getValues();
                            if (equipmentType == null) continue;
                            if (Arrays.asList(equipmentType).contains("Rune")) {
                                hasRuneItem = true;
                                break;
                            }
                        }
                    }
                    // If we still don't have a legal Rune weapon, don't bother
                    if (!hasRuneItem) break;

                    List<Integer> targets = new ArrayList<>();
                    Set<BoardNode> tiles = getNeighboursInRange(dgs, f.getPosition(), 3);
                    for (BoardNode tile : tiles)
                    {
                        int neighbourID = ((PropertyInt) tile.getProperty(playersHash)).value;
                        if (neighbourID == -1 || neighbourID == f.getComponentID()) continue;
                        if (hasLineOfSight(dgs, f.getPosition(), ((PropertyVector2D) tile.getProperty(coordinateHash)).values)) {
                            Figure target = (Figure) dgs.getComponentById(neighbourID);

                            // Check if the target is an Air Elemental who just used the Air ability
                            // If they are, do not add them to the list if we cannot target them
                            if (target instanceof Monster)
                                if (((Monster) target).hasPassive(MonsterAbilities.MonsterPassive.AIR))
                                    if (!checkAdjacent(dgs, f, target))
                                        continue;
                            targets.add(neighbourID);
                        }
                    }

                    if (!targets.isEmpty())
                    {
                        BreakTheRune breakRune = new BreakTheRune(f.getComponentID(), targets);
                        if (breakRune.canExecute(dgs))
                            actions.add(breakRune);
                    }



                }

                case "Quick Casting ->" -> {

                }

                // - SCOUT SKILLS
                // Thief
                case "Greedy" -> {
                    // Search for Search tokens within 3 spaces that we can see
                    for (DToken token : dgs.getTokens()) {
                        if (token.getDescentTokenType() == DescentTypes.DescentToken.Search
                                && token.getPosition() != null
                                && (inRange(f.getPosition(), token.getPosition(), 3))
                                && hasLineOfSight(dgs, f.getPosition(), token.getPosition())) {
                            for (DescentAction da : token.getEffects()) {
                                if (da.canExecute(dgs)) actions.add(da.copy());
                            }
                        }
                    }
                }

                // --- WARRIOR SKILLS ---
                // Berserker
                case "Rage" -> actions.addAll(rageAttackActions(dgs, f));
            }

        }

        return actions;
    }

    private static List<AbstractAction> rageAttackActions(DescentGameState dgs, Figure f) {

        boolean reach = checkReach(dgs, f);

        List<Integer> targets = getMeleeTargets(dgs, f, reach);
        List<RageAttack> actions = new ArrayList<>();

        for (Integer target : targets) {
            RageAttack rage = new RageAttack(f.getComponentID(), target, reach);
            if (rage.canExecute(dgs)) actions.add(rage);
        }

        Collections.sort(actions, Comparator.comparingInt(RageAttack::getDefendingFigure));

        List<AbstractAction> sortedActions = new ArrayList<>();

        sortedActions.addAll(actions);

        return sortedActions;
    }
}
