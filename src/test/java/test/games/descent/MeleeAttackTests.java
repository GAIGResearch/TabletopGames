package test.games.descent;

import core.Game;
import games.descent2e.DescentForwardModel;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.components.*;
import org.junit.Before;
import org.junit.Test;
import utilities.Vector2D;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MeleeAttackTests {

    DescentGameState state;
    DescentForwardModel fm = new DescentForwardModel();

    @Before
    public void setup() {
        state = new DescentGameState(new DescentParameters(234), 2);
        fm.setup(state);
        assertEquals("Avric Albright", state.getHeroes().get(0).getComponentName());
    }

    @Test
    public void firstPlayerIsNotOverlord() {
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void attackRollsDoesDamage() {
        Figure actingFigure = state.getActingFigure();
        Figure victim = state.getMonsters().get(0).get(0);
        List<Item> weapons = ((Hero)actingFigure).getWeapons();

        assertEquals(1, weapons.size());

        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttackDamageOnly(actingFigure.getComponentID(), victim.getComponentID());
        assertEquals(0, state.getAttackDicePool().getSize());
        attack.execute(state);
        assertEquals(attack, state.currentActionInProgress());
        assertEquals(2, state.getAttackDicePool().getSize());
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.YELLOW));
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.BLUE));
        assertTrue(state.getAttackDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage();
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());
    }

    @Test
    public void monsterCanAttackHero() {
        Monster attacker = state.getMonsters().get(0).get(1);
        Figure victim = state.getActingFigure();

        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttackDamageOnly(attacker.getComponentID(), victim.getComponentID());
        assertEquals(0, state.getAttackDicePool().getSize());
        attack.execute(state);
        assertEquals(2, state.getAttackDicePool().getSize());
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.YELLOW));
        assertEquals(1, state.getAttackDicePool().getNumber(DiceType.BLUE));
        assertTrue(state.getAttackDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage();
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());
    }

    @Test
    public void monsterRollsDefenceDieAfterAttack() {
        Figure actingFigure = state.getActingFigure();
        Figure victim = state.getMonsters().get(0).get(0);
        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttack(actingFigure.getComponentID(), victim.getComponentID());
        attack.execute(state);
        assertEquals(1, state.getDefenceDicePool().getSize());
        assertEquals(1, state.getDefenceDicePool().getNumber(DiceType.GREY));
        assertEquals(0, state.getDefenceDicePool().getNumber(DiceType.BROWN));
        assertTrue(state.getDefenceDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage();
        int shields = state.getDefenceDicePool().getShields();
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
        MeleeAttack attack = new MeleeAttack(actingFigure.getComponentID(), victim.getComponentID());
        attack.execute(state);
        assertEquals(1, state.getDefenceDicePool().getSize());
        assertEquals(1, state.getDefenceDicePool().getNumber(DiceType.GREY));
        assertEquals(0, state.getDefenceDicePool().getNumber(DiceType.BROWN));
        assertTrue(state.getDefenceDicePool().hasRolled());
        int damage = state.getAttackDicePool().getDamage();
        int shields = state.getDefenceDicePool().getShields();
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
        for (int loop = 0; loop < 50; loop++) {
            MeleeAttack attack = new RangedAttack(actingFigure.getComponentID(), victim.getComponentID());
            attack.execute(state);
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
        assertTrue(missed > 0);
        assertTrue(outOfRange > 0);
        assertTrue(missed > outOfRange);
    }


}
