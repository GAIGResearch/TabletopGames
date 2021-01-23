package games.dicemonastery;

import core.actions.AbstractAction;

import java.util.*;

public interface IExtendedSequence {

    List<AbstractAction> followOnActions(DiceMonasteryGameState state);

    int getCurrentPlayer(DiceMonasteryGameState state);

    void registerActionTaken(DiceMonasteryGameState state, AbstractAction action);

    boolean executionComplete(DiceMonasteryGameState state);

    IExtendedSequence copy();
}
