package games.conquest.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.conquest.CQGameState;
import games.conquest.components.Cell;
import games.conquest.components.Troop;
import utilities.Vector2D;

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
public class MoveTroop extends CQAction {
    public MoveTroop(int pid, Vector2D target) {
        super(pid, target);
    }

    @Override
    public boolean canExecute(CQGameState cqgs) {
        if (cqgs.getGamePhase() != CQGameState.CQGamePhase.MovementPhase) return false;
        Troop troop = cqgs.getSelectedTroop();
        if (troop == null) return false;
        Cell target = cqgs.getCell(highlight != null ? highlight : cqgs.highlight);
        if (target == null || !target.isWalkable(cqgs)) return false;
        int distance = cqgs.getDistance(cqgs.getCell(troop.getLocation()), target);
        return distance <= troop.getMovement(); // can only execute if within movement distance.
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
        if (!canExecute(cqgs)) return false;
        Troop troop = cqgs.getSelectedTroop();
        Cell target = cqgs.getCell(this.highlight != null ? this.highlight : cqgs.highlight);
        troop.move(target, cqgs); // actually move troop.
        if (troop.getMovement() == 0) {
            // If selected troop has exhausted movement, change to CombatPhase
            cqgs.setGamePhase(CQGameState.CQGamePhase.CombatPhase);
        }
        return gs.setActionInProgress(this);
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public MoveTroop copy() {
        return new MoveTroop(playerId, highlight);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MoveTroop)) return false;
        MoveTroop mtObj = (MoveTroop) obj;
        return mtObj.playerId == playerId && mtObj.highlight.equals(highlight);
    }

    @Override
    public String _toString() {
        return "Move Troop";
    }
}
