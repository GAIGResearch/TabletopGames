package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.Objects;

public class ChoosePile extends AbstractAction {

    public final int playerId;
    public final int pileId;

    ChoosePile(int pileId, int playerId) {
        this.pileId = pileId;
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public ChoosePile copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChoosePile that = (ChoosePile) o;
        return playerId == that.playerId && pileId == that.pileId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, pileId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Choose " + gameState.getComponentById(pileId).getComponentName();
    }

    @Override
    public String toString() {
        return "Choose pile " + pileId;
    }
}
