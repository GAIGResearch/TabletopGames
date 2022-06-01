package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.Triggers;
import games.descent2e.components.*;

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

    final int attackingFigure;
    int attackingPlayer;
    final int defendingFigure;
    int defendingPlayer;
    AttackPhase phase = NOT_STARTED;
    int interruptPlayer;

    public MeleeAttack(int attackingFigure, int defendingFigure) {
        this.attackingFigure = attackingFigure;
        this.defendingFigure = defendingFigure;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        DescentGameState state = (DescentGameState) gs;
        attackingPlayer = state.getComponentById(attackingFigure).getOwnerId();
        defendingPlayer = state.getComponentById(defendingFigure).getOwnerId();

        phase = PRE_ATTACK_ROLL;
        interruptPlayer = attackingPlayer;
        if (attackingPlayer == 0) {
            Monster monster = (Monster) state.getComponentById(attackingFigure);
            Hero hero = (Hero) state.getComponentById(defendingFigure);
            state.setAttackDicePool(monster.getAttackDice());
            state.setDefenceDicePool(hero.getDefence());
        } else {
            Monster monster = (Monster) state.getComponentById(defendingFigure);
            Hero hero = (Hero) state.getComponentById(attackingFigure);
            Item weapon = hero.getWeapons().stream()
                    .findFirst().orElseThrow(() -> new AssertionError("Weapon not found : " + attackingFigure));
            state.setAttackDicePool(weapon.getDicePool());
            state.setDefenceDicePool(monster.getDefenceDice());
        }
        // The one thing we do now is construct the dice pool to use
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
                executePhase(state);
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

    private void executePhase(DescentGameState state) {
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                throw new AssertionError("Should never be executed");
            case PRE_ATTACK_ROLL:
                // roll dice
                state.getAttackDicePool().roll(state.getRandom());
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
                if (attackMissed(state)) // no damage done, so can skip the defence roll
                    phase = ALL_DONE;
                else
                    defenceRoll(state);
                    phase = POST_DEFENCE_ROLL;
                break;
            case POST_DEFENCE_ROLL:
                damageRoll(state);
                phase = POST_DAMAGE;
                break;
            case POST_DAMAGE:
                applyDamage(state);
                phase = ALL_DONE;
                break;
        }
    }

    protected void defenceRoll(DescentGameState state) {
        state.getDefenceDicePool().roll(state.getRandom());
    }
    protected void damageRoll(DescentGameState state) {
        state.getAttackDicePool().roll(state.getRandom());
    }
    protected void applyDamage(DescentGameState state) {
        int damage = state.getAttackDicePool().getDamage();
        int defence = state.getDefenceDicePool().getShields();
        damage = Math.max(damage - defence, 0);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        int startingHealth = defender.getAttribute(Figure.Attribute.Health).getValue();
        defender.setAttribute(Figure.Attribute.Health, Math.max(startingHealth - damage, 0));
    }

    protected boolean attackMissed(DescentGameState state) {
        return state.getAttackDicePool().hasRolled() && state.getAttackDicePool().getDamage() == 0;
    }

    @Override
    public MeleeAttack copy() {
        MeleeAttack retValue = new MeleeAttack(attackingFigure, defendingFigure);
        retValue.attackingPlayer = attackingPlayer;
        retValue.defendingPlayer = defendingPlayer;
        retValue.phase = phase;
        retValue.interruptPlayer = interruptPlayer;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MeleeAttack) {
            MeleeAttack other = (MeleeAttack) obj;
            return other.attackingFigure == attackingFigure &&
                    other.attackingPlayer == attackingPlayer && other.defendingFigure == defendingFigure &&
                    other.defendingPlayer == defendingPlayer && other.phase == phase && other.interruptPlayer == interruptPlayer;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackingFigure, attackingPlayer, defendingFigure, defendingPlayer, phase.ordinal(), interruptPlayer);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
        // TODO: Extend this to pull in details of card and figures involved
    }

    @Override
    public String toString() {
        return String.format("Melee Attack (Wpn: %d on %d", attackingPlayer, attackingFigure);
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
