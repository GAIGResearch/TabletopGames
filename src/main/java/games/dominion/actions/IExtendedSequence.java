package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.DominionGameState;

import java.util.*;

public interface IExtendedSequence {

    List<AbstractAction> followOnActions(DominionGameState state);

    int getCurrentPlayer(DominionGameState state);

    void registerActionTaken(DominionGameState state, AbstractAction action);

    boolean executionComplete();

    IExtendedSequence copy();
}
