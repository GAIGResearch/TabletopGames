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

import static games.descent2e.abilities.HeroAbilities.HeroAbility.*;

public class HeroAbilities {

    public enum HeroAbility {
        CopyAllyDefense,
        DamageMinusOne,
        DamageToFatigue,
        HealCondition,
        HealFatigueOnWait,
        RerollOnce,
        StunAdjacent,
        SurgeRecoverOneHeart,
        NONE;
    }

    // Self-Contained Class for all Hero Abilities

    // ----- HEALER -----

    // Ashrian's Hero Ability
    // Any Minion Monsters that are activated adjacent to Ashrian are forced to take the Stunned condition
    public static void ashrian(DescentGameState dgs) {
        Figure actingFigure = dgs.getActingFigure();
        if (!(actingFigure instanceof Monster) || !actingFigure.getNActionsExecuted().isMinimum()) return;
        if (actingFigure.getName().toLowerCase().contains("minion")) {
            for (Hero hero : dgs.getHeroes()) {
                if (hero.getAbility().equals(StunAdjacent)) {
                    Vector2D position = actingFigure.getPosition();
                    Vector2D other = hero.getPosition();
                    if (DescentHelper.inRange(position, other, 1)) {
                        hero.setUsedHeroAbility(true);
                        actingFigure.addCondition(DescentTypes.DescentCondition.Stun);
                    }
                }
            }
        }
    }

    // Avric Albright's Hero Ability
    // If we are a Hero (including Avric himself) within 3 spaces of Avric, we gain a Surge action of Recover 1 Heart
    public static void avric(DescentGameState dgs) {
        Figure actingFigure = dgs.getActingFigure();
        if (!(actingFigure instanceof Hero)) return;
        for (Hero hero : dgs.getHeroes()) {
            if (hero.getAbility().equals(SurgeRecoverOneHeart)) {
                Vector2D position = actingFigure.getPosition();
                Vector2D other = hero.getPosition();
                SurgeAttackAction surge = new SurgeAttackAction(Surge.RECOVER_1_HEART, actingFigure.getComponentID());
                if (actingFigure.equals(hero) || (DescentHelper.inRange(position, other, 3)))
                {
                    hero.setUsedHeroAbility(true);
                    if (!actingFigure.getAbilities().contains(surge)) {
                        actingFigure.addAbility(surge);
                    }
                } else {
                    if (actingFigure.getAbilities().contains(surge)) {
                        actingFigure.removeAbility(surge);
                    }
                }
            }
        }
    }

    // ----- MAGE -----

    // Leoric of the Book's Hero Ability
    // If a Monster is within 3 spaces of Leoric, its attacks deal -1 Heart (to a minimum of 1)
    public static int leoric(DescentGameState dgs, Figure actingFigure, int damage) {
        for (Hero hero : dgs.getHeroes()) {
            if (hero.getAbility().equals(DamageMinusOne)) {
                Vector2D position = actingFigure.getPosition();
                Vector2D other = hero.getPosition();
                if (DescentHelper.inRange(position, other, 3)) {
                    hero.setUsedHeroAbility(true);
                    // Leoric can only reduce damage to a minimum of 1
                    if (damage > 1)
                        return damage - 1;
                }
            }
        }
        return damage;
    }

    // Widow Tarha's Hero Ability
    // Once per round, when we make an attack roll, we may reroll one attack or power die, and must keep the new result
    public static int tarha(DescentGameState dgs, DescentDice dice) {
        int face = dice.getFace();
        Figure actingFigure = dgs.getActingFigure();
        if (!(actingFigure instanceof Hero)) return face;
        Hero f = (Hero) actingFigure;
        // We can only do this once per round
        if (f.getAbility().equals(RerollOnce) && !f.hasUsedHeroAbility()) {
            DiceType type = dice.getColour();
            //System.out.println("Widow Tarha rerolls the " + type + " die");
            DicePool reroll = DicePool.constructDicePool(new HashMap<DiceType, Integer>() {{
                put(type, 1);
            }});
            reroll.roll((dgs.getRnd()));

            //System.out.println("Old Result: " + face + " (Range: " + dice.getRange() + ", Surge: " + dice.getSurge() + ", Damage: " + dice.getDamage() +")");
            face = reroll.getDice(0).getFace();
            //System.out.println("New Result: " + face + " (Range: " + reroll.getDice(0).getRange() + ", Surge: " + reroll.getDice(0).getSurge() + ", Damage: " + reroll.getDice(0).getDamage() + ")");
            f.setUsedHeroAbility(true);
            f.setRerolled(true);
        }
        return face;
    }

    // ----- SCOUT -----

    // Jain Fairwood's Hero Ability
    // When we take damage, we can convert some (or all) of that damage into Fatigue, up to our max Fatigue
    public static int jain(DescentGameState dgs, int heroId, int reduce)
    {
        Figure actingFigure = (Figure) dgs.getComponentById(heroId);
        if (!(actingFigure instanceof Hero)) return 0;
        if (((Hero) actingFigure).getAbility().equals(DamageToFatigue))
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
            ((Hero) actingFigure).setUsedHeroAbility(true);
        }
        return reduce;
    }

    // Tomble Burrowell's Hero Ability
    // If we are targeted by an attack, and we are adjacent to an ally
    // We can add their defense pool to our own defense pool before we roll
    public static DicePool tomble (DescentGameState dgs, int tomble, int ally)
    {
        Hero hero = (Hero) dgs.getComponentById(tomble);
        Figure other = (Figure) dgs.getComponentById(ally);
        DicePool defensePool = hero.getDefenceDice().copy();
        if (hero.getAbility().equals(CopyAllyDefense)) {
            Vector2D position = hero.getPosition();
            Vector2D otherPosition = other.getPosition();
            if (DescentHelper.inRange(position, otherPosition, 1)) {
                hero.setUsedHeroAbility(true);
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
    public static List<AbstractAction> grisban(DescentGameState dgs) {
        List<AbstractAction> removeConditions = new ArrayList<>();
        Figure actingFigure = dgs.getActingFigure();
        if ((actingFigure instanceof Hero));
        {
            if (grisbanCanAct(dgs, (Hero) actingFigure)) {
                for (DescentTypes.DescentCondition condition : actingFigure.getConditions()) {
                    RemoveCondition removeCondition = new RemoveCondition(actingFigure.getComponentID(), condition);
                    if (removeCondition.canExecute(dgs)) {
                        ((Hero) actingFigure).setUsedHeroAbility(true);
                        removeConditions.add(removeCondition);
                    }
                }
            }
        }
        return removeConditions;
    }

    // Ensures that Grisban's turn isn't ended before he can use his Hero Ability
    public static boolean grisbanCanAct(DescentGameState dgs, Hero actingFigure) {
        if (actingFigure.getAbility().equals(HealCondition))
            return actingFigure.hasRested() && (actingFigure.getConditions().size() > 0) && !actingFigure.hasRemovedConditionThisTurn();
        return false;
    }

    // Syndrael's Hero Ability
    // If Syndrael has not moved this turn, recover 2 Fatigue
    public static void syndrael(DescentGameState dgs) {
        Figure actingFigure = dgs.getActingFigure();
        if (!(actingFigure instanceof Hero)) return;
        if (((Hero) actingFigure).getAbility().equals(HealFatigueOnWait) && !actingFigure.hasMoved()) {
            actingFigure.decrementAttribute(Figure.Attribute.Fatigue, 2);
            ((Hero) actingFigure).setUsedHeroAbility(true);
        }
    }
}
