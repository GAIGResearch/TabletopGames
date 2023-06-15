package core.interfaces;

import core.AbstractGameState;
import utilities.ActionTreeNode;

public interface IOrderedActionSpace {

    // initialise the structure of the action tree
    public ActionTreeNode initActionTree(AbstractGameState gameState);

    // function to generate the action tree
    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState);

}
