package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Component;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Move;
import games.descent2e.actions.Triggers;
import games.descent2e.components.*;

import java.util.*;

import static games.descent2e.actions.Triggers.*;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.*;
import static games.descent2e.actions.attack.MeleeAttack.Interrupters.*;

public class MeleeAttack extends DescentAction implements IExtendedSequence {

    enum Interrupters {
        ATTACKER, DEFENDER, OTHERS, ALL
    }

    // The two argument constructor for AttackPhase specifies
    // which trigger is relevant (the first), and which players can use actions at this point (the second)
    public enum AttackPhase {
        NOT_STARTED,
        PRE_ATTACK_ROLL(START_ATTACK, DEFENDER),
        POST_ATTACK_ROLL(ROLL_OWN_DICE, ATTACKER),
        SURGE_DECISIONS(SURGE_DECISION, ATTACKER),
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
    String attackerName;
    final int defendingFigure;
    int defendingPlayer;
    String defenderName;
    AttackPhase phase = NOT_STARTED;
    int interruptPlayer;
    int surgesToSpend;
    int extraRange, pierce, extraDamage;
    boolean isStunning; // TODO: This doesn't actually stun the target (making them lose their next go) yet

    Set<Surge> surgesUsed = new HashSet<>();

    public MeleeAttack(int attackingFigure, int defendingFigure) {
        super(ACTION_POINT_SPEND);
        this.attackingFigure = attackingFigure;
        this.defendingFigure = defendingFigure;
    }

    @Override
    public boolean execute(DescentGameState state) {
        state.setActionInProgress(this);
        attackingPlayer = state.getComponentById(attackingFigure).getOwnerId();
        defendingPlayer = state.getComponentById(defendingFigure).getOwnerId();

        phase = PRE_ATTACK_ROLL;
        interruptPlayer = attackingPlayer;
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        state.setAttackDicePool(attacker.getAttackDice());
        state.setDefenceDicePool(defender.getDefenceDice());

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
            if (playerHasInterruptOption(state)) {
                foundInterrupt = true;
         //       System.out.println("Interrupt for player " + interruptPlayer);
                // we need to get a decision from this player
            } else {
                interruptPlayer = (interruptPlayer + 1) % state.getNPlayers();
                if (phase.interrupt == null || interruptPlayer == attackingPlayer) {
                    // we have completed the loop, and start again with the attacking player
                    executePhase(state);
                    interruptPlayer = attackingPlayer;
                }
            }
        } while (!foundInterrupt && phase != ALL_DONE);
    }

    private boolean playerHasInterruptOption(DescentGameState state) {
        if (phase.interrupt == null || phase.interrupters == null) return false;
        if (phase == SURGE_DECISIONS && surgesToSpend == 0) return false;
        // first we see if the interruptPlayer is one who may interrupt
        switch (phase.interrupters) {
            case ATTACKER:
                if (interruptPlayer != attackingPlayer)
                    return false;
                break;
            case DEFENDER:
                if (interruptPlayer != defendingPlayer)
                    return false;
                break;
            case OTHERS:
                if (interruptPlayer == attackingPlayer)
                    return false;
                break;
            case ALL:
                // always fine
        }
        // second we see if they can interrupt (i.e. have a relevant card/ability)
        return !_computeAvailableActions(state).isEmpty();
    }

    private void executePhase(DescentGameState state) {
      //  System.out.println("Executing phase " + phase);
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                throw new AssertionError("Should never be executed");
            case PRE_ATTACK_ROLL:
                // roll dice
                damageRoll(state);
                phase = POST_ATTACK_ROLL;
                break;
            case POST_ATTACK_ROLL:
                // Any rerolls are executed via interrupts
                // once done we see how many surges we have to spend
                surgesToSpend = state.getAttackDicePool().getSurge();
                phase = surgesToSpend > 0 ? SURGE_DECISIONS : PRE_DEFENCE_ROLL;
                break;
            case SURGE_DECISIONS:
                // any surge decisions are executed via interrupts
                surgesUsed.clear();
                phase = PRE_DEFENCE_ROLL;
                break;
            case PRE_DEFENCE_ROLL:
                if (attackMissed(state)) // no damage done, so can skip the defence roll
                    phase = ALL_DONE;
                else {
                    defenceRoll(state);
                    phase = POST_DEFENCE_ROLL;
                }
                break;
            case POST_DEFENCE_ROLL:
                phase = POST_DAMAGE;
                break;
            case POST_DAMAGE:
                applyDamage(state);
                phase = ALL_DONE;
                break;
        }
        // and reset interrupts
    }

    protected void defenceRoll(DescentGameState state) {
        state.getDefenceDicePool().roll(state.getRandom());
    }

    protected void damageRoll(DescentGameState state) {
        state.getAttackDicePool().roll(state.getRandom());
    }

    protected void applyDamage(DescentGameState state) {
        int damage = state.getAttackDicePool().getDamage() + extraDamage;
        int defence = state.getDefenceDicePool().getShields() - pierce;
        if (defence < 0) defence = 0;
        damage = Math.max(damage - defence, 0);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        int startingHealth = defender.getAttribute(Figure.Attribute.Health).getValue();
        if (startingHealth - damage <= 0) {
            // Death
            if (defender instanceof Hero) {
                ((Hero)defender).setDefeated(true);
                // Remove from map
                Move.remove(state, defender);
                // Overlord may draw a card TODO
            } else {
                // A monster
                Monster m = (Monster)defender;

                // Remove from board
                Move.remove(state, m);

                // Remove from state lists
                for (List<Monster> monsterGroup: state.getMonsters()) {
                    monsterGroup.remove(m);
                }
            }
        } else {
            defender.setAttribute(Figure.Attribute.Health, Math.max(startingHealth - damage, 0));
        }

        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        attacker.getNActionsExecuted().increment();
    }

    public boolean attackMissed(DescentGameState state) {
        return state.getAttackDicePool().hasRolled() && (
                state.getAttackDicePool().getRange() < 0 || state.getAttackDicePool().getDamage() == 0);
    }

    @Override
    public MeleeAttack copy() {
        MeleeAttack retValue = new MeleeAttack(attackingFigure, defendingFigure);
        retValue.attackingPlayer = attackingPlayer;
        retValue.defendingPlayer = defendingPlayer;
        retValue.phase = phase;
        retValue.interruptPlayer = interruptPlayer;
        retValue.surgesToSpend = surgesToSpend;
        retValue.extraRange = extraRange;
        retValue.extraDamage = extraDamage;
        retValue.surgesUsed = new HashSet<>(surgesUsed);
        retValue.pierce = pierce;
        retValue.isStunning = isStunning;
        return retValue;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return true;  // TODO
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MeleeAttack) {
            MeleeAttack other = (MeleeAttack) obj;
            return other.attackingFigure == attackingFigure &&
                    other.surgesToSpend == surgesToSpend && other.extraDamage == extraRange &&
                    other.isStunning == isStunning && other.extraRange == extraRange && other.pierce == pierce &&
                    other.attackingPlayer == attackingPlayer && other.defendingFigure == defendingFigure &&
                    other.surgesUsed.equals(surgesUsed) &&
                    other.defendingPlayer == defendingPlayer && other.phase == phase && other.interruptPlayer == interruptPlayer;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackingFigure, attackingPlayer, defendingFigure,
                pierce, extraRange, isStunning, extraDamage, surgesUsed,
                defendingPlayer, phase.ordinal(), interruptPlayer, surgesToSpend);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();
        defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        attackerName = attackerName.replace("Hero: ", "");
        defenderName = defenderName.replace("Hero: ", "");
        return String.format("Melee Attack by " + attackerName + " on " + defenderName);
        //return toString();
        // TODO: Extend this to pull in details of card and figures involved
    }

    @Override
    public String toString() {
        return String.format("Melee Attack by %d on %d", attackingFigure, defendingFigure);
    }

    public void registerSurge(Surge surge) {
        if (surgesUsed.contains(surge))
            throw new IllegalArgumentException(surge + " has already been used");
        surgesUsed.add(surge);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState state = (DescentGameState) gs;
        List<AbstractAction> retValue = state.getInterruptActionsFor(interruptPlayer, phase.interrupt);
        // now we filter this for any that have been used
        if (phase == SURGE_DECISIONS) {
            retValue.removeIf(a -> {
               if (a instanceof SurgeAttackAction) {
                   SurgeAttackAction surge = (SurgeAttackAction)a;
                   return surgesUsed.contains(surge.surge);
               }
               return false;
            });
            if (!retValue.isEmpty())
                retValue.add(new EndSurgePhase());
        }
        return retValue;
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

    public void addRange(int rangeBonus) {
        extraRange += rangeBonus;
    }
    public void addPierce(int pierceBonus) {
        pierce += pierceBonus;
    }
    public void setStunning(boolean stun) {
        isStunning = stun;
    }
    public void addDamage(int damageBonus) {
        extraDamage += damageBonus;
    }

}
