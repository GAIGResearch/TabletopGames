package games.conquest.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.conquest.CQGameState;
import games.conquest.components.Troop;
import utilities.Vector2D;

/**
 * <p>Actions are unit things players can do in the game (e.g. play a card, move a pawn, roll dice, attack etc.).</p>
 * <p>Actions in the game can (and should, if applicable) extend one of the other existing actions, in package {@link core.actions}.
 * Or, a game may simply reuse one of the existing core actions.</p>
 * <p>Actions may have parameters, so as not to duplicate actions for the same type of functionality,
 * e.g. playing card of different types (see {@link games.sushigo.actions.ChooseCard} action from SushiGo as an example).
 * Include these parameters in the class constructor.</p>
 * <p>They need to extend at a minimum the {@link AbstractAction} super class and implement the {@link AbstractAction#execute(AbstractGameState)} method.
 * This is where the main functionality of the action should be inserted, which modifies the given game state appropriately (e.g. if the action is to play a card,
 * then the card will be moved from the player's hand to the discard pile, and the card's effect will be applied).</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>MUST NOT</b> keep references to game components. Instead, store the {@link Component#getComponentID()}
 * in variables for any components that must be referenced in the action. Then, in the execute() function,
 * use the {@link AbstractGameState#getComponentById(int)} function to retrieve the actual reference to the component,
 * given your componentID.</p>
 */
public class SelectTroop extends CQAction {
    public SelectTroop(int pid, Vector2D target) {
        super(pid, target);
    }

    @Override
    public boolean canExecute(CQGameState cqgs) {
        if (cqgs.getSelectedTroop() != null) return false; // a troop was already selected.
        Troop troop = cqgs.getTroopByLocation(highlight != null ? highlight : cqgs.highlight);
        // true if a troop is selected, it's your troop, and it's not chastised.
        return troop != null && troop.getOwnerId() == playerId && troop.getMovement() != 0;
    }

    /**
     * Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        CQGameState cqgs = (CQGameState) gs;
        if (!canExecute(cqgs)) return false;
        if (highlight == null) highlight = cqgs.highlight;
        Troop troop = cqgs.getTroopByLocation(highlight);
        cqgs.setSelectedTroop(troop.getComponentID());
        cqgs.setGamePhase(CQGameState.CQGamePhase.MovementPhase);
        return gs.setActionInProgress(this);
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public SelectTroop copy() {
        return new SelectTroop(playerId, highlight);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SelectTroop)) return false;
        SelectTroop stObj = (SelectTroop) obj;
        return stObj.playerId == playerId && stObj.highlight.equals(highlight);
    }

    @Override
    String _toString() {
        return "Select Troop";
    }
}
