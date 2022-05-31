package test.games.descent;

import core.Game;
import games.descent2e.DescentForwardModel;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.components.DiceType;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Item;
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
    public void attackRollsDicePool() {
        Figure actingFigure = state.getActingFigure();
        Figure victim = state.getMonsters().get(0).get(0);
        List<Item> weapons = ((Hero)actingFigure).getWeapons(state);
        // TODO: check this works for a Hero
        // then generalise to monsters too
        assertEquals(1, weapons.size());

        int startHP = victim.getAttribute(Figure.Attribute.Health).getValue();
        MeleeAttack attack = new MeleeAttack(weapons.get(0).getComponentID(), actingFigure.getComponentID(), 1, victim.getComponentID(), 0);
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

}
