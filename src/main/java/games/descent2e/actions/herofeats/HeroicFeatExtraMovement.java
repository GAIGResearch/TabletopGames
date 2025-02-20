package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.StopMove;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.EndCurrentPhase;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.moveActions;
import static games.descent2e.actions.Triggers.ANYTIME;
import static games.descent2e.actions.herofeats.HeroicFeatExtraMovement.ExtraMovePhase.*;
import static games.descent2e.actions.herofeats.HeroicFeatExtraMovement.Interrupters.ALLY;
import static games.descent2e.actions.herofeats.HeroicFeatExtraMovement.Interrupters.HERO;

public class HeroicFeatExtraMovement extends DescentAction implements IExtendedSequence {

    // Syndrael Heroic Feat

    enum Interrupters {
        HERO, ALLY
    }

    public enum ExtraMovePhase {
        NOT_STARTED,
        SWAP(ANYTIME, HERO),
        PRE_HERO_MOVE(ANYTIME, HERO),
        POST_HERO_MOVE,
        PRE_ALLY_MOVE(ANYTIME, ALLY),
        POST_ALLY_MOVE,
        ALL_DONE;

        public final Triggers interrupt;
        public final HeroicFeatExtraMovement.Interrupters interrupters;

        ExtraMovePhase(Triggers interruptType, HeroicFeatExtraMovement.Interrupters who) {
            interrupt = interruptType;
            interrupters = who;
        }

        ExtraMovePhase() {
            interrupt = null;
            interrupters = null;
        }
    }

    HeroicFeatExtraMovement.ExtraMovePhase phase = NOT_STARTED;
    int heroPlayer;
    int allyPlayer;
    int interruptPlayer;
    int heroID;
    int allyID;
    boolean swapped, swapOption = false;
    int oldHeroMovePoints;
    boolean oldHeroHasMoved;
    int oldAllyMovePoints;
    boolean oldAllyHasMoved;

    boolean skip = false;

    public HeroicFeatExtraMovement(int hero, int ally) {
        super(Triggers.HEROIC_FEAT);
        this.heroID = hero;
        this.allyID = ally;
    }

    @Override
    public String getString(AbstractGameState gameState) {

        String heroName = ((Hero) gameState.getComponentById(heroID)).getName().replace("Hero: ", "");
        String allyName = ((Hero) gameState.getComponentById(allyID)).getName().replace("Hero: ", "");

        return String.format("Heroic Feat: " + heroName + " and " + allyName + " make a free move action");
    }

    public String toString() {
        return "Heroic Feat: Extra Movement for " + heroID + " and " + allyID;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState dgs = (DescentGameState) state;
        Figure hero = (Figure) dgs.getComponentById(heroID);
        Figure targetAlly = (Figure) dgs.getComponentById(allyID);
        List<AbstractAction> retVal = new ArrayList<>();
        List<AbstractAction> moveActions;
        switch (phase) {
            case SWAP:
                // If we have not yet chosen if we want to swap, we can do so now
                // This ensures we are only asked once
                //System.out.println("Swap option: " + swapOption + "\nSwapped: " + swapped);
                if (!swapOption) {
                    SwapOrder noSwap = new SwapOrder(hero.getComponentID(), targetAlly.getComponentID(), false);
                    SwapOrder yesSwap = new SwapOrder(hero.getComponentID(), targetAlly.getComponentID(), true);
                    if (noSwap.canExecute(dgs))
                        retVal.add(noSwap);
                    if (yesSwap.canExecute(dgs))
                        retVal.add(yesSwap);
                }
                break;
            case PRE_HERO_MOVE:
                moveActions = moveActions(dgs, hero);
                if (!moveActions.isEmpty())
                {
                    StopMove stopMove = new StopMove(hero.getComponentID());
                    if (stopMove.canExecute(dgs))
                        retVal.add(stopMove);
                    retVal.addAll(moveActions);
                }
                break;
            case PRE_ALLY_MOVE:
                moveActions = moveActions(dgs, targetAlly);
                if (!moveActions.isEmpty())
                {
                    StopMove stopMove = new StopMove(targetAlly.getComponentID());
                    if (stopMove.canExecute(dgs))
                        retVal.add(stopMove);
                    retVal.addAll(moveActions);
                }
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
        state.setActionInProgress(this);
        movePhaseForward((DescentGameState) state);
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return (phase == ALL_DONE);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        dgs.setActionInProgress(this);

        Figure hero = (Figure) dgs.getComponentById(heroID);
        Figure targetAlly = (Figure) dgs.getComponentById(allyID);

        heroPlayer = hero.getOwnerId();
        allyPlayer = targetAlly.getOwnerId();

        phase = SWAP;
        interruptPlayer = heroPlayer;

        oldHeroMovePoints = hero.getAttribute(Figure.Attribute.MovePoints).getValue();
        oldAllyMovePoints = targetAlly.getAttribute(Figure.Attribute.MovePoints).getValue();
        oldHeroHasMoved = hero.hasMoved();
        oldAllyHasMoved = targetAlly.hasMoved();

        hero.setAttributeToMax(Figure.Attribute.MovePoints);
        targetAlly.setAttributeToMax(Figure.Attribute.MovePoints);
        hero.setHasMoved(false);
        targetAlly.setHasMoved(false);

        movePhaseForward(dgs);

        hero.addActionTaken(getString(dgs));

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
                //System.out.println("Interrupt for player " + interruptPlayer);
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
        if (interruptPlayer == state.getOverlordPlayer())
            return false;
        switch (phase.interrupters) {
            case HERO:
                if (interruptPlayer != heroPlayer)
                    return false;
                break;
            case ALLY:
                if (interruptPlayer != allyPlayer)
                    return false;
                break;
        }
        // second we see if they can interrupt (i.e. have a relevant card/ability)
        return !_computeAvailableActions(state).isEmpty();
    }

    private void executePhase(DescentGameState state) {
        //System.out.println("Executing phase " + phase);
        Figure hero = (Figure) state.getComponentById(heroID);
        Figure targetAlly = (Figure) state.getComponentById(allyID);
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                // TODO Fix this temporary solution: it should not keep looping back to ALL_DONE, put the error back in
                break;
            //throw new AssertionError("Should never be executed");
            case SWAP:
                // If we chose not to swap, move to PRE_HERO_MOVE
                if (!swapped)
                    phase = PRE_HERO_MOVE;
                else
                    phase = PRE_ALLY_MOVE;
                break;
            case PRE_HERO_MOVE:
                phase = POST_HERO_MOVE;
                break;
            case POST_HERO_MOVE:
                if (!swapped)
                    phase = PRE_ALLY_MOVE;
                else {
                    if (hero instanceof Hero) {((Hero) hero).setFeatAvailable(false);}
                    hero.setAttribute(Figure.Attribute.MovePoints, oldHeroMovePoints);
                    targetAlly.setAttribute(Figure.Attribute.MovePoints, oldAllyMovePoints);
                    hero.setHasMoved(oldHeroHasMoved);
                    targetAlly.setHasMoved(oldAllyHasMoved);
                    phase = ALL_DONE;
                }
                break;
            case PRE_ALLY_MOVE:
                phase = POST_ALLY_MOVE;
                break;
            case POST_ALLY_MOVE:
                if (!swapped) {
                    if (hero instanceof Hero) {((Hero) hero).setFeatAvailable(false);}
                    hero.setAttribute(Figure.Attribute.MovePoints, oldHeroMovePoints);
                    targetAlly.setAttribute(Figure.Attribute.MovePoints, oldAllyMovePoints);
                    hero.setHasMoved(oldHeroHasMoved);
                    targetAlly.setHasMoved(oldAllyHasMoved);
                    phase = ALL_DONE;
                }
                else
                    phase = PRE_HERO_MOVE;
                break;
        }
        // and reset interrupts
    }

    @Override
    public HeroicFeatExtraMovement copy() {
        HeroicFeatExtraMovement retVal = new HeroicFeatExtraMovement(heroID, allyID);
        retVal.heroID = heroID;
        retVal.allyID = allyID;
        retVal.phase = phase;
        retVal.interruptPlayer = interruptPlayer;
        retVal.heroPlayer = heroPlayer;
        retVal.allyPlayer = allyPlayer;
        retVal.swapOption = swapOption;
        retVal.swapped = swapped;
        retVal.oldHeroMovePoints = oldHeroMovePoints;
        retVal.oldAllyMovePoints = oldAllyMovePoints;
        retVal.oldHeroHasMoved = oldHeroHasMoved;
        retVal.oldAllyHasMoved = oldAllyHasMoved;
        retVal.skip = skip;
        return retVal;
    }

    public boolean equals(Object obj) {
        if (obj instanceof HeroicFeatExtraMovement) {
            HeroicFeatExtraMovement other = (HeroicFeatExtraMovement) obj;
            return other.heroID == heroID && other.allyID == allyID &&
                    other.phase == phase && other.interruptPlayer == interruptPlayer &&
                    other.heroPlayer == heroPlayer && other.allyPlayer == allyPlayer &&
                    other.swapOption == swapOption && other.swapped == swapped &&
                    other.oldHeroMovePoints == oldHeroMovePoints && other.oldAllyMovePoints == oldAllyMovePoints &&
                    other.oldHeroHasMoved == oldHeroHasMoved && other.oldAllyHasMoved == oldAllyHasMoved &&
                    other.skip == skip;
        }
        return false;
    }
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), phase, heroPlayer, allyPlayer, interruptPlayer, heroID, allyID,
                swapped, swapOption, oldHeroMovePoints, oldAllyMovePoints, oldHeroHasMoved, oldAllyHasMoved, skip);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure hero = (Figure) dgs.getComponentById(heroID);
        return !(hero instanceof Hero) || ((Hero) hero).isFeatAvailable();
    }

    // Allows us to choose if we should swap the order of the Syndrael and the chosen ally
    // To decide who moves first
    public void swap(boolean swap) {
        swapped = swap;
    }
    public boolean getSwapped() {
        return swapped;
    }
    public void setSwapOption(boolean swap) {
        this.swapOption = swap;
    }
    public boolean getSwapOption() {
        return swapOption;
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
