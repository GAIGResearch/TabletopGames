package games.descent;

import core.actions.AbstractAction;
import core.properties.PropertyInt;
import games.descent2e.DescentForwardModel;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.*;
import games.descent2e.actions.monsterfeats.Howl;
import games.descent2e.components.*;
import org.junit.Before;
import org.junit.Test;
import utilities.Vector2D;

import java.util.List;

import static org.junit.Assert.*;

public class MeleeAttackTests {

    DescentGameState state;
    DescentForwardModel fm = new DescentForwardModel();

    @Before
    public void setup() {
        long seed = 234;
        // a rather cruddy way of ensuring we get the right hero in the right place
        do {
            seed++;
            DescentParameters params = new DescentParameters();
            params.setRandomSeed(seed);
            state = new DescentGameState(new DescentParameters(), 2);
            fm.setup(state);
        } while (!state.getHeroes().get(0).getName().equals("Hero: Avric Albright"));
    }

    @Test
    public void firstPlayerIsNotOverlord() {
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void attackRollsDoesDamage() {
        Figure actingFigure = state.getActingFigure();
        Figure victim = state.getMonsters().get(0).get(0);
        List<DescentCard> weapons = ((Hero) actingFigure).getWeapons();

        assertEquals(1, weapons.size());

        // Force the dice to roll a result that goes all the way to the end without interruptions
        DicePool attackDice = actingFigure.getAttackDice().copy();
        while (attackDice.getRange() <= 0 || attackDice.getDamage() <= 0)
            attackDice.roll(state.getRnd());

        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttackOverride(
                actingFigure.getComponentID(), victim.getComponentID(), attackDice, null);
        assertEquals(0, state.getAttackDicePool().getSize());
        attack.execute(state);

        // Might pause because we roll a Surge or have a reaction - here we ensure it continues
        while (!attack.executionComplete(state)) {
            AbstractAction action = attack._computeAvailableActions(state).get(0);
            action.execute(state);
            attack._afterAction(state, action);
        }
        assertEquals(attack, state.currentActionInProgress());
        assertEquals(2, state.getAttackDicePool().getSize());
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.YELLOW));
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.BLUE));
        assertTrue(state.getAttackDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage() + attack.getExtraDamage();
        int shields = state.getDefenceDicePool().getShields() + attack.getExtraDefence() - attack.getPierce();
        damage = Math.max(damage - shields, 0);
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());
    }

    @Test
    public void monsterCanAttackHero() {
        Monster attacker = state.getMonsters().get(0).get(1);
        Figure victim = state.getActingFigure();

        // Force the dice to roll a result that goes all the way to the end without interruptions
        DicePool attackDice = attacker.getAttackDice().copy();
        while (attackDice.getRange() <= 0 || attackDice.getDamage() <= 0)
            attackDice.roll(state.getRnd());

        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttackDamageOnly(
                attacker.getComponentID(), victim.getComponentID(), attackDice, null);
        assertEquals(0, state.getAttackDicePool().getSize());
        attack.execute(state);

        // Might pause because we roll a Surge or have a reaction - here we ensure it continues
        while (!attack.executionComplete(state)) {
            AbstractAction action = attack._computeAvailableActions(state).get(0);
            action.execute(state);
            attack._afterAction(state, action);
        }


        assertEquals(2, state.getAttackDicePool().getSize());
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.YELLOW));
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.BLUE));
        assertTrue(state.getAttackDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage() + attack.getExtraDamage();
        int shields = state.getDefenceDicePool().getShields() + attack.getExtraDefence() - attack.getPierce();
        damage = Math.max(damage - shields, 0);
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());
    }

    @Test
    public void monsterRollsDefenceDieAfterAttack() {
        Figure actingFigure = state.getActingFigure();
        Figure victim = state.getMonsters().get(0).get(0);

        // Force the dice to roll a result that goes all the way to the end without interruptions
        DicePool attackDice = actingFigure.getAttackDice().copy();
        while (attackDice.getRange() <= 0 || attackDice.getDamage() <= 0)
            attackDice.roll(state.getRnd());

        // Ensure we always roll a shield
        DicePool defenceDice = victim.getDefenceDice().copy();
        while (defenceDice.getShields() <= 0)
            defenceDice.roll(state.getRnd());

        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttackOverride(
                actingFigure.getComponentID(), victim.getComponentID(), attackDice, defenceDice);
        attack.execute(state);

        // Might pause because we roll a Surge or have a reaction - here we ensure it continues
        while (!attack.executionComplete(state)) {
            AbstractAction action = attack._computeAvailableActions(state).get(0);
            action.execute(state);
            attack._afterAction(state, action);
        }

        assertEquals(1, state.getDefenceDicePool().getSize());
        assertEquals(1, state.getDefenceDicePool().getNumber(DiceType.GREY));
        assertEquals(0, state.getDefenceDicePool().getNumber(DiceType.BROWN));
        assertTrue(state.getDefenceDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage() + attack.getExtraDamage();
        int shields = state.getDefenceDicePool().getShields() + attack.getExtraDefence() - attack.getPierce();
        damage = Math.max(damage - shields, 0);
        assertTrue(shields > 0);
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());
    }

    @Test
    public void heroRollsDefenceDieAfterAttack() {
        Figure actingFigure = state.getMonsters().get(0).get(0);
        Figure victim = state.getActingFigure();
        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();

        // Force the dice to roll a result that goes all the way to the end without interruptions
        DicePool attackDice = actingFigure.getAttackDice().copy();
        while (attackDice.getRange() <= 0 || attackDice.getDamage() <= 0)
            attackDice.roll(state.getRnd());

        // Ensure we always roll a shield
        DicePool defenceDice = victim.getDefenceDice().copy();
        while (defenceDice.getShields() <= 0)
            defenceDice.roll(state.getRnd());

        MeleeAttack attack = new MeleeAttackOverride(
                actingFigure.getComponentID(), victim.getComponentID(), attackDice, defenceDice);
        attack.execute(state);

        // Might pause because we roll a Surge or have a reaction - here we ensure it continues
        while (!attack.executionComplete(state)) {
            AbstractAction action = attack._computeAvailableActions(state).get(0);
            action.execute(state);
            attack._afterAction(state, action);
        }

        assertEquals(1, state.getDefenceDicePool().getSize());
        assertEquals(1, state.getDefenceDicePool().getNumber(DiceType.GREY));
        assertEquals(0, state.getDefenceDicePool().getNumber(DiceType.BROWN));
        assertTrue(state.getDefenceDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage() + attack.getExtraDamage();
        int shields = state.getDefenceDicePool().getShields() + attack.getExtraDefence() - attack.getPierce();
        damage = Math.max(damage - shields, 0);
        assertTrue(shields > 0);
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());
    }


    @Test
    public void rangedAttackHits() {
        Figure actingFigure = state.getActingFigure();
        Figure victim = state.getMonsters().get(0).get(0);
        actingFigure.setPosition(new Vector2D(4, 3));
        victim.setPosition(new Vector2D(5, 6));
        // this gives a Chebyshev distance of 3, which should mena some attacks miss

        int missed = 0, outOfRange = 0, noDamage = 0;
        for (int loop = 0; loop < 100; loop++) {
            MeleeAttack attack = new RangedAttack(actingFigure.getComponentID(), victim.getComponentID());
            attack.execute(state);
            //        System.out.println(state.getAttackDicePool().toString());
            assertTrue(state.getAttackDicePool().hasRolled());
            if (attack.attackMissed(state)) {
                missed++;
            }
            if (state.getAttackDicePool().getRange() < 3) {
                outOfRange++;
            }
            if (state.getAttackDicePool().getDamage() == 0)
                noDamage++;
        }
        System.out.printf("Missed: %d, Out of Range: %d, No Damage Done: %d%n", missed, outOfRange, noDamage);
        assertTrue(missed >= 0);
        assertTrue(outOfRange >= 0);
        assertEquals(missed, outOfRange);
        assertTrue(noDamage >= 0);
    }


    @Test
    public void meleeAttackHits() {
        Figure actingFigure = state.getActingFigure();
        Figure victim = state.getMonsters().get(0).get(0);

        int missed = 0, outOfRange = 0, noDamage = 0;
        for (int loop = 0; loop < 100; loop++) {
            MeleeAttack attack = new MeleeAttackDamageOnly(actingFigure.getComponentID(), victim.getComponentID());
            attack.execute(state);
            //        System.out.println(state.getAttackDicePool().toString());
            assertTrue(state.getAttackDicePool().hasRolled());
            if (attack.attackMissed(state)) {
                missed++;
            }
            if (state.getAttackDicePool().getRange() < 0) {
                outOfRange++;
            }
            if (state.getAttackDicePool().getDamage() == 0)
                noDamage++;
        }
        System.out.printf("Missed: %d, Out of Range: %d, No Damage Done: %d%n", missed, outOfRange, noDamage);
        assertTrue(missed >= 0);
        assertTrue(outOfRange >= 0);
        assertEquals(missed, outOfRange);
        assertTrue(noDamage >= 0);
    }

    @Test
    public void hasSurgeAbility() {
        Hero hero = state.getHeroes().get(0);
        assertEquals("Hero: Avric Albright", hero.getName());
        assertEquals(2, hero.getAbilities().size());
        for (Triggers da : hero.getAbilities().get(0).getTriggers())
            assertEquals(Triggers.SURGE_DECISION, da);
    }

    @Test
    public void stopAtSurgeDecision() {
        Figure victim = state.getMonsters().get(0).get(0);
        Figure actingFigure = state.getActingFigure();
        DicePool attackDice = fixBlueAndYellowDice(2, 1);

        MeleeAttack attack = new MeleeAttackOverride(
                actingFigure.getComponentID(), victim.getComponentID(),
                attackDice, null);
        attack.execute(state);
        assertFalse(attack.executionComplete(state));
        assertEquals(attack, state.currentActionInProgress());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.contains(new SurgeAttackAction(Surge.STUN, actingFigure.getComponentID())));
        assertTrue(actions.contains(new EndSurgePhase()));
        assertTrue(actions.contains(new SurgeAttackAction(Surge.RECOVER_1_FATIGUE, actingFigure.getComponentID())));
        int index = actions.indexOf(new SurgeAttackAction(Surge.STUN, actingFigure.getComponentID()));

        actions.get(index).execute(state);
        attack._afterAction(state, actions.get(index));
        assertTrue(attack.executionComplete(state));
    }

    private DicePool fixBlueAndYellowDice(int blueFace, int yellowFace) {
        DicePool retValue = DicePool.constructDicePool("BLUE", "YELLOW");
        retValue.roll(state.getRnd());
        retValue.setFace(0, blueFace);
        retValue.setFace(1, yellowFace);
        return retValue;
    }

    @Test
    public void canChooseToDoNothingAtSurgeDecisions() {
        Figure victim = state.getMonsters().get(0).get(0);
        Figure actingFigure = state.getActingFigure();
        DicePool attackDice = fixBlueAndYellowDice(2, 1);

        MeleeAttack attack = new MeleeAttackOverride(
                actingFigure.getComponentID(), victim.getComponentID(),
                attackDice, null);
        attack.execute(state);
        assertFalse(attack.executionComplete(state));
        assertEquals(attack, state.currentActionInProgress());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.contains(new SurgeAttackAction(Surge.STUN, actingFigure.getComponentID())));
        assertTrue(actions.contains(new EndSurgePhase()));
        assertTrue(actions.contains(new SurgeAttackAction(Surge.RECOVER_1_FATIGUE, actingFigure.getComponentID())));
        int index = actions.indexOf(new EndSurgePhase());
        actions.get(index).execute(state);
        attack._afterAction(state, actions.get(index));
        assertTrue(attack.executionComplete(state));
    }

    @Test
    public void cannotChooseTheSameSurgeTwice() {
        Figure victim = state.getMonsters().get(0).get(0);
        Figure actingFigure = state.getActingFigure();
        DicePool attackDice = fixBlueAndYellowDice(5, 1);  // we now have 2 surges

        MeleeAttack attack = new MeleeAttackOverride(
                actingFigure.getComponentID(), victim.getComponentID(),
                attackDice, null);
        attack.execute(state);
        assertFalse(attack.executionComplete(state));
        assertEquals(attack, state.currentActionInProgress());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new SurgeAttackAction(Surge.STUN, actingFigure.getComponentID())));

        int index = actions.indexOf(new SurgeAttackAction(Surge.STUN, actingFigure.getComponentID()));

        actions.get(index).execute(state);
        attack._afterAction(state, actions.get(index));

        if (!attack.executionComplete(state)) {
            actions = fm.computeAvailableActions(state);
            assertFalse(actions.contains(new SurgeAttackAction(Surge.STUN, actingFigure.getComponentID())));
            actions.get(0).execute(state);
            attack._afterAction(state, actions.get(0));
        }

        assertTrue(attack.executionComplete(state));
    }

    @Test
    public void monsterOnlyAttacksOnce() {
        Monster attacker = state.getMonsters().get(1).get(0);
        Figure victim = state.getActingFigure();

        Vector2D attackerPos = new Vector2D(4, 3);
        Vector2D victimPos = new Vector2D(5, 4);
        attacker.setPosition(attackerPos);
        victim.setPosition(victimPos);
        ((Hero) victim).setAbility(HeroAbilities.HeroAbility.SurgeRecoverOneHeart);

        while (!state.getActingFigure().equals(attacker)) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(0));
        }
        assertEquals(state.getActingFigure(), attacker);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.stream().noneMatch(a -> a instanceof Howl));

        // Force the dice to roll a result that goes all the way to the end without interruptions
        DicePool attackDice = attacker.getAttackDice().copy();
        while (attackDice.getRange() <= 0 || attackDice.getDamage() <= 0)
            attackDice.roll(state.getRnd());
        assertEquals(2, attackDice.getSize());
        state.setAttackDicePool(DicePool.empty); // to clear previous settings

        MeleeAttack attack = new MeleeAttackDamageOnly(
                attacker.getComponentID(), victim.getComponentID(), attackDice, null);
        assertEquals(0, state.getAttackDicePool().getSize());
        attack.execute(state);

        // Might pause because we roll a Surge or have a reaction - here we ensure it continues
        while (!attack.executionComplete(state)) {
            AbstractAction action = attack._computeAvailableActions(state).get(0);
            action.execute(state);
            attack._afterAction(state, action);
        }

        assertEquals(2, state.getAttackDicePool().getSize());
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.YELLOW));
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.BLUE));
        assertTrue(state.getAttackDicePool().hasRolled());
        assertTrue(attack.executionComplete(state));

        actions = fm.computeAvailableActions(state);

        assertTrue(actions.stream().noneMatch(a -> a instanceof MeleeAttack));
        assertTrue(actions.stream().noneMatch(a -> a instanceof Howl));
    }

    @Test
    public void monsterOnlyUsesAbilityOnce() {
        Monster attacker = state.getMonsters().get(1).get(0);
        Figure victim = state.getActingFigure();

        Vector2D attackerPos = new Vector2D(4, 3);
        Vector2D victimPos = new Vector2D(5, 4);
        attacker.setPosition(attackerPos);
        victim.setPosition(victimPos);
        ((Hero) victim).setAbility(HeroAbilities.HeroAbility.SurgeRecoverOneHeart);

        while (state.getActingFigure().getComponentID() != attacker.getComponentID()) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(0));
            System.out.println(state.getActingFigure());
        }
        assertEquals(state.getActingFigure(), attacker);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.stream().noneMatch(a -> a instanceof Howl));
        Howl howl = (Howl) actions.stream().filter(a -> a instanceof Howl).findFirst().get();
        // Force the dice to roll a result that goes all the way to the end without interruptions
        howl.execute(state);
        AbstractAction action = howl._computeAvailableActions(state).get(0);
        action.execute(state);
        howl._afterAction(state, action);
        assertTrue(howl.executionComplete(state));

        actions = fm.computeAvailableActions(state);

        assertTrue(actions.stream().noneMatch(a -> a instanceof MeleeAttack));
        assertTrue(actions.stream().noneMatch(a -> a instanceof Howl));
    }

    @Test
    public void heroDefeatsMonster() {
        Figure actingFigure = state.getActingFigure();
        Figure victim = state.getMonsters().get(0).get(0);
        Vector2D victimPos = victim.getPosition();
        assertEquals(victim.getComponentID(), ((PropertyInt) state.getMasterBoard().getElement(victimPos).getProperty("players")).value);
        List<DescentCard> weapons = ((Hero) actingFigure).getWeapons();

        assertEquals(1, weapons.size());

        // Force the dice to roll a result that goes all the way to the end without interruptions
        DicePool attackDice = actingFigure.getAttackDice().copy();
        while (attackDice.getRange() <= 0 || attackDice.getDamage() <= 0)
            attackDice.roll(state.getRnd());

        DicePool defenceDice = victim.getDefenceDice().copy();
        defenceDice.roll(state.getRnd());

        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttackOverride(
                actingFigure.getComponentID(), victim.getComponentID(), attackDice, defenceDice);
        assertEquals(0, state.getAttackDicePool().getSize());
        attack.addDamage(1000); // Ensure that we kill the monster
        attack.execute(state);

        // Might pause because we roll a Surge or have a reaction - here we ensure it continues
        while (!attack.executionComplete(state)) {
            AbstractAction action = attack._computeAvailableActions(state).get(0);
            action.execute(state);
            attack._afterAction(state, action);
        }
        assertEquals(attack, state.currentActionInProgress());
        assertEquals(2, state.getAttackDicePool().getSize());
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.YELLOW));
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.BLUE));
        assertTrue(state.getAttackDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage() + attack.getExtraDamage();
        int shields = state.getDefenceDicePool().getShields() + attack.getExtraDefence() - attack.getPierce();
        damage = Math.max(damage - shields, 0);
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());
        assertFalse(state.getMonsters().get(0).contains(victim));
        assertEquals(-1, ((PropertyInt) state.getMasterBoard().getElement(victimPos).getProperty("players")).value);
    }

    @Test
    public void monsterDefeatsHero() {
        Monster attacker = state.getMonsters().get(0).get(1);
        Figure victim = state.getActingFigure();

        assertFalse(((Hero) victim).isDefeated());

        // Force the dice to roll a result that goes all the way to the end without interruptions
        DicePool attackDice = attacker.getAttackDice().copy();
        while (attackDice.getRange() <= 0 || attackDice.getDamage() <= 0)
            attackDice.roll(state.getRnd());

        DicePool defenceDice = victim.getDefenceDice().copy();
        defenceDice.roll(state.getRnd());

        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttackOverride(
                attacker.getComponentID(), victim.getComponentID(), attackDice, defenceDice);
        assertEquals(0, state.getAttackDicePool().getSize());
        attack.addDamage(1000); // Ensure that we kill the monster
        attack.execute(state);

        // Might pause because we roll a Surge or have a reaction - here we ensure it continues
        while (!attack.executionComplete(state)) {
            AbstractAction action = attack._computeAvailableActions(state).get(0);
            action.execute(state);
            attack._afterAction(state, action);
        }
        assertEquals(attack, state.currentActionInProgress());
        assertEquals(2, state.getAttackDicePool().getSize());
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.YELLOW));
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.BLUE));
        assertTrue(state.getAttackDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage() + attack.getExtraDamage();
        int shields = state.getDefenceDicePool().getShields() + attack.getExtraDefence() - attack.getPierce();
        damage = Math.max(damage - shields, 0);
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());
        assertTrue(((Hero) victim).isDefeated());
    }

}
