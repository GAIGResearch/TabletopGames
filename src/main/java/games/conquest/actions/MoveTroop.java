package games.conquest.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.conquest.CQGameState;
import games.conquest.components.Cell;
import games.conquest.components.Troop;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>The extended actions framework supports 2 use-cases: <ol>
 *     <li>A sequence of decisions required to complete an action (e.g. play a card in a game area - which card? - which area?).
 *     This avoids very large action spaces in favour of more decisions throughout the game (alternative: all unit actions
 *     with parameters supplied at initialization, all combinations of parameters computed beforehand).</li>
 *     <li>A sequence of actions triggered by specific decisions (e.g. play a card which forces another player to discard a card - other player: which card to discard?)</li>
 * </ol></p>
 * <p>Extended actions should implement the {@link IExtendedSequence} interface and appropriate methods, as detailed below.</p>
 * <p>They should also extend the {@link AbstractAction} class, or any other core actions. As such, all guidelines in {@link EndTurn} apply here as well.</p>
 */
public class MoveTroop extends AbstractAction implements IExtendedSequence {

    // The extended sequence usually keeps record of the player who played this action, to be able to inform the game whose turn it is to make decisions
    public final int playerId;
    private boolean executed;

    public MoveTroop(int pid) {
        playerId = pid;
    }

    /**
     * Forward Model delegates to this from {@link core.StandardForwardModel#computeAvailableActions(AbstractGameState)}
     * if this Extended Sequence is currently active.
     *
     * @param gs The current game state
     * @return the list of possible actions for the {@link AbstractGameState#getCurrentPlayer()}.
     * These may be instances of this same class, with more choices between different values for a not-yet filled in parameter.
     */
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        CQGameState cqgs = (CQGameState) gs;
        List<AbstractAction> actions = new ArrayList<>();
        actions.add(new AttackTroop(playerId));
        actions.add(new ApplyCommand(playerId));
        actions.add(new EndTurn());
        return actions;
    }

    /**
     * TurnOrder delegates to this from {@link core.turnorders.TurnOrder#getCurrentPlayer(AbstractGameState)}
     * if this Extended Sequence is currently active.
     *
     * @param state The current game state
     * @return The player ID whose move it is.
     */
    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    /**
     * <p>Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.</p>
     * <p>In extended sequences, this function makes a call to the
     * {@link AbstractGameState#setActionInProgress(IExtendedSequence)} method with the argument <code>`this`</code>
     * to indicate that this action has multiple steps and is now in progress. This call could be wrapped in an <code>`if`</code>
     * statement if sometimes the action simply executes an effect in one step, or all parameters have values associated.</p>
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        CQGameState cqgs = (CQGameState) gs;
        Troop troop = cqgs.getSelectedTroop();
        if (troop == null) return false;
        Cell target = cqgs.getCell(cqgs.highlight);
        if (target == null || cqgs.getTroopByLocation(target) != null) return false;
        int distance = cqgs.getDistance(troop.getLocation(), target.position);
        if (distance > troop.getMovement()) return false; // too far to be allowed.
        troop.move(target, cqgs); // actually move troop.
        if (troop.getMovement() == 0) {
            // If selected troop has exhausted movement, change to CombatPhase
            cqgs.setGamePhase(CQGameState.CQGamePhase.CombatPhase);
        }
        return gs.setActionInProgress(this);
    }

    /**
     * <p>This is called by ForwardModel whenever an action is about to be taken. It enables the IExtendedSequence
     * to maintain local state in whichever way is most suitable.</p>
     *
     * <p>After this call, the state of IExtendedSequence should be correct ahead of the next decision to be made.
     * In some cases, there is no need to implement anything in this method - if for example you can tell if all
     * actions are complete from the state directly, then that can be implemented purely in {@link #executionComplete(AbstractGameState)}</p>
     *
     * @param state The current game state
     * @param action The action about to be taken (so the game state has not yet been updated with it)
     */
    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
    }

    /**
     * @param state The current game state
     * @return True if this extended sequence has now completed and there is nothing left to do.
     */
    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public MoveTroop copy() {
        // TODO: copy non-final variables appropriately
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: compare all other variables in the class
        return obj instanceof EndTurn;
    }

    @Override
    public int hashCode() {
        // TODO: return the hash of all other variables in the class
        return 0;
    }

    @Override
    public String toString() {
        return "Move Troop";
    }

    /**
     * @param gameState - game state provided for context.
     * @return A more descriptive alternative to the toString action, after access to the game state to e.g.
     * retrieve components for which only the ID is stored on the action object, and include the name of those components.
     * Optional.
     */
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
