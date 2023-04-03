package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.catan.components.CatanCard;

import java.util.List;
import java.util.Objects;

public class PlayDevCard extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public final CatanCard.CardType type;

    boolean executed;

    public PlayDevCard(int playerID, CatanCard.CardType type) {
        this.playerID = playerID;
        this.type = type;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return gs.setActionInProgress(this);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return null;  // TODO
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        // TODO
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public PlayDevCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayDevCard)) return false;
        PlayDevCard that = (PlayDevCard) o;
        return playerID == that.playerID && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, type);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return playerID + " plays dev card " + type;
    }
}
