package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.toads.components.ToadCard;
import games.toads.ToadGameState;

import java.util.List;
import java.util.stream.Collectors;

public class UndoOpponentFlank  implements IExtendedSequence {

    public final int decisionPlayer;
    boolean complete = false;

    public UndoOpponentFlank(ToadGameState state) {
        this.decisionPlayer = state.getCurrentPlayer();
        ToadCard flankCard = state.getHiddenFlankCard(1 - decisionPlayer);
        if (flankCard == null) {
            throw new AssertionError("No flank card to undo");
        }
   //     state.getPlayerHand(1 - decisionPlayer).add(flankCard);
        state.unsetHiddenFlankCard(1 - decisionPlayer);

        // we have now hacked the state to undo the flank
        // the first action now is to choose one
        state.setActionInProgress(this);
    }

    // for copying
    private UndoOpponentFlank(int decisionPlayer) {
        this.decisionPlayer = decisionPlayer;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // Choose a Flank card for the opponent to play
        ToadGameState toadState = (ToadGameState) state;
        return toadState.getPlayerHand(1 - decisionPlayer).stream()
                .map(PlayFlankCard::new)
                .collect(Collectors.toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return  1 - decisionPlayer;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof PlayFlankCard) {
            complete = true;
        } else {
            throw new AssertionError("Invalid action type" + action);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public UndoOpponentFlank copy() {
        UndoOpponentFlank retValue = new UndoOpponentFlank(decisionPlayer);
        retValue.complete = complete;
        return retValue;
    }

    @Override
    public String toString() {
        return "Undo Opponent Flank";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UndoOpponentFlank other)) return false;
        return decisionPlayer == other.decisionPlayer && complete == other.complete;
    }

    @Override
    public int hashCode() {
        return 3923 * decisionPlayer + (complete ? 1 : 0) * 31;
    }
}
