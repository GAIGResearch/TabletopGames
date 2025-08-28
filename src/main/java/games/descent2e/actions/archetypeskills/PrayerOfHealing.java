package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.components.GridBoard;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.*;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static games.descent2e.components.DiceType.BROWN;
import static games.descent2e.components.DiceType.YELLOW;
import static utilities.Utils.getNeighbourhood;

public class PrayerOfHealing extends Heal {

    boolean armorOfFaith = false;
    boolean cleansingTouch = false;
    boolean divineFury = false;
    boolean holyPower = false;
    int surges = 0;

    public static HashMap<DiceType, Integer> armorOfFaithDice = new HashMap<DiceType, Integer>() {{
        put(BROWN, 1);
    }};

    public static HashMap<DiceType, Integer> divineFuryDice = new HashMap<DiceType, Integer>() {{
        put(YELLOW, 1);
    }};

    public PrayerOfHealing(int targetID, int cardID) {
        super(targetID, cardID);
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
        super.execute(dgs);
        dgs.getActingFigure().getAttribute(Figure.Attribute.Fatigue).increment();

        Hero target = (Hero) dgs.getComponentById(targetID);
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

        return true;
    }

    @Override
    public boolean canExecute(DescentGameState dgs)
    {
        if (!super.canExecute(dgs)) return false;

        Figure f = dgs.getActingFigure();
        if (!(f instanceof Hero)) return false;

        if (f.getAttribute(Figure.Attribute.Fatigue).isMaximum()) return false;

        Figure target = (Figure) dgs.getComponentById(targetID);

        List<DescentCard> skills = ((Hero) f).getSkills().getComponents();
        for (DescentCard skill : skills)
        {
            String name = skill.getProperty("name").toString();

            // If we have Armor of Faith or Divine Fury, we can always execute the action
            // We might not want the healing specifically, only the dice bonuses
            if (name.equals("Armor of Faith")) return true;
            if (name.equals("Divine Fury")) return true;

            // If we have Cleansing Touch, we can execute the action if the target has a negative condition
            if (name.equals("Cleansing Touch")) {
                for (DescentTypes.DescentCondition condition : DescentTypes.DescentCondition.values())
                {
                    if (target.hasCondition(condition))
                        return true;
                }
            }
        }

        // If we have none of these upgrades, we can only execute the action if the target needs healing
        return (target.getAttribute(Figure.Attribute.Health).getValue() < target.getAttribute(Figure.Attribute.Health).getMaximum());
    }

    @Override
    public String getString(AbstractGameState state)
    {
        String targetName = state.getComponentById(targetID).getComponentName().replace("Hero: ", "");
        String string = "Prayer of Healing: Heal " + targetName + " for 1 Red Power Die";
        if (healthRecovered > 0)
            string += " (" + healthRecovered + " Health";
        if (surges > 0)
            string += "+ 1 Fatigue";
        if (armorOfFaith)
            string += ", Armor of Faith";
        if (cleansingTouch)
            string += ", Cleansing Touch";
        if (divineFury)
            string += ", Divine Fury";
        string += ")";
        return string;
    }

    @Override
    public String toString()
    {
        return "Prayer of Healing: Heal " + targetID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PrayerOfHealing heal = (PrayerOfHealing) o;
        return armorOfFaith == heal.armorOfFaith && cleansingTouch == heal.cleansingTouch && divineFury == heal.divineFury && holyPower == heal.holyPower && surges == heal.surges;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), armorOfFaith, cleansingTouch, divineFury, holyPower, surges);
    }

    @Override
    public DescentAction copy()
    {
        PrayerOfHealing heal = new PrayerOfHealing(targetID, cardID);
        heal.armorOfFaith = armorOfFaith;
        heal.cleansingTouch = cleansingTouch;
        heal.divineFury = divineFury;
        heal.holyPower = holyPower;
        heal.surges = surges;
        heal.healthRecovered = healthRecovered;
        return heal;
    }
}
