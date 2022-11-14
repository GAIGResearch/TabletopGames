package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

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
        return "Revive";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Hero hero = (Hero) gs.getComponentById(heroID);
        hero.setDefeated(false);
        // Health recovery: roll 2 red dice
        DicePool.revive.roll(gs.getRandom());
        hero.setAttribute(Figure.Attribute.Health, DicePool.revive.getDamage());
        gs.getActingFigure().getNActionsExecuted().increment();
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
}
