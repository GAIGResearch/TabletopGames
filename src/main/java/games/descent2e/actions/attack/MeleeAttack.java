package games.descent2e.actions.attack;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.abilities.HeroAbilities;
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
        PRE_DEFENCE_ROLL(ROLL_OTHER_DICE, DEFENDER),
        POST_DEFENCE_ROLL(ROLL_OWN_DICE, DEFENDER),
        PRE_DAMAGE(TAKE_DAMAGE, DEFENDER),
        POST_DAMAGE(ROLL_OWN_DICE, DEFENDER),
        NEXT_TARGET,        // This is only used in MultiAttacks, where we repeat the attack on the next target
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

    protected final int attackingFigure;
    int attackingPlayer;
    protected String attackerName;
    protected int defendingFigure;
    int defendingPlayer;
    protected String defenderName;
    AttackPhase phase = NOT_STARTED;
    int interruptPlayer;
    int surgesToSpend;
    int extraRange, pierce, extraDamage, mending;
    boolean isDiseasing;
    boolean isImmobilizing;
    boolean isPoisoning;
    boolean isStunning;

    int damage;
    boolean skip = false;

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
        DicePool attackPool = attacker.getAttackDice();
        DicePool defencePool = defender.getDefenceDice();

        state.setAttackDicePool(attackPool);
        state.setDefenceDicePool(defencePool);

        movePhaseForward(state);

        attacker.getNActionsExecuted().increment();
        attacker.setHasAttacked(true);

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

    void movePhaseForward(DescentGameState state) {
        // The goal here is to work out which player may have an interrupt for the phase we are in
        // If none do, then we can move forward to the next phase directly.
        // If one (or more) does, then we stop, and go back to the main game loop for this
        // decision to be made
        boolean foundInterrupt = false;
        do {
            if (playerHasInterruptOption(state)) {
                foundInterrupt = true;
                // System.out.println("Melee Attack Interrupt: " + phase + ", Interrupter:" + phase.interrupters + ", Interrupt:" + phase.interrupt + ", Player: " + interruptPlayer);
                // we need to get a decision from this player
            } else {
                skip = false;
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

    void executePhase(DescentGameState state) {
        // System.out.println("Executing phase " + phase);
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                // TODO Fix this temporary solution: it should not keep looping back to ALL_DONE, put the error back in
                break;
                //throw new AssertionError("Should never be executed");
            case PRE_ATTACK_ROLL:
                // roll dice
                damageRoll(state);
                state.getActingFigure().setRerolled(false);
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
                {
                    Figure attacker = (Figure) state.getComponentById(attackingFigure);
                    //System.out.println("Attack missed!");
                    phase = ALL_DONE;
                }
                else {
                    defenceRoll(state);
                    phase = POST_DEFENCE_ROLL;
                }
                break;
            case POST_DEFENCE_ROLL:
                calculateDamage(state);
                phase = PRE_DAMAGE;
                break;
            case PRE_DAMAGE:
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

    protected void calculateDamage(DescentGameState state) {
        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);

        damage = state.getAttackDicePool().getDamage() + extraDamage;
        int defence = state.getDefenceDicePool().getShields() - pierce;

        // Leoric of the Book's Hero Ability
        // If a Monster is within 3 spaces of Leoric, its attacks deal -1 Heart (to a minimum of 1)
        if (attacker instanceof Monster)
        {
            damage = HeroAbilities.leoric(state, attacker, damage);
        }

        if (defence < 0) defence = 0;
        damage = Math.max(damage - defence, 0);

        attacker.setCurrentAttack(this);
        defender.setCurrentAttack(this);
    }
    protected void applyDamage(DescentGameState state) {

        Figure attacker = (Figure) state.getComponentById(attackingFigure);
        Figure defender = (Figure) state.getComponentById(defendingFigure);
        defenderName = defender.getComponentName();

        int startingHealth = defender.getAttribute(Figure.Attribute.Health).getValue();
        if (startingHealth - damage <= 0) {
            // Death
            if (defender instanceof Hero) {
                ((Hero)defender).setDefeated(state,true);
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
            // Conditions only apply if damage is done
            if (damage > 0)
                applyConditions(defender);
            defender.setAttribute(Figure.Attribute.Health, Math.max(startingHealth - damage, 0));
        }

        if(mending > 0)
        {
            attacker.incrementAttribute(Figure.Attribute.Health, mending);
        }
    }

    public boolean attackMissed(DescentGameState state) {
        return state.getAttackDicePool().hasRolled() && (
                state.getAttackDicePool().getRange() < 0 || state.getAttackDicePool().getDamage() == 0);
    }

    public void applyConditions(Figure defender)
    {
        // Applies the Diseased condition
        if (isDiseasing) {
            defender.addCondition(DescentTypes.DescentCondition.Disease);
        }
        // Applies the Immobilized condition
        if (isImmobilizing) {
            defender.addCondition(DescentTypes.DescentCondition.Immobilize);
        }
        // Applies the Poisoned condition
        if (isPoisoning) {
            defender.addCondition(DescentTypes.DescentCondition.Poison);
        }
        // Applies the Stunned condition
        if (isStunning) {
            defender.addCondition(DescentTypes.DescentCondition.Stun);
        }
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
        retValue.mending = mending;
        retValue.surgesUsed = new HashSet<>(surgesUsed);
        retValue.pierce = pierce;
        retValue.isDiseasing = isDiseasing;
        retValue.isImmobilizing = isImmobilizing;
        retValue.isPoisoning = isPoisoning;
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
                    other.surgesToSpend == surgesToSpend && other.extraDamage == extraDamage &&
                    other.isDiseasing == isDiseasing && other.isImmobilizing == isImmobilizing &&
                    other.isPoisoning == isPoisoning && other.isStunning == isStunning &&
                    other.extraRange == extraRange && other.pierce == pierce && other.mending == mending &&
                    other.attackingPlayer == attackingPlayer && other.defendingFigure == defendingFigure &&
                    other.surgesUsed.equals(surgesUsed) &&
                    other.defendingPlayer == defendingPlayer && other.phase == phase && other.interruptPlayer == interruptPlayer;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackingFigure, attackingPlayer, defendingFigure, pierce,
                extraRange, isDiseasing, isImmobilizing, isPoisoning, isStunning, extraDamage, mending,
                surgesUsed, defendingPlayer, phase.ordinal(), interruptPlayer, surgesToSpend);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        attackerName = gameState.getComponentById(attackingFigure).getComponentName();

        // Sometimes the game will remove the dead enemy off the board before
        // it can state in the Action History the attack that killed them
        if (gameState.getComponentById(defendingFigure) != null) {
            defenderName = gameState.getComponentById(defendingFigure).getComponentName();
        }
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
        if (phase == POST_ATTACK_ROLL) {
            if (!retValue.isEmpty())
                retValue.add(new EndRerollPhase());
        }
        if (phase == PRE_DAMAGE) {
            if (!retValue.isEmpty())
                retValue.add(new EndCurrentPhase());
        }
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return interruptPlayer;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
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
    public void addMending(int mendBonus) {
        mending += mendBonus;
    }
    public void setDiseasing(boolean disease) {
        isDiseasing = disease;
    }
    public void setImmobilizing(boolean immobilize) {
        isImmobilizing = immobilize;
    }
    public void setPoisoning(boolean poison) {
        isPoisoning = poison;
    }
    public void setStunning(boolean stun) {
        isStunning = stun;
    }
    public void addDamage(int damageBonus) {
        extraDamage += damageBonus;
    }
    public void reduceDamage (int damageReduction) {
        damage = Math.max(0, damage - damageReduction);
    }
    public int getDamage() {
        return damage;
    }

    public int getDefendingFigure()
    {
        return defendingFigure;
    }

}
