package core.interfaces;

import core.AbstractGameState;

import java.util.stream.IntStream;

public interface IOrderedActionSpace {

    //Returns the action space (how many actions) as int
    public int getActionSpace();

    //Returns the fixed action space (all actions in an array)
    public int[] getFixedActionSpace();

    //Returns an action mask of valid and invalid actions
    public int[] getActionMask(AbstractGameState gameState);

    //Given an ID, plays an action
    void nextPython(AbstractGameState state, int actionID);
}
