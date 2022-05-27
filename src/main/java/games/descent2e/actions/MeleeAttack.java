package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.components.Figure;
import utilities.Vector2D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static games.descent2e.actions.MeleeAttack.AttackPhase.NOT_STARTED;
import static games.descent2e.actions.MeleeAttack.AttackPhase.PRE_ATTACK_ROLL;

public class MeleeAttack extends AbstractAction implements IExtendedSequence {

    public enum AttackPhase {
        NOT_STARTED, PRE_ATTACK_ROLL, POST_ATTACK_ROLL, SURGE_DECISIONS, PRE_DEFENCE_ROLL,
        POST_DEFENCE_ROLL, POST_DAMAGE
    }
    final Vector2D target;
    final int weaponCardId;
    final int attackingFigure;
    final int attackingPlayer;
    AttackPhase phase = NOT_STARTED;
    int interruptPlayer;

    public MeleeAttack(Vector2D target, int weaponCardId, int attackingFigure, int attackingPlayer) {
        this.target = target;
        this.weaponCardId = weaponCardId;
        this.attackingFigure = attackingFigure;
        this.attackingPlayer = attackingPlayer;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        DescentGameState state = (DescentGameState) gs;

        phase = PRE_ATTACK_ROLL;
        interruptPlayer = (attackingPlayer + 1) % gs.getNPlayers();
        // TODO : We really want to whizz through here to find the
        // possible interrupts, which may mean we do not need to go
        // back through the overhead of the main game loop at all.
        interruptPlayer = nextPlayerToInterrupt(state, attackingPlayer, )
        Component weaponCard = state.getComponentById(weaponCardId);
        Figure figure = (Figure) state.getComponentById(attackingFigure);

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

    @Override
    public MeleeAttack copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MeleeAttack;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "MeleeAttack";
    }


    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        switch (phase) {
            case NOT_STARTED:
                throw new AssertionError("Should not be reachable");
            case PRE_ATTACK_ROLL:
                break;
            case POST_ATTACK_ROLL:
                break;
            case SURGE_DECISIONS:
                break;
            case PRE_DEFENCE_ROLL:
                break;
            case POST_DEFENCE_ROLL:
                break;
            case POST_DAMAGE:
                break;
        }
        throw new AssertionError("Not implemented");
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        switch (phase) {
            case NOT_STARTED:
                throw new AssertionError("Should not be reachable");
            case PRE_ATTACK_ROLL:
                return interruptPlayer;
            case POST_ATTACK_ROLL:
                break;
            case SURGE_DECISIONS:
                break;
            case PRE_DEFENCE_ROLL:
                break;
            case POST_DEFENCE_ROLL:
                break;
            case POST_DAMAGE:
                break;
        }
        throw new AssertionError("Not implemented");
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return false;
    }

}
