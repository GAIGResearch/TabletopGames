package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
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
import static games.descent2e.actions.Triggers.ANYTIME;
import static games.descent2e.actions.herofeats.DoubleMoveAttack.DoubleMoveAttackPhase.*;
import static games.descent2e.actions.herofeats.DoubleMoveAttack.Interrupters.*;

public class DoubleMoveAttack extends DescentAction implements IExtendedSequence {

    enum Interrupters {
        HERO
    }

    public enum DoubleMoveAttackPhase {
        NOT_STARTED,
        PRE_HERO_ACTION(ANYTIME, HERO),
        POST_HERO_ACTION,
        ALL_DONE;

        public final Triggers interrupt;
        public final DoubleMoveAttack.Interrupters interrupters;

        DoubleMoveAttackPhase(Triggers interruptType, DoubleMoveAttack.Interrupters who) {
            interrupt = interruptType;
            interrupters = who;
        }

        DoubleMoveAttackPhase() {
            interrupt = null;
            interrupters = null;
        }
    }

    DoubleMoveAttackPhase phase = NOT_STARTED;
    int heroPlayer;
    int interruptPlayer;
    int oldMovePoints;
    boolean oldFreeAttack;
    boolean oldHasMoved;

    boolean skip = false;

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
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState dgs = (DescentGameState) state;
        if (interruptPlayer == dgs.getOverlordPlayer())
            return new ArrayList<>();  // Overlord cannot interrupt
        List<AbstractAction> retVal = new ArrayList<>();
        List<AbstractAction> moveActions;
        List<RangedAttack> attacks = new ArrayList<>();;

        Figure hero = dgs.getActingFigure();

        if (!hero.hasUsedExtraAction())
        {
            List<Integer> targets = DescentHelper.getRangedTargets(dgs, hero);
            if (!targets.isEmpty())
                for (Integer target : targets)
                    attacks.add(new FreeAttack(hero.getComponentID(), target, false));
        }

        switch (phase) {
            case PRE_HERO_ACTION:
                moveActions = moveActions(dgs, hero);
                if (!moveActions.isEmpty())
                {
                    StopMove stopMove = new StopMove(hero.getComponentID());
                    // Jain should only stop if she has not made her free attack yet
                    if (stopMove.canExecute(dgs) && hero.hasUsedExtraAction())
                        retVal.add(stopMove);
                    retVal.addAll(moveActions);
                }
                if (!attacks.isEmpty())
                    retVal.addAll(attacks);
                break;
            default:
                break;
        }

        if (retVal.isEmpty())
            retVal.add(new EndCurrentPhase());

        return retVal;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return interruptPlayer;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        // after the interrupt action has been taken, we can continue to see who interrupts next
   //     state.setActionInProgress(this);  //????
        movePhaseForward((DescentGameState) state);
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return (phase == ALL_DONE);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        heroPlayer = dgs.getCurrentPlayer();
        Figure hero = dgs.getActingFigure();
        interruptPlayer = heroPlayer;
        oldMovePoints = hero.getAttribute(Figure.Attribute.MovePoints).getValue();
        oldFreeAttack = hero.hasUsedExtraAction();
        oldHasMoved = hero.hasMoved();
        phase = PRE_HERO_ACTION;

        // Jain can move up to double her speed, and attack at any point during the move (before, during and after)
        hero.setAttribute(Figure.Attribute.MovePoints, hero.getAttributeMax(Figure.Attribute.MovePoints) * 2);
        hero.setUsedExtraAction(false);
        hero.setHasMoved(false);

        movePhaseForward(dgs);

        hero.addActionTaken(toString());
        dgs.setActionInProgress(this);

        return true;
    }

    private void movePhaseForward(DescentGameState state) {
        // The goal here is to work out which player may have an interrupt for the phase we are in
        // If none do, then we can move forward to the next phase directly.
        // If one (or more) does, then we stop, and go back to the main game loop for this
        // decision to be made
        boolean foundInterrupt = false;
        do {
            if (playerHasInterruptOption(state)) {
                foundInterrupt = true;
                // System.out.println("Heroic Feat Interrupt: " + phase + ", Interrupter:" + phase.interrupters + ", Interrupt:" + phase.interrupt + ", Player: " + interruptPlayer);
                // we need to get a decision from this player
            } else {
                skip = false;
                interruptPlayer = (interruptPlayer + 1) % state.getNPlayers();
                if (phase.interrupt == null || interruptPlayer == heroPlayer) {
                    // we have completed the loop, and start again with the attacking player
                    executePhase(state);
                    interruptPlayer = heroPlayer;
                }
            }
        } while (!foundInterrupt && phase != ALL_DONE);
    }

    private boolean playerHasInterruptOption(DescentGameState state) {
        if (skip) return false;
        if (phase.interrupt == null || phase.interrupters == null) return false;
        // first we see if the interruptPlayer is one who may interrupt
        if (phase.interrupters == Interrupters.HERO) {
            if (interruptPlayer == state.getOverlordPlayer())
                return false;
            if (interruptPlayer != heroPlayer)
                return false;
        }
        // second we see if they can interrupt (i.e. have a relevant card/ability)
        return !_computeAvailableActions(state).isEmpty();
    }

    private void executePhase(DescentGameState state) {
        // System.out.println("Executing phase " + phase);
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                // TODO Fix this temporary solution: it should not keep looping back to ALL_DONE, put the error back in
                break;
            //throw new AssertionError("Should never be executed");
            case PRE_HERO_ACTION:
                phase = POST_HERO_ACTION;
                break;
            case POST_HERO_ACTION:
                Figure hero = state.getActingFigure();
                hero.setAttribute(Figure.Attribute.MovePoints, oldMovePoints);
                hero.setUsedExtraAction(oldFreeAttack);
                hero.setHasMoved(oldHasMoved);
                if (hero instanceof Hero) {((Hero) hero).setFeatAvailable(false);}
                hero.getNActionsExecuted().increment();
                phase = ALL_DONE;
                break;
        }
        // and reset interrupts
    }

    @Override
    public DoubleMoveAttack copy() {
        DoubleMoveAttack retValue = new DoubleMoveAttack();
        retValue.phase = phase;
        retValue.heroPlayer = heroPlayer;
        retValue.interruptPlayer = interruptPlayer;
        retValue.oldMovePoints = oldMovePoints;
        retValue.oldFreeAttack = oldFreeAttack;
        retValue.oldHasMoved = oldHasMoved;
        retValue.skip = skip;
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
        return heroPlayer == that.heroPlayer && interruptPlayer == that.interruptPlayer &&
                oldMovePoints == that.oldMovePoints && oldFreeAttack == that.oldFreeAttack &&
                oldHasMoved == that.oldHasMoved && phase == that.phase && skip == that.skip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), phase, heroPlayer, interruptPlayer, oldMovePoints, oldFreeAttack, oldHasMoved, skip);
    }

    public boolean getSkip()
    {
        return skip;
    }

    public void setSkip(boolean s)
    {
        skip = s;
    }
}
