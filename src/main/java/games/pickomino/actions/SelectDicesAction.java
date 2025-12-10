package games.pickomino.actions;

import java.util.Arrays;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.pickomino.PickominoGameState;
import games.pickomino.PickominoTile;

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
public class SelectDicesAction extends AbstractAction {

    // Value of the dice to select (1-6)
    private final int diceValue;
    private final boolean stop;

    public SelectDicesAction(int diceValue, boolean stop) {
        super();
        this.diceValue = diceValue;
        this.stop = stop;
    }

    /**
     * Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the {@link AbstractGameState#getComponentById(int)} method.
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        if (diceValue < 1 || diceValue > 6) return false; // dice value must be between 1 and 6
        PickominoGameState pgs = (PickominoGameState) gs;

        // Check that the dice value is not already assigned
        // and that there are dices to assign
        if (pgs.assignedDices[diceValue - 1] > 0 || pgs.currentRoll[diceValue - 1] == 0) return false;
        // add the dice value to the assigned dices
        else {
            int diceCount = pgs.currentRoll[diceValue - 1];
            pgs.assignedDices[diceValue - 1] = diceCount;
            pgs.remainingDices -= diceCount;
            Arrays.fill(pgs.currentRoll, 0);
            int valueIncrement = (diceValue == 6 ? 5 : diceValue) * diceCount;
            pgs.totalDicesValue += valueIncrement;
            if (pgs.getCoreGameParameters().verbose) {
                String diceName = (diceValue == 6) ? "worm" : String.valueOf(diceValue);
                System.out.println("p" + pgs.getCurrentPlayer() + " selects " + diceCount + "x" + diceName + 
                    " (value: " + valueIncrement + ", total: " + pgs.totalDicesValue + ")");
            }
        }

        if (stop) {
            // select the tile to pick up

            // If another player has exactly the tile with the value of the dices then steal the tile
            for (int i = 0; i < pgs.getNPlayers(); i++) {
                if (i != pgs.getCurrentPlayer()){
                    if(pgs.playerTiles.get(i).getSize() > 0
                       && pgs.playerTiles.get(i).peek().getValue() == pgs.totalDicesValue){
                        PickominoTile stolenTile = pgs.playerTiles.get(i).draw();
                        pgs.playerTiles.get(pgs.getCurrentPlayer()).add(stolenTile);
                        if (pgs.getCoreGameParameters().verbose) {
                            System.out.println("p" + pgs.getCurrentPlayer() + " steals tile " + 
                                stolenTile.getValue() + " (score: " + stolenTile.getScore() + ") from p" + i);
                        }
                        return true;
                    }
                }
            }

            // Otherwise, pick the tile from the remaining tiles with the highest value below the total dices value
            int highestValueBelowTotalDicesValue = 0;
            int highestValueBelowTotalDicesValueIndex = -1;
            for (int i = 0; i < pgs.remainingTiles.getSize(); i++) {
                PickominoTile tile = pgs.remainingTiles.peek(i);
                if (tile.getValue() < pgs.totalDicesValue && tile.getValue() > highestValueBelowTotalDicesValue) {
                    highestValueBelowTotalDicesValue = tile.getValue();
                    highestValueBelowTotalDicesValueIndex = i;
                }
            }

            if(highestValueBelowTotalDicesValueIndex != -1) {
                PickominoTile tile = pgs.remainingTiles.pick(highestValueBelowTotalDicesValueIndex);
                pgs.playerTiles.get(pgs.getCurrentPlayer()).add(tile);
                if (pgs.getCoreGameParameters().verbose) {
                    System.out.println("p" + pgs.getCurrentPlayer() + " picks up tile " + 
                        tile.getValue() + " (score: " + tile.getScore() + ")");
                }
                return true;
            } else {
                return false; // This should not happen, but it is better to be safe.
            }
        }
        return true;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. PickominoAction) and NOT the super class AbstractAction.
     * <p>If all variables in this class are final or effectively final (which they should be),
     * then you can just return <code>`this`</code>.</p>
     */
    @Override
    public SelectDicesAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SelectDicesAction sd) {
            return sd.diceValue == diceValue && sd.stop == stop;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 67 * diceValue + (stop ? 1 : 0);
    }

    @Override
    public String toString() {
        return "Select Dices: " + diceValue;
    }

    /**
     * @param gameState - game state provided for context.
     * @return A more descriptive alternative to the toString action, after access to the game state to e.g.
     * retrieve components for which only the ID is stored on the action object, and include the name of those components.
     * Optional.
     */
    @Override
    public String getString(AbstractGameState gameState) {
        PickominoGameState pgs = (PickominoGameState) gameState;
        return "p" + pgs.getCurrentPlayer() + " selects dice " + diceValue;
    }

    public boolean isStop() {
        return this.stop;
    }

}

