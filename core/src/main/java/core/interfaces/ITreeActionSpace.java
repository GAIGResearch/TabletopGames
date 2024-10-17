package core.interfaces;

import core.AbstractGameState;
import utilities.ActionTreeNode;

public interface ITreeActionSpace {
    /* Instead of storing the available actions as a list this interface overwrites that by constructing a tree structure.
    * The user may define arbitrary levels depending on the game's requirements */

    // initialise the structure of the action tree, this remains fixed for the whole game
    public ActionTreeNode initActionTree(AbstractGameState gameState);

    // function to update the tree with the valid actions at the current state
    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState);

}
