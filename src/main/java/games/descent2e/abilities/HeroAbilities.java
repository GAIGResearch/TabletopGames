package games.descent2e.abilities;

import core.actions.AbstractAction;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.attack.Surge;
import games.descent2e.actions.attack.SurgeAttackAction;
import games.descent2e.actions.conditions.RemoveCondition;
import games.descent2e.components.*;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HeroAbilities {

    // Self-Contained Class for all Hero Abilities

    // ----- HEALER -----

    // Ashrian's Hero Ability
    // If we are a Monster, and we start our turn adjacent to Ashrian, we are forced to take the Stunned condition
    public static void ashrian(DescentGameState dgs, Monster actingFigure) {
        if (actingFigure.getNActionsExecuted().isMinimum()) {
            Hero ashrian = dgs.getHeroByName("Ashrian");
            if (ashrian != null) {
                Vector2D position = actingFigure.getPosition();
                Vector2D other = ashrian.getPosition();
                if (DescentHelper.inRange(position, other, 1)) {
                    ashrian.setUsedHeroAbility(true);
                    actingFigure.addCondition(DescentTypes.DescentCondition.Stun);
                }
            }
        }
    }

    // Avric Albright's Hero Ability
    // If we are a Hero (including Avric himself) within 3 spaces of Avric, we gain a Surge action of Recover 1 Heart
    public static void avric(DescentGameState dgs, Hero actingFigure) {
        Hero avric = dgs.getHeroByName("Avric Albright");
        if (avric != null) {
            Vector2D position = actingFigure.getPosition();
            Vector2D other = avric.getPosition();
            SurgeAttackAction surge = new SurgeAttackAction(Surge.RECOVER_1_HEART, actingFigure.getComponentID());
            if (actingFigure.equals(avric) ||
                    (DescentHelper.inRange(position, other, 3))) {
                avric.setUsedHeroAbility(true);
                if (!actingFigure.getAbilities().contains(surge)) {
                    actingFigure.addAbility(surge);
                }
            }
            else
            {
                if (actingFigure.getAbilities().contains(surge)) {
                    actingFigure.removeAbility(surge);
                }
            }
        }
    }

    // ----- MAGE -----

    // Leoric of the Book's Hero Ability
    // If a Monster is within 3 spaces of Leoric, its attacks deal -1 Heart (to a minimum of 1)
    public static int leoric(DescentGameState dgs, Figure actingFigure, int damage) {
        Hero leoric = dgs.getHeroByName("Leoric");
        if (leoric != null) {
            Vector2D position = actingFigure.getPosition();
            Vector2D other = leoric.getPosition();
            if (DescentHelper.inRange(position, other, 3)) {
                leoric.setUsedHeroAbility(true);
                // Leoric can only reduce damage to a minimum of 1
                if (damage > 1)
                    return damage - 1;
            }
        }
        return damage;
    }

    // Widow Tarha's Hero Ability
    // Once per round, when we make an attack roll, we may reroll one attack or power die, and must keep the new result
    public static int tarha(DescentGameState dgs, Figure actingFigure, DescentDice dice) {
        int face = dice.getFace();
        Hero tarha = dgs.getHeroByName("Widow Tarha");
        if (actingFigure.equals(tarha)) {
            // We can only do this once per round
            if (!((Hero) actingFigure).hasUsedHeroAbility()) {
                DiceType type = dice.getColour();
                System.out.println("Widow Tarha rerolls the " + type + " die");
                DicePool reroll = DicePool.constructDicePool(new HashMap<DiceType, Integer>() {{
                    put(type, 1);
                }});
                reroll.roll((dgs.getRandom()));

                System.out.println("Old Result: " + face + " (Range: " + dice.getRange() + ", Surge: " + dice.getSurge() + ", Damage: " + dice.getDamage() +")");
                face = reroll.getDice(0).getFace();
                System.out.println("New Result: " + face + " (Range: " + reroll.getDice(0).getRange() + ", Surge: " + reroll.getDice(0).getSurge() + ", Damage: " + reroll.getDice(0).getDamage() + ")");
                tarha.setUsedHeroAbility(true);
                tarha.setRerolled(true);
            }
        }
        return face;
    }

    // ----- SCOUT -----

    // Jain Fairwood's Hero Ability
    // When we take damage, we can convert some (or all) of that damage into Fatigue, up to our max Fatigue
    public static int jain(DescentGameState dgs, Figure actingFigure, int reduce)
    {
        if (actingFigure.equals(dgs.getHeroByName("Jain")))
        {
            int maxFatigue = actingFigure.getAttributeMax(Figure.Attribute.Fatigue);
            int currentFatigue = actingFigure.getAttributeValue(Figure.Attribute.Fatigue);
            if (currentFatigue + reduce <= maxFatigue)
            {
                actingFigure.incrementAttribute(Figure.Attribute.Fatigue, reduce);
            }
            else
            {
                actingFigure.setAttributeToMax(Figure.Attribute.Fatigue);
                reduce = maxFatigue - currentFatigue;
            }
        }
        return reduce;
    }

    // Tomble Burrowell's Hero Ability
    // If we are targeted by an attack, and we are adjacent to an ally
    // We can add their defense pool to our own defense pool before we roll
    public static DicePool tomble (DescentGameState dgs, Figure actingFigure, Figure other)
    {
        Hero tomble = dgs.getHeroByName("Tomble Burrowell");
        DicePool defensePool = actingFigure.getDefenceDice().copy();
        if (actingFigure.equals(tomble)) {
            Vector2D position = actingFigure.getPosition();
            Vector2D otherPosition = other.getPosition();
            if (DescentHelper.inRange(position, otherPosition, 1)) {
                tomble.setUsedHeroAbility(true);
                DicePool allyDefensePool = other.getDefenceDice();
                List<DescentDice> allDice = new ArrayList<>(defensePool.getComponents());
                allDice.addAll(allyDefensePool.getComponents());
                defensePool.setDice(allDice);
            }
        }
        return defensePool;
    }

    // ----- WARRIOR -----

    // Grisban the Thirsty's Hero Ability
    // If we have used the Rest action this turn, we can remove 1 Condition from ourselves
    public static List<AbstractAction> grisban(DescentGameState dgs, Hero actingFigure) {
        List<AbstractAction> removeConditions = new ArrayList<>();
        if (grisbanCanAct(dgs, actingFigure)) {
            for (DescentTypes.DescentCondition condition : actingFigure.getConditions()) {
                RemoveCondition removeCondition = new RemoveCondition(actingFigure, condition);
                if (removeCondition.canExecute(dgs)) {
                    actingFigure.setUsedHeroAbility(true);
                    removeConditions.add(removeCondition);
                }
            }
        }
        return removeConditions;
    }

    // Ensures that Grisban's turn isn't ended before he can use his Hero Ability
    public static boolean grisbanCanAct(DescentGameState dgs, Hero actingFigure) {
        if (actingFigure.equals(dgs.getHeroByName("Grisban")))
            return actingFigure.hasRested() && (actingFigure.getConditions().size() > 0) && !actingFigure.hasRemovedConditionThisTurn();
        return false;
    }

    // Syndrael's Hero Ability
    // If Syndrael has not moved this turn, recover 2 Fatigue
    public static void syndrael(DescentGameState dgs, Figure actingFigure) {
        if (actingFigure.equals(dgs.getHeroByName("Syndrael")) && !actingFigure.hasMoved()) {
            actingFigure.decrementAttribute(Figure.Attribute.Fatigue, 2);
            ((Hero) actingFigure).setUsedHeroAbility(true);
        }
    }
}
