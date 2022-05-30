package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;

import java.util.*;

import static games.descent2e.actions.Triggers.*;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.*;
import static games.descent2e.actions.attack.MeleeAttack.Interrupters.*;

public class MeleeAttack extends AbstractAction implements IExtendedSequence {

    enum Interrupters {
        ATTACKER, DEFENDER, OTHERS, ALL
    }

    // The two argument constructor for AttackPhase specifies
    // which trigger is relevant (the first), and which players can use actions at this point (the second)
    public enum AttackPhase {
        NOT_STARTED,
        PRE_ATTACK_ROLL(START_ATTACK, DEFENDER),
        POST_ATTACK_ROLL(ROLL_OWN_DICE, ATTACKER),
        SURGE_DECISIONS,
        PRE_DEFENCE_ROLL,
        POST_DEFENCE_ROLL(ROLL_OWN_DICE, DEFENDER),
        POST_DAMAGE(ROLL_OWN_DICE, DEFENDER),
        ALL_DONE;

        public final Triggers interrupt;
        public final Interrupters interrupters;

        AttackPhase(Triggers interruptType, Interrupters who) {
            interrupt = interruptType;
            interrupters = who;
        }

        AttackPhase() {
            interrupt = null;
            interrupters = null;
        }
    }

    final int weaponCardId;
    final int attackingFigure;
    final int attackingPlayer;
    final int defendingFigure;
    final int defendingPlayer;
    AttackPhase phase = NOT_STARTED;
    int interruptPlayer;

    public MeleeAttack(int weaponCardId, int attackingFigure, int attackingPlayer, int defendingFigure, int defendingPlayer) {
        this.weaponCardId = weaponCardId;
        this.attackingFigure = attackingFigure;
        this.attackingPlayer = attackingPlayer;
        this.defendingFigure = defendingFigure;
        this.defendingPlayer = defendingPlayer;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        DescentGameState state = (DescentGameState) gs;

        phase = PRE_ATTACK_ROLL;
        interruptPlayer = attackingPlayer;
        Component weaponCard = state.getComponentById(weaponCardId);
        Figure figure = (Figure) state.getComponentById(attackingFigure);
        movePhaseForward(state);

        // When executing a melee attack we need to:
        // 1) roll the dice (with possible interrupt beforehand)
        // 2) Possibly invoke re-roll options (via interrupts)
        // 3) and then - if there are any surges - decide how to use them
        // 4) and then get the target to roll their defence dice
        // 5) with possible rerolls
        // 6) then do the damage
        // 7) target can use items/abilities to modify damage
        return true;
    }

    private void movePhaseForward(DescentGameState state) {
        // The goal here is to work out which player may have an interrupt for the phase we are in
        // If none do, then we can move forward to the next phase directly.
        // If one (or more) does, then we stop, and go back to the main game loop for this
        // decision to be made
        boolean foundInterrupt = false;
        do {
            // check next player
            interruptPlayer = (interruptPlayer + 1) % state.getNPlayers();
            if (phase.interrupt == null || interruptPlayer == attackingPlayer) {
                // we have completed the loop
                executePhase();
                interruptPlayer = attackingPlayer;
            }
            if (playerHasInterruptOption(state)) {
                foundInterrupt = true;
                // we need to get a decision from this player
            }
        } while (!foundInterrupt && phase != ALL_DONE);
    }

    private boolean playerHasInterruptOption(DescentGameState state) {
        if (phase.interrupt == null || phase.interrupters == null) return false;
        // first we see if the interruptPlayer is one who may interrupt
        switch (phase.interrupters) {
            case ATTACKER:
                if (interruptPlayer != attackingPlayer)
                    return false;
            case DEFENDER:
                if (interruptPlayer != defendingPlayer)
                    return false;
            case OTHERS:
                if (interruptPlayer == attackingPlayer)
                    return false;
            case ALL:
                // always fine
        }
        // second we see if they can interrupt (i.e. have a relevant card/ability)
        return state.playerHasAvailableInterrupt(interruptPlayer, phase.interrupt);
    }

    private void executePhase() {
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                throw new AssertionError("Should never be executed");
            case PRE_ATTACK_ROLL:
                // TODO : Roll dice
                phase = POST_ATTACK_ROLL;
                break;
            case POST_ATTACK_ROLL:
                // Any rerolls are executed via interrupts
                phase = SURGE_DECISIONS;
                break;
            case SURGE_DECISIONS:
                // any surge decisions are executed via interrupts
                phase = PRE_DEFENCE_ROLL;
                break;
            case PRE_DEFENCE_ROLL:
                if (attackMissed()) // no damage done, so can skip the defence roll
                    phase = ALL_DONE;
                else
                    phase = POST_DEFENCE_ROLL;
                break;
            case POST_DEFENCE_ROLL:
                phase = POST_DAMAGE;
                break;
            case POST_DAMAGE:
                // TODO: Implement the damage done
                phase = ALL_DONE;
                break;
        }
    }

    protected boolean attackMissed() {
        // TODO: Interrogate the current dice pool on the game state
        // if there are no damage icons, then we missed
        return false;
    }

    @Override
    public MeleeAttack copy() {
        MeleeAttack retValue = new MeleeAttack(weaponCardId, attackingFigure, attackingPlayer, defendingFigure, defendingPlayer);
        retValue.phase = phase;
        retValue.interruptPlayer = interruptPlayer;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MeleeAttack) {
            MeleeAttack other = (MeleeAttack) obj;
            return other.weaponCardId == weaponCardId && other.attackingFigure == attackingFigure &&
                    other.attackingPlayer == attackingPlayer && other.defendingFigure == defendingFigure &&
                    other.defendingPlayer == defendingPlayer && other.phase == phase && other.interruptPlayer == interruptPlayer;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(weaponCardId, attackingFigure, attackingPlayer, defendingFigure, defendingPlayer, phase.ordinal(), interruptPlayer);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
        // TODO: Extend this to pull in details of card and figures involved
    }

    @Override
    public String toString() {
        return String.format("Melee Attack (Wpn: %d by %d on %d", weaponCardId, attackingPlayer, attackingFigure);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState state = (DescentGameState) gs;
        return state.getInterruptActionsFor(interruptPlayer, phase.interrupt);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return interruptPlayer;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        // after the interrupt action has been taken, we can continue to see who interrupts next
        movePhaseForward((DescentGameState) state);
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return phase == ALL_DONE;
    }

}
