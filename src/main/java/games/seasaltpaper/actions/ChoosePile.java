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
        // TODO also make the entire pile visible to the playerId
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return this==obj;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, pileId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "CHOOSE " + gameState.getComponentById(pileId).getComponentName();
    }
}
