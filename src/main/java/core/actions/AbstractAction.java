package core.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;

import java.util.Set;

public abstract class AbstractAction implements IPrintable {

    /**
     * Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the AbstractGameState.getComponentById(int id) method.
     *
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    public abstract boolean execute(AbstractGameState gs);

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    public abstract AbstractAction copy();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String getString(AbstractGameState gameState);

    /**
     * Returns the string representation of this action from the perspective of the given player.
     * @param gs - game state to be used to generate the string.
     *
     *  May optionally be implemented if Actions are not fully visible
     *  The only impact this has is in the GUI, to avoid this giving too much information to the player.
     *
     * @param perspectivePlayer - player to whom the action should be represented.
     * @return - string representation of this action.
     */
    public String getString(AbstractGameState gs, int perspectivePlayer) {
        return getString(gs);
    }

    /**
     * The GUI formally supports multiple players viewing a game. This in practice is only going to be used
     * for games with (near) perfect information. For games that actually implement hidden information in
     * a move (Resistance, Hanabi, Sushi Go, etc), we will only need the game actions to implement
     * getString(AbstractGameState, int). This is a helper method to make this downstream implementation
     * easier without trying to puzzle out what it means to have multiple players viewing a game with hidden information.
     *
     * @param gs
     * @param perspectiveSet
     * @return
     */
    public String getString(AbstractGameState gs, Set<Integer> perspectiveSet) {
        // We assume that the current player is the one who has most information about the action.
        // If they are part of the perspective set, then we use them. Otherwise, we use the first player in the set
        // on the basis that all other players have the same (limited) information.
        // Where these assumptions are not true, then override this iin the relevant Action implementation.
        Integer currentPlayer = gs.getCurrentPlayer();
        int perspective = perspectiveSet.contains(currentPlayer) ? currentPlayer : perspectiveSet.stream().findFirst().orElse(currentPlayer);
        return getString(gs, perspective);
    }

    public String getTooltip(AbstractGameState gs) {
        return "";
    }
}
