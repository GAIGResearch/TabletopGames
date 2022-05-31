package test.games.descent;

import core.Game;
import games.descent2e.DescentForwardModel;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.*;
import org.junit.Before;
import org.junit.Test;

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
        MeleeAttack attack = new MeleeAttack(actingFigure.getComponentID(), victim.getComponentID());
        assertEquals(0, state.getDicePool().getSize());
        attack.execute(state);
        assertEquals(attack, state.currentActionInProgress());
        assertEquals(2, state.getDicePool().getSize());
        assertEquals(1, state.getDicePool().getNumber(DiceType.YELLOW));
        assertEquals(1, state.getDicePool().getNumber(DiceType.BLUE));
        assertTrue(state.getDicePool().hasRolled());
        int damage = state.getDicePool().getDamage();
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());

    }

    @Test
    public void monsterCanAttackHero() {
        Monster attacker = state.getMonsters().get(0).get(1);
        Figure victim = state.getActingFigure();

        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttack(attacker.getComponentID(), victim.getComponentID());
        assertEquals(0, state.getDicePool().getSize());
        attack.execute(state);
        assertEquals(2, state.getDicePool().getSize());
        assertEquals(1, state.getDicePool().getNumber(DiceType.YELLOW));
        assertEquals(1, state.getDicePool().getNumber(DiceType.BLUE));
        assertTrue(state.getDicePool().hasRolled());
        int damage = state.getDicePool().getDamage();
        assertTrue(attack.executionComplete(state));
        assertEquals(Math.max(startHP - damage, 0), victim.getAttribute(Figure.Attribute.Health).getValue());

    }

}
