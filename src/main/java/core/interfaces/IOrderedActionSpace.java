package core.interfaces;

import core.AbstractGameState;
import utilities.ActionTreeNode;

import java.util.List;
import java.util.stream.IntStream;

public interface IOrderedActionSpace {

    //Returns the action space (how many actions) as int
    public int getActionSpace();

    //Returns the fixed action space (all actions in an array)
    public int[] getFixedActionSpace();

    //Returns an action mask of valid and invalid actions
    public int[] getActionMask(AbstractGameState gameState);

    // function to generate the action tree
    public ActionTreeNode generateActionTree(AbstractGameState gameState);

    //Given an ID, plays an action
    void nextPython(AbstractGameState state, int actionID);
}
