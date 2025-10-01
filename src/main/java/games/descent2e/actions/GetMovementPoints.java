package games.descent2e.actions;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

public class GetMovementPoints extends DescentAction {
    public GetMovementPoints() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    @Override
    public String toString() { return "Get Movement Points"; }

    @Override
    public boolean execute(DescentGameState gs) {
        Figure f = gs.getActingFigure();
        f.setAttributeToMax(Figure.Attribute.MovePoints);
        f.getNActionsExecuted().increment();

        // Heroes are allowed to make Trade actions if they make a Move action
        if (f instanceof Hero hero) {

            // If we have already made a Movement action this turn, and we have the Demonhide Leather
            // we take a Fatigue penalty at the end of our turn
            if (hero.hasBonus(DescentTypes.SkillBonus.DoubleMovePenalty))
                if (hero.canTrade() && hero.hasMoved())
                    hero.addBonus(DescentTypes.SkillBonus.FatiguePenalty);

            hero.setTrade(true);
        }

        f.addActionTaken(toString());
        return true;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public int hashCode() {
        return 111500;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();

        if (f instanceof Monster)
        {
            // Zombies can only move once per turn
            if (((Monster) f).hasPassive(MonsterAbilities.MonsterPassive.SHAMBLING)) {
                if (f.getActionsTaken().contains("Get Movement Points")) {
                    return false;
                }
            }
        }

        return !f.hasCondition(DescentTypes.DescentCondition.Immobilize) && !f.getNActionsExecuted().isMaximum() && f.getAttribute(Figure.Attribute.MovePoints).isMinimum();
    }
}
