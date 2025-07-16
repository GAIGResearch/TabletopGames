package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Move;
import games.descent2e.actions.StopMove;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.EndCurrentPhase;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.moveActions;

public class DoubleMoveAttack extends DescentAction implements IExtendedSequence {

    int heroPlayer;
    int oldMovePoints;
    boolean oldFreeAttack;
    boolean oldHasMoved;
    boolean completed = false;

    public DoubleMoveAttack() {
        super(Triggers.ACTION_POINT_SPEND);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: Move twice your speed, and can attack anytime during it.";
    }

    public String toString() {
        return "Heroic Feat: Move twice and attack";
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        DescentGameState dgs = (DescentGameState) state;
        List<AbstractAction> retVal = new ArrayList<>();
        List<AbstractAction> moveActions;
        List<RangedAttack> attacks = new ArrayList<>();

        Figure hero = dgs.getActingFigure();

        if (!hero.hasUsedExtraAction()) {
            List<Integer> targets = DescentHelper.getRangedTargets(dgs, hero);
            if (!targets.isEmpty())
                for (Integer target : targets)
                    attacks.add(new FreeAttack(hero.getComponentID(), target, false));
        }

        moveActions = moveActions(dgs, hero);
        if (!moveActions.isEmpty()) {
            StopMove stopMove = new StopMove(hero.getComponentID());
            // Jain should only stop if she has not made her free attack yet
            if (stopMove.canExecute(dgs) && hero.hasUsedExtraAction())
                retVal.add(stopMove);
            retVal.addAll(moveActions);
        }
        if (!attacks.isEmpty())
            retVal.addAll(attacks);

        if (retVal.isEmpty())
            retVal.add(new EndCurrentPhase());

        return retVal;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return heroPlayer;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof EndCurrentPhase) {
            completed = true;
        }

        if (executionComplete(state)) {
            DescentGameState dgs = (DescentGameState) state;
            Hero hero = (Hero) dgs.getActingFigure();

            // Restore the hero's saved attributes
            hero.setAttribute(Figure.Attribute.MovePoints, oldMovePoints);
            hero.setUsedExtraAction(oldFreeAttack);
            hero.setHasMoved(oldHasMoved);
            hero.setFeatAvailable(false);
            hero.getNActionsExecuted().increment();
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return completed;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        heroPlayer = dgs.getCurrentPlayer();
        Figure hero = dgs.getActingFigure();
        oldMovePoints = hero.getAttribute(Figure.Attribute.MovePoints).getValue();
        oldFreeAttack = hero.hasUsedExtraAction();
        oldHasMoved = hero.hasMoved();

        // Jain can move up to double her speed, and attack at any point during the move (before, during and after)
        hero.setAttribute(Figure.Attribute.MovePoints, hero.getAttributeMax(Figure.Attribute.MovePoints) * 2);
        hero.setUsedExtraAction(false);
        hero.setHasMoved(false);

        hero.addActionTaken(toString());
        dgs.setActionInProgress(this);
        return true;
    }

    @Override
    public DoubleMoveAttack copy() {
        DoubleMoveAttack retValue = new DoubleMoveAttack();
        retValue.heroPlayer = heroPlayer;
        retValue.oldMovePoints = oldMovePoints;
        retValue.oldFreeAttack = oldFreeAttack;
        retValue.oldHasMoved = oldHasMoved;
        retValue.completed = completed;
        return retValue;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure hero = dgs.getActingFigure();
        if (hero instanceof Hero && !((Hero) hero).isFeatAvailable()) return false;
        return !hero.getNActionsExecuted().isMaximum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DoubleMoveAttack that = (DoubleMoveAttack) o;
        return heroPlayer == that.heroPlayer && completed == that.completed &&
                oldMovePoints == that.oldMovePoints && oldFreeAttack == that.oldFreeAttack &&
                oldHasMoved == that.oldHasMoved;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), heroPlayer, oldMovePoints, oldFreeAttack, oldHasMoved, completed);
    }
}
