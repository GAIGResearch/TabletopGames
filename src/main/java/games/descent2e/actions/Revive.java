package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.Objects;

import static games.descent2e.actions.Triggers.ACTION_POINT_SPEND;

public class Revive extends DescentAction{

    int heroID;

    public Revive(int heroID) {
        super(ACTION_POINT_SPEND);
        this.heroID = heroID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Revive " + gameState.getComponentById(heroID).getComponentName();
    }

    @Override
    public String toString() {
        return "Revive " + heroID;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Hero hero = (Hero) gs.getComponentById(heroID);
        hero.setDefeated(gs,false);
        // Health recovery: roll 2 red dice
        DicePool.revive.roll(gs.getRnd());
        hero.setAttribute(Figure.Attribute.Health, DicePool.revive.getDamage());
        gs.getActingFigure().getNActionsExecuted().increment();
        gs.getActingFigure().addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Hero h = (Hero)dgs.getComponentById(heroID);
        return h.isDefeated() && !dgs.getActingFigure().getNActionsExecuted().isMaximum();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Revive r ) {
            return r.heroID == heroID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return heroID * 31 + 39137894;
    }
}
