package games.dominion.actions;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;
import games.dominion.*;
import games.dominion.cards.CardType;

import java.util.Arrays;
import java.util.Objects;

public abstract class DominionAttackAction extends DominionAction implements IExtendedSequence {

    public DominionAttackAction(CardType type, int playerId) {
        super(type, playerId);
    }
    public DominionAttackAction(CardType type, int playerId, boolean dummy) {
        super(type, playerId, dummy);
    }

    int currentTarget;
    boolean[] reactionsInitiated;
    boolean[] attacksInitiated;
    boolean[] attacksComplete;

    /**
     * This must be called by the implementing sub-class at the correct point in the card logic
     *
     * @param state
     * @return
     */
    void initiateAttack(DominionGameState state) {
        // for any state changes due to the immediate effects of the card on the player.
        // then what we need to do is the cycling through each of the other players to allow them
        // to play Reaction cards, and then suffer the direct attack effects of the card.
        state.setActionInProgress(this);
        currentTarget = (player + 1) % state.getNPlayers();
        reactionsInitiated = new boolean[state.getNPlayers()];
        attacksComplete = new boolean[state.getNPlayers()];
        attacksInitiated = new boolean[state.getNPlayers()];
        reactionsInitiated[player] = true;
        attacksComplete[player] = true;
        nextPhaseOfReactionAttackCycle(state);
    }

    private void nextPhaseOfReactionAttackCycle(DominionGameState state) {
        // we cycle through each other player in turn to give them the opportunity to play Reactions
        // Then, after they have played their Reactions, we execute the actual attack code (in the sub-class)
        // if they are undefended
        int loopCount = 0;
        do {
            if (!(reactionsInitiated[currentTarget] && attacksComplete[currentTarget])) {
                // at least one of these two booleans must therefore be false
                if (!reactionsInitiated[currentTarget]) { // we are in AttackReaction phase
                    AttackReaction reaction = new AttackReaction(state, player, currentTarget);
                    reactionsInitiated[currentTarget] = true;
                    if (!reaction.executionComplete(state)) {
                        // there is some reaction to process - so put that on the stack
                        state.setActionInProgress(reaction);
                        return;
                    }
                }
                // we are in now Attack phase phase
                if (state.isDefended(currentTarget)) {
                    attacksInitiated[currentTarget] = true;
                    attacksComplete[currentTarget] = true;
                } else {
                    if (!attacksInitiated[currentTarget])
                        executeAttack(state);
                    attacksInitiated[currentTarget] = true;
                    if (isAttackComplete(currentTarget, state)) {
                        attacksComplete[currentTarget] = true;
                    } else {
                        return;
                    }
                }
            }
            currentTarget = (currentTarget + 1) % state.getNPlayers();
            loopCount++;
            if (loopCount > 100) {
                throw new AssertionError("WTF?");
            }
        } while (state.currentActionInProgress() == this && currentTarget != player);
    }

    @Override
    public boolean executionComplete(AbstractGameState gs) {
        // this will only be called if this is top of the Reaction Stack (i.e. if the last reaction has completed)
        DominionGameState state = (DominionGameState) gs;
        nextPhaseOfReactionAttackCycle(state);
        for (int i = 0; i < attacksComplete.length; i++) {
            if (!attacksComplete[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public DominionAttackAction copy() {
        DominionAttackAction retValue = this._copy();
        retValue.currentTarget = currentTarget;
        retValue.reactionsInitiated = reactionsInitiated != null ? reactionsInitiated.clone() : null;
        retValue.attacksComplete = attacksComplete != null ? attacksComplete.clone() : null;
        retValue.attacksInitiated = attacksInitiated != null ? attacksInitiated.clone() : null;
        return retValue;
    }

    /**
     * Delegates copying of the state of the subclass.
     * The returned value will then be updated with the copied state of DominionAttackAction (in copy())
     *
     * @return Instance of the sub-class with all local state copied
     */
    public abstract DominionAttackAction _copy();

    /**
     * The victim id is stored in currentTarget - so we do not include in parameterisation
     * @param state current game state
     */
    public abstract void executeAttack(DominionGameState state);

    public abstract boolean isAttackComplete(int currentTarget, DominionGameState state);

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DominionAttackAction other) {
            return super.equals(obj)
                    && other.currentTarget == currentTarget
                    && Arrays.equals(reactionsInitiated, other.reactionsInitiated)
                    && Arrays.equals(attacksInitiated, other.attacksInitiated)
                    && Arrays.equals(attacksComplete, other.attacksComplete);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int retValue = Objects.hash(currentTarget) + 31 * super.hashCode();
        return retValue + 11 * Arrays.hashCode(reactionsInitiated) + 53 * Arrays.hashCode(attacksInitiated) +
        997 * Arrays.hashCode(attacksComplete);
    }
}
