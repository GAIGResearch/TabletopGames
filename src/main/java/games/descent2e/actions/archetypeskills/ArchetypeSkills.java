package games.descent2e.actions.archetypeskills;

import com.google.common.collect.Iterables;
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
import games.descent2e.actions.monsterfeats.Air;
import games.descent2e.actions.tokens.SearchAction;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import games.descent2e.components.tokens.DToken;
import utilities.Pair;

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
                    // Can only use the surge if we have the Fatigue to spare,
                    // and we have a Magic or Rune item equipped
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
                        }
                        else f.removeAbility(surge);
                    }
                    else f.removeAbility(surge);
                }

                case "Ghost Armor" -> {
                    GhostArmor ghostArmor = new GhostArmor(f.getComponentID(), skill.getComponentID());
                    if (!f.hasAbility(ghostArmor))
                        f.addAbility(ghostArmor);
                }

                case "Exploding Rune" -> {
                    // TODO: Fix Blast
                }

                case "Inscribe Rune" -> {
                    // Most Runemaster abilities require a weapon with a Rune trait to use
                    // This effectively enables the weapon to have the Rune even if it doesn't normally
                    if (!f.hasBonus(DescentTypes.SkillBonus.InscribeRune))
                        f.addBonus(DescentTypes.SkillBonus.InscribeRune);
                }

                case "Runic Sorcery" -> {
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

                    DescentTypes.AttackType attackType = getAttackType(f);
                    boolean reach = checkReach(dgs, f);
                    List<Integer> targets;

                    if (attackType == DescentTypes.AttackType.MELEE || attackType == DescentTypes.AttackType.BOTH)
                    {
                        targets = getMeleeTargets(dgs, f, reach);
                        for (Integer target : targets) {
                            RunicSorcery runicSorcery = new RunicSorcery(f.getComponentID(), target, true, reach);
                            if (runicSorcery.canExecute(dgs))
                                actions.add(runicSorcery);
                        }
                    }
                    if (attackType == DescentTypes.AttackType.RANGED || attackType == DescentTypes.AttackType.BOTH)
                    {
                        targets = getRangedTargets(dgs, f);
                        for (Integer target : targets) {
                            RunicSorcery runicSorcery = new RunicSorcery(f.getComponentID(), target);
                            if (runicSorcery.canExecute(dgs))
                                actions.add(runicSorcery);
                        }
                    }
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
                            if (Air.checkAir(dgs, f, target))
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

                case "Quick Casting" -> {

                    // Enables us to make additional attacks immediately after attacking, once per turn
                    if (!f.hasBonus(DescentTypes.SkillBonus.QuickCasting)) {
                        f.addBonus(DescentTypes.SkillBonus.QuickCasting);
                        QuickCasting.setCardID(skill.getComponentID());
                    }
                    if (!QuickCasting.isEnabled())
                        QuickCasting.enable();
                }

                // - SCOUT SKILLS
                // Thief
                case "Greedy" -> {
                    // Search for Search tokens within 3 spaces that we can see
                    Set<BoardNode> greedySearch = getNeighboursInRange(dgs, f.getPosition(), 3);
                    for (DToken token : dgs.getTokens()) {
                        if (token.getDescentTokenType() == DescentTypes.DescentToken.Search
                                && token.getPosition() != null) {
                            BoardNode target = dgs.getMasterBoard().getElement(token.getPosition());
                            if (greedySearch.contains(target)) {
                                for (DescentAction da : token.getEffects()) {

                                    // If a default Search action is allowed, don't bother with Greedy
                                    // as it has already been added to the list of actions earlier
                                    if (!(da instanceof Greedy search)) {
                                        if (da.canExecute(dgs))
                                            break;
                                        continue;
                                    }
                                    search.setItemID(dgs);
                                    if (da.canExecute(dgs))
                                        actions.add(da.copy());
                                }
                            }
                        }
                    }
                }

                case "Appraisal" -> {
                    if (dgs.getHistory().isEmpty()) break;
                    Pair<Integer, AbstractAction> lastAction = Iterables.getLast(dgs.getHistory());
                    if (lastAction.a == f.getOwnerId() && lastAction.b instanceof SearchAction search) {
                        if (dgs.getSearchCards().getSize() == 0) break;
                        String nextItem = dgs.getSearchCards().get(0).getComponentName();
                        Appraisal appraisal = new Appraisal(search.getItemID(), nextItem);
                        if (appraisal.canExecute(dgs))
                            actions.add(appraisal);
                    }
                }

                case "Dirty Tricks" -> {
                    DescentTypes.AttackType attackType = getAttackType(f);
                    boolean reach = checkReach(dgs, f);
                    List<Integer> targets;

                    if (attackType == DescentTypes.AttackType.MELEE || attackType == DescentTypes.AttackType.BOTH)
                    {
                        targets = getMeleeTargets(dgs, f, reach);
                        for (Integer target : targets) {
                            DirtyTricks dirtyTricks = new DirtyTricks(f.getComponentID(), target, true, reach);
                            if (dirtyTricks.canExecute(dgs))
                                actions.add(dirtyTricks);
                        }
                    }

                    if (attackType == DescentTypes.AttackType.RANGED || attackType == DescentTypes.AttackType.BOTH)
                    {
                        targets = getRangedTargets(dgs, f);
                        for (Integer target : targets) {
                            DirtyTricks dirtyTricks = new DirtyTricks(f.getComponentID(), target);
                            if (dirtyTricks.canExecute(dgs))
                                actions.add(dirtyTricks);
                        }
                    }

                }

                case "Sneaky" -> {
                    if (!f.hasBonus(DescentTypes.SkillBonus.Sneaky))
                        f.addBonus(DescentTypes.SkillBonus.Sneaky);
                    if (!f.hasMoved() && f.getNActionsExecuted().isMinimum())
                        Sneaky.setLinesOfSight(dgs, f);
                    // TODO: Add Sneaky action to open/close doors freely
                }

                case "Caltrops" -> {
                    if (!f.hasBonus(DescentTypes.SkillBonus.Caltrops)) {
                        f.addBonus(DescentTypes.SkillBonus.Caltrops);
                        Caltrops caltrops = new Caltrops(f.getComponentID(), skill.getComponentID());
                        if (!f.hasAbility(caltrops))
                            f.addAbility(caltrops);
                    }
                }

                case "Tumble" -> {
                    if (f.canIgnoreEnemies())
                        f.setCanIgnoreEnemies(false);
                    Tumble tumble = new Tumble(f.getComponentID(), skill.getComponentID());
                    if (tumble.canExecute(dgs))
                        actions.add(tumble);
                }

                case "Unseen" -> {
                    if (f.hasBonus(DescentTypes.SkillBonus.Unseen))
                        f.removeBonus(DescentTypes.SkillBonus.Unseen);
                    Unseen unseen = new Unseen(f.getComponentID(), skill.getComponentID());
                    if (unseen.canExecute(dgs))
                        actions.add(unseen);
                }

                case "Lurk" -> {
                    if (f.hasBonus(DescentTypes.SkillBonus.Lurk))
                        f.removeBonus(DescentTypes.SkillBonus.Lurk);
                    Lurk.setCardID(skill.getComponentID());
                }

                case "Bushwhack" -> {
                    List<Integer> targets = getRangedTargets(dgs, f);
                    // Bushwhack can only be used if we only have one enemy in line of sight
                    if (targets.size() == 1) {
                        DescentTypes.AttackType attackType = getAttackType(f);
                        boolean reach = checkReach(dgs, f);
                        BushwhackAttack bushwack;

                        if (attackType == DescentTypes.AttackType.MELEE || attackType == DescentTypes.AttackType.BOTH)
                        {
                            bushwack = new BushwhackAttack(f.getComponentID(), targets.get(0), true, reach, skill.getComponentID());
                            if (bushwack.canExecute(dgs))
                                actions.add(bushwack);
                        }

                        if (attackType == DescentTypes.AttackType.RANGED || attackType == DescentTypes.AttackType.BOTH)
                        {
                            bushwack = new BushwhackAttack(f.getComponentID(), targets.get(0), false, false, skill.getComponentID());
                            if (bushwack.canExecute(dgs))
                                actions.add(bushwack);
                        }
                    }
                }

                // --- WARRIOR SKILLS ---
                // Berserker
                case "Rage" -> {
                    boolean reach = checkReach(dgs, f);
                    List<Integer> targets = getMeleeTargets(dgs, f, reach);
                    List<RageAttack> toSort = new ArrayList<>();
                    for (Integer target : targets) {
                        RageAttack rage = new RageAttack(f.getComponentID(), target, reach);
                        if (rage.canExecute(dgs)) toSort.add(rage);
                    }
                    toSort.sort(Comparator.comparingInt(RageAttack::getDefendingFigure));
                    actions.addAll(toSort);
                }

                case "Counter Attack" -> {
                    if (!f.hasBonus(DescentTypes.SkillBonus.CounterAttack)) {
                        f.addBonus(DescentTypes.SkillBonus.CounterAttack);
                        CounterAttack.setCardID(skill.getComponentID());
                    }
                }

                case "Cripple" -> {
                    List<Integer> targets = getMeleeTargets(dgs, f, false);
                    for (Integer target : targets) {
                        Cripple cripple = new Cripple(f.getComponentID(), target, skill.getComponentID());
                        if (cripple.canExecute(dgs))
                            actions.add(cripple);
                    }
                }

                case "Brute" -> {
                    if (!f.hasBonus(DescentTypes.SkillBonus.Brute)) {
                        // Increase our maximum Health by +4, and every time we are revived, we recover +2 extra Health
                        int bruteBonus = 4;
                        f.addBonus(DescentTypes.SkillBonus.Brute);
                        f.getAttribute(Figure.Attribute.Health).setMaximum(f.getAttributeMax(Figure.Attribute.Health) + bruteBonus);
                        f.incrementAttribute(Figure.Attribute.Health, bruteBonus);
                    }
                }

                case "Charge" -> {
                    int range = f.getAttributeMax(Figure.Attribute.MovePoints) + 1
                            + (checkReach(dgs, f) ? 1 : 0);
                    Charge charge = new Charge(f.getComponentID(), range);
                    if (charge.canExecute(dgs)) {
                        charge.setTargets(dgs, f);
                        actions.add(charge);
                    }
                }

                case "Weapon Mastery" -> {
                    WeaponMastery mastery = new WeaponMastery(f.getComponentID(), skill.getComponentID());

                    Deck<DescentCard> hand = f.getHandEquipment();
                    boolean hasOnlyMelee = (hand != null);
                    if (hasOnlyMelee) {
                        for (DescentCard item : hand.getComponents()) {
                            if (item.getAttackType() != DescentTypes.AttackType.MELEE) {
                                hasOnlyMelee = false;
                                break;
                            }
                            if (hand.getComponents().size() == 1)
                            {
                                String[] equipSlots = ((PropertyStringArray) item.getProperty("equipSlots")).getValues();
                                if (equipSlots.length < 2) {
                                    hasOnlyMelee = false;
                                    break;
                                }
                                if (!Arrays.stream(equipSlots).allMatch(i -> Objects.equals(i, "hand"))) {
                                    hasOnlyMelee = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (hasOnlyMelee)
                        if (!f.hasAbility(mastery))
                            f.addAbility(mastery);
                    else
                        if (!f.hasAbility(mastery))
                            f.addAbility(mastery);


                }

                case "Whirlwind" -> {
                    // Whirlwind exclusively targets adjacent monsters
                    List<Integer> targets = getMeleeTargets(dgs, f, false);
                    if (targets.isEmpty()) break;
                    Whirlwind whirlwind = new Whirlwind(f.getComponentID(), targets);
                    if (whirlwind.canExecute(dgs))
                        actions.add(whirlwind);
                }

                case "Execute" -> {
                    for (int i = 0; i < f.getAttributeMax(Figure.Attribute.Fatigue); i++) {
                        Execute execute = new Execute(f.getComponentID(), skill.getComponentID(), i+1);
                        if (!f.hasAbility(execute))
                            f.addAbility(execute);
                    }

                }

                case "Death Rage" -> {
                    boolean reach = checkReach(dgs, f);
                    List<Integer> targets = getMeleeTargets(dgs, f, reach);
                    List<DeathRageAttack> toSort = new ArrayList<>();
                    for (Integer target : targets) {
                        DeathRageAttack deathRage = new DeathRageAttack(f.getComponentID(), target, reach);
                        if (deathRage.canExecute(dgs)) toSort.add(deathRage);
                    }
                    toSort.sort(Comparator.comparingInt(DeathRageAttack::getDefendingFigure));
                    actions.addAll(toSort);
                }
            }

        }

        return actions;
    }
}
