package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import core.components.GridBoard;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.conditions.RemoveCondition;
import games.descent2e.components.*;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static games.descent2e.components.DiceType.BROWN;
import static games.descent2e.components.DiceType.YELLOW;
import static utilities.Utils.getNeighbourhood;

public class PrayerOfHealing extends Heal implements IExtendedSequence {

    List<Integer> targets = new ArrayList<>();
    int index = 0;
    boolean armorOfFaith = false;
    boolean cleansingTouch = false;
    boolean divineFury = false;
    boolean holyPower = false;
    int surges = 0;
    boolean complete = false;

    public static HashMap<DiceType, Integer> armorOfFaithDice = new HashMap<DiceType, Integer>() {{
        put(BROWN, 1);
    }};

    public static HashMap<DiceType, Integer> divineFuryDice = new HashMap<DiceType, Integer>() {{
        put(YELLOW, 1);
    }};

    public PrayerOfHealing(int targetID, int cardID) {
        super(targetID, cardID);
        this.targets = List.of(targetID);
    }

    public PrayerOfHealing(List<Integer> targets, int cardID) {
        super(targets.get(0), cardID);
        this.targets = new ArrayList<>(targets);
    }

    public void setArmorOfFaith(boolean armorOfFaith) {
        this.armorOfFaith = armorOfFaith;
    }

    public void setCleansingTouch(boolean cleansingTouch) {
        this.cleansingTouch = cleansingTouch;
    }

    public void setDivineFury(boolean divineFury) {
        this.divineFury = divineFury;
    }

    public void setHolyPower(boolean holyPower) {
        this.holyPower = holyPower;
    }

    public boolean isArmorOfFaith() {
        return armorOfFaith;
    }

    public boolean isCleansingTouch() {
        return cleansingTouch;
    }

    public boolean isDivineFury() {
        return divineFury;
    }

    public boolean isHolyPower() {
        return holyPower;
    }

    public static void removePrayerBonus(Hero hero)
    {
        hero.removeBonus(DescentTypes.SkillBonus.ArmorOfFaith);
        hero.removeBonus(DescentTypes.SkillBonus.DivineFury);
    }

    public static void addArmorOfFaithDice(DescentGameState state)
    {
        List<DescentDice> dice = new ArrayList<>(state.getDefenceDicePool().copy().getComponents());
        dice.addAll(DicePool.constructDicePool(armorOfFaithDice).getComponents());
        DicePool newPool = new DicePool(dice);
        state.setDefenceDicePool(newPool);
    }

    public static void addDivineFuryDice(DescentGameState state)
    {
        List<DescentDice> dice = new ArrayList<>(state.getAttackDicePool().copy().getComponents());
        dice.addAll(DicePool.constructDicePool(divineFuryDice).getComponents());
        DicePool newPool = new DicePool(dice);
        state.setAttackDicePool(newPool);
    }

    @Override
    public boolean execute(DescentGameState dgs)
    {
        dgs.setActionInProgress(this);
        super.execute(dgs);
        dgs.getActingFigure().getAttribute(Figure.Attribute.Fatigue).increment();

        Hero target = (Hero) dgs.getComponentById(targetID);
        applyBuffs(target);

        healthRecovered = DicePool.heal.getDamage();
        for (int i = 1; i < targets.size(); i++)
        {
            targetID = targets.get(i);
            target = (Hero) dgs.getComponentById(targetID);
            target.incrementAttribute(Figure.Attribute.Health, healthRecovered);
            applyBuffs(target);
            if (target.isDefeated())
                target.setDefeated(dgs, false);
        }

        while (index < targets.size())
        {
            if (!_computeAvailableActions(dgs).isEmpty()) break;
            index++;
        }

        if (index >= targets.size())
            complete = true;

        return true;
    }

    public void applyBuffs(Hero target)
    {
        if (armorOfFaith)
        {
            target.addBonus(DescentTypes.SkillBonus.ArmorOfFaith);
        }

        if (divineFury)
        {
            target.addBonus(DescentTypes.SkillBonus.DivineFury);
        }

        if (holyPower)
        {
            int surge = DicePool.heal.getSurge();
            if (surge > 0)
            {
                target.getAttribute(Figure.Attribute.Fatigue).decrement();
            }
        }
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        if (!super.canExecute(dgs)) return false;

        Figure f = dgs.getActingFigure();
        if (!(f instanceof Hero)) return false;

        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

        for (int targetID : targets) {

            Figure target = (Figure) dgs.getComponentById(targetID);

            List<DescentCard> skills = ((Hero) f).getSkills().getComponents();
            for (DescentCard skill : skills) {
                String name = skill.getProperty("name").toString();

                // If we have Armor of Faith or Divine Fury, we can always execute the action
                // We might not want the healing specifically, only the dice bonuses
                if (name.equals("Armor of Faith")) return true;
                if (name.equals("Divine Fury")) return true;

                // If we have Cleansing Touch, we can execute the action if the target has a negative condition
                if (name.equals("Cleansing Touch")) {
                    for (DescentTypes.DescentCondition condition : DescentTypes.DescentCondition.values()) {
                        if (target.hasCondition(condition))
                            return true;
                    }
                }
            }

            // If we have none of these upgrades, we can only execute the action if the target needs healing
            if (target.getAttribute(Figure.Attribute.Health).getValue() < target.getAttribute(Figure.Attribute.Health).getMaximum())
                return true;
        }

        // Don't execute if none of the targets are damage, have conditions, or would not gain dice from using this
        return false;
    }

    @Override
    public String getString(AbstractGameState state)
    {
        String targetName = state.getComponentById(targetID).getComponentName().replace("Hero: ", "");
        String string = "Prayer of Healing: Heal " + targetName;

        for (int i = 1; i < targets.size(); i++)
            string += " and " + state.getComponentById(targets.get(i)).getComponentName().replace("Hero: ", "");

        string += " for 1 Red Power Die";
        if (healthRecovered > 0)
            string += " (" + healthRecovered + " Health)";
        if (surges > 0)
            string = string.replace(")"," + 1 Stamina)");
        if (armorOfFaith)
            string = string.replace(")",", Armor of Faith)");
        if (cleansingTouch)
            string = string.replace(")",", Cleansing Touch)");
        if (divineFury)
            string = string.replace(")",", Divine Fury)");
        return string;
    }

    @Override
    public String toString()
    {
        if (targets.size() == 1) return "Prayer of Healing: Heal " + targetID;
        String string = "Prayer of Healing: Heal " + targets.get(0);
        for (int i = 1; i < targets.size(); i++)
            string += " and " + targets.get(i);
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PrayerOfHealing heal = (PrayerOfHealing) o;
        return Objects.equals(targets, heal.targets) && armorOfFaith == heal.armorOfFaith &&
                cleansingTouch == heal.cleansingTouch && divineFury == heal.divineFury &&
                holyPower == heal.holyPower && surges == heal.surges &&
                complete == heal.complete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targets, armorOfFaith, cleansingTouch, divineFury, holyPower, surges, complete);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> retVal = new ArrayList<>();
        Figure target = (Figure) state.getComponentById(targets.get(index));
        for (DescentTypes.DescentCondition condition : target.getConditions())
        {
            RemoveCondition removeCondition = new RemoveCondition(target.getComponentID(), condition);
            if (removeCondition.canExecute((DescentGameState) state))
                retVal.add(removeCondition);
        }
        return retVal;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        if (index < targets.size()) return state.getComponentById(targets.get(index)).getOwnerId();
        return ((DescentGameState) state).getActingFigure().getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        // We only have interrupt actions if we have Cleansing Touch
        if (!cleansingTouch)
            complete = true;
        else {
            if (action instanceof RemoveCondition)
                index++;
        }

        while (index < targets.size())
        {
            if (!_computeAvailableActions(state).isEmpty()) break;
            index++;
        }

        if (index >= targets.size())
            complete = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public PrayerOfHealing copy()
    {
        PrayerOfHealing heal = new PrayerOfHealing(targets, cardID);
        heal.armorOfFaith = armorOfFaith;
        heal.cleansingTouch = cleansingTouch;
        heal.divineFury = divineFury;
        heal.holyPower = holyPower;
        heal.surges = surges;
        heal.healthRecovered = healthRecovered;
        heal.complete = complete;
        return heal;
    }
}
